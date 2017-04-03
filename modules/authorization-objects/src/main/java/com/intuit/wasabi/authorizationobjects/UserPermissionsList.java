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

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

public class UserPermissionsList {
    @ApiModelProperty(required = true)
    private List<UserPermissions> permissionsList = new ArrayList<>();

    public UserPermissionsList() {
        super();
    }

    public List<UserPermissions> getPermissionsList() {
        return permissionsList;
    }

    public void setPermissionsList(List<UserPermissions> permissionsList) {
        this.permissionsList = permissionsList;
    }

    public UserPermissionsList(int initialSize) {
        super();
        permissionsList = new ArrayList<>(initialSize);
    }

    public void addPermissions(UserPermissions value) {
        permissionsList.add(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof UserPermissionsList))
            return false;

        UserPermissionsList other = (UserPermissionsList) obj;
        return new EqualsBuilder()
                .append(getPermissionsList().size(), other.getPermissionsList().size())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(1, 31)
                .append(getPermissionsList().size())
                .toHashCode();
    }
}
