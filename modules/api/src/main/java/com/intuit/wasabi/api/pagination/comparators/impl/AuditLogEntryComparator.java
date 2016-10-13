/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.api.pagination.comparators.impl;

import com.intuit.wasabi.api.pagination.comparators.PaginationComparator;
import com.intuit.wasabi.api.pagination.comparators.PaginationComparatorProperty;
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;

import java.util.Calendar;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implements the {@link PaginationComparator} for {@link AuditLogEntry} objects.
 */
public class AuditLogEntryComparator extends PaginationComparator<AuditLogEntry> {

    /**
     * Initializes an AuditLogEntryComparator.
     *
     * Sets the default sort order to descending time, that means
     * the most recent events are first.
     */
    public AuditLogEntryComparator() {
        super("-time");
    }

    /**
     * Implementation of {@link PaginationComparatorProperty} for {@link AuditLogEntry}s.
     *
     * @see PaginationComparatorProperty
     */
    private enum Property implements PaginationComparatorProperty<AuditLogEntry> {
        FIRSTNAME(auditLogEntry -> auditLogEntry.getUser().getFirstName(), String::compareToIgnoreCase),
        LASTNAME(auditLogEntry -> auditLogEntry.getUser().getLastName(), String::compareToIgnoreCase),
        USER(auditLogEntry -> auditLogEntry.getUser().getUsername().toString(), String::compareToIgnoreCase),
        USERNAME(auditLogEntry -> auditLogEntry.getUser().getUsername().toString(), String::compareToIgnoreCase),
        USERID(auditLogEntry -> auditLogEntry.getUser().getUserId(), String::compareToIgnoreCase),
        MAIL(auditLogEntry -> auditLogEntry.getUser().getEmail(), String::compareToIgnoreCase),
        ACTION(AuditLogAction::getDescription, String::compareToIgnoreCase),
        EXPERIMENT(auditLogEntry -> auditLogEntry.getExperimentLabel().toString(), String::compareToIgnoreCase),
        BUCKET(auditLogEntry -> auditLogEntry.getBucketLabel().toString(), String::compareToIgnoreCase),
        APP(auditLogEntry -> auditLogEntry.getApplicationName().toString(), String::compareToIgnoreCase),
        TIME(AuditLogEntry::getTime, Calendar::compareTo),
        ATTRIBUTE(AuditLogEntry::getChangedProperty, String::compareToIgnoreCase),
        BEFORE(AuditLogEntry::getBefore, String::compareToIgnoreCase),
        AFTER(AuditLogEntry::getAfter, String::compareToIgnoreCase),
        DESCRIPTION(AuditLogAction::getDescription, String::compareToIgnoreCase),
        ;

        private final Function<AuditLogEntry, ?> propertyExtractor;
        private final BiFunction<?, ?, Integer> comparisonFunction;

        /**
         * Creates a Property.
         *
         * @param propertyExtractor the property extractor
         * @param comparisonFunction the comparison function
         * @param <T> the property type
         */
        <T> Property(Function<AuditLogEntry, T> propertyExtractor, BiFunction<T, T, Integer> comparisonFunction) {
            this.propertyExtractor = propertyExtractor;
            this.comparisonFunction = comparisonFunction;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Function<AuditLogEntry, ?> getPropertyExtractor() {
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
    public int compare(AuditLogEntry left, AuditLogEntry right) {
        return super.compare(left, right, Property.class);
    }
}
