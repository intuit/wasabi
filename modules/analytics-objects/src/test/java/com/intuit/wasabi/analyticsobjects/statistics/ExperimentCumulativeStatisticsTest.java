package com.intuit.wasabi.analyticsobjects.statistics;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created on 4/14/16.
 */
@RunWith(MockitoJUnitRunner.class)
public class ExperimentCumulativeStatisticsTest {
    ExperimentCumulativeStatistics experimentCumulativeStatistics;
    List<DailyStatistics> dailyStatisticsList = new ArrayList<>();

    @Before
    public void setup() {
        experimentCumulativeStatistics = new ExperimentCumulativeStatistics.Builder()
                .withDays(dailyStatisticsList).build();
    }

    @Test
    public void testBasicMethods() {
        ExperimentCumulativeStatistics cloned = experimentCumulativeStatistics.clone();
        assertThat(cloned.equals(experimentCumulativeStatistics), is(true));
        assertThat(cloned.hashCode(), is(experimentCumulativeStatistics.hashCode()));
        assertThat(cloned.toString(), is(experimentCumulativeStatistics.toString()));
        assertThat(cloned.getDays().equals(experimentCumulativeStatistics.getDays()), is(true));
    }
}
