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
import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A comparator to sort objects for pagination. To implement a specific comparator,
 * inherit from this class and implement the function {@link java.util.Comparator#compare(Object, Object)} like this:
 * <pre>{@code
 * @literal @Override
 *  public int compare(T left, T right) {
 *      return super.compare(left, right, Property.class);
 *  }
 * }</pre>
 * where {@code T} is the object you want to compare (e.g. Experiment) and {@code Property}
 * is an {@code enum} implementing {@link PaginationComparatorProperty}, thus supplying
 * the property extractors and comparators needed to compare each individual property.
 *
 * The enum keys specify the keys for the sort order. That means if the Property is implemented
 * like this:
 * <pre>{@code
 * private enum Property implements PaginationComparatorProperty<Object> {
 *     string(Object::toString, String::compareToIgnoreCase);
 *     <--snip-->
 * }
 * }</pre>
 * one can sort {@code Object}s by their toString method by supplying either
 * {@code string} or {@code -string} as sort order, the former resulting in
 * ascending order and the latter in descending order. If multiple enum fields
 * exist, a sort after multiple fields is possible. Consider also sorting by {@link Object#hashCode},
 * with the enum key being {@code hash}, then one could first sort by {@code string} ascending
 * and {@code hashcCde} descending with the sort order {@code string,-hash}.
 *
 * An example emplementations for {@code Object} would be (adjust accordingly or see linked
 * Comparators below for more examples):
 *
 * <pre>{@code
 * public class ObjectComparator extends PaginationComparator<Object> {
 *     @literal @Override
 *      public int compare(Object left, Object right) {
 *          return super.compare(left, right, Property.class);
 *      }
 *
 *      private enum Property implements PaginationComparatorProperty<Object> {
 *          string(Object::toString, String::compareToIgnoreCase),
 *          hash(Object::hashCode, Integer::compareTo);
 *
 *          private final Function<Object, ?> propertyExtractor;
 *          private final BiFunction<?, ?, Integer> comparisonFunction;
 *
 *          <T> Property(Function<Object, T> propertyExtractor, BiFunction<T, T, Integer> comparisonFunction) {
 *              this.propertyExtractor = propertyExtractor;
 *              this.comparisonFunction = comparisonFunction;
 *          }
 *
 *         @literal @Override
 *          public Function<Object, ?> getPropertyExtractor() {
 *              return propertyExtractor;
 *          }
 *
 *         @literal @Override
 *          public BiFunction<?, ?, Integer> getComparisonFunction() {
 *              return comparisonFunction;
 *          }
 *     }
 * }
 * }</pre>
 *
 * As can be seen, all sorting logic is already in the enum entry declarations.
 *
 * @see com.intuit.wasabi.api.pagination.comparators.impl.AuditLogComparator
 * @see com.intuit.wasabi.api.pagination.comparators.impl.ExperimentComparator
 *
 * @param <T> The object type, in the examples {@link Object}.
 */
public abstract class PaginationComparator<T> implements Comparator<T> {

    private String sortOrder = "";

    /**
     * Initialies a PagnitionComparator with an empty sort order.
     */
    public PaginationComparator() {
        this("");
    }

    /**
     * Initialies a PaginationComparator with a default sort order.
     * See class documentation for more information about the sort order.
     *
     * @param defaultSortOrder the default sort order.
     */
    protected PaginationComparator(String defaultSortOrder) {
        this.sortOrder = defaultSortOrder;
    }

    /**
     * Sets the sort order and returns the comparator so that it can be used
     * in {@link java.util.stream.Stream#sorted(Comparator)} calls:
     * <pre>{@code stream().sorted(this.setSortorder(newsort))}</pre>
     *
     * @param sortOrder a new sort order
     * @return this
     */
    public PaginationComparator<T> setSortorder(String sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    /**
     * Compares two objects by this instance's sort order.
     *
     * Splits the sort order on {@code ,} and tries to sort by each of the supplied fields.
     * The strings obtained after splitting must be valid keys for {@code enumType} (or valid keys
     * prefixed with a hyphen {@code -} for descending order), otherwise the sorting fails and a
     * {@link PaginationException} is thrown with {@link ErrorCode#SORT_KEY_UNPROCESSABLE}.
     *
     * The comparison in general follows {@link java.util.Comparator#compare(Object, Object)},
     * where depending on how the comparison logic is implemented by the passed
     * {@code enumType} of type {@link PaginationComparatorProperty}.
     *
     * This method returns either -1, 0, or 1:
     * <dl>
     *     <dt>-1</dt>
     *         <dd>If the right object should appear after the left object.</dd>
     *     <dt>0</dt>
     *         <dd>If the order of the two given object is unimportant, that means for
     *         the given sort order they are considered to be equal. This is the default.</dd>
     *     <dt>1</dt>
     *         <dd>If the left object should appear after the right object.</dd>
     * </dl>
     *
     * If one objects value for a sort key is null, it will always be sorted after the other object.
     * See {@link #compareNull(Object, Object, boolean)} for more details on this.
     * Additionally if the first key in the sort order suffices, the comparison follows the fail-fast
     * principle and returns the value. Otherwise it sorts as long as needed to break the tie between
     * the two objects.
     *
     * For information on how to implement the {@code enumType}, take a look at the examples at
     * {@link PaginationComparator} or {@link PaginationComparatorProperty}.
     *
     * @see #compareNull(Object, Object, boolean)
     * @see #compareByProperty(Object, Object, Function, BiFunction, boolean)
     *
     * @param left left object
     * @param right right object
     * @param enumType an enum implementing {@link PaginationComparatorProperty}
     * @param <V> The enum type implementing {@link PaginationComparatorProperty}
     * @return -1, 0, 1, see above for more details.
     */
    protected <V extends Enum<V> & PaginationComparatorProperty> int compare(T left, T right, Class<V> enumType) {
        for (String sort : sortOrder.toLowerCase().split(",")) {
            boolean descending = sort.startsWith("-");

            String propertyKey = descending ? sort.substring(1) : sort;

            if (StringUtils.isBlank(propertyKey)) {
                continue;
            }

            V property;
            try {
                property = Enum.valueOf(enumType, propertyKey);
            } catch (IllegalArgumentException illegalArgumentException) {
                throw new PaginationException(ErrorCode.SORT_KEY_UNPROCESSABLE, "The request can not be sorted by " + propertyKey, illegalArgumentException);
            }

            int result = compareByProperty(left, right, property.getPropertyExtractor(), property.getComparisonFunction(), descending);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    /**
     * Compares two objects by one of their properties.
     *
     * @param left left object
     * @param right right object
     * @param propertyExtractor
     * @param comparisonFunc
     * @param descending
     * @param <V>
     * @return
     */
    private <V> int compareByProperty(T left, T right, Function<T, V> propertyExtractor, BiFunction<V, V, Integer> comparisonFunc, boolean descending) {
        V property1 = null;
        V property2 = null;

        try {
            property1 = propertyExtractor.apply(left);
        } catch (NullPointerException ignored) {
        }
        try {
            property2 = propertyExtractor.apply(right);
        } catch (NullPointerException ignored) {
        }

        int result;
        if ((result = compareNull(property1, property2, descending)) == 2) {
            result = comparisonFunc.apply(property1, property2);
        }
        return result * (descending ? -1 : 1);
    }

    private int compareNull(Object left, Object right, boolean descending) {
        if (left != null && right != null) {
            return 2;
        }
        if (left != null) {
            return descending ? 1 : -1;
        }
        if (right != null) {
            return descending ? -1 : 1;
        }
        return 0;
    }
}
