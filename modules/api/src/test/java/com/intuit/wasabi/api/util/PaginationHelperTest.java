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
package com.intuit.wasabi.api.util;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link PaginationHelper}
 */
public class PaginationHelperTest {

    @Test
    public void testFromIndex() throws Exception {

        // invalid: just return 0
        // parameters
        assertEquals(0, PaginationHelper.fromIndex(0, 1, 2));
        assertEquals(0, PaginationHelper.fromIndex(1, 0, 2));
        assertEquals(0, PaginationHelper.fromIndex(-1, 1, 2));
        assertEquals(0, PaginationHelper.fromIndex(1, -1, 2));
        assertEquals(0, PaginationHelper.fromIndex(1, 1, -1));
        // impossible pages
        assertEquals(0, PaginationHelper.fromIndex(20, 10, 5));
        assertEquals(0, PaginationHelper.fromIndex(2, 10, 10));
        assertEquals(0, PaginationHelper.fromIndex(2, 100, 100));

        // valid
        assertEquals(0, PaginationHelper.fromIndex(1, 10, 2));
        assertEquals(10, PaginationHelper.fromIndex(2, 10, 11));
        assertEquals(264, PaginationHelper.fromIndex(23, 12, 1230));
    }


    @Test
    public void testToIndex() throws Exception {
        // invalid: just return 0
        // parameters
        assertEquals(0, PaginationHelper.toIndex(0, 1, 2));
        assertEquals(0, PaginationHelper.toIndex(1, 0, 2));
        assertEquals(0, PaginationHelper.toIndex(-1, 1, 2));
        assertEquals(0, PaginationHelper.toIndex(1, -1, 2));
        assertEquals(0, PaginationHelper.toIndex(1, 1, -1));
        // impossible pages
        assertEquals(0, PaginationHelper.toIndex(20, 10, 5));
        assertEquals(0, PaginationHelper.toIndex(2, 10, 10));
        assertEquals(0, PaginationHelper.toIndex(2, 100, 100));

        // valid
        assertEquals(2, PaginationHelper.toIndex(1, 10, 2));
        assertEquals(11, PaginationHelper.toIndex(2, 10, 11));
        assertEquals(276, PaginationHelper.toIndex(23, 12, 1230));
    }

    @Test
    public void testNumberOfPages() throws Exception {
        // invalid: just return 1
        assertEquals(1, PaginationHelper.numberOfPages(5, 10));
        assertEquals(1, PaginationHelper.numberOfPages(-3, 32));
        assertEquals(1, PaginationHelper.numberOfPages(0, 0));
        assertEquals(1, PaginationHelper.numberOfPages(23, -1));

        // valid
        assertEquals(1, PaginationHelper.numberOfPages(10, 10));
        assertEquals(12, PaginationHelper.numberOfPages(119, 10));
        assertEquals(12, PaginationHelper.numberOfPages(120, 10));
        assertEquals(13, PaginationHelper.numberOfPages(121, 10));
        assertEquals(189, PaginationHelper.numberOfPages(4325, 23));
    }

    @Test
    public void testLinkHeaderValue() throws Exception {
        String lh = PaginationHelper.linkHeaderValue("/logs/test", 100, 3, 5, 1, new HashMap<String, String>());

        assertEquals("</api/v1/logs/test?per_page=5&page=1>; rel=\"First\", </api/v1/logs/test?per_page=5&page=20>; rel=\"Last\", </api/v1/logs/test?per_page=5&page=2>; rel=\"Previous\", </api/v1/logs/test?per_page=5&page=2>; rel=\"Page 2\", </api/v1/logs/test?per_page=5&page=4>; rel=\"Next\", </api/v1/logs/test?per_page=5&page=4>; rel=\"Page 4\"", lh);
    }

    @Test
    public void testPrepareDateFilter() throws Exception {
        assertEquals("", PaginationHelper.prepareDateFilter("", ""));
        assertEquals("filterMask", PaginationHelper.prepareDateFilter("filterMask", ""));
        assertEquals("filterMask,time", PaginationHelper.prepareDateFilter("filterMask,time", ""));
        assertEquals("filterMask,time={+0100}", PaginationHelper.prepareDateFilter("filterMask,time", "+0100"));
        assertEquals("filterMask,time={+0100}", PaginationHelper.prepareDateFilter("filterMask", "+0100"));
        assertEquals("filterMask,time={+0100}", PaginationHelper.prepareDateFilter("filterMask,time=", "+0100"));
        assertEquals("filterMask,time={+0100}Sep 22", PaginationHelper.prepareDateFilter("filterMask,time=Sep 22", "+0100"));
        assertEquals("filterMask,time={+0200}", PaginationHelper.prepareDateFilter("filterMask,time={+0200}", "+0100"));
        assertEquals("filterMask,time={+0200}Sep 22", PaginationHelper.prepareDateFilter("filterMask,time={+0200}Sep 22", "+0100"));
        assertEquals("filterMask,time,action=ed exp", PaginationHelper.prepareDateFilter("filterMask,time,action=ed exp", ""));
        assertEquals("filterMask,time={+0100},action=ed exp", PaginationHelper.prepareDateFilter("filterMask,time,action=ed exp", "+0100"));
        assertEquals("filterMask,action=ed exp,time={+0100}", PaginationHelper.prepareDateFilter("filterMask,action=ed exp", "+0100"));
        assertEquals("filterMask,time={+0100},action=ed exp", PaginationHelper.prepareDateFilter("filterMask,time=,action=ed exp", "+0100"));
        assertEquals("filterMask,time={+0100}Sep 22,action=ed exp", PaginationHelper.prepareDateFilter("filterMask,time=Sep 22,action=ed exp", "+0100"));
        assertEquals("filterMask,time={+0200},action=ed exp", PaginationHelper.prepareDateFilter("filterMask,time={+0200},action=ed exp", "+0100"));
        assertEquals("filterMask,time={+0200}Sep 22,action=ed exp", PaginationHelper.prepareDateFilter("filterMask,time={+0200}Sep 22,action=ed exp", "+0100"));

    }
}