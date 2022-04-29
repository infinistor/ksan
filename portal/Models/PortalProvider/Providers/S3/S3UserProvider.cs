/*
* Copyright (c) 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using PortalData;
using PortalData.Requests.Accounts;
using PortalData.Responses.Accounts;
using PortalModels;
using PortalProviderInterface;
using PortalResources;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using MTLib.AspNetCore;
using MTLib.CommonData;
using MTLib.Core;
using MTLib.EntityFramework;
using MTLib.Reflection;

namespace PortalProvider.Providers.Accounts
{
    /// <summary>S3 사용자 프로바이더 클래스</summary>
    public class S3UserProvider : BaseProvider<PortalModel>, IS3UserProvider
    {
        /// <summary>역할 매니져</summary>
        protected readonly RoleManager<NNApplicationRole> m_roleManager;
        protected readonly int ACCESS_KEY_LENGTH = 20;
        protected readonly int SECRET_KEY_LENGTH = 40;
        protected readonly  Regex regex = new Regex(@"^([0-9a-zA-Z]+)@([0-9a-zA-Z]+)(\.[0-9a-zA-Z]+){1,}$");

        /// <summary>생성자</summary>
        /// <param name="dbContext">DB 컨텍스트</param>
        /// <param name="configuration">역할 정보</param>
        /// <param name="userManager">사용자 관리자</param>
        /// <param name="systemLogProvider">시스템 로그 프로바이더</param>
        /// <param name="userActionLogProvider">사용자 동작 로그 프로바이더</param>
        /// <param name="serviceScopeFactory">서비스 팩토리</param>
        /// <param name="logger">로거</param>
        /// <param name="roleManager">역할 관리자</param>
        public S3UserProvider(
            PortalModel dbContext,
            IConfiguration configuration,
            UserManager<NNApplicationUser> userManager,
            ISystemLogProvider systemLogProvider,
            IUserActionLogProvider userActionLogProvider,
            IServiceScopeFactory serviceScopeFactory,
            ILogger<S3UserProvider> logger,
            RoleManager<NNApplicationRole> roleManager
            )
            : base(dbContext, configuration, userManager, systemLogProvider, userActionLogProvider, serviceScopeFactory, logger)
        {
            m_roleManager = roleManager;
        }

        /// <summary>S3 사용자를 추가한다.</summary>
        /// <param name="request">S3 사용자 정보</param>
        /// <returns>S3 사용자 등록 결과</returns>
        public async Task<ResponseData<ResponseAddS3User>> Add(RequestAddS3User request)
        {
            ResponseData<ResponseAddS3User> result = new ResponseData<ResponseAddS3User>();

            try
            {
                // 사용자명이 없는 경우
                if (request.Name.IsEmpty())
                    return new ResponseData<ResponseAddS3User>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON_ACCOUNT_REQUIRE_NAME);

                // 이름 중복 검사
                ResponseData responseDuplicatedName = await CheckUserNameDuplicated(request.Name);

                // 중복검사 실패시
                if (responseDuplicatedName.Result != EnumResponseResult.Success)
                    return new ResponseData<ResponseAddS3User>(EnumResponseResult.Error, responseDuplicatedName.Code, responseDuplicatedName.Message);

                //이메일 체크				
                string Email = null;

                //이메일을 설정했다면
                if (!request.Email.IsEmpty())
                {
                    // 이메일 중복 검사
                    ResponseData responseDuplicatedEmail = await CheckEmailDuplicated(request.Email);

                    // 이메일이 중복된 경우
                    if (responseDuplicatedEmail.Result != EnumResponseResult.Success)
                        return new ResponseData<ResponseAddS3User>(EnumResponseResult.Error, responseDuplicatedEmail.Code, responseDuplicatedEmail.Message);
                    Email = request.Email;
                }

                // 요청이 유효한 경우
                try
                {
                    // S3 사용자 객체를 생성한다.
                    S3User user = new S3User
                    {
                        Id = Guid.NewGuid().ToString().Replace("-", ""),
                        Name = request.Name,
                        Email = Email,
                        AccessKey = RandomText(ACCESS_KEY_LENGTH),
                        SecretKey = RandomTextLong(SECRET_KEY_LENGTH),
                    };

                    //S3 사용자 등록
                    await m_dbContext.S3Users.AddAsync(user);
                    await m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

                    result.Data.CopyValueFrom(user);
                    result.Result = EnumResponseResult.Success;
                }
                catch (Exception ex)
                {
                    NNException.Log(ex);

                    result.Code = Resource.EC_COMMON__EXCEPTION;
                    result.Message = Resource.EM_COMMON__EXCEPTION;
                }
            }
            catch (Exception ex)
            {
                NNException.Log(ex);

                result.Code = Resource.EC_COMMON__EXCEPTION;
                result.Message = Resource.EM_COMMON__EXCEPTION;
            }
            return result;
        }

        /// <summary>S3 사용자를 수정한다.</summary>
        /// <param name="id">S3 사용자 식별자</param>
        /// <param name="request">S3 사용자 정보</param>
        /// <returns>S3 사용자 수정 결과</returns>
        public async Task<ResponseData> Update(string id, RequestAddS3User request)
        {
            ResponseData result = new ResponseData();

            try
            {
                // 사용자 식별자가 유효하지 않은 경우
                if (id.IsEmpty())
                    return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON_ACCOUNT_INVALID_USERID);

                // 사용자명이 없는 경우
                if (request.Name.IsEmpty())
                    return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON_ACCOUNT_REQUIRE_NAME);

                // 사용자 계정을 가져온다.
                S3User user = await m_dbContext.S3Users.Where(i => i.Id == id).FirstOrDefaultAsync();

                // 해당 계정을 찾을 수 없는 경우
                if (user == null)
                    return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

                //이름이 변경되었을 경우 중복검사
                if (!user.Name.Equals(request.Name))
                {
                    // 이름 중복 검사
                    ResponseData responseDuplicatedName = await CheckUserNameDuplicated(request.Name);

                    // 이름이 중복된 경우
                    if (responseDuplicatedName.Result != EnumResponseResult.Success)
                        return new ResponseData(EnumResponseResult.Error, responseDuplicatedName.Code, responseDuplicatedName.Message);

                    // 사용자명 변경
                    user.Name = request.Name;
                }

                //이메일이 변경되었을 경우 중복검사
                if (!user.Email.Equals(request.Email))
                {

                    //이메일을 제거하려고 하는 경우
                    if (request.Email.IsEmpty())
                    {
                        user.Email = null;
                    }
                    else
                    {
                        // 이메일 중복 검사
                        ResponseData responseDuplicatedEmail = await CheckEmailDuplicated(request.Email);

                        // 이메일이 중복된 경우
                        if (responseDuplicatedEmail.Result != EnumResponseResult.Success)
                            return new ResponseData(EnumResponseResult.Error, responseDuplicatedEmail.Code, responseDuplicatedEmail.Message);

                        user.Email = request.Email;
                    }
                }

                //설정 적용
                if (m_dbContext.HasChanges())
                    await this.m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

                result.Result = EnumResponseResult.Success;
                result.Code = Resource.SC_COMMON__SUCCESS;
                result.Message = Resource.SM_COMMON__UPDATED;
            }
            catch (Exception ex)
            {
                NNException.Log(ex);

                result.Result = EnumResponseResult.Error;
                result.Code = Resource.EC_COMMON__EXCEPTION;
                result.Message = Resource.EM_COMMON__EXCEPTION;
            }
            return result;
        }

        /// <summary>S3 사용자를 삭제한다.</summary>
        /// <param name="id">S3 사용자 식별자</param>
        /// <returns>S3 사용자 삭제 결과</returns>
        public async Task<ResponseData> Remove(string id)
        {
            ResponseData result = new ResponseData();

            try
            {
                // 아이디가 유효하지 않은 경우
                if (id.IsEmpty())
                    return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON_ACCOUNT_INVALID_USERID);

                // 사용자 계정을 가져온다.
                S3User user = await m_dbContext.S3Users.AsNoTracking().Where(i => i.Id == id).FirstOrDefaultAsync();

                // 해당 계정을 찾을 수 없는 경우
                if (user == null)
                    return new ResponseData(EnumResponseResult.Error, Resource.EC_COMMON_ACCOUNT_NOT_FOUND, Resource.EM_COMMON_ACCOUNT_NOT_FOUND);

                //계정 삭제
                try
                {
                    m_dbContext.S3Users.Remove(user);
                    await this.m_dbContext.SaveChangesWithConcurrencyResolutionAsync();

                    result.Result = EnumResponseResult.Success;
                }
                catch (Exception ex)
                {
                    NNException.Log(ex);

                    result.Code = Resource.EC_COMMON__EXCEPTION;
                    result.Message = Resource.EM_COMMON__EXCEPTION;
                }
            }
            catch (Exception ex)
            {
                NNException.Log(ex);

                result.Code = Resource.EC_COMMON__EXCEPTION;
                result.Message = Resource.EM_COMMON__EXCEPTION;
            }
            return result;
        }

		/// <summary>전체 사용자를 가져온다.</summary>
		/// <param name="skip">건너뛸 레코드 수 (옵션, 기본 0)</param>
		/// <param name="countPerPage">페이지 당 레코드 수 (옵션, 기본 100)</param>
		/// <param name="orderFields">정렬필드목록 (Email, Name(기본값))</param>
		/// <param name="orderDirections">정렬방향목록 (asc, desc)</param>
		/// <param name="searchFields">검색필드목록 (Id, Email, Name)</param>
		/// <param name="searchKeyword">검색어 (옵션)</param>
		/// <returns>S3 사용자 목록</returns>
		public async Task<ResponseList<ResponseAddS3User>> GetUsers(int skip = 0, int countPerPage = 100,
			List<string> orderFields = null, List<string> orderDirections = null,
			List<string> searchFields = null, string searchKeyword = "")
		{
			ResponseList<ResponseAddS3User> result = new ResponseList<ResponseAddS3User>();

			try{
				
				ClearDefaultOrders();
				AddDefaultOrders("Name", "asc");

				// 정렬 필드를 초기화 한다.
				InitOrderFields(ref orderFields, ref orderDirections);

				// 검색 필드를  초기화한다.
				InitSearchFields(ref searchFields);

				result.Data = await m_dbContext.S3Users.AsNoTracking().Where(i =>
						searchFields == null || searchFields.Count == 0 || searchKeyword.IsEmpty()
						|| (searchFields.Contains("email") && i.Email.Contains(searchKeyword))
						|| (searchFields.Contains("name") && i.Name.Contains(searchKeyword)))
					.OrderByWithDirection(orderFields, orderDirections)
					.CreateListAsync<dynamic, ResponseAddS3User>(skip, countPerPage);

				result.Result = EnumResponseResult.Success;

			} catch (Exception ex)
			{
				NNException.Log(ex);

				result.Code = Resource.EC_COMMON__EXCEPTION;
				result.Message = Resource.EM_COMMON__EXCEPTION;
			}
			return result;
		}
        /************************************************************************************************************/

        /// <summary>S3 사용자 이름 중복 여부를 검사한다.</summary>
        /// <param name="userName">S3 사용자 이름</param>
        /// <returns>검사 결과 객체</returns>
        public async Task<ResponseData> CheckUserNameDuplicated(string userName)
        {
            ResponseData result = new ResponseData();
            try
            {
                // 요청이 유효하지 않은 경우
                if (userName.IsEmpty())
                {
                    result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
                    result.Message = Resource.EM_COMMON_ACCOUNT_REQUIRE_NAME;
                    result.Result = EnumResponseResult.Error;
                }
                // 요청이 유효한 경우
                else
                {
                    // 해당 사용자 이름이 존재하는 경우
                    if (await m_dbContext.S3Users.AsNoTracking().Where(i => i.Name == userName).AnyAsync())
                    {
                        result.Code = Resource.EC_COMMON__DUPLICATED_DATA;
                        result.Message = Resource.EM_COMMON_NAME_ALREADY_EXIST;
                        result.Result = EnumResponseResult.Error;
                    }
                    // 존재하지 않을 경우
                    else result.Result = EnumResponseResult.Success;
                }
            }
            catch (Exception ex)
            {
                NNException.Log(ex);

                result.Code = Resource.EC_COMMON__EXCEPTION;
                result.Message = Resource.EM_COMMON__EXCEPTION;
                result.Result = EnumResponseResult.Error;
            }
            return result;
        }

        /// <summary>이메일 중복 여부를 검사한다.</summary>
        /// <param name="email">이메일</param>
        /// <returns>검사 결과 객체</returns>
        public async Task<ResponseData> CheckEmailDuplicated(string email)
        {
            ResponseData result = new ResponseData();
            try
            {
                // 요청이 유효하지 않은 경우
                if (email.IsEmpty())
                {
                    result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
                    result.Message = Resource.EM_COMMON_ACCOUNT_REQUIRE_EMAIL;
                    result.Result = EnumResponseResult.Error;
                }
                // 이메일 주소의 형식이 올바르지 않을 경우
                else if(!regex.IsMatch(email))
                {
                    result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
                    result.Message = Resource.EM_COMMON_ACCOUNT_INVALID_EMAIL;
                    result.Result = EnumResponseResult.Error;
                }
                // 요청이 유효한 경우
                else
                {
                    // S3 사용자중 해당 이메일이 존재하는 경우
                    if (await m_dbContext.S3Users.AsNoTracking().Where(i => i.Email == email).AnyAsync())
                    {
                        result.Code = Resource.EC_COMMON__DUPLICATED_DATA;
                        result.Message = Resource.EM_COMMON_ACCOUNT_ALREADY_AUTH_EMAIL;
                        result.Result = EnumResponseResult.Error;
                    }
                    // 존재하지 않을 경우
                    else result.Result = EnumResponseResult.Success;
                }
            }
            catch (Exception ex)
            {
                NNException.Log(ex);

                result.Result = EnumResponseResult.Error;
                result.Code = Resource.EC_COMMON__EXCEPTION;
                result.Message = Resource.EM_COMMON__EXCEPTION;
            }
            return result;
        }


        /// <summary>특정 사용자 식별자에 대한 사용자 정보 객체를 가져온다.</summary>
        /// <param name="id">사용자 식별자</param>
        /// <param name="includeDeletedUser">삭제된 사용자도 포함할지 여부</param>
        /// <returns>사용자 정보 객체</returns>
        public async Task<ResponseData<ResponseAddS3User>> GetUserById(string id)
        {
            ResponseData<ResponseAddS3User> result = new ResponseData<ResponseAddS3User>();
            try
            {
                // 사용자 식별자가 유효하지 않은 경우
                if (id.IsEmpty())
                {
                    result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
                    result.Message = Resource.EM_COMMON_ACCOUNT_INVALID_USERID;
                    result.Result = EnumResponseResult.Error;
                }

				// 사용자 ID로 해당 사용자 정보를 반환한다.
				ResponseAddS3User user = await m_dbContext.S3Users.AsNoTracking().Where(i => i.Id == id).FirstOrDefaultAsync<S3User, ResponseAddS3User>();

				// 해당 계정을 찾을 수 없는 경우
				if (user == null)
                {
                    result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
                    result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
                    result.Result = EnumResponseResult.Error;
                }
                else
                {
                    result.Data = user;
                    result.Result = EnumResponseResult.Success;   
                }
            }
            catch (Exception ex)
            {
                NNException.Log(ex);

                result.Code = Resource.EC_COMMON__EXCEPTION;
                result.Message = Resource.EM_COMMON__EXCEPTION;
                result.Result = EnumResponseResult.Error;
            }
            return result;
        }


        /// <summary>특정 사용자 식별자에 대한 사용자 정보 객체를 가져온다.</summary>
        /// <param name="id">사용자 식별자</param>
        /// <returns>사용자 정보 객체</returns>
        public async Task<ResponseData<ResponseAddS3User>> GetUserByName(string name)
        {
            ResponseData<ResponseAddS3User> result = new ResponseData<ResponseAddS3User>();
            try
            {
                // 사용자 식별자가 유효하지 않은 경우
                if (name.IsEmpty())
                {
                    result.Code = Resource.EC_COMMON__INVALID_INFORMATION;
                    result.Message = Resource.EM_COMMON_ACCOUNT_REQUIRE_NAME;
                    result.Result = EnumResponseResult.Error;
                }
                    
				// 사용자 이름으로 해당 사용자 정보를 반환한다.
				ResponseAddS3User user = await m_dbContext.S3Users.AsNoTracking().Where(i => i.Name == name).FirstOrDefaultAsync<S3User, ResponseAddS3User>();

				// 해당 계정을 찾을 수 없는 경우
				if (user == null)
                {
                    result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
                    result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
                    result.Result = EnumResponseResult.Error;
                }
                else
                {
                    result.Data = user;
                    result.Result = EnumResponseResult.Success;   
                }
            }
            catch (Exception ex)
            {
                NNException.Log(ex);

                result.Code = Resource.EC_COMMON__EXCEPTION;
                result.Message = Resource.EM_COMMON__EXCEPTION;
                result.Result = EnumResponseResult.Error;
            }
            return result;
        }
        /// <summary>특정 로그인 이메일에 대한 사용자 정보 객체를 가져온다.</summary>
        /// <param name="email">이메일 주소</param>
        /// <returns>사용자 정보 객체</returns>
        public async Task<ResponseData<ResponseAddS3User>> GetUserByEmail(string email)
        {
            ResponseData<ResponseAddS3User> result = new ResponseData<ResponseAddS3User>();
            try
            {
                // 사용자 식별자가 유효하지 않은 경우
                if (email.IsEmpty())
                    return new ResponseData<ResponseAddS3User>(EnumResponseResult.Error, Resource.EC_COMMON__INVALID_INFORMATION, Resource.EM_COMMON_ACCOUNT_REQUIRE_EMAIL);
				// 사용자 이메일로 해당 사용자 정보를 반환한다.
				ResponseAddS3User user = await m_dbContext.S3Users.AsNoTracking().Where(i => i.Email == email).FirstOrDefaultAsync<S3User, ResponseAddS3User>();
				
				// 해당 계정을 찾을 수 없는 경우
				if (user == null)
                {
                    result.Code = Resource.EC_COMMON_ACCOUNT_NOT_FOUND;
                    result.Message = Resource.EM_COMMON_ACCOUNT_NOT_FOUND;
                    result.Result = EnumResponseResult.Error;
                }
                else
                {
                    result.Data = user;
                    result.Result = EnumResponseResult.Success;   
                }
            }
            catch (Exception ex)
            {
                NNException.Log(ex);

                result.Code = Resource.EC_COMMON__EXCEPTION;
                result.Message = Resource.EM_COMMON__EXCEPTION;
                result.Result = EnumResponseResult.Error;
            }
			return result;
        }

        /************************************************************************************************************/
        protected readonly char[] TEXT = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".ToCharArray();
        protected readonly char[] TEXT_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".ToCharArray();
        /// <summary>랜덤한 문자열(대문자+숫자)을 생성한다.</summary>
        /// <param name="Length">문자열 길이</param>
        /// <returns>생성한 문자열</returns>
        protected string RandomText(int Length)
        {
            Random rand = new Random();
            var chars = Enumerable.Range(0, Length).Select(x => TEXT[rand.Next(0, TEXT.Length)]);
            return new string(chars.ToArray());
        }

        /// <summary>랜덤한 문자열(대문자+소문자+숫자)을 생성한다.</summary>
        /// <param name="Length">문자열 길이</param>
        /// <returns>생성한 문자열</returns>
        protected string RandomTextLong(int Length)
        {
            Random rand = new Random();
            var chars = Enumerable.Range(0, Length).Select(x => TEXT_STRING[rand.Next(0, TEXT_STRING.Length)]);
            return new string(chars.ToArray());
        }
    }
}
