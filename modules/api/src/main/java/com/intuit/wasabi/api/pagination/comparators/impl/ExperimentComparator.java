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

import com.intuit.wasabi.api.pagination.comparators.PaginationComparator;
import com.intuit.wasabi.api.pagination.comparators.PaginationComparatorProperty;
import com.intuit.wasabi.experimentobjects.Experiment;

import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implements the {@link PaginationComparator} for {@link Experiment} objects.
 */
public class ExperimentComparator extends PaginationComparator<Experiment> {

    /**
     * Initializes the ExperimentComparator.
     * <p>
     * Sets the default sort order to the descending modification time ({@code -modification_time}),
     * that means the latest changes come first.
     */
    public ExperimentComparator() {
        super("-modification_time");
    }

    /**
     * Implementation of {@link PaginationComparatorProperty} for {@link Experiment}s.
     *
     * @see PaginationComparatorProperty
     */
    private enum Property implements PaginationComparatorProperty<Experiment> {
        application_name(experiment -> experiment.getApplicationName().toString(), String::compareToIgnoreCase),
        experiment_label(experiment -> experiment.getLabel().toString(), String::compareToIgnoreCase),
        created_by(Experiment::getCreatorID, String::compareToIgnoreCase),
        creation_time(Experiment::getCreationTime, Date::compareTo),
        start_time(Experiment::getStartTime, Date::compareTo),
        end_time(Experiment::getEndTime, Date::compareTo),
        modification_time(Experiment::getModificationTime, Date::compareTo),
        state(experiment -> experiment.getState().name(), String::compareToIgnoreCase),
        favorite(Experiment::isFavorite, Boolean::compareTo);

        private final Function<Experiment, ?> propertyExtractor;
        private final BiFunction<?, ?, Integer> comparisonFunction;

        /**
         * Creates a Property.
         *
         * @param propertyExtractor  the property extractor
         * @param comparisonFunction the comparison function
         * @param <T>                the property type
         */
        <T> Property(Function<Experiment, T> propertyExtractor, BiFunction<T, T, Integer> comparisonFunction) {
            this.propertyExtractor = propertyExtractor;
            this.comparisonFunction = comparisonFunction;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Function<Experiment, ?> getPropertyExtractor() {
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
    public int compare(Experiment left, Experiment right) {
        return super.compare(left, right, Property.class);
    }
}
