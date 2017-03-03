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
package com.intuit.wasabi.api.pagination.filters;

import com.intuit.wasabi.exceptions.PaginationException;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Tests for {@link PaginationFilter}.
 */
public class PaginationFilterTest {

    private Object testObject = new Object() {
        @Override
        public String toString() {
            return "match this";
        }

        @Override
        public int hashCode() {
            return 1234;
        }
    };

    /**
     * The example TestObjectFilter implementation to test the PaginationFilter more conveniently.
     */
    public static class TestObjectFilter extends PaginationFilter<Object> {
        @Override
        public boolean test(Object object) {
            return super.test(object, Property.class);
        }

        private static String nullPointerLambda(Object ignored) {
            throw new NullPointerException("This simulates null pointer accesses.");
        }

        private enum Property implements PaginationFilterProperty<Object> {
            string(Object::toString, StringUtils::containsIgnoreCase),
            hash(Object::hashCode, (hash, filter) ->
                    StringUtils.containsIgnoreCase(Integer.toString(hash), filter)),
            alwaysnull(TestObjectFilter::nullPointerLambda, (ignored, ignored2) -> true);

            private final Function<Object, ?> propertyExtractor;
            private final BiPredicate<?, String> filterPredicate;

            <T> Property(Function<Object, T> propertyExtractor, BiPredicate<T, String> filterPredicate) {
                this.propertyExtractor = propertyExtractor;
                this.filterPredicate = filterPredicate;
            }

            @Override
            public Function<Object, ?> getPropertyExtractor() {
                return propertyExtractor;
            }

            @Override
            public BiPredicate<?, String> getFilterPredicate() {
                return filterPredicate;
            }
        }
    }

    @Test
    public void testReplaceFilter() throws Exception {
        PaginationFilter<Object> objectFilter = new TestObjectFilter();

        PaginationFilter<Object> objectFilterHandle = objectFilter.replaceFilter("myFilter", "+0400");
        Assert.assertEquals("filter was not updated.", "myFilter", objectFilter.getFilter());
        Assert.assertEquals("timezoneOffset was not updated.", "+0400", objectFilter.getTimeZoneOffset());
        Assert.assertEquals("The returned object should have the same hash.", objectFilter.hashCode(), objectFilterHandle.hashCode());

        objectFilter.replaceFilter("mySecondFilter", "");
        Assert.assertEquals("filter was not updated.", "mySecondFilter", objectFilter.getFilter());
        Assert.assertEquals("timezoneOffset changed.", "+0400", objectFilter.getTimeZoneOffset());

        objectFilter.replaceFilter(null, null);
        Assert.assertEquals("filter was not reset.", "", objectFilter.getFilter());
        Assert.assertEquals("timezoneOffset was not reset.", "+0000", objectFilter.getTimeZoneOffset());
    }

    @Test
    public void testTest() throws Exception {
        TestObjectFilter objectFilter = new TestObjectFilter();

        HashMap<String, Boolean> testCases = new HashMap<>();
        testCases.put("", true);
        testCases.put(null, true);
        testCases.put("no match, fulltext", false);
        testCases.put("match", true);
        testCases.put("no match fulltext,hash=23", false);
        testCases.put("no match, fulltext,hash=23", false);
        testCases.put("match,hash=no match", false);
        testCases.put(",hash=no match", false);
        testCases.put("23", true);
        testCases.put("hash=34", true);
        testCases.put("hash=no match", false);
        testCases.put("hash=no match,moreKeys=values", false);
        testCases.put("fulltext,hash=no match,moreKeys=values", false);
        testCases.put("fulltext, more fulltext,hash=no match,moreKeys=values", false);

        for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
            objectFilter.replaceFilter(testCase.getKey(), "");
            try {
                Assert.assertEquals("Case handled incorrectly: " + testCase.getKey(),
                        testCase.getValue(),
                        objectFilter.test(testObject, TestObjectFilter.Property.class));
            } catch (Exception exception) {
                Assert.fail("Failed due to exception: " + exception.getMessage() + " - Test was: " + testCase.getKey() + " -> " + testCase.getValue());
            }
        }
    }

    @Test
    public void testTestFields() throws Exception {
        TestObjectFilter objectFilter = new TestObjectFilter();

        HashMap<String, Boolean> testCases = new HashMap<>();
        testCases.put("", true);
        testCases.put(null, true);
        testCases.put("hash=34", true);
        testCases.put("hash=23,string=match", true);
        testCases.put("hash=no match", false);
        testCases.put("hash=23,string=no match", false);
        testCases.put("hash=no match,string=match", false);
        testCases.put("hash=no match,string=no match", false);
        testCases.put("hash=no match,moreKeys=values", false); // since we fail fast, this does not throw!

        for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
            objectFilter.replaceFilter(testCase.getKey(), "");
            try {
                Assert.assertEquals("Case handled incorrectly: " + testCase.getKey(),
                        testCase.getValue(),
                        objectFilter.testFields(testObject, TestObjectFilter.Property.class));
            } catch (Exception exception) {
                Assert.fail("Failed due to exception: " + exception.getMessage()
                        + " - Test was: " + testCase.getKey() + " -> " + testCase.getValue());
            }
        }

        testCases.clear();
        testCases.put("moreKeys=values,hash=no match", false);
        testCases.put("hash=23,moreKeys=values", false);
        testCases.put("moreKeys=values,hash=23", false);

        for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
            objectFilter.replaceFilter(testCase.getKey(), "");
            try {
                objectFilter.testFields(testObject, TestObjectFilter.Property.class);
                Assert.fail("Failed, no exception was thrown - Test was: "
                        + testCase.getKey() + " -> " + testCase.getValue());
            } catch (PaginationException ignored) {
                // this should happen
            } catch (Exception exception) {
                Assert.fail("Failed due to wrong exception: " + exception.getClass()
                        + " = Test was: " + testCase.getKey() + " -> " + testCase.getValue());
            }
        }
    }

    @Test
    public void testTestFulltext() throws Exception {
        TestObjectFilter objectFilter = new TestObjectFilter();

        objectFilter.replaceFilter("ch th", "");
        Assert.assertTrue("Did not find 'ch th'.",
                objectFilter.testFulltext(testObject, TestObjectFilter.Property.class));

        objectFilter.replaceFilter("123", "");
        Assert.assertTrue("Did not find '123'.",
                objectFilter.testFulltext(testObject, TestObjectFilter.Property.class));

        objectFilter.replaceFilter("no match", "");
        Assert.assertFalse("Did match 'no match'",
                objectFilter.testFulltext(testObject, TestObjectFilter.Property.class));
    }

    @Test
    public void testFilterByProperty() throws Exception {
        TestObjectFilter objectFilter = new TestObjectFilter();

        Assert.assertFalse("Matched 'no match'!",
                objectFilter.filterByProperty(testObject, "no match",
                        Object::toString, String::contains));

        Assert.assertFalse("Matched 'null'!",
                objectFilter.filterByProperty(testObject, "match",
                        ignored -> null, String::contains));

        Assert.assertTrue("Didn't match 'match'!",
                objectFilter.filterByProperty(testObject, "match",
                        Object::toString, String::contains));

        Assert.assertFalse("Match an intermediate null pointer!",
                objectFilter.filterByProperty(testObject, "no match",
                        TestObjectFilter::nullPointerLambda, String::contains));
    }

    @Test
    public void testModifyFilterForKey() throws Exception {
        TestObjectFilter objectFilter = new TestObjectFilter();
        objectFilter.registerFilterModifierForProperties(FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET,
                TestObjectFilter.Property.string);

        Assert.assertEquals("Did not modify string!",
                FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET.apply("timezone", objectFilter),
                objectFilter.modifyFilterForKey(TestObjectFilter.Property.string, "timezone"));
        Assert.assertEquals("Did modify hash!",
                "",
                objectFilter.modifyFilterForKey(TestObjectFilter.Property.hash, ""));
    }

    @Test
    public void testGetKeyValuePartOfFilter() throws Exception {
        TestObjectFilter objectFilter = new TestObjectFilter();

        HashMap<String, String> testCases = new HashMap<>();
        testCases.put("", "");
        testCases.put(null, "");
        testCases.put("no match, fulltext", "");
        testCases.put("match", "");
        testCases.put("no match fulltext,hash=23", "hash=23");
        testCases.put("no match, fulltext,hash=23", "hash=23");
        testCases.put("match,hash=no match", "hash=no match");
        testCases.put(",hash=no match", "hash=no match");
        testCases.put("23", "");
        testCases.put("hash=34", "hash=34");
        testCases.put("hash=no match", "hash=no match");
        testCases.put("hash=no match,moreKeys=values", "hash=no match,moreKeys=values");
        testCases.put("fulltext,hash=no match,moreKeys=values", "hash=no match,moreKeys=values");
        testCases.put("fulltext, more fulltext,hash=no match,moreKeys=values", "hash=no match,moreKeys=values");

        for (Map.Entry<String, String> testCase : testCases.entrySet()) {
            try {
                Assert.assertEquals("Case handled incorrectly: " + testCase.getKey(),
                        testCase.getValue(),
                        objectFilter.getKeyValuePartOfFilter(testCase.getKey()));
            } catch (Exception exception) {
                Assert.fail("Failed due to exception: " + exception.getClass()
                        + " - Test was: " + testCase.getKey() + " -> " + testCase.getValue());
            }
        }
    }

    @Test
    public void testGetFulltextPartOfFilter() throws Exception {
        TestObjectFilter objectFilter = new TestObjectFilter();

        HashMap<String, String> testCases = new HashMap<>();
        testCases.put("", "");
        testCases.put(null, "");
        testCases.put("no match, fulltext", "no match, fulltext");
        testCases.put("match", "match");
        testCases.put("no match fulltext,hash=23", "no match fulltext");
        testCases.put("no match, fulltext,hash=23", "no match, fulltext");
        testCases.put("match,hash=no match", "match");
        testCases.put(",hash=no match", "");
        testCases.put("23", "23");
        testCases.put("hash=34", "");
        testCases.put("hash=no match", "");
        testCases.put("hash=no match,moreKeys=values", "");
        testCases.put("fulltext,hash=no match,moreKeys=values", "fulltext");
        testCases.put("fulltext, more fulltext,hash=no match,moreKeys=values", "fulltext, more fulltext");

        for (Map.Entry<String, String> testCase : testCases.entrySet()) {
            try {
                Assert.assertEquals("Case handled incorrectly: " + testCase.getKey(),
                        testCase.getValue(),
                        objectFilter.getFulltextPartOfFilter(testCase.getKey()));
            } catch (Exception exception) {
                Assert.fail("Failed due to exception: " + exception.getMessage()
                        + " - Test was: " + testCase.getKey() + " -> " + testCase.getValue());
            }
        }
    }

    @Test
    public void testRegisterFilterModifierForProperties() throws Exception {
        TestObjectFilter objectFilter1 = new TestObjectFilter();
        objectFilter1.registerFilterModifierForProperties(FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET,
                TestObjectFilter.Property.hash);
        Assert.assertTrue("Modifier not correctly added (objectFilter1).",
                objectFilter1.filterModifiers.containsKey(TestObjectFilter.Property.hash));
        Assert.assertEquals("Modifier not correct (objectFilter1).",
                FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET,
                objectFilter1.filterModifiers.get(TestObjectFilter.Property.hash));
        Assert.assertEquals("Incorrect number of modifiers (objectFilter1).",
                1,
                objectFilter1.filterModifiers.size());

        TestObjectFilter objectFilter2 = new TestObjectFilter();
        objectFilter2.registerFilterModifierForProperties(FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET,
                TestObjectFilter.Property.hash, TestObjectFilter.Property.string);
        Assert.assertEquals("Modifier not correct (objectFilter2, hash).",
                FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET,
                objectFilter2.filterModifiers.get(TestObjectFilter.Property.hash));
        Assert.assertEquals("Modifier not correct (objectFilter2, string).",
                FilterUtil.FilterModifier.APPEND_TIMEZONEOFFSET,
                objectFilter2.filterModifiers.get(TestObjectFilter.Property.string));
        Assert.assertEquals("Incorrect number of modifiers (objectFilter2).",
                2,
                objectFilter2.filterModifiers.size());

    }

    @Test
    public void testExcludeFromFulltext() throws Exception {
        TestObjectFilter objectFilter = new TestObjectFilter();
        objectFilter.excludeFromFulltext(TestObjectFilter.Property.string);

        objectFilter.replaceFilter("match", "");
        Assert.assertFalse("Did match 'match' although the field was excluded.",
                objectFilter.testFulltext(testObject, TestObjectFilter.Property.class));

        objectFilter.replaceFilter("123", "");
        Assert.assertTrue("Did not find '123' with excluded string.",
                objectFilter.testFulltext(testObject, TestObjectFilter.Property.class));
    }

    @Test
    public void testGetTimeZoneOffset() throws Exception {
        PaginationFilter<Object> objectFilter = new TestObjectFilter();

        Assert.assertEquals("Default timezoneOffset is not correct.", objectFilter.getTimeZoneOffset(), "+0000");

        objectFilter.replaceFilter("", "-0700");
        Assert.assertEquals("New timezoneOffset is not correct.", objectFilter.getTimeZoneOffset(), "-0700");
    }

    @Test
    public void testGetFilter() throws Exception {
        PaginationFilter<Object> objectFilter = new TestObjectFilter();

        Assert.assertEquals("Default filter is not correct.", objectFilter.getFilter(), "");

        objectFilter.replaceFilter("myFilter", "");
        Assert.assertEquals("New filter is not correct.", objectFilter.getFilter(), "myFilter");
    }

}
