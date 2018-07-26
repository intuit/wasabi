/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.api;

import com.intuit.wasabi.authentication.Authentication;
import com.intuit.wasabi.authenticationobjects.LoginToken;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.authorizationobjects.Role;
import com.intuit.wasabi.authorizationobjects.UserRole;
import com.intuit.wasabi.authorizationobjects.UserRoleList;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.experimentobjects.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.core.Response;

import static java.nio.charset.Charset.forName;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationResourceTest {

    private static final String USERPASS = new String(encodeBase64("admin@example.com:admin01".getBytes(forName("UTF-8"))), forName("UTF-8"));
    private static final String AUTHHEADER = "Basic: " + USERPASS;
    private static final String TOKENHEADER = "Bearer: " + USERPASS;
    private static final Application.Name TESTAPP = Application.Name.valueOf("test_app");
    private static final Application.Name TESTAPP2 = Application.Name.valueOf("test_app2");
    private static final UserInfo.Username USER = UserInfo.Username.valueOf("admin@example.com");


    public AuthenticationResource authenticationResource;
    @Mock
    Authentication authentication;

    @Mock
    Authorization authorization;

    @Before
    public void setup() {
        authenticationResource =
                new AuthenticationResource(authentication,
                        new HttpHeader("application-name", "600"),
                        authorization,false,1);
    }

    @Test
    public void logUserIn() throws Exception {

        try {
            authenticationResource.logUserIn(AUTHHEADER, "");
            fail();
        } catch (AuthenticationException ignored) {
        }

        LoginToken loginToken = LoginToken.withAccessToken("access").build();
        when(authentication.logIn(AUTHHEADER)).thenReturn(loginToken);
        Response response = authenticationResource.logUserIn(AUTHHEADER, "client_credentials");
        assert (loginToken.equals(response.getEntity()));
    }

    @Test
    public void verifyToken() throws Exception {

        LoginToken loginToken = LoginToken.withAccessToken("access").build();
        when(authentication.verifyToken(TOKENHEADER)).thenReturn(loginToken);
        Response response = authenticationResource.verifyToken(TOKENHEADER);
        assert (loginToken.equals(response.getEntity()));
    }

    @Test
    public void logUserOut() throws Exception {
        Response response = authenticationResource.logUserOut(TOKENHEADER);
        assert (response.getStatus() == 204);
    }

    @Test
    public void getUserExists() throws Exception {
        UserInfo userInfo = UserInfo.newInstance(USER).build();

        UserRole userRole = UserRole.newInstance(TESTAPP, Role.ADMIN).withUserID(USER).build();
        UserRole userRole1 = UserRole.newInstance(TESTAPP2, Role.READWRITE).withUserID(USER).build();
        UserRoleList userRoleList = new UserRoleList();
        userRoleList.addRole(userRole);
        userRoleList.addRole(userRole1);

        when(authentication.getUserExists("username@a.b")).thenReturn(userInfo);
        when(authorization.getUser(AUTHHEADER)).thenReturn(USER);
        when(authorization.getUserRoleList(USER)).thenReturn(userRoleList);

        Response response = authenticationResource.getUserExists("username@a.b", AUTHHEADER);
        assert (userInfo.equals(response.getEntity()));
        Mockito.verify(authorization).getUser(AUTHHEADER);
    }
}
