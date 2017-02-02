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

import java.util.Date;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * An implementation of the PaginationFilterProperty is best
 * achieved by implementing an {@code enum} which implements
 * this interface.
 * <p>
 * This interface assures that a {@link PaginationFilter} can work
 * with a property extractor (which is a {@link Function}) to get
 * properties for comparison, as well as with a filter predicate
 * (which is a {@link BiPredicate}) to filter the
 * object by its extracted property.<br />
 * Usually the property extractor is just function reference to a getter, but
 * sometimes more sophisticated approaches are needed: it might be necessary
 * to format the returns of a getter or similar.<br />
 * The filter function is usually a function reference to some existing (implicit)
 * BiPredicate which takes the extracted property and the filter string
 * as arguments, for example:
 * {@link org.apache.commons.lang3.StringUtils#containsIgnoreCase(CharSequence, CharSequence)}.
 * However, if the property in question is no String, more sophisticated methods/functions might
 * be needed to be able to filter the two properties.
 * <p>
 * Example:
 * <pre>{@code
 *  private enum Property implements PaginationFilterProperty<Object> {
 *      string(Object::toString, StringUtils::containsIgnoreCase),
 *      hash(Object::hashCode, (hash, filter) ->
 *                             StringUtils.containsIgnoreCase(Integer.toString(hash), filter));
 *
 *      private final Function<Object, ?> propertyExtractor;
 *      private final BiPredicate<?, String> filterPredicate;
 *
 *      <T> Property(Function<Object, T> propertyExtractor, BiPredicate<T, String> filterPredicate) {
 *          this.propertyExtractor = propertyExtractor;
 *          this.filterPredicate = filterPredicate;
 *      }
 *
 *     {@literal @}Override
 *      public Function<Object, ?> getPropertyExtractor() {
 *          return propertyExtractor;
 *      }
 *
 *     {@literal @}Override
 *      public BiPredicate<?, String> getFilterPredicate() {
 *          return filterPredicate;
 *      }
 * }
 * }</pre>
 * <p>
 * Some useful helper predicates, especially for handling dates and times, are implemented in
 * {@link FilterUtil}.
 *
 * @param <T> The type to be filtered, in the example above {@link Object}.
 * @see FilterUtil
 */
public interface PaginationFilterProperty<T> {

    /**
     * Returns a Function which allows to get
     * a value of some type from an object of type T.
     * <p>
     * Usually this is a getter, for example:
     * {@code Person::getName}. Another common pattern is
     * something similar to {@code person -> person.getName().toString()}.
     * <p>
     * The returned function may return {@code null}, which will cause
     * the object to be ignored.
     *
     * @return a function to retrieve a property.
     */
    Function<T, ?> getPropertyExtractor();

    /**
     * Returns a {@link java.util.function.Predicate} which allows to test
     * a value extracted by {@link #getPropertyExtractor()}.
     * <p>
     * Common values are for example:
     * <ul>
     * <li>{@link org.apache.commons.lang.StringUtils#containsIgnoreCase(String, String)}</li>
     * <li>{@link Integer#equals(Object)} </li>
     * <li>{@link java.util.Date#before(Date)}</li>
     * </ul>
     *
     * @return a function to test one argument against some condition.
     */
    BiPredicate<?, String> getFilterPredicate();
}
