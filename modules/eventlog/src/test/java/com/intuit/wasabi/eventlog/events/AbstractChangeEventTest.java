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
package com.intuit.wasabi.eventlog.events;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link AbstractChangeEvent} and a test method for subclasses.
 */
public class AbstractChangeEventTest {

    @Test
    public void testAbstractChangeEvent() throws Exception {
        String[] invalidPropertyNames = {null, "", " ", "\t", "\n"};
        for (String invalidPropertyName : invalidPropertyNames) {
            try {
                new AbstractChangeEvent(null, invalidPropertyName, null, null) {
                    @Override
                    public String getDefaultDescription() {
                        return null;
                    }
                };
                Assert.fail("AbstractChangeEvent must not allow the propertyName to be " + invalidPropertyName + "!");
            } catch (IllegalArgumentException ignored) {
            }
        }

        AbstractChangeEvent ace = new AbstractChangeEvent(null, "label", null, null) {
            @Override
            public String getDefaultDescription() {
                return "ACE";
            }
        };

        Assert.assertEquals("label", ace.getPropertyName());
        Assert.assertEquals(null, ace.getBefore());
        Assert.assertEquals(null, ace.getAfter());
        Assert.assertEquals("ACE", ace.getDefaultDescription());

        AbstractChangeEvent aceComplete = new AbstractChangeEvent(null, "label", "gren", "green") {
            @Override
            public String getDefaultDescription() {
                return "ACE";
            }
        };

        testValidSystemEvent(aceComplete, "label", "gren", "green");

    }

    /**
     * Tests an event for its for its inherited features (that means if the properties are passed on correctly).
     * Assumes creationTime to be in [now-3s, now+3s], and that the event is caused by the System user. Does not check
     * the description.
     *
     * @param event The event to check.
     * @param expectedPropertyName the expected property name
     * @param expectedBefore the expected before-value
     * @param expectedAfter the expected after-value
     */
    public static void testValidSystemEvent(AbstractChangeEvent event, String expectedPropertyName, String expectedBefore, String expectedAfter) {
        Assert.assertEquals(expectedPropertyName, event.getPropertyName());
        Assert.assertEquals(expectedBefore, event.getBefore());
        Assert.assertEquals(expectedAfter, event.getAfter());

        AbstractEventTest.testValidSystemEvent(event);
    }
}
