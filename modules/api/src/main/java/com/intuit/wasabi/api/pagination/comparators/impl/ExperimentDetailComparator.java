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
package com.intuit.wasabi.api.pagination.comparators.impl;

import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;
import com.intuit.wasabi.api.pagination.comparators.PaginationComparator;
import com.intuit.wasabi.api.pagination.comparators.PaginationComparatorProperty;

import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implements the {@link PaginationComparator} for {@link ExperimentDetail} objects.
 */
public class ExperimentDetailComparator extends PaginationComparator<ExperimentDetail> {

    /**
     * Initializes the ExperimentDetailComparator.
     * <p>
     * Sets the default sort order to the descending modification time ({@code -modification_time}),
     * that means the ExperimentDetails with the latest changes come first.
     */
    public ExperimentDetailComparator() {
        super("-modification_time");
    }

    /**
     * Implementation of {@link PaginationComparatorProperty} for {@link ExperimentDetail}s.
     *
     * @see PaginationComparatorProperty
     */
    private enum Property implements PaginationComparatorProperty<ExperimentDetail> {
        application_name(experimentDetail -> experimentDetail.getApplicationName().toString(), String::compareToIgnoreCase),
        experiment_label(experimentDetail -> experimentDetail.getLabel().toString(), String::compareToIgnoreCase),
        state(experimentDetail -> experimentDetail.getState().name(), String::compareToIgnoreCase),
        modification_time(ExperimentDetail::getModificationTime, Date::compareTo),
        favorite(ExperimentDetail::isFavorite, Boolean::compareTo);


        private final Function<ExperimentDetail, ?> propertyExtractor;
        private final BiFunction<?, ?, Integer> comparisonFunction;

        /**
         * Creates a Property.
         *
         * @param propertyExtractor  the property extractor
         * @param comparisonFunction the comparison function
         * @param <T>                the property type
         */
        <T> Property(Function<ExperimentDetail, T> propertyExtractor, BiFunction<T, T, Integer> comparisonFunction) {
            this.propertyExtractor = propertyExtractor;
            this.comparisonFunction = comparisonFunction;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Function<ExperimentDetail, ?> getPropertyExtractor() {
            return propertyExtractor;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BiFunction<?, ?, Integer> getComparisonFunction() {
            return comparisonFunction;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(ExperimentDetail left, ExperimentDetail right) {
        return super.compare(left, right, Property.class);
    }
}
