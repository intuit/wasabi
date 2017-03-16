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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class EmailLinksListTest {

    @Test
    public void testToStringWithEmptyLinkList() {
        EmailLinksList list = EmailLinksList.newInstance().build();
        assertEquals("", list.toString());
    }

    @Test
    public void testToStringWithOneLinkList() {
        List<String> list = new ArrayList<String>();
        list.add("one");
        EmailLinksList elist = EmailLinksList.withEmailLinksList(list).build();
        assertEquals("one" + EmailLinksList.LINE_SEPARATOR, elist.toString());
    }

    @Test
    public void testToStringWithTwoLinkList() {
        List<String> list = new ArrayList<String>();
        list.add("one");
        list.add("two");

        EmailLinksList elist = EmailLinksList.withEmailLinksList(list).build();
        assertEquals("one" + EmailLinksList.LINE_SEPARATOR + "two" + EmailLinksList.LINE_SEPARATOR, elist.toString());
    }


    //NOTE: this method is created after the code is writen by someone else. Since the exact intent of
    //method is unknown to the test creator, I decided to test it as the code logic is written
    //if we don't intended to return null when build method is called twice, this method can be removed
    @Test
    public void testBuilderBuildMethodCalledTwice() {
        List<String> list = new ArrayList<String>();
        list.add("one");
        list.add("two");

        EmailLinksList.Builder builder = EmailLinksList.withEmailLinksList(list);

        EmailLinksList elist = builder.build();
        assertEquals("one" + EmailLinksList.LINE_SEPARATOR + "two" + EmailLinksList.LINE_SEPARATOR, elist.toString());
        //The second time bulid is called it should be null
        elist = builder.build();
        assertNull(elist);
    }


    @Test
    public void fromAnotherEmailList() {
        EmailLinksList emailLinksList = EmailLinksList.newInstance().build();
        assertThat(emailLinksList.hashCode(), is(630));
        EmailLinksList emailLinksList1 = EmailLinksList.from(emailLinksList).build();
        assertThat(emailLinksList1.hashCode(), is(630));

        assertThat(emailLinksList.equals(null), is(false));
        assertThat(emailLinksList.equals(emailLinksList), is(true));
        assertThat(emailLinksList.equals(""), is(false));

    }
}
