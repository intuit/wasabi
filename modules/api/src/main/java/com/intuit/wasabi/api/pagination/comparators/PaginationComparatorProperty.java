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

import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An implementation of the PaginationComparatorProperty is best
 * achieved by implementing an {@code enum} which implements
 * this interface.
 * <p>
 * This interface assures that a {@link PaginationComparator} can work
 * with a property extractor (which is a {@link Function}) to get
 * properties for comparison, as well as with a comparison function
 * (which is a {@link BiFunction}) to compare the two extracted properties.
 * Usually the property extractor is just function reference to a getter, but
 * sometimes more sophisticated approaches are needed: it might be necessary
 * to format the returns of a getter or similar.
 * The comparison function is usually a function reference to some existing (implicit)
 * implementation of {@link java.util.Comparator#compare}, for example
 * {@link String#compareToIgnoreCase(String)}.
 * <p>
 * Example:
 * <pre>{@code
 *  private enum Property implements PaginationComparatorProperty<Object> {
 *      string(Object::toString, String::compareToIgnoreCase),
 *      hash(Object::hashCode, Integer::compareTo);
 *
 *      private final Function<Object, ?> propertyExtractor;
 *      private final BiFunction<?, ?, Integer> comparisonFunction;
 *
 *      <T> Property(Function<Object, T> propertyExtractor, BiFunction<T, T, Integer> comparisonFunction) {
 *          this.propertyExtractor = propertyExtractor;
 *          this.comparisonFunction = comparisonFunction;
 *      }
 *
 *     {@literal @}Override
 *      public Function<Object, ?> getPropertyExtractor() {
 *          return propertyExtractor;
 *      }
 *
 *     {@literal @}Override
 *      public BiFunction<?, ?, Integer> getComparisonFunction() {
 *          return comparisonFunction;
 *      }
 * }
 * }</pre>
 *
 * @param <T> The type to be compared, in the example above {@link Object}.
 */
public interface PaginationComparatorProperty<T> {

    /**
     * Returns a Function which allows to get
     * a value of some type from an object of type T.
     * <p>
     * Usually this is a getter, for example:
     * {@code Person::getName}. Another common pattern is
     * something similar to {@code person -> person.getName().toString()}.
     * <p>
     * The returned function may return {@code null}, which will cause
     * the object to be sorted to the end.
     *
     * @return a function to retrieve a property.
     */
    Function<T, ?> getPropertyExtractor();

    /**
     * Returns a function which allows to comparing one object
     * to another, returning -1, 0, or 1.
     * <p>
     * Common values are for example:
     * <ul>
     * <li>{@link String#compareToIgnoreCase(String)}</li>
     * <li>{@link Integer#compareTo(Integer)}</li>
     * <li>{@link Date#compareTo(Date)}</li>
     * </ul>
     *
     * @return a function comparing one object to another.
     */
    BiFunction<?, ?, Integer> getComparisonFunction();
}
