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

import com.intuit.wasabi.api.pagination.comparators.PaginationComparatorTest;
import com.intuit.wasabi.api.pagination.filters.PaginationFilterTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Test
    public void testPaginate() throws Exception {
        Object[][] objectDefinitions = new Object[][]{
                {"A", 22},
                {"B", 23},
                {"Z", 10},
                {"D", 12},
                {"E", 13},
                {"C", 42},
                {"Z", 11},
                {"E", 12},
                {"X", 35},
                {"X", 35}
        };

        List<Object> objectList = new ArrayList<>();
        for (Object[] objectDefinition : objectDefinitions) {
            objectList.add(new Object() {
                @Override
                public String toString() {
                    return (String) objectDefinition[0];
                }

                @Override
                public int hashCode() {
                    return (int) objectDefinition[1];
                }
            });
        }

        class TestCase {
            private final String filter;
            private final String sort;
            private final int page;
            private final int perPage;
            private final int total;
            private final Integer[] expectedOrder;

            private TestCase(String filter, String sort, int page, int perPage, int total, Integer[] expectedOrder) {
                this.filter = filter;
                this.sort = sort;
                this.page = page;
                this.perPage = perPage;
                this.total = total;
                this.expectedOrder = expectedOrder;
            }

            public String toString() {
                return "TestCase(" + filter + ", " + sort + ", " + page + ", " + perPage + ", " + total + ")";
            }
        }

        List<TestCase> testCaseList = new ArrayList<>();
        testCaseList.add(new TestCase("", "", 1, 2, 10, new Integer[]{0, 1}));
        testCaseList.add(new TestCase("", "", 2, 2, 10, new Integer[]{2, 3}));
        testCaseList.add(new TestCase("", "", 5, 2, 10, new Integer[]{8, 9}));
        testCaseList.add(new TestCase("", "", 0, -1, 10, new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));
        testCaseList.add(new TestCase("", "", 5, 5, 10, new Integer[]{}));
        testCaseList.add(new TestCase("X", "", 1, 2, 2, new Integer[]{8, 9}));
        testCaseList.add(new TestCase("1,string=E", "", 1, 2, 2, new Integer[]{4, 7}));
        testCaseList.add(new TestCase("E,hash=3", "", 1, 2, 1, new Integer[]{4}));
        testCaseList.add(new TestCase("", "string", 1, 5, 10, new Integer[]{0, 1, 5, 3, 4}));
        testCaseList.add(new TestCase("", "-hash", 1, 5, 10, new Integer[]{5, 8, 9, 1, 0}));
        testCaseList.add(new TestCase("E", "hash", 1, 5, 2, new Integer[]{7, 4}));
        testCaseList.add(new TestCase("E", "hash", 1, 1, 2, new Integer[]{7}));
        testCaseList.add(new TestCase("E", "hash", 2, 1, 2, new Integer[]{4}));
        testCaseList.add(new TestCase("1", "string,-hash", 2, 3, 5, new Integer[]{6, 2}));

        PaginationHelper<Object> paginationHelper = new PaginationHelper<>(
                new PaginationFilterTest.TestObjectFilter(),
                new PaginationComparatorTest.TestObjectComparator());

        for (TestCase testCase : testCaseList) {
            List<Object> copyList = new ArrayList<>(objectList.size());
            copyList.addAll(objectList);

            Map<String, Object> result = paginationHelper.paginate("objects", copyList,
                    testCase.filter, "+0000", testCase.sort, testCase.page,
                    testCase.perPage);

            @SuppressWarnings("unchecked")
            Integer[] actualOrder = ((List<Object>) result.get("objects")).stream()
                    .map(objectList::indexOf)
                    .collect(Collectors.toList())
                    .toArray(new Integer[]{});

            Assert.assertArrayEquals("TestCase failed (order): " + testCase,
                    testCase.expectedOrder,
                    actualOrder);

            Assert.assertEquals("TestCase failed (number): " + testCase,
                    testCase.total,
                    result.get("totalEntries"));
        }
    }


}
