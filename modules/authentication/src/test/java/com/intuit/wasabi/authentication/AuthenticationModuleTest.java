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
package com.intuit.wasabi.authentication;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.intuit.wasabi.userdirectory.UserDirectoryModule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AuthenticationModuleTest {

    @Test
    public void testClassNotFound() {
        try {
            Injector injector = Guice.createInjector(new AuthenticationModule());
            Authentication defaultAuthentication = injector.getInstance(Authentication.class);
            fail("Should throw exception");
        } catch (CreationException e) {
            //ignore
        }
    }

    @Test
    public void testOk() {
        System.getProperties().put("user.lookup.class.name",
                "com.intuit.wasabi.userdirectory.impl.DefaultUserDirectory");
        System.getProperties().put("authentication.class.name",
                "com.intuit.wasabi.authentication.impl.DefaultAuthentication");
        System.getProperties().put("http.proxy.port", "8080");
        Injector injector = Guice.createInjector(new UserDirectoryModule(), new AuthenticationModule());
        Authentication defaultAuthentication = injector.getInstance(Authentication.class);
        assertNotNull(defaultAuthentication);

    }
}
