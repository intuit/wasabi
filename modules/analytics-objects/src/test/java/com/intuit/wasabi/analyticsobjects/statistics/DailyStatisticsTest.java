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
package com.intuit.wasabi.analyticsobjects.statistics;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for the {@link DailyStatistics}
 */
public class DailyStatisticsTest {

    @Test
    public void builderTest() {
        DailyStatistics dailyStatistics = new DailyStatistics.Builder().setDate("1970-01-01")
                .withCumulative(new ExperimentStatistics()).withPerDay(new ExperimentBasicStatistics()).build();
        assertThat(dailyStatistics.toString(), is("DailyStatistics[perDay="
                + "ExperimentBasicStatistics[buckets=<null>,jointActionRate=<null>,"
                + "actionRates=<null>,actionCounts=<null>,impressionCounts=<null>,"
                + "jointActionCounts=<null>],cumulative=ExperimentStatistics[buckets=<null>,"
                + "experimentProgress=<null>,jointProgress=<null>,actionProgress=<null>,"
                + "jointActionRate=<null>,actionRates=<null>,actionCounts=<null>,"
                + "impressionCounts=<null>,jointActionCounts=<null>],date=1970-01-01]"));
    }

    @Test
    public void gettersTest() {
        ExperimentStatistics experimentStatistics = new ExperimentStatistics();
        ExperimentBasicStatistics experimentBasicStatistics = new ExperimentBasicStatistics();
        DailyStatistics dailyStatistics = new DailyStatistics.Builder().setDate("2000-01-01")
                .withCumulative(experimentStatistics).withPerDay(experimentBasicStatistics).build();

        assertThat(dailyStatistics.getCumulative(), is(experimentStatistics));
        assertThat(dailyStatistics.getDate(), is("2000-01-01"));
        assertThat(dailyStatistics.getPerDay(), is(experimentBasicStatistics));
    }

    @Test
    public void HashCodeEqualsCloneTest() {
        DailyStatistics dailyStatistics = new DailyStatistics.Builder().setDate("1970-01-01")
                .withCumulative(new ExperimentStatistics()).withPerDay(new ExperimentBasicStatistics()).build();
        assertThat(dailyStatistics.hashCode(), is(-1533723416));
        DailyStatistics dailyStatistics1 = dailyStatistics.clone();
        assertThat(dailyStatistics, equalTo(dailyStatistics1));
    }
}
