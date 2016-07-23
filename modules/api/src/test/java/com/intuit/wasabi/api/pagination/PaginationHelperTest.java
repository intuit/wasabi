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
package com.intuit.wasabi.api.pagination;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link PaginationHelper}.
 */
public class PaginationHelperTest {

    @Test
    public void testFromIndex() throws Exception {
        PaginationHelper paginationHelper = new PaginationHelper<>(null, null);
        // invalid: just return 0
        // parameters
        assertEquals(0, paginationHelper.fromIndex(0, 1, 2));
        assertEquals(0, paginationHelper.fromIndex(1, 0, 2));
        assertEquals(0, paginationHelper.fromIndex(-1, 1, 2));
        assertEquals(0, paginationHelper.fromIndex(1, -1, 2));
        assertEquals(0, paginationHelper.fromIndex(1, 1, -1));
        // impossible pages
        assertEquals(0, paginationHelper.fromIndex(20, 10, 5));
        assertEquals(0, paginationHelper.fromIndex(2, 10, 10));
        assertEquals(0, paginationHelper.fromIndex(2, 100, 100));

        // valid
        assertEquals(0, paginationHelper.fromIndex(1, 10, 2));
        assertEquals(10, paginationHelper.fromIndex(2, 10, 11));
        assertEquals(264, paginationHelper.fromIndex(23, 12, 1230));
    }

    @Test
    public void testToIndex() throws Exception {
        PaginationHelper paginationHelper = new PaginationHelper<>(null, null);
        // invalid: just return 0
        // parameters
        assertEquals(0, paginationHelper.toIndex(0, 1, 2));
        assertEquals(0, paginationHelper.toIndex(1, 0, 2));
        assertEquals(0, paginationHelper.toIndex(-1, 1, 2));
        assertEquals(0, paginationHelper.toIndex(1, -1, 2));
        assertEquals(0, paginationHelper.toIndex(1, 1, -1));
        // impossible pages
        assertEquals(0, paginationHelper.toIndex(20, 10, 5));
        assertEquals(0, paginationHelper.toIndex(2, 10, 10));
        assertEquals(0, paginationHelper.toIndex(2, 100, 100));

        // valid
        assertEquals(2, paginationHelper.toIndex(1, 10, 2));
        assertEquals(11, paginationHelper.toIndex(2, 10, 11));
        assertEquals(276, paginationHelper.toIndex(23, 12, 1230));
    }


}
