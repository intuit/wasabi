/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.authenticationobjects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class LoginTokenTest {

    private String access_token = "testToken";
    private String token_type = "basic";

    private LoginToken token;

    @Before
    public void setUp() throws Exception {
        token = getLoginToken();
    }

    /**
     * @return
     */
    private LoginToken getLoginToken() {
        return LoginToken.withAccessToken(access_token)
                .withTokenType(token_type)
                .build();
    }

    @Test
    public void testLoginTokenSet() {
        token.setAccess_token(access_token);
        token.setToken_type(token_type);
        assertEquals(access_token, token.getAccess_token());
        assertEquals(token_type, token.getToken_type());
    }

    @Test
    public void testAssignmentFromOther() {
        LoginToken other = LoginToken.from(token).build();
        assertEquals(token, other);
    }

}
