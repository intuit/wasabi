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
package com.intuit.wasabi.assignmentobjects;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for the {@link com.intuit.wasabi.assignmentobjects.User.ID}
 */
public class UserTest {

    private User.ID userID = User.ID.valueOf("1234567890");

    @Test
    public void testUserID() {
        assertTrue(userID.hashCode() == User.ID.valueOf("1234567890").hashCode());
        assertTrue(userID.toString().contains("1234567890"));

        assertTrue(userID.equals(User.ID.valueOf("1234567890")));
        assertFalse(userID.equals(User.ID.valueOf("42")));
    }

}
