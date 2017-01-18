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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class PermissionTest {

    @Test
    public void testPermission() {
        assertEquals(Permission.CREATE, Permission.toPermission("CREATE"));
        assertEquals(null, Permission.toPermission("CREATEE"));
    }

    @Test
    public void testJacksonSerialization() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Permission.class, new Permission.Serializer());
        objectMapper.registerModule(module);
        StringWriter stringWriter = new StringWriter();
        objectMapper.writeValue(stringWriter, Permission.ADMIN);
        assertThat(stringWriter.toString(), is("\"ADMIN\""));
    }

//    @Test
//    public void testJacksonDeserialization() throws IOException{
//        ObjectMapper objectMapper = new ObjectMapper();
//        SimpleModule module = new SimpleModule();
//        module.addDeserializer(Permission.class, new Permission.Deserializer());
//        objectMapper.registerModule(module);
//        Permission permission = objectMapper.readValue("\"CREATE\"", Permission.class);
//        System.out.println(permission);
//    }

}
