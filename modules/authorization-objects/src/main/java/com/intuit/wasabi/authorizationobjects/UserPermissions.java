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
import com.intuit.wasabi.experimentobjects.Application;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

public class UserPermissions {

    @ApiModelProperty(value = "name of the application", required = true)
    private Application.Name applicationName;
    @ApiModelProperty(value = "permissions associated with the user", required = true)
    private List<Permission> permissions;


    public Application.Name getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(Application.Name applicationName) {
        this.applicationName = applicationName;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    protected UserPermissions() {
        super();
    }

    public static Builder newInstance(Application.Name applicationName, List<Permission> permissions) {
        return new Builder(applicationName, permissions);
    }

    public static Builder from(UserPermissions userPermissions) {
        return new Builder(userPermissions);
    }

    public static class Builder {

        private Builder(Application.Name applicationName, List<Permission> permissions) {
            instance = new UserPermissions();
            instance.permissions = Preconditions.checkNotNull(permissions);
            instance.applicationName = applicationName;
        }

        private Builder(UserPermissions other) {
            this(other.applicationName, other.permissions);
        }

        public UserPermissions build() {
            UserPermissions result = instance;
            instance = null;
            return result;
        }

        private UserPermissions instance;
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
