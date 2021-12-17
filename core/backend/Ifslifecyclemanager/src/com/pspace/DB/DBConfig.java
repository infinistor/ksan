/*
* Copyright 2021 PSPACE, inc. KSAN Development Team ksan@pspace.co.kr
* KSAN is a suite of free software: you can redistribute it and/or modify it under the terms of
* the GNU General Public License as published by the Free Software Foundation, either version 
* 3 of the License.  See LICENSE.md for details
*
* 본 프로그램 및 관련 소스코드, 문서 등 모든 자료는 있는 그대로 제공이 됩니다.
* KSAN 프로젝트의 개발자 및 개발사는 이 프로그램을 사용한 결과에 따른 어떠한 책임도 지지 않습니다.
* KSAN 개발팀은 사전 공지, 허락, 동의 없이 KSAN 개발에 관련된 모든 결과물에 대한 LICENSE 방식을 변경 할 권리가 있습니다.
*/
package com.pspace.DB;

public class DBConfig {
    public String Host;
    public int Port;
    public String DatabaseName;
    public String User;
    public String Password;

    public DBConfig()
    {
        Init();
    }

    public DBConfig(String Host, int Port, String DatabaseName, String User, String Password)
    {
        this.Host = Host;
        this.Port = Port;
        this.DatabaseName = DatabaseName;
        this.User = User;
        this.Password = Password;
    }

    public void Init()
    {
        Host = "";
        Port = 0;
        DatabaseName = "";
        User = "";
        Password = "";
    }
    
    @Override
    public String toString()
    {
        return String.format(
        "%s{\n" + 
            "\t%s : %s,\n" + 
            "\t%s : %d,\n" + 
            "\t%s : %s,\n" + 
            "\t%s : %s,\n" + 
            "\t%s : %s,\n" + 
            "\t%s : %s\n" + 
        "}",
        "DBConfig",
            "Host", Host,
            "Port", Port,
            "DatabaseName", DatabaseName,
            "User", User,
            "Password", Password
        );
    }
    
}
