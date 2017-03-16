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
package com.intuit.wasabi.api.pagination.comparators;

import com.intuit.wasabi.exceptions.PaginationException;
import org.junit.Assert;
import org.junit.Test;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Tests for {@link PaginationComparator}.
 */
public class PaginationComparatorTest {

    private Object testObject1 = new Object() {
        @Override
        public String toString() {
            return "toString";
        }

        @Override
        public int hashCode() {
            return 1234;
        }
    };

    private Object testObject2 = new Object() {
        @Override
        public String toString() {
            return "toString";
        }

        @Override
        public int hashCode() {
            return 2345;
        }
    };

    public static class TestObjectComparator extends PaginationComparator<Object> {
        @Override
        public int compare(Object left, Object right) {
            return super.compare(left, right, Property.class);
        }

        private static String nullPointerLambda1234(Object object) {
            if (1234 == object.hashCode()) {
                throw new NullPointerException("This simulates null pointer accesses.");
            }
            return object.toString();
        }

        private static String nullPointerLambda2345(Object object) {
            if (2345 == object.hashCode()) {
                throw new NullPointerException("This simulates null pointer accesses.");
            }
            return object.toString();
        }

        private enum Property implements PaginationComparatorProperty<Object> {
            string(Object::toString, String::compareToIgnoreCase),
            hash(Object::hashCode, Integer::compareTo),
            alwaysnpeforhash1234(TestObjectComparator::nullPointerLambda1234,
                    (ignored, ignored2) -> 0),
            alwaysnpeforhash2345(TestObjectComparator::nullPointerLambda2345,
                    (ignored, ignored2) -> 0);

            private final Function<Object, ?> propertyExtractor;
            private final BiFunction<?, ?, Integer> comparisonFunction;

            <T> Property(Function<Object, T> propertyExtractor, BiFunction<T, T, Integer> comparisonFunction) {
                this.propertyExtractor = propertyExtractor;
                this.comparisonFunction = comparisonFunction;
            }

            @Override
            public Function<Object, ?> getPropertyExtractor() {
                return propertyExtractor;
            }

            @Override
            public BiFunction<?, ?, Integer> getComparisonFunction() {
                return comparisonFunction;
            }
        }
    }

    @Test
    public void testReplaceSortOrder() throws Exception {
        PaginationComparator<Object> objectComparator = new TestObjectComparator();

        PaginationComparator<Object> objectComparatorHandle = objectComparator.replaceSortorder("-newSort,otherSort");
        Assert.assertEquals("SortOrder was not updated.", "-newSort,otherSort", objectComparator.getSortOrder());
        Assert.assertEquals("The returned object should have the same hash.", objectComparator.hashCode(), objectComparatorHandle.hashCode());

        objectComparator.replaceSortorder(null);
        Assert.assertEquals("SortOrder was not reset.", "", objectComparator.getSortOrder());
    }

    @Test
    public void testCompare() throws Exception {
        TestObjectComparator objectComparator = new TestObjectComparator();

        Assert.assertEquals("1 & 2 test, empty sort order", 0,
                objectComparator.compare(testObject1, testObject2));
        Assert.assertEquals("2 & 1 test, empty sort order", 0,
                objectComparator.compare(testObject2, testObject1));

        objectComparator.replaceSortorder("string");
        Assert.assertEquals("Identity test, string", 0,
                objectComparator.compare(testObject1, testObject1));
        Assert.assertEquals("1 & 2 test, string", 0,
                objectComparator.compare(testObject1, testObject2));
        Assert.assertEquals("2 & 1 test, string", 0,
                objectComparator.compare(testObject2, testObject1));

        objectComparator.replaceSortorder("hash");
        Assert.assertEquals("Identity test, hash", 0,
                objectComparator.compare(testObject1, testObject1));
        Assert.assertEquals("1 & 2 test, hash", -1,
                objectComparator.compare(testObject1, testObject2));
        Assert.assertEquals("2 & 1 test, hash", 1,
                objectComparator.compare(testObject2, testObject1));

        objectComparator.replaceSortorder("-hash");
        Assert.assertEquals("Identity test, -hash", 0,
                objectComparator.compare(testObject1, testObject1));
        Assert.assertEquals("1 & 2 test, -hash", 1,
                objectComparator.compare(testObject1, testObject2));
        Assert.assertEquals("2 & 1 test, -hash", -1,
                objectComparator.compare(testObject2, testObject1));

        objectComparator.replaceSortorder("string,hash");
        Assert.assertEquals("Identity test, string,hash", 0,
                objectComparator.compare(testObject1, testObject1));
        Assert.assertEquals("1 & 2 test, string,hash", -1,
                objectComparator.compare(testObject1, testObject2));
        Assert.assertEquals("2 & 1 test, string,hash", 1,
                objectComparator.compare(testObject2, testObject1));

        objectComparator.replaceSortorder("string,-hash");
        Assert.assertEquals("Identity test, string,-hash", 0,
                objectComparator.compare(testObject1, testObject1));
        Assert.assertEquals("1 & 2 test, string,-hash", 1,
                objectComparator.compare(testObject1, testObject2));
        Assert.assertEquals("2 & 1 test, string,-hash", -1,
                objectComparator.compare(testObject2, testObject1));

        try {
            objectComparator.replaceSortorder("noSortOrder");
            Assert.assertEquals("Identity test, noSortOrder", 0,
                    objectComparator.compare(testObject1, testObject1));
        } catch (PaginationException ignored) {
            // this should happen
        } catch (Exception exception) {
            Assert.fail("Failed due to wrong exception: " + exception.getClass());
        }
    }

    @Test
    public void testCompareByProperty() throws Exception {
        TestObjectComparator objectComparator = new TestObjectComparator();

        Assert.assertEquals("Identity test, toString, asc", 0,
                objectComparator.compareByProperty(testObject1, testObject1,
                        Object::toString, String::compareToIgnoreCase, false)
        );
        Assert.assertEquals("Identity test, toString, desc", 0,
                objectComparator.compareByProperty(testObject1, testObject1,
                        Object::toString, String::compareToIgnoreCase, true)
        );

        Assert.assertEquals("1 & 2 test, toString, asc", 0,
                objectComparator.compareByProperty(testObject1, testObject2,
                        Object::toString, String::compareToIgnoreCase, false)
        );
        Assert.assertEquals("1 & 2 test, toString, desc", 0,
                objectComparator.compareByProperty(testObject1, testObject2,
                        Object::toString, String::compareToIgnoreCase, true)
        );

        Assert.assertEquals("1 & 2 test, hash, asc", -1,
                objectComparator.compareByProperty(testObject1, testObject2,
                        Object::hashCode, Integer::compareTo, false)
        );
        Assert.assertEquals("1 & 2 test, hash, desc", 1,
                objectComparator.compareByProperty(testObject1, testObject2,
                        Object::hashCode, Integer::compareTo, true)
        );

        Assert.assertEquals("1 & 2 test, npe left, asc", 1,
                objectComparator.compareByProperty(testObject1, testObject2,
                        TestObjectComparator::nullPointerLambda1234, String::compareToIgnoreCase, false)
        );
        Assert.assertEquals("1 & 2 test, npe left, desc", 1,
                objectComparator.compareByProperty(testObject1, testObject2,
                        TestObjectComparator::nullPointerLambda1234, String::compareToIgnoreCase, true)
        );
        Assert.assertEquals("1 & 2 test, npe right, asc", -1,
                objectComparator.compareByProperty(testObject1, testObject2,
                        TestObjectComparator::nullPointerLambda2345, String::compareToIgnoreCase, false)
        );
        Assert.assertEquals("1 & 2 test, npe right, desc", -1,
                objectComparator.compareByProperty(testObject1, testObject2,
                        TestObjectComparator::nullPointerLambda2345, String::compareToIgnoreCase, true)
        );
    }

    @Test
    public void testCompareNull() throws Exception {
        TestObjectComparator objectComparator = new TestObjectComparator();

        Assert.assertEquals("objectComparator.compareNull(null, null, false)",
                0, objectComparator.compareNull(null, null, false));
        Assert.assertEquals("objectComparator.compareNull(new Object(), null, false)",
                -1, objectComparator.compareNull(new Object(), null, false));
        Assert.assertEquals("objectComparator.compareNull(null, new Object(), false)",
                1, objectComparator.compareNull(null, new Object(), false));
        Assert.assertEquals("objectComparator.compareNull(new Object(), new Object(), false)",
                2, objectComparator.compareNull(new Object(), new Object(), false));

        Assert.assertEquals("objectComparator.compareNull(null, null, true)",
                0, objectComparator.compareNull(null, null, true));
        Assert.assertEquals("objectComparator.compareNull(new Object(), null, true)",
                1, objectComparator.compareNull(new Object(), null, true));
        Assert.assertEquals("objectComparator.compareNull(null, new Object(), true)",
                -1, objectComparator.compareNull(null, new Object(), true));
        Assert.assertEquals("objectComparator.compareNull(new Object(), new Object(), true)",
                2, objectComparator.compareNull(new Object(), new Object(), true));
    }
}
