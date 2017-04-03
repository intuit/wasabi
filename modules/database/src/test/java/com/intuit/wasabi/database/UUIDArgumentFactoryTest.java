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
package com.intuit.wasabi.database;


import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;


public class UUIDArgumentFactoryTest {

    UUIDArgumentFactory factory = null;

    @Before
    public void setUp() {
        factory = new UUIDArgumentFactory();
    }

    @Test
    public void testAcceptslTrue() {
        assertEquals(true, factory.accepts(null, UUID.randomUUID(), null));
    }

    @Test
    public void testAcceptsStringFalse() {
        assertEquals(false, factory.accepts(null, "uuid", null));
    }

    @Test
    public void testBuildExperimentId() {
        assertEquals(UUIDArgument.class,
                factory.build(null, UUID.randomUUID(), null).getClass());
    }
}
