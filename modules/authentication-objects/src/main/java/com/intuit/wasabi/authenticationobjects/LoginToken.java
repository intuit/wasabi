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

public class LoginToken {

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    @ApiModelProperty(value = "access_token", required = true)
    private String access_token;
    @ApiModelProperty(value = "token_type")
    private String token_type;


    protected LoginToken() {
        super();
    }

    public static Builder withAccessToken(String access_token) {
        return new Builder(access_token);
    }

    public static Builder from(LoginToken iusTicket) {
        return new Builder(iusTicket);
    }

    public static class Builder {

        private LoginToken instance;

        private Builder(String access_token) {
            super();
            instance = new LoginToken();
            instance.access_token = Preconditions.checkNotNull(access_token);
        }

        private Builder(LoginToken other) {
            this(other.access_token);
            instance.token_type = other.token_type;
        }

        public Builder withTokenType(final String token_type) {
            instance.token_type = token_type;
            return this;
        }

        public LoginToken build() {
            LoginToken result = instance;
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
        return "LoginToken{" +
                "access_token=hidden" +
                "token_type=" + token_type + '}';
    }
}
