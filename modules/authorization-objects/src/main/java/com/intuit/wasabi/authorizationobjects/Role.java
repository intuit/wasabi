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


import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

@JsonSerialize(using = Role.Serializer.class)
public enum Role {

    READONLY(Permission.READ),
    READWRITE(Permission.CREATE, Permission.READ, Permission.UPDATE, Permission.DELETE),
    ADMIN(Permission.CREATE, Permission.READ, Permission.UPDATE, Permission.DELETE, Permission.ADMIN),
    SUPERADMIN(Permission.CREATE, Permission.READ, Permission.UPDATE, Permission.DELETE,
            Permission.ADMIN, Permission.SUPERADMIN);

    //TODO: make enum not store states
    private final transient List<Permission> Permissions;

    Role(Permission... permissions) {
        this.Permissions = asList(permissions);
    }

    public List<Permission> getRolePermissions() {
        return Permissions;
    }

    public static Role toRole(String role) {
        for (Role role1 : Role.values()) {
            if (role1.name().equalsIgnoreCase(role)) {
                return role1;
            }
        }
        return null;
    }

    public static class Serializer extends JsonSerializer<Role> {
        @Override
        public void serialize(Role role, JsonGenerator generator, SerializerProvider provider)
                throws IOException {
            generator.writeString(role.toString());
        }
    }

}
