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
package com.intuit.wasabi.experimentobjects;

import com.intuit.wasabi.experimentobjects.exceptions.InvalidIdentifierException;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This class tests the functionality of the Context object.
 */
public class ContextTest {

    @Test
    public void testContextCreation() {
        //with builder
        Context con = Context.newInstance("foo").build();
        assertEquals("foo", con.getContext());
        con.setContext("foo2");
        assertEquals("foo2", con.getContext());

        //protected Constructor
        con = new Context();
        assertEquals("PROD", con.getContext());
    }

    @Test(expected = NullPointerException.class)
    public void testContextCreationWithNull() {
        Context.newInstance(null).build();
    }

    @Test
    public void testContextCreationWithIllegalName() {
        List<String> badNames = Arrays.asList("1QA", "PRODÜCTION", " PROD", "Pre Prod");
        for (String name : badNames) {
            try {
                Context.newInstance(name);
                fail();
            } catch (InvalidIdentifierException e) {
                //all names are invalid Identifieres!
            }
        }

    }

    @Test
    public void testCopyConstructor() {

        Context con = Context.newInstance("a$123_a").build();
        Context con2 = Context.from(con).build();

        assertEquals(con.getContext(), con2.getContext());

    }

    @Test
    public void testEquals() {
        Context con = Context.newInstance("qa").build();
        assertFalse(con.equals(null));
        assertTrue(con.equals(con));
        assertFalse(con.equals("qa"));

        Context con2 = Context.newInstance("Qa").build();
        assertFalse(con.equals(con2));
        assertTrue(con.equals(Context.from(con).build()));

        assertEquals(con.hashCode(), Context.from(con).build().hashCode());
    }


}
