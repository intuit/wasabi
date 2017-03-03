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
package com.intuit.wasabi.authentication;


import com.intuit.wasabi.authenticationobjects.LoginToken;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.exceptions.AuthenticationException;

/**
 * Classes implementing this interface allow for login, logout, user lookup and basic session handling.
 */
public interface Authentication {

    /**
     * Attempts to login the user identified via the specified authHeader.
     *
     * @param authHeader the authentication header
     * @return a login token for this user on success
     * @throws AuthenticationException if not successful
     */
    LoginToken logIn(String authHeader);


    /**
     * Attempts to verify the user token retrieved via the specified tokenHeader.
     *
     * @param tokenHeader the token header
     * @return a login token for this user on success
     * @throws AuthenticationException if not successful
     */
    LoginToken verifyToken(String tokenHeader);

    /**
     * Attempts to logOut the user using the retrieved token.
     *
     * @param tokenHeader the token header
     * @return true on success, false otherwise
     * @throws AuthenticationException if the logout failed
     */
    boolean logOut(String tokenHeader);

    /**
     * Looks up if a user with the specified email exists.
     *
     * @param userEmail the user mail
     * @return the UserInfo of the particular user
     * @throws AuthenticationException if it not possible to retrieve the user info
     */
    UserInfo getUserExists(String userEmail);

}
