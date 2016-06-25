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


@JsonSerialize(using = Permission.Serializer.class)
public enum Permission {

    CREATE, READ, UPDATE, DELETE, ADMIN, SUPERADMIN;

    public static Permission toPermission(String permission) {
        for (Permission permission1 : Permission.values()) {
            if (permission1.name().equalsIgnoreCase(permission)) {
                return permission1;
            }
        }
        return null;
    }

    public static class Serializer extends JsonSerializer<Permission> {
        @Override
        public void serialize(Permission permission, JsonGenerator generator, SerializerProvider provider)
                throws IOException {
            generator.writeString(permission.toString());
        }
    }
}
