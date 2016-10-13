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
package com.intuit.wasabi.experimentobjects;

import com.intuit.wasabi.exceptions.InvalidIdentifierException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * This tests the {@link Page} class.
 */
public class PageTest{

    private Page.Name pageName = Page.Name.valueOf("testPage");

    private Page page = Page.withName(pageName).build();

    @Test
    public void testPageCreation() {
        assertEquals(page.getName(),Page.Name.valueOf("testPage"));
        page.setName(pageName);
        assertEquals(page.getName(), pageName);
        assertEquals("Page[name=testPage]",page.toString());
    }

    @Test
    public void testEquals(){
        assertTrue(page.equals(page));
        assertFalse(page.equals(null));
        assertFalse(page.equals(42));


        assertTrue(pageName.equals(pageName));
        assertFalse(pageName.equals(null));
        assertFalse(pageName.equals(42));

        assertEquals(page.hashCode(), page.hashCode());
        assertEquals(pageName.hashCode(),pageName.hashCode());
    }

    @Test
    public void testNameLength() {
        String name = "aNameThatIsShort Enough";
        assertEquals(name, Page.Name.valueOf(name).toString());

        StringBuffer outputBuffer = new StringBuffer();
        for (int i = 0; i < 257; i++){
            outputBuffer.append("a");
        }
        name = outputBuffer.toString();

        try{
            Page.Name.valueOf(name);
            fail();
        }catch (InvalidIdentifierException e){
            // meant to happen for names longer than 256 characters
        }
    }



}
