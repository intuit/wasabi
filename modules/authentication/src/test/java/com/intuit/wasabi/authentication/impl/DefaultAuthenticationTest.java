/**
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
 */
/**
 *
 */
package com.intuit.wasabi.authentication.impl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.intuit.wasabi.authentication.Authentication;
import com.intuit.wasabi.authentication.AuthenticationModule;
import com.intuit.wasabi.authentication.TestUtil;
import com.intuit.wasabi.authenticationobjects.LoginToken;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.userdirectory.UserDirectoryModule;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * Unit test for Default Authenication Test
 */
public class DefaultAuthenticationTest {

    public static final String WASABI_ADMIN_WASABI_ADMIN = "admin:admin";
    public static final String WASABI_ADMIN_ADMIN_WASABI = "wasabi_admin:admin_wasabi";
    public static final String WASABI_READER_WASABI01 = "wasabi_reader:wasabi01";
    public static final String WASABI_ADMIN = "admin";
    public static final String ADMIN_EXAMPLE_COM = "admin@example.com";
    private Authentication defaultAuthentication = null;

    @Before
    public void setUp() throws Exception {
        System.getProperties().put("user.lookup.class.name",
                "com.intuit.wasabi.userdirectory.impl.DefaultUserDirectory");
        System.getProperties().put("authentication.class.name",
                "com.intuit.wasabi.authentication.impl.DefaultAuthentication");
        System.getProperties().put("http.proxy.port", "8080");
        Injector injector = Guice.createInjector(new UserDirectoryModule(), new AuthenticationModule());
        defaultAuthentication = injector.getInstance(Authentication.class);
        assertNotNull(defaultAuthentication);

    }

    @Test(expected = AuthenticationException.class)
    public void testLogIn() {
        defaultAuthentication.logIn(null);
    }

    @Test(expected = AuthenticationException.class)
    public void testVerifyToken() throws Exception {
        LoginToken token = defaultAuthentication.verifyToken("Basic amFiYmFAaW50dWl0LmNvbTpqYWJiYTAx");
        LoginToken expected = LoginToken.withAccessToken("amFiYmFAaW50dWl0LmNvbTpqYWJiYTAx").withTokenType("Basic").build();
        assertThat(token, is(expected));

        //finally cause an exception if token is null
        defaultAuthentication.verifyToken(null);
    }

    @Test(expected = AuthenticationException.class)
    public void testVerifyBadToken() throws Exception {
        LoginToken token = defaultAuthentication.verifyToken("Basic THISISABADTOKEN");
        LoginToken expected = LoginToken.withAccessToken("THISISABADTOKEN").withTokenType("Basic").build();
        assertThat(token, is(expected));
    }

    @Test(expected = AuthenticationException.class)
    public void testVerifyBadTokenWithTamperedPassword() throws Exception {
        LoginToken token = defaultAuthentication.verifyToken("Basic amFiYmFfYWRtaW46YmFkX3Bhc3N3b3Jk");
        LoginToken expected = LoginToken.withAccessToken("amFiYmFfYWRtaW46YmFkX3Bhc3N3b3Jk").withTokenType("Basic").build();
        assertThat(token, is(expected));
    }

    @Test
    public void testLogOut() throws Exception {
        boolean result = defaultAuthentication.logOut(null);
        assertThat(result, is(true));
        result = defaultAuthentication.logOut("ANYTHING");
        assertThat(result, is(true));
    }

    @Test(expected = AuthenticationException.class)
    public void testGetNullUserExists() throws Exception {
        defaultAuthentication.getUserExists(null);
    }

    @Test
    public void testGetUserExists() {
        UserInfo user = defaultAuthentication.getUserExists(ADMIN_EXAMPLE_COM);
        UserInfo expected = UserInfo.from(UserInfo.Username.valueOf(WASABI_ADMIN))
                .withEmail(ADMIN_EXAMPLE_COM)
                .withLastName("Admin")
                .withUserId(WASABI_ADMIN)
                .withFirstName("Wasabi").build();
        assertEquals(user, expected);
    }

    @Test(expected = AuthenticationException.class)
    public void testBadPasswordLogIn() {
        LoginToken token = defaultAuthentication.logIn(DefaultAuthentication.BASIC + " " +
                new String(Base64.encodeBase64(WASABI_ADMIN_ADMIN_WASABI.getBytes(TestUtil.CHARSET))));
        assertThat(token,
                is(LoginToken.withAccessToken(
                        new String(Base64.encodeBase64(WASABI_ADMIN_ADMIN_WASABI.getBytes(TestUtil.CHARSET))))
                        .withTokenType(DefaultAuthentication.BASIC).build()
                )
        );
    }

    @Test
    public void testGoodPasswordLogIn() {
        LoginToken token = defaultAuthentication.logIn(DefaultAuthentication.BASIC + " " +
                new String(Base64.encodeBase64(WASABI_ADMIN_WASABI_ADMIN.getBytes(TestUtil.CHARSET))));
        assertThat(token,
                is(LoginToken.withAccessToken(
                        new String(Base64.encodeBase64(WASABI_ADMIN_WASABI_ADMIN.getBytes(TestUtil.CHARSET))))
                        .withTokenType(DefaultAuthentication.BASIC).build()
                )
        );
        // Test user with special charactors in the key
        token = defaultAuthentication.logIn(DefaultAuthentication.BASIC + " " +
                new String(Base64.encodeBase64(WASABI_READER_WASABI01.getBytes(TestUtil.CHARSET))));
        assertThat(token,
                is(LoginToken.withAccessToken(
                        new String(Base64.encodeBase64(WASABI_READER_WASABI01.getBytes(TestUtil.CHARSET))))
                        .withTokenType(DefaultAuthentication.BASIC).build()
                )
        );
    }
}
