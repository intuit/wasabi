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
import com.intuit.wasabi.experimentobjects.exceptions.ErrorCode;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class PaginationFilter<T> implements Predicate<T> {
    private String filter = "";
    private String timeZoneOffset = "+0000";

    private final HashMap<PaginationFilterProperty, FilterUtil.FilterModifier> filterModifiers = new HashMap<>();
    private final List<PaginationFilterProperty> excludeFromFulltext = new ArrayList<>();

    public PaginationFilter<T> setFilter(final String filter, final String timeZoneOffset) {
        this.filter = filter;
        this.timeZoneOffset = timeZoneOffset;
        return this;
    }

    private <V> boolean filterByProperty(T object, String filterValue, Function<T, V> propertyExtractor, BiPredicate<V, String> filterFunction) {
        V property;
        try {
            property = propertyExtractor.apply(object);
        } catch (NullPointerException ignored) {
            return false;
        }

        return property != null && filterFunction.test(property, filterValue);
    }

    String getTimeZoneOffset() {
        return timeZoneOffset;
    }

    private <V extends Enum<V> & PaginationFilterProperty> boolean testFulltext(T object, Class<V> enumType) {

        return Arrays.asList(enumType.getEnumConstants()).parallelStream()
                .filter(field -> !excludeFromFulltext.contains(field))
                .anyMatch(field -> filterByProperty(object,
                        modifyFilterForKey(field, this.filter),
                        field.getPropertyExtractor(),
                        field.getFilterPredicate()));
    }

    private <V extends Enum<V> & PaginationFilterProperty> String modifyFilterForKey(V key, String value) {
        return filterModifiers.containsKey(key) ? filterModifiers.get(key).apply(value, this) : value;
    }

    private <V extends Enum<V> & PaginationFilterProperty> boolean testFields(T object, Class<V> enumType) {
        String delimiter = ",";
        String separator = "=";
        String pattern = "[a-z_]+" + separator + ".*";

        try (Scanner filterScanner = new Scanner(this.filter)) {
            filterScanner.useDelimiter(delimiter);

            while (filterScanner.hasNext(pattern)) {
                String[] keyValuePattern = filterScanner.next(pattern).split(separator);

                V key;
                try {
                    key = Enum.valueOf(enumType, keyValuePattern[0]);
                } catch (IllegalArgumentException illegalArgumentException) {
                    throw new PaginationException(ErrorCode.FILTER_KEY_UNPROCESSABLE, "The request can not be filtered by " + keyValuePattern[0], illegalArgumentException);
                }

                String filterValue = modifyFilterForKey(key, keyValuePattern[1]);

                if (!filterByProperty(object, filterValue, key.getPropertyExtractor(), key.getFilterPredicate())) {
                    return false;
                }
            }
        }
        return false;
    }

    public <V extends Enum<V> & PaginationFilterProperty> boolean test(T object, Class<V> enumType) {
        if (StringUtils.isBlank(this.filter)) {
            return true;
        } else if (this.filter.contains("=")) {
            return testFields(object, enumType);
        }
        return testFulltext(object, enumType);
    }

    protected final void registerFilterModifierForProperties(FilterUtil.FilterModifier modifier, PaginationFilterProperty... properties) {
        Arrays.asList(properties).forEach(p -> filterModifiers.put(p, modifier));
    }

    protected final void excludeFromFulltext(PaginationFilterProperty... properties) {
        excludeFromFulltext.addAll(Arrays.asList(properties));
    }
}
