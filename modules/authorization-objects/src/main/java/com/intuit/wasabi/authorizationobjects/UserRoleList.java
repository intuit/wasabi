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
package com.intuit.wasabi.authorizationobjects;

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserRoleList {

    @ApiModelProperty(required = true)
    private List<UserRole> roleList = new ArrayList<>();

    public UserRoleList() {
        super();
    }

    public UserRoleList(int initialSize) {
        super();
        roleList = new ArrayList<>(initialSize);
    }

    public List<UserRole> getRoleList() {
        return roleList;
    }

    public void setRoleList(List<UserRole> roleList) {
        this.roleList = roleList;
    }

    public void addRole(UserRole value) {
        roleList.add(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (Objects.isNull(obj)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof UserRoleList)) {
            return false;
        }

        UserRoleList other = (UserRoleList) obj;
        return new EqualsBuilder()
                .append(getRoleList().size(), other.getRoleList().size())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(1, 31)
                .append(getRoleList().size())
                .toHashCode();
    }
}
