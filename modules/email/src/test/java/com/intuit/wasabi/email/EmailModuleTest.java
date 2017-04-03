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
package com.intuit.wasabi.email;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class EmailModuleTest {

    @Test(expected = NullPointerException.class)
    public void testClassNotFound() {
        try {
            Injector injector = Guice.createInjector(new EmailModule());
            EmailService service = injector.getInstance(EmailService.class);
            fail("Should throw exception");
            assertNull(service);
        } catch (CreationException e) {
            //ignore
            assertEquals(e.getCause().getClass(), NullPointerException.class);
        }
    }
}
