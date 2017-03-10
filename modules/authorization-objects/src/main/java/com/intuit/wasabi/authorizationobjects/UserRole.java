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
package com.intuit.wasabi.authorizationobjects;

import com.google.common.base.Preconditions;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.Application;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class UserRole {

    @ApiModelProperty(value = "name of the application", dataType = "String", required = true)
    private Application.Name applicationName;
    @ApiModelProperty(value = "Role associated with the user for a given application", required = true)
    private Role role;
    @ApiModelProperty(value = "user name", dataType = "String", required = false)
    private UserInfo.Username userID;
    @ApiModelProperty(value = "user email", required = false)
    private String userEmail;
    @ApiModelProperty(value = "first name", required = false)
    private String firstName;
    @ApiModelProperty(value = "last name", required = false)
    private String lastName;

    public Application.Name getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(Application.Name applicationName) {
        this.applicationName = applicationName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserInfo.Username getUserID() {
        return userID;
    }

    public void setUserID(UserInfo.Username userID) {
        this.userID = userID;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
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

    protected UserRole() {
        super();
    }

    public static Builder newInstance(Application.Name applicationName, Role role) {
        return new Builder(applicationName, role);
    }

    public static Builder from(UserRole userRole) {
        return new Builder(userRole);
    }

    public static class Builder {

        public Builder(Application.Name applicationName, Role role) {
            instance = new UserRole();
            instance.applicationName = Preconditions.checkNotNull(applicationName);
            instance.role = Preconditions.checkNotNull(role);
        }

        public Builder withUserID(final UserInfo.Username userID) {
            this.instance.userID = userID;
            return this;
        }

        public Builder withUserEmail(final String userEmail) {
            this.instance.userEmail = userEmail;
            return this;
        }

        public Builder withFirstName(final String firstName) {
            this.instance.firstName = firstName;
            return this;
        }

        public Builder withLastName(final String lastName) {
            this.instance.lastName = lastName;
            return this;
        }

        private Builder(UserRole other) {
            this(other.applicationName, other.role);
        }

        public UserRole build() {
            UserRole result = instance;
            instance = null;
            return result;
        }

        private UserRole instance;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

}
