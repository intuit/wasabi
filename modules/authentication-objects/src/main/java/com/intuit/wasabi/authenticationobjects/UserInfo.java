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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.IOException;

/**
 * Information about admin users, IE not users in the assignments/events sense
 */
public class UserInfo {

    @ApiModelProperty(value = "user name", required = true)
    private Username username;
    @ApiModelProperty(value = "user password", required = false)
    private String password;
    @ApiModelProperty(value = "numerical user ID", required = false)
    private String userId;
    @ApiModelProperty(value = "the first name of the user", required = false)
    private String firstName;
    @ApiModelProperty(value = "the last name of the user", required = false)
    private String lastName;
    @ApiModelProperty(value = "the user's email", required = false)
    private String email;

    protected UserInfo() {
        super();
    }

    public Username getUsername() {
        return username;
    }

    public void setUsername(Username username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the userId
     */
    public String getUserId() {
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public static Builder newInstance(Username username) {
        return new Builder(username);
    }

    public static Builder from(Username username) {
        return new Builder(username);
    }

    public static class Builder {

        public Builder(Username username) {
            instance = new UserInfo();
            instance.username = Preconditions.checkNotNull(username);
        }

        public Builder withUserId(final String userId) {
            this.instance.userId = userId;
            return this;
        }

        public Builder withFirstName(final String name) {
            this.instance.firstName = name;
            return this;
        }

        public Builder withLastName(final String name) {
            this.instance.lastName = name;
            return this;
        }

        public Builder withEmail(final String email) {
            this.instance.email = email;
            return this;
        }

        public Builder withPassword(final String password) {
            this.instance.password = password;
            return this;
        }

        public UserInfo build() {
            UserInfo result = instance;
            instance = null;
            return result;
        }

        private UserInfo instance;
    }

    @Override
    public String toString() {
        return "UserInfo [username=" + username + ", userId=" + userId +
                ", firstName=" + firstName + ", lastName=" +
                lastName + ", email=" + email + "]";
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(1, 31)
                .append(username)
                .append(userId)
                .append(firstName)
                .append(lastName)
                .append(email)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof UserInfo)) {
            return false;
        }

        UserInfo other = (UserInfo) obj;
        return new EqualsBuilder()
                .append(username, other.getUsername())
                .append(userId, other.getUserId())
                .append(firstName, other.getFirstName())
                .append(lastName, other.getLastName())
                .append(email, other.getEmail())
                .isEquals();
    }

    @JsonSerialize(using = UserInfo.Username.Serializer.class)
    @JsonDeserialize(using = UserInfo.Username.Deserializer.class)
    public static class Username {

        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        private Username(String username) {
            super();
            this.username = Preconditions.checkNotNull(username);
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
            return username;
        }

        public static Username valueOf(String value) {
            return new Username(value.toLowerCase());
        }

        public static class Serializer extends JsonSerializer<Username> {
            @Override
            public void serialize(Username username, JsonGenerator generator,
                                  SerializerProvider provider)
                    throws IOException {
                generator.writeString(username.toString());
            }
        }

        public static class Deserializer extends JsonDeserializer<Username> {
            @Override
            public Username deserialize(JsonParser parser,
                                        DeserializationContext context)
                    throws IOException {
                return Username.valueOf(parser.getText());
            }
        }
    }
}
