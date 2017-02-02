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

import com.intuit.wasabi.api.pagination.filters.impl.AuditLogEntryFilter;
import com.intuit.wasabi.exceptions.PaginationException;
import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import org.apache.commons.lang3.StringUtils;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A filter to filter objects for pagination. To implement a specific filter,
 * inherit from this class and implement the function {@link Predicate#test(Object)} like this:
 * <pre>{@code
 * {@literal @}Override
 *  public boolean test(T object) {
 *      return super.test(object, Property.class);
 *  }
 * }</pre>
 * where {@code T} is the object you want to test (e.g. Experiment) and {@code Property}
 * is an {@code enum} implementing {@link PaginationFilterProperty}, thus supplying
 * the property extractors and filter predicates needed to test each individual property.
 * <p>
 * The enum keys specify the keys for the filter. That means if the Property is implemented
 * like this:
 * <pre>{@code
 * private enum Property implements PaginationFilterProperty<Object> {
 *     string(Object::toString, String::equalsIgnoreCase);
 *     <--snip-->
 * }
 * }</pre>
 * one can filter {@code Object}s by their toString method for exact matches (ignoring case)
 * by supplying a filter {@code string=MyObject}. Only objects which have a string value of
 * exactly that string (ignoring case) will then be kept by the pagination.
 * <p>
 * There are two basic types of filtering: Fulltext search and single field search.<br>
 * The <b>fulltext search</b> basically just searches through all properties (except for those excluded via
 * {@link #excludeFromFulltext}, see there for details) until it finds a match. In other words: All objects
 * are considered not to match, until one test succeeds.<br />
 * The <b>single field search</b> works the other way around: All values are considered to match until
 * they do not fulfill one of the conditions.<br />
 * A fulltext search can be constrained by single field values. That means it will only be done on those
 * objects which have not been excluded by the single field search.
 * <p>
 * The filter pattern to pass as {@code ?filter=FILTERPATTERN} works like this:
 * <ul>
 * <li><b>fulltext</b> (e.g. {@code exper}: Must not contain {@code =}.
 * Tests in all properties for the given string (here {@code exper}).</li>
 * <li><b>key=value</b> (e.g. {@code hits=13}: Must not contain {@code ,} or {@code =} in neither key nor value.
 * Tests in the property key (here {@code hits}) for the given value (here {@code 13}).</li>
 * <li><b>key=value,key2=value2,...</b> (e.g. {@code hits=13,name=myexp}):
 * All conditions have to be fulfilled to pass the tests (conjunction). Same rules as above apply.</li>
 * <li><b>fulltext,key=value</b> (e.g. {@code exper,hits=13} or {@code exper,hits=13,name=myexp}):</li>
 * First constrains the properties on the (conjunctive) single properties and performs a fulltext
 * search on the remaining objects. The same rules as above apply.</li>
 * </ul>
 * <p>
 * An example implementation for {@code Object} would be (adjust accordingly or see linked
 * filters below for more examples):
 * <p>
 * <pre>{@code
 * public class ObjectFilter extends PaginationFilter<Object> {
 *     {@literal @}Override
 *      public boolean test(Object object) {
 *          return super.test(object, Property.class);
 *      }
 *
 *      private enum Property implements PaginationFilterProperty<Object> {
 *          string(Object::toString, StringUtils::containsIgnoreCase),
 *          hash(Object::hashCode, (hash, filter) ->
 *                                 StringUtils.containsIgnoreCase(Integer.toString(hash), filter));
 *
 *          private final Function<Object, ?> propertyExtractor;
 *          private final BiPredicate<?, String> filterPredicate;
 *
 *          <T> Property(Function<Object, T> propertyExtractor, BiPredicate<T, String> filterPredicate) {
 *              this.propertyExtractor = propertyExtractor;
 *              this.filterPredicate = filterPredicate;
 *          }
 *
 *         {@literal @}Override
 *          public Function<Object, ?> getPropertyExtractor() {
 *              return propertyExtractor;
 *          }
 *
 *         {@literal @}Override
 *          public BiPredicate<?, String> getFilterPredicate() {
 *              return filterPredicate;
 *          }
 *     }
 * }
 * }</pre>
 * <p>
 * As can be seen, all filter logic is already in the enum entry declarations.
 * <p>
 * <i>Gotcha:</i> Due to the nature of the design and the way timezones are handled, sometimes pre-processing
 * of certain (date) properties is needed. For this one can employ a modification of the filter value before passing
 * it to the filter predicates.<br />
 * As an example, {@link com.intuit.wasabi.api.pagination.filters.FilterUtil.FilterModifier#APPEND_TIMEZONEOFFSET} will
 * append the timezone offset to the filter such that the predicates have it available. However they then have
 * to split it from the real filter again by using {@link FilterUtil#extractTimeZone(String)}! See
 * {@link AuditLogEntryFilter.Property#time} for an example implementation.
 * <br />
 * To use such a modification use
 * {@link #registerFilterModifierForProperties(FilterUtil.FilterModifier, PaginationFilterProperty[])} to register it
 * in your filter implementation.
 *
 * @param <T> The object type, in the examples {@link Object}.
 * @see AuditLogEntryFilter
 * @see com.intuit.wasabi.api.pagination.filters.impl.ExperimentFilter
 */
public abstract class PaginationFilter<T> implements Predicate<T> {

    /**
     * The complete filter string passed by the {@link com.intuit.wasabi.api.pagination.PaginationHelper}.
     */
    private String filter = "";
    /**
     * The timezone offset passed by the {@link com.intuit.wasabi.api.pagination.PaginationHelper}.
     */
    private String timeZoneOffset = "+0000";

    /**
     * Separates a filter key from its value: key=value
     */
    private final String SEPARATOR = "=";
    /**
     * Delimites filter commands, appears between two filters: a=b,c=d
     */
    private final String DELIMITER = ",";

    /**
     * Stores the modifiers for properties.
     */
    /*test*/ final HashMap<PaginationFilterProperty, FilterUtil.FilterModifier> filterModifiers = new HashMap<>();
    /**
     * Stores the list of properties to exclude from fulltext search.
     */
    private final List<PaginationFilterProperty> excludeFromFulltext = new ArrayList<>();

    /**
     * Sets the filter and timezone offsets for filtering and returns such that it can be done in place
     * and be passed as a filter to streams:
     * {@code stream().filter(paginationFilter.setFilter(filter, timeZoneOffset))}.
     * <p>
     * If the empty string is passed as a timezone, it is set unchanged.
     * If null is passed as timezone, it is set to the default +0000.
     *
     * @param filter         the filter string
     * @param timeZoneOffset the timezone offset to UTC (should be compatible with
     *                       {@link ZoneOffset#of(String)}).
     * @return {@code this}
     */
    public PaginationFilter<T> replaceFilter(final String filter, final String timeZoneOffset) {
        this.filter = filter == null ? "" : filter;
        if (timeZoneOffset == null) {
            this.timeZoneOffset = "+0000";
        } else {
            this.timeZoneOffset = timeZoneOffset.isEmpty() ? this.timeZoneOffset : timeZoneOffset;
        }
        return this;
    }

    /**
     * Tests an object's properties according the current filter.
     *
     * @param object   the object to test
     * @param enumType the possible properties
     * @param <V>      the enum's type
     * @return true or false depending on the test result. For details see this class' documentation
     * {@link PaginationFilter}.
     */
    public final <V extends Enum<V> & PaginationFilterProperty> boolean test(T object, Class<V> enumType) {
        if (StringUtils.isBlank(filter)) {
            return true;
        } else if (filter.contains(SEPARATOR)) {
            if (StringUtils.containsNone(StringUtils.substringBefore(filter, SEPARATOR), DELIMITER)) {
                return testFields(object, enumType);
            }
            return testFields(object, enumType) && testFulltext(object, enumType);
        }
        return testFulltext(object, enumType);
    }

    /**
     * Parses through the current filter to find {@code key=value} patterns and tests the corresponding
     * object properties accordingly.
     * <p>
     * Returns true unless an object does not pass a filter.
     *
     * @param object   the object to test
     * @param enumType the allowed properties
     * @param <V>      the property enum type
     * @return the test result
     */
    /*test*/
    final <V extends Enum<V> & PaginationFilterProperty> boolean testFields(T object, Class<V> enumType) {
        try (Scanner filterScanner = new Scanner(getKeyValuePartOfFilter(filter))) {
            filterScanner.useDelimiter(DELIMITER);

            // iterate over all single field patterns
            String pattern = "[a-zA-Z_]+" + SEPARATOR + ".*";
            while (filterScanner.hasNext(pattern)) {
                String[] keyValuePattern = filterScanner.next(pattern).split(SEPARATOR);

                V key;
                try {
                    key = Enum.valueOf(enumType, keyValuePattern[0]);
                } catch (IllegalArgumentException illegalArgumentException) {
                    throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE, "The request can not be filtered by key '" + keyValuePattern[0] + "'.", illegalArgumentException);
                }

                String filterValue = modifyFilterForKey(key, keyValuePattern[1]);

                if (!filterByProperty(object, filterValue, key.getPropertyExtractor(), key.getFilterPredicate())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Performs a fulltext search on all non-excluded fields (see {@link #excludeFromFulltext}).
     * Any successful test causes this method to return true, otherwise it returns false.
     *
     * @param object   the object
     * @param enumType the property values
     * @param <V>      the property value enum type
     * @return the test result
     */
    /*test*/
    final <V extends Enum<V> & PaginationFilterProperty> boolean testFulltext(T object, Class<V> enumType) {
        return Arrays.asList(enumType.getEnumConstants()).parallelStream()
                .filter(field -> !excludeFromFulltext.contains(field))
                .anyMatch(field -> filterByProperty(object,
                        modifyFilterForKey(field, getFulltextPartOfFilter(filter)),
                        field.getPropertyExtractor(),
                        field.getFilterPredicate()));
    }

    /**
     * Gets a property from the object with the {@code propertyExtractor}. On failure (by NullPointerException)
     * {@code false} is returned.
     * <p>
     * If the property is {@code null}, {@code false} is returned. Otherwise the property is tested with the passed
     * {@code filterFunction} according to the passed filter value.
     *
     * @param object            the object to be tested
     * @param filterValue       the value to be tested with
     * @param propertyExtractor the property extractor
     * @param filterFunction    the filter function
     * @param <V>               the type of the object's property
     * @return false for failures and null-properties, otherwise the return value of the {@code filterFunction}
     */
    /*test*/
    final <V> boolean filterByProperty(T object, String filterValue, Function<T, V> propertyExtractor, BiPredicate<V, String> filterFunction) {
        V property;
        try {
            property = propertyExtractor.apply(object);
        } catch (NullPointerException ignored) {
            return false;
        }

        return property != null && filterFunction.test(property, filterValue);
    }

    /**
     * Applies stored filter modification if needed, otherwise just returns {@code value}.
     *
     * @param key   the property key
     * @param value the value to modify
     * @param <V>   the property enum type
     * @return the modified or untouched value
     */
    /*test*/
    final <V extends Enum<V> & PaginationFilterProperty> String modifyFilterForKey(V key, String value) {
        return filterModifiers.containsKey(key) ? filterModifiers.get(key).apply(value, this) : value;
    }

    /**
     * Returns a filter string where the fulltext string is removed.
     * <p>
     * Examples for filters and return values:
     * <ul>
     * <li>{@code Jul 15, 2014,appname=myApp} <i>returns</i> {@code appname=myApp}</li>
     * <li>Jul 15, 2014} <i>returns</i> the empty String</i></li>
     * <li>appname=myApp,experiment=myExp} <i>returns</i> {@code appname=myApp,experiment=myExp}</li>
     * <li>,,,=wrongFormatButStill} <i>returns</i> {@code =wrongFormatButStill}</li>
     * </ul>
     *
     * @param filter the filter
     * @return the key-value part of the filter
     */
    /*test*/
    final String getKeyValuePartOfFilter(String filter) {
        if (filter == null) {
            return "";
        }

        if (!filter.contains(SEPARATOR)) {
            return "";
        }

        if (!filter.contains(DELIMITER)) {
            return filter;
        }

        return filter.substring(StringUtils.lastIndexOf(filter.substring(0, filter.indexOf(SEPARATOR)), DELIMITER) + 1);
    }

    /**
     * Returns a filter string where the key-value part is removed.
     * <p>
     * Examples for filters and return values:
     * <ul>
     * <li>{@code Jul 15, 2014,appname=myApp} <i>returns</i> {@code Jul 15, 2014}</li>
     * <li>{@code Jul 15, 2014} <i>returns</i> {@code Jul 15, 2014}</li>
     * <li>{@code Jul 15, 2014,} <i>returns</i> {@code Jul 15, 2014,}</li>
     * <li>{@code appname=myApp,experiment=myExp} <i>returns the empty String</i></li>
     * <li>{@code ,,,=wrongFormatButStill} <i>returns</i> {@code ,,}</li>
     * </ul>
     *
     * @param filter the filter
     * @return the fulltext part of the filter
     */
    /*test*/
    final String getFulltextPartOfFilter(String filter) {
        if (filter == null) {
            return "";
        }

        if (!filter.contains(SEPARATOR)) {
            return filter;
        }

        if (!filter.contains(DELIMITER) || filter.indexOf(SEPARATOR) < filter.indexOf(DELIMITER)) {
            return "";
        }

        return filter.substring(0, StringUtils.lastIndexOf(filter.substring(0, filter.indexOf(SEPARATOR)), DELIMITER));
    }

    /**
     * Registers a filter modifier for a property. Filter modifiers will be applied to the filters just before the
     * partial filter string is passed to a property filter. See this class' documentation ({@link PaginationFilter})
     * for more details on how to use it.
     *
     * @param modifier   The modifier to use.
     * @param properties The properties for which this modifier should be used.
     */
    protected final void registerFilterModifierForProperties(FilterUtil.FilterModifier modifier, PaginationFilterProperty... properties) {
        Arrays.asList(properties).forEach(p -> filterModifiers.put(p, modifier));
    }

    /**
     * Stores the given properties in a lookup list. Whenever a fulltext search is performed, the registered properties
     * are excluded.<br />
     * For example if the property {@code Property.time} would be registered and someone filtered by a fulltext string,
     * the time field would be skipped.
     *
     * @param properties a list of properties to exclude
     */
    protected final void excludeFromFulltext(PaginationFilterProperty... properties) {
        excludeFromFulltext.addAll(Arrays.asList(properties));
    }

    /**
     * Returns the current timeZoneOffset.
     *
     * @return the timeZoneOffset.
     */
    final String getTimeZoneOffset() {
        return timeZoneOffset;
    }

    /**
     * Returns the current filter.
     *
     * @return the filter.
     */
    final String getFilter() {
        return filter;
    }
}
