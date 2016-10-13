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
package com.intuit.wasabi.tests.service.auth;

import com.intuit.wasabi.tests.library.APIServerConnector;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;
import com.intuit.wasabi.tests.library.util.RetryTest;
import com.intuit.wasabi.tests.model.APIUser;
import com.intuit.wasabi.tests.model.AccessToken;
import com.intuit.wasabi.tests.model.Experiment;
import com.intuit.wasabi.tests.model.factory.APIUserFactory;
import com.intuit.wasabi.tests.model.factory.AccessTokenFactory;
import com.intuit.wasabi.tests.model.factory.ExperimentFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import static com.intuit.wasabi.tests.library.util.ModelAssert.assertEqualModelItems;

/**
 * Tests the user authentication endpoint.
 */
public class AuthenticationTest extends TestBase {

    private APIUser apiUser;
    private String username;
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private String validTokenPattern;
    private AccessToken token;

    /**
     * Reads parameters from TestNG configuration.
     * For username, password, and email it also tries to read them from the appProperties:
     * {@code user-name}, {@code password}, and {@code user-email}.
     * However, TestNG configurations take precedence, unless they match the defaults (see below).
     * <p>
     * <p>
     * <p>
     * {@code validTokenPattern} can be null (and will be null if not supplied via TestNG XML). If it
     * is null, the appProperties will be searched for "validTokenPattern". If those can not be found
     * either, all tests concerning the validTokenPattern will be ignored and counted as automatic
     * passes.
     *
     * @param username          the user name, default: usernameXYZ123456
     * @param password          the password, default: passwordXYZ123456
     * @param email             the email address, default: mail@example.org
     * @param firstName         the first name, default: John
     * @param lastName          the last name, default: Doe
     * @param validTokenPattern a regex pattern to validate the access token. See above for more details.
     */
    @Parameters({"username", "password", "email", "firstName", "lastName", "validTokenPattern"})
    public AuthenticationTest(@Optional("usernameXYZ123456") String username,
                              @Optional("passwordXYZ123456") String password,
                              @Optional("mail@example.org") String email,
                              @Optional("John") String firstName,
                              @Optional("Doe") String lastName,
                              @Optional String validTokenPattern) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.validTokenPattern = validTokenPattern;
    }

    /**
     * Sets up the apiUser.
     */
    @BeforeClass
    public void init() {
        if (username.equals("usernameXYZ123456")) {
            username = appProperties.getProperty("user-name");
        }
        if (password.equals("passwordXYZ123456")) {
            password = appProperties.getProperty("password");
        }
        if (email.equals("mail@example.org")) {
            email = appProperties.getProperty("user-email");
        }
        if (Objects.isNull(validTokenPattern)) {
            validTokenPattern = appProperties.getProperty("validTokenPattern");
        }
        apiUser = APIUserFactory.createAPIUser()
                .setUsername(username)
                .setPassword(password)
                .setEmail(email)
                .setFirstName(firstName)
                .setLastName(lastName);
    }

    /**
     * Checks if the default user exists.
     */
    @Test(dependsOnGroups = {"ping"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void checkUser() {
        APIUser apiUserNew = getUserExists(apiUser);
        Assert.assertEquals(apiUserNew.email, apiUser.email, "E-Mails do not match.");
    }

    /**
     * Checks if an invalid user exists.
     */
    @Test(dependsOnGroups = {"ping"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void checkInvalidUser() {
        APIUser invalidApiUser = APIUserFactory.createAPIUser().setEmail("non-existing-mail@example.org");
        APIUser apiUserNew = getUserExists(invalidApiUser, HttpStatus.SC_UNAUTHORIZED);
        Assert.assertNull(apiUserNew.email);
    }

    /**
     * Checks if an invalid user exists.
     */
    @Test(dependsOnGroups = {"ping"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void checkInvalidUserEmailNull() {
        APIUser invalidApiUser = APIUserFactory.createAPIUser().setEmail(null);
        APIUser apiUserNew = getUserExists(invalidApiUser, HttpStatus.SC_NOT_FOUND);
        Assert.assertNull(apiUserNew.email);
    }

    /**
     * Tries to login with the default user.
     */
    @Test(dependsOnMethods = {"checkUser"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void login() {
        AccessToken token = postLogin(apiUser);
        assertEqualAdmin(token);
    }

    private void assertEqualAdmin(AccessToken token) {
        if (Objects.nonNull(token)) {
            Assert.assertEquals(token.accessToken,
                    new String(Base64.encodeBase64((username + ":" + password).getBytes(Charset.defaultCharset()))),
                    token + " did not match admin");
        }
    }

    /**
     * Tries to login with invalid users and requests.
     */
    @Test(dependsOnGroups = {"ping"})
    public void loginFail() {
        postLogin(null, "wrong_grantype", HttpStatus.SC_UNAUTHORIZED);

        // would expect SC_UNAUTHORIZED as well
        postLogin(null, null, HttpStatus.SC_INTERNAL_SERVER_ERROR);

        postLogin(APIUserFactory
                        .createAPIUser()
                        .setUsername("invalidusername")
                        .setPassword("1234Password"),
                "cliencredentials", HttpStatus.SC_UNAUTHORIZED);


        postLogin(new APIUser(apiUser).setPassword("1234Password"),
                "cliencredentials", HttpStatus.SC_UNAUTHORIZED);
    }

    /**
     * Tries to login with the default user.
     */
    @Test(dependsOnMethods = {"checkUser"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void sessionLogin() {
        token = postLogin(apiUser);
        assertEqualAdmin(token);
    }

    /**
     * Tries to verify a token.
     */
    @Test(dependsOnMethods = {"sessionLogin"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void sessionVerifyToken() {
        AccessToken newToken = getVerifyToken(token);
        assertEqualModelItems(newToken, token);
    }

    /**
     * Tries to do a request with the received token.
     */
    @Test(dependsOnMethods = {"sessionVerifyToken"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void sessionRequestWithToken() {
        APIServerConnector asc = apiServerConnector.clone();
        asc.setAuthToken(token.tokenType, token.accessToken);
        asc.setUserNameAndPassword(null, null);
        Experiment created = postExperiment(ExperimentFactory.createExperiment(), HttpStatus.SC_CREATED, asc);
        List<Experiment> experiments = getExperiments(HttpStatus.SC_OK, asc);
        Assert.assertTrue(experiments.contains(created));
        deleteExperiment(created);
        experiments = getExperiments(HttpStatus.SC_OK, asc);
        Assert.assertFalse(experiments.contains(created));
    }

    /**
     * Tries to logout from a session
     * TODO: This does not seem to do anything?
     */
    @Test(dependsOnMethods = {"sessionRequestWithToken", "sessionVerifyInvalidToken"}, retryAnalyzer = RetryAnalyzer.class, alwaysRun = true)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void sessionLogout() {
        getLogout(token);
    }


    /**
     * Tries to verify invalid tokens.
     */
    @Test(dependsOnMethods = {"sessionVerifyToken"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void sessionVerifyInvalidToken() {
        AccessToken invalidToken = AccessTokenFactory.createAccessToken();
        AccessToken newToken = getVerifyToken(invalidToken, HttpStatus.SC_UNAUTHORIZED);
        assertEqualModelItems(newToken, token, null, false);

        invalidToken.setTokenType("Bearer").setAccessToken("1234567890");
        newToken = getVerifyToken(invalidToken, HttpStatus.SC_UNAUTHORIZED);
        assertEqualModelItems(newToken, token, null, false);

        invalidToken.setTokenType("OtherRealm").setAccessToken(token.accessToken);
        newToken = getVerifyToken(invalidToken, HttpStatus.SC_UNAUTHORIZED);
        assertEqualModelItems(newToken, token, null, false);
    }

    /**
     * Tries to verify a modified (thus invalid) token.
     */
    @Test(dependsOnMethods = {"sessionVerifyToken"}, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(maxTries = 3, warmup = 2000)
    public void sessionVerifyModifiedToken() {
        token = postLogin(apiUser);
        AccessToken accessToken = new AccessToken(token);
        accessToken.accessToken = token.accessToken.substring(0, token.accessToken.length() / 2)
                + "a"
                + token.accessToken.substring(token.accessToken.length() / 2, token.accessToken.length());

        AccessToken newToken = getVerifyToken(accessToken, HttpStatus.SC_UNAUTHORIZED);
        assertEqualModelItems(newToken, accessToken, null, false);
    }

    /**
     * Tries to login with a formerly valid access token.
     * FIXME: Currently the logout does nothing since username and password is never really used and all tokens will be returned as admin
     */
//    @Test(dependsOnMethods = {"sessionLogout"}, retryAnalyzer = RetryAnalyzer.class)
//    @Test(retryAnalyzer = RetryAnalyzer.class)
//    @RetryTest(maxTries = 3, warmup = 2000)
//    public void requestAfterLogout() {
//        APIServerConnector asc = apiServerConnector.clone();
//        asc.setUserNameAndPassword(null, null);
//        asc.setAuthToken(token.token_type, token.access_token);
//        postExperiment(ExperimentFactory.createExperiment(), HttpStatus.SC_UNAUTHORIZED, asc);
//    }
}
