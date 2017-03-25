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
package com.intuit.wasabi.tests.library.util;

import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;
import com.intuit.wasabi.tests.model.ModelItem;
import org.slf4j.Logger;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides static assertion methods.
 */
public class ModelAssert {

    private static final Logger LOGGER = getLogger(ModelAssert.class);

    /**
     * Writes an info log about this assertion strategy.
     *
     * @param actual     the actual model item
     * @param expected   the expected model item
     * @param strategy   the serialization strategy (can be null)
     * @param equalItems if false, the items must not be equal
     */
    private static void logInfo(Object actual, Object expected, SerializationStrategy strategy, boolean equalItems) {
        String logString = String.format("Comparing actual %s to expected %s (must be "
                        + (equalItems ? "" : "un") + "equal):\n\t%s\n\t%s",
                actual.getClass().getSimpleName(),
                expected.getClass().getSimpleName(),
                actual,
                expected);
        if (expected instanceof ModelItem) {
            logString += String.format("\n\tStrategy: %s",
                    (strategy == null ? ((ModelItem) expected).getSerializationStrategy() : strategy));
        }
        LOGGER.info(logString);
    }

    /**
     * Asserts that two ModelItems have the same values for all their attributes except the excluded ones.
     * <p>
     * The serialization strategy of the {@code expected} ModelItems is used for this assertion.
     *
     * @param actual   the actual ModelItem
     * @param expected the expected ModelItem
     */
    public static void assertEqualModelItems(ModelItem actual, ModelItem expected) {
        assertEqualModelItems(actual, expected, null);
    }

    /**
     * Asserts that two ModelItems have the same values for all their attributes except the excluded ones.
     * <p>
     * If the serialization strategy is not {@code null}, the serialization strategy of the ModelItems is replaced
     * for this assertion, but restored afterwards.
     *
     * @param actual       the actual ModelItem
     * @param expected     the expected ModelItem
     * @param tempStrategy the (temporary) serialization strategy
     */
    public static void assertEqualModelItems(ModelItem actual, ModelItem expected, SerializationStrategy tempStrategy) {
        assertEqualModelItems(actual, expected, tempStrategy, true);
    }

    /**
     * Asserts that two ModelItems have the same values for all their attributes except the excluded ones.
     * <p>
     * If the serialization strategy is not {@code null}, the serialization strategy of the ModelItems is replaced
     * for this assertion, but restored afterwards.
     *
     * @param actual       the actual ModelItem
     * @param expected     the expected ModelItem
     * @param tempStrategy the (temporary) serialization strategy
     * @param equalItems   if false, the items must not be equal
     */
    public static void assertEqualModelItems(ModelItem actual, ModelItem expected, SerializationStrategy tempStrategy, boolean equalItems) {
        logInfo(actual, expected, tempStrategy, equalItems);
        if (expected == null) {
            if (equalItems) {
                Assert.assertNull(actual, "actual is not null");
            } else {
                Assert.assertNotNull(actual, "actual is null");
            }
            return;
        }

        if (equalItems) {
            Assert.assertNotNull(actual, "actual is null");
        } else {
            if (actual == null) {
                Assert.assertTrue(true);
                return;
            }
        }

        SerializationStrategy serializationStrategy = expected.getSerializationStrategy();
        try {
            if (tempStrategy != null) {
                expected.setSerializationStrategy(tempStrategy);
            }

            if (equalItems) {
                Assert.assertEquals(actual, expected, "The two tested items are not equal.");
            } else {
                Assert.assertNotEquals(actual, expected, "The two tested items are equal.");
            }
        } finally {
            if (tempStrategy != null) {
                expected.setSerializationStrategy(serializationStrategy);
            }
        }
    }

    /**
     * Asserts that two lists of ModelItems are equal (in order and per ModelItem).
     * <p>
     * Uses the {@link SerializationStrategy} of each item in {@code expected}.
     *
     * @param actual   list one
     * @param expected list two
     */
    public static void assertEqualModelItems(List<? extends ModelItem> actual, List<? extends ModelItem> expected) {
        assertEqualModelItems(actual, expected, null);
    }

    /**
     * Asserts that two lists of ModelItems are equal (in order and per ModelItem).
     * <p>
     * Uses the supplied {@link SerializationStrategy} or the one of each item in {@code expected} if
     * {@code tempStrategy} is {@code null}.
     * {@link #assertEqualModelItems(ModelItem, ModelItem, SerializationStrategy)} for more info.
     *
     * @param actual       list one
     * @param expected     list two
     * @param tempStrategy the (temporary) serialization strategy
     */
    public static void assertEqualModelItems(List<? extends ModelItem> actual, List<? extends ModelItem> expected, SerializationStrategy tempStrategy) {
        assertEqualModelItems(actual, expected, tempStrategy, true);
    }

    /**
     * Asserts that two lists of ModelItems are equal (in order and per ModelItem).
     * <p>
     * Uses the supplied {@link SerializationStrategy} or the one of each item in {@code expected} if
     * {@code tempStrategy} is {@code null}.
     * {@link #assertEqualModelItems(ModelItem, ModelItem, SerializationStrategy)} for more info.
     *
     * @param actual       list one
     * @param expected     list two
     * @param tempStrategy the (temporary) serialization strategy
     * @param equalItems   if false, the items must not be equal
     */
    public static void assertEqualModelItems(List<? extends ModelItem> actual, List<? extends ModelItem> expected, SerializationStrategy tempStrategy, boolean equalItems) {
        logInfo(actual, expected, tempStrategy, equalItems);
        if (expected == null) {
            if (equalItems) {
                Assert.assertNull(actual, "actual is not null");
            } else {
                Assert.assertNotNull(actual, "actual is null");
            }
            return;
        }

        if (equalItems) {
            Assert.assertNotNull(actual, "actual is null");
        } else {
            if (actual == null) {
                Assert.assertTrue(true);
                return;
            }
        }

        if (equalItems) {
            Assert.assertEquals(expected.size(), actual.size());
        } else {
            if (expected.size() != actual.size()) {
                Assert.assertTrue(true);
                return;
            }
        }

        for (int i = 0; i < expected.size(); ++i) {
            assertEqualModelItems(actual.get(i), expected.get(i), tempStrategy, equalItems);
        }
    }

    /**
     * Asserts that two lists of ModelItems contain the same elements, thus are equal ignoring the order.
     * <p>
     * Uses the {@link SerializationStrategy} of each item in {@code expected}.
     *
     * @param actual   list one
     * @param expected list two
     */
    public static void assertEqualModelItemsNoOrder(List<? extends ModelItem> actual, List<? extends ModelItem> expected) {
        assertEqualModelItemsNoOrder(actual, expected, null);
    }

    /**
     * Asserts that two lists of ModelItems contain the same elements, thus are equal ignoring the order.
     * <p>
     * It is possible to supply a custom exclusion strategy, otherwise the serialization strategy of the
     * {@code expected} items are used.
     * For lists containing different types of ModelItems, it is recommended to set the appropriate
     * serialization strategies beforehand and simply pass {@code null} or to simply use
     * {@link #assertEqualModelItemsNoOrder(List, List)}.
     *
     * @param actual       list one
     * @param expected     list two
     * @param tempStrategy the (temporary) serialization strategy
     */
    public static void assertEqualModelItemsNoOrder(List<? extends ModelItem> actual, List<? extends ModelItem> expected, SerializationStrategy tempStrategy) {
        assertEqualModelItemsNoOrder(actual, expected, tempStrategy, true);
    }

    /**
     * Asserts that two lists of ModelItems contain the same elements, thus are equal ignoring the order.
     * <p>
     * It is possible to supply a custom exclusion strategy, otherwise the serialization strategy of the
     * {@code expected} items are used.
     * For lists containing different types of ModelItems, it is recommended to set the appropriate
     * serialization strategies beforehand and simply pass {@code null} or to simply use
     * {@link #assertEqualModelItemsNoOrder(List, List)}.
     *
     * @param actual       list one
     * @param expected     list two
     * @param tempStrategy the (temporary) serialization strategy
     * @param equalItems   if false, the items must not be equal
     */
    public static void assertEqualModelItemsNoOrder(List<? extends ModelItem> actual, List<? extends ModelItem> expected, SerializationStrategy tempStrategy, boolean equalItems) {
        logInfo(actual, expected, tempStrategy, equalItems);
        if (expected == null) {
            if (equalItems) {
                Assert.assertNull(actual, "actual is not null");
            } else {
                Assert.assertNotNull(actual, "actual is null");
            }
            return;
        }

        if (equalItems) {
            Assert.assertNotNull(actual, "actual is null");
        } else {
            if (actual == null) {
                Assert.assertTrue(true);
                return;
            }
        }

        if (equalItems) {
            Assert.assertEquals(expected.size(), actual.size());
        } else {
            if (expected.size() != actual.size()) {
                Assert.assertTrue(true);
                return;
            }
        }

        // save serialization strategies (might overwrite some but since they are static it should not matter)
        Stack<SerializationStrategy> strategiesExpected = new Stack<>();
        Stack<SerializationStrategy> strategiesActual = new Stack<>();
        if (tempStrategy != null) {
            for (ModelItem mItem : expected) {
                strategiesExpected.push(mItem.getSerializationStrategy());
                mItem.setSerializationStrategy(tempStrategy);
            }
            for (ModelItem mItem : actual) {
                strategiesActual.push(mItem.getSerializationStrategy());
                mItem.setSerializationStrategy(tempStrategy);
            }
        }

        if (equalItems) {
            Assert.assertEqualsNoOrder(actual.toArray(new ModelItem[actual.size()]),
                    expected.toArray(new ModelItem[expected.size()]), "The two lists do not match.");
        } else {
            Assert.assertFalse(expected.containsAll(actual) && actual.containsAll(expected), "collections are equal");
        }

        // restore serialization strategies in reversed order, thus hopefully getting the same state as it was before.
        if (tempStrategy != null) {
            List<ModelItem> revActual = new ArrayList<>(actual.size());
            revActual.addAll(actual);
            Collections.reverse(revActual);
            for (ModelItem mItem : revActual) {
                mItem.setSerializationStrategy(strategiesActual.pop());
            }

            List<ModelItem> revExpected = new ArrayList<>(actual.size());
            revExpected.addAll(expected);
            Collections.reverse(revExpected);
            for (ModelItem mItem : revExpected) {
                mItem.setSerializationStrategy(strategiesExpected.pop());
            }
        }
    }

    /**
     * Asserts that two maps of ModelItems contain the same elements and have the same number of elements.
     * Uses the serialization strategies of the expected items.
     *
     * @param actual   map one
     * @param expected map two
     */
    public static void assertEqualModelItems(Map<? extends ModelItem, ? extends ModelItem> actual, Map<? extends ModelItem, ? extends ModelItem> expected) {
        assertEqualModelItems(actual, expected, null);
    }

    /**
     * Asserts that two maps of ModelItems contain the same elements and have the same number of elements.
     * Uses the serialization strategies of the expected items if {@code tempStrategy} is {@code null}.
     *
     * @param actual       map one
     * @param expected     map two
     * @param tempStrategy the (temporary) serialization strategy
     */
    public static void assertEqualModelItems(Map<? extends ModelItem, ? extends ModelItem> actual, Map<? extends ModelItem, ? extends ModelItem> expected, SerializationStrategy tempStrategy) {
        assertEqualModelItems(actual, expected, tempStrategy, true);
    }

    /**
     * Asserts that two maps of ModelItems contain the same elements and have the same number of elements.
     * Uses the serialization strategies of the expected items if {@code tempStrategy} is {@code null}.
     *
     * @param actual       map one
     * @param expected     map two
     * @param tempStrategy the (temporary) serialization strategy
     * @param equalItems   if false, the items must not be equal
     */
    public static void assertEqualModelItems(Map<? extends ModelItem, ? extends ModelItem> actual, Map<? extends ModelItem, ? extends ModelItem> expected, SerializationStrategy tempStrategy, boolean equalItems) {
        logInfo(actual, expected, tempStrategy, equalItems);
        if (expected == null) {
            if (equalItems) {
                Assert.assertNull(actual, "actual is not null");
            } else {
                Assert.assertNotNull(actual, "actual is null");
            }
            return;
        }

        if (equalItems) {
            Assert.assertNotNull(actual, "actual is null");
        } else {
            if (actual == null) {
                Assert.assertTrue(true);
                return;
            }
        }

        if (equalItems) {
            Assert.assertEquals(expected.size(), actual.size(), "Maps have different sizes.");
        } else {
            if (expected.size() != actual.size()) {
                Assert.assertTrue(true);
                return;
            }
        }

        for (ModelItem key : expected.keySet()) {
            assertEqualModelItems(actual.get(key), expected.get(key), tempStrategy, equalItems);
        }
    }

}
