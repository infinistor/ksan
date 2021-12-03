FROM mcr.microsoft.com/dotnet/aspnet:5.0-buster-slim AS base
RUN apt-get update
# Sendmail 설치
RUN apt-get --assume-yes install sendmail
# 접속권한 관련 파일 복사
ADD access /etc/mail/
# 접속권한 관련 파일 반영
RUN rm /etc/mail/access.db
RUN makemap hash /etc/mail/access < /etc/mail/access
# 접속권한 관련 파일 복사
ADD sendmail.mc /etc/mail/
# Sendmail Config 반영
RUN m4 /etc/mail/sendmail.mc > /etc/mail/sendmail.cf
RUN apt-get install -y supervisor
