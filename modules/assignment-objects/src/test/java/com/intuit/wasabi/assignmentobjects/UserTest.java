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
package com.intuit.wasabi.assignmentobjects;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class UserTest {

    private User.ID userID;

    public UserTest() {
        userID = User.ID.valueOf("1234567890");
    }

    @Test
    public void testUserID() {
        assertThat(userID.hashCode(), is(userID.hashCode()));
        assertNotNull(userID.toString());
    }

    @Test
    public void testUserIDFromOther() {
        User.ID userID1 = User.ID.valueOf("abcdefg");
        assertEquals(userID1, userID1);
        assertTrue(!userID1.equals(new Object()));
        assertNotNull(userID);
        assertTrue(!userID1.equals(userID));
    }

}
