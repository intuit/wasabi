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

import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.State;
import org.joda.time.DateMidnight;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RollupTest {

    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @Before
    public void before() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        DateTimeZone.setDefault(DateTimeZone.UTC);
    }

    @Test
    public void rollupFreshEnoughWhenOnEarliestAllowedDay() throws Exception {
        final DateMidnight latestRollupDate = new DateMidnight(2013, 10, 7);
        final DateMidnight today = new DateMidnight(2013, 10, 9);
        final Date endDate = df.parse("2013-10-12T01:00:00+0000");
        final Date modifiedDate = df.parse("2013-10-12T23:00:00+0000");
        final State state = State.RUNNING;
        final int maxAgeAllowedInDays = 2;

        Rollup rollup = rollupWith(latestRollupDate, today, maxAgeAllowedInDays, endDate, modifiedDate, state);
        assertThat(rollup.isFreshEnough(), is(true));
    }

    @Test
    public void rollupFreshEnoughWhenFromToday() throws Exception {
        final DateMidnight latestRollupDate = new DateMidnight(2013, 10, 7);
        final DateMidnight today = new DateMidnight(2013, 10, 7);
        final Date endDate = df.parse("2013-10-12T00:01:00+0000");
        final Date modifiedDate = df.parse("2013-10-12T23:00:00+0000");
        final State state = State.RUNNING;
        final int maxAgeAllowedInDays = 2;

        Rollup rollup = rollupWith(latestRollupDate, today, maxAgeAllowedInDays, endDate, modifiedDate, state);
        assertThat(rollup.isFreshEnough(), is(true));
    }

    @Test
    public void rollupFreshEnoughWhenBetweenEarliestAllowedDayAndToday() throws Exception {
        final DateMidnight latestRollupDate = new DateMidnight(2013, 10, 7);
        final DateMidnight today = new DateMidnight(2013, 10, 8);
        final Date endDate = df.parse("2013-10-12T01:00:00+0000");
        final Date modifiedDate = df.parse("2013-10-12T23:00:00+0000");
        final State state = State.RUNNING;
        final int maxAgeAllowedInDays = 2;

        Rollup rollup = rollupWith(latestRollupDate, today, maxAgeAllowedInDays, endDate, modifiedDate, state);
        assertThat(rollup.isFreshEnough(), is(true));
    }

    @Test
    public void rollupFreshEnoughWhenFromEndDate() throws Exception {
        final DateMidnight latestRollupDate = new DateMidnight(2013, 10, 7);
        final DateMidnight today = new DateMidnight(2013, 10, 12);
        final Date endDate = df.parse("2013-10-7T01:00:00+0000");
        final Date modifiedDate = df.parse("2013-10-12T23:00:00+0000");
        final State state = State.RUNNING;
        final int maxAgeAllowedInDays = 2;

        Rollup rollup = rollupWith(latestRollupDate, today, maxAgeAllowedInDays, endDate, modifiedDate, state);
        assertThat(rollup.isFreshEnough(), is(true));
    }

    @Test
    public void rollupFreshEnoughWhenFromModifiedDate() throws Exception {
        final DateMidnight latestRollupDate = new DateMidnight(2013, 10, 7);
        final DateMidnight today = new DateMidnight(2013, 10, 12);
        final Date endDate = df.parse("2013-10-12T01:00:00+0000");
        final Date modifiedDate = df.parse("2013-10-07T23:00:00+0000");
        final State state = State.TERMINATED;
        final int maxAgeAllowedInDays = 2;

        Rollup rollup = rollupWith(latestRollupDate, today, maxAgeAllowedInDays, endDate, modifiedDate, state);
        assertThat(rollup.isFreshEnough(), is(true));
    }

    @Test
    public void rollupNotFreshEnoughWhenBeforeEarliestAllowedDay() throws Exception {
        final DateMidnight latestRollupDate = new DateMidnight(2013, 10, 6);
        final DateMidnight today = new DateMidnight(2013, 10, 9);
        final Date endDate = df.parse("2013-10-09T01:00:00+0000");
        final Date modifiedDate = df.parse("2013-10-09T23:00:00+0000");
        final State state = State.TERMINATED;
        final int maxAgeAllowedInDays = 2;

        Rollup rollup = rollupWith(latestRollupDate, today, maxAgeAllowedInDays, endDate, modifiedDate, state);
        assertThat(rollup.isFreshEnough(), is(false));
    }

    @Test
    public void rollupNotFreshEnoughNotTerminated() throws Exception {
        final DateMidnight latestRollupDate = new DateMidnight(2013, 10, 7);
        final DateMidnight today = new DateMidnight(2013, 10, 12);
        final Date endDate = df.parse("2013-10-12T01:00:00+0000");
        final Date modifiedDate = df.parse("2013-10-07T23:00:00+0000");
        final State state = State.RUNNING;
        final int maxAgeAllowedInDays = 2;

        Rollup rollup = rollupWith(latestRollupDate, today, maxAgeAllowedInDays, endDate, modifiedDate, state);
        assertThat(rollup.isFreshEnough(), is(false));
    }

    @Test
    public void rollupNotFreshEnoughWhenNonexistent() throws Exception {
        final DateMidnight latestRollupDate = null;
        final DateMidnight today = new DateMidnight(2013, 10, 9);
        final Date endDate = df.parse("2013-10-12T01:00:00+0000");
        final Date modifiedDate = df.parse("2013-10-12T23:00:00+0000");
        final State state = State.RUNNING;
        final int maxAgeAllowedInDays = 2;

        Rollup rollup = rollupWith(latestRollupDate, today, maxAgeAllowedInDays, endDate, modifiedDate, state);
        assertThat(rollup.isFreshEnough(), is(false));
    }

    private Rollup rollupWith(final DateMidnight latestRollupDate, final DateMidnight today,
                              final int maxAgeAllowedInDays, final Date endDate, final Date modifiedDate, final State state) {

        final Experiment.ID RANDOM_ID = Experiment.ID.newInstance();
        Experiment experiment = Experiment.withID(RANDOM_ID)
                .withEndTime(endDate)
                .withModificationTime(modifiedDate)
                .withState(state)
                .build();

        return new Rollup(experiment, null) {
            @Override
            protected DateMidnight fetchLatestRollupDate() {
                return latestRollupDate;
            }

            @Override
            protected Integer getMaxAllowedRollupAgeDays() {
                return maxAgeAllowedInDays;
            }

            @Override
            protected DateMidnight today() {
                return today;
            }
        };
    }
}
