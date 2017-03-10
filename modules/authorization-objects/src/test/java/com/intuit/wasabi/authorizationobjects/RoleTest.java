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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class RoleTest {

    @Test
    public void testRole() {
        assertEquals(Role.ADMIN, Role.toRole("ADMIN"));
        assertEquals(null, Role.toRole("ADMINE"));
    }

    @Test
    public void testRolePermissions() {
        List<Permission> permissions = Role.ADMIN.getRolePermissions();
        assertTrue(permissions.contains(Permission.CREATE));
        assertTrue(permissions.contains(Permission.READ));
        assertTrue(permissions.contains(Permission.UPDATE));
        assertTrue(permissions.contains(Permission.DELETE));
        assertTrue(permissions.contains(Permission.ADMIN));
    }


    @Test
    public void testJacksonSerialization() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Role.class, new Role.Serializer());
        objectMapper.registerModule(module);
        StringWriter stringWriter = new StringWriter();
        objectMapper.writeValue(stringWriter, Role.SUPERADMIN);
        assertThat(stringWriter.toString(), is("\"SUPERADMIN\""));
    }

}
