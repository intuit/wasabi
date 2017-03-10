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
package com.intuit.wasabi.analytics.impl;

import com.intuit.wasabi.database.Transaction;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Map;

import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.wasabi.analytics.impl.AnalyticsImpl.PROPERTY_NAME;
import static java.lang.Integer.parseInt;

/**
 * Roll up stats class
 */
public class Rollup {

    private final DateMidnight latestAvailableRollupDate;
    private final boolean cumulative;
    private final Experiment experiment;
    private final Transaction transaction;
    private Integer rollupMaxAge = 0;

    public Rollup(Experiment exp, Transaction transaction) {
        this(exp, false, transaction);
    }

    public Rollup(Experiment exp, boolean cumulative, Transaction transaction) {
        this.cumulative = cumulative;
        this.experiment = exp;
        this.transaction = transaction;
        latestAvailableRollupDate = fetchLatestRollupDate();

        rollupMaxAge = parseInt(create(PROPERTY_NAME, AnalyticsImpl.class).getProperty("rollup.max.age", "0"));
    }

    DateMidnight fetchLatestRollupDate() {
        @SuppressWarnings("rawtypes")
        List<Map> dayRow = queryDatabase();
        if (dayRow.isEmpty()) {
            return null;
        }
        return new DateMidnight(dayRow.get(0).get("day"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map> queryDatabase() {
        String query = "select day from experiment_rollup where experiment_id = ? ";
        if (cumulative) {
            query += "and cumulative = ? ";
        }
        query += "order by day desc limit ?";

        if (cumulative) {
            return transaction.select(query, experiment.getID(), 1, 1);
        } else {
            return transaction.select(query, experiment.getID(), 1);
        }
    }


    Integer getMaxAllowedRollupAgeDays() {
        return rollupMaxAge;
    }

    DateMidnight today() {
        return new DateMidnight(DateTimeZone.UTC);
    }

    DateMidnight comparisonDate() {
        DateMidnight earliest = today();
        DateMidnight lastDay = experiment.calculateLastDay();
        if (lastDay.isBefore(earliest)) {
            earliest = lastDay;
        }
        return earliest;
    }

    public boolean isFreshEnough() {
        if (latestAvailableRollupDate == null) {
            return false;
        }
        DateMidnight earliestValidDate = comparisonDate().minusDays(getMaxAllowedRollupAgeDays());
        return !latestAvailableRollupDate.isBefore(earliestValidDate);
    }

    public String latestAvailableRollupDateAsString() {
        return latestAvailableRollupDate.toString("YYYY-MM-dd");
    }
}
