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
package com.intuit.wasabi.authenticationobjects;

import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class LoginCredentials {

    @ApiModelProperty(value = "username", required = true)
    private UserInfo.Username username;
    @ApiModelProperty(value = "password", required = true)
    private String password;
    @ApiModelProperty(value = "name of the application; e.g. \"QBO\"", required = false)
    private String namespaceId;

    protected LoginCredentials() {
        super();
    }

    public UserInfo.Username getUsername() {
        return username;
    }

    public void setUsername(UserInfo.Username username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

    public static Builder withUsername(UserInfo.Username username) {
        return new Builder(username);
    }

    public static class Builder {

        private LoginCredentials instance;

        private Builder(UserInfo.Username username) {
            super();
            instance = new LoginCredentials();
            instance.username = Preconditions.checkNotNull(username);
        }

        public Builder withPassword(final String password) {
            instance.password = password;
            return this;
        }

        public Builder withNamespaceId(final String namespaceId) {
            instance.namespaceId = namespaceId;
            return this;
        }

        public LoginCredentials build() {
            LoginCredentials result = instance;
            instance = null;
            return result;
        }
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return "LoginCredentials{" +
                "username=" + username +
                ", namespaceId=" + namespaceId + '}';
    }

}
