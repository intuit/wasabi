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

public abstract class PaginationComparator<T> implements Comparator<T> {

    private String sortOrder = "";

    public PaginationComparator() {
        this("");
    }

    protected PaginationComparator(String defaultSortOrder) {
        this.sortOrder = defaultSortOrder;
    }

    public PaginationComparator<T> setSortorder(String sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    protected <V extends Enum<V> & PaginationComparatorProperty> int compare(T o1, T o2, Class<V> enumType) {
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

            int result = compareByProperty(o1, o2, property.getPropertyExtractor(), property.getComparisonFunction(), descending);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private <V> int compareByProperty(T object1, T object2, Function<T, V> propertyExtractor, BiFunction<V, V, Integer> comparisonFunc, boolean descending) {
        V property1 = null;
        V property2 = null;

        try {
            property1 = propertyExtractor.apply(object1);
        } catch (NullPointerException ignored) {
        }
        try {
            property2 = propertyExtractor.apply(object2);
        } catch (NullPointerException ignored) {
        }

        int result;
        if ((result = compareNull(property1, property2, descending)) == 2) {
            result = comparisonFunc.apply(property1, property2);
        }
        return result * (descending ? -1 : 1);
    }

    private int compareNull(Object o1, Object o2, boolean descending) {
        if (o1 != null && o2 != null) {
            return 2;
        }
        if (o1 != null) {
            return descending ? 1 : -1;
        }
        if (o2 != null) {
            return descending ? -1 : 1;
        }
        return 0;
    }
}
