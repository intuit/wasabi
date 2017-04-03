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
package com.intuit.wasabi.analyticsobjects;

import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics;
import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics.BinomialMetric;
import com.intuit.wasabi.analyticsobjects.metrics.DecisionAdjuster;
import com.intuit.wasabi.analyticsobjects.metrics.NormalApproxMetric;
import com.intuit.wasabi.analyticsobjects.metrics.NormalApproxSymMetric;
import com.intuit.wasabi.experimentobjects.Context;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ParametersTest {

    @Test
    public void parse() {
        Date date = new Date();
        Parameters parameters = new Parameters();
        parameters.setMetric(BinomialMetrics.NORMAL_APPROX_SYM);
        parameters.setConfidenceLevel(0.6);
        parameters.setEffectSize(-0.9);
        parameters.setSingleShot(false);
        parameters.setTimeZone(TimeZone.getTimeZone("PST"));
        parameters.setToTime(date);
        parameters.setFromTime(date);
        parameters.parse();
        BinomialMetric metric = parameters.getMetricImpl();
        assertThat(parameters.getTimeZone(), is(TimeZone.getTimeZone("PST")));
        assertThat(parameters.getToTime(), is(date));
        assertThat(parameters.getFromTime(), is(date));
        assertThat(parameters.getMetric(), is(BinomialMetrics.NORMAL_APPROX_SYM));
        assertThat("metric is valid", parameters.getMetricImpl(), instanceOf(NormalApproxSymMetric.class));
        assertThat("confidence_level converted to double", parameters.getConfidenceLevel(), equalTo(0.6));
        assertThat("effect_size converted to double", parameters.getEffectSize(), equalTo(-0.9));
        assertThat("single_shot remains false", parameters.isSingleShot(), is(false));
        assertThat("metric contains confidence level", metric.getConfidenceLevel(), equalTo(0.6));
        assertThat("metric contains max decisions", metric.getMaxDecisions(),
                equalTo(DecisionAdjuster.DEFAULT_MAX_DECISIONS));

        parameters = new Parameters();
        parameters.setMetric(BinomialMetrics.NORMAL_APPROX);
        parameters.setConfidenceLevel(0.6);
        parameters.setEffectSize(-0.9);
        parameters.setSingleShot(true);
        parameters.parse();
        metric = parameters.getMetricImpl();
        assertThat("metric is valid", parameters.getMetricImpl(), instanceOf(NormalApproxMetric.class));
        assertThat("confidence_level converted to double", parameters.getConfidenceLevel(), equalTo(0.6));
        assertThat("effect_size converted to double", parameters.getEffectSize(), equalTo(-0.9));
        assertThat("single_shot remains false", parameters.isSingleShot(), is(true));
        assertThat("metric contains confidence level", metric.getConfidenceLevel(), equalTo(0.6));
        assertThat("metric contains max decisions", metric.getMaxDecisions(), equalTo(1.0));

        for (final BinomialMetrics metric_enum : BinomialMetrics.values()) {
            parameters = new Parameters();
            parameters.setMetric(metric_enum);
            parameters.parse();
            assertThat(metric_enum.toString() + " was constructed", parameters.getMetricImpl(),
                    instanceOf(BinomialMetric.class));
        }
    }

    @Test
    public void testClone() {
        Date date = new Date();
        Parameters parameters = new Parameters();
        parameters.setMetric(BinomialMetrics.NORMAL_APPROX_SYM);
        parameters.setConfidenceLevel(0.6);
        parameters.setEffectSize(-0.9);
        parameters.setSingleShot(false);
        parameters.setTimeZone(TimeZone.getTimeZone("PST"));
        parameters.setToTime(date);
        parameters.setFromTime(date);

        Parameters clonedParameters = parameters.clone();

        assertThat(clonedParameters.hashCode(), is(parameters.hashCode()));
        assertThat(clonedParameters.getMode(), is(parameters.getMode()));
        assertThat(clonedParameters.getActions(), is(parameters.getActions()));
        assertThat(clonedParameters.getContext(), is(parameters.getContext()));
        assertThat(clonedParameters.equals(parameters), is(true));

        clonedParameters.setContext(Context.valueOf("TEST"));
        assertThat(clonedParameters.getContext(), is(Context.valueOf("TEST")));
        List<String> acts = new ArrayList<>();
        clonedParameters.setActions(acts);
        assertThat(clonedParameters.getActions(), is(acts));

    }

    @Test(expected = IllegalArgumentException.class)
    public void parseParametersConfidenceLevelHigh() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setConfidenceLevel(1.0);
        parameters.parse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseParametersConfidenceLevelLow() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setConfidenceLevel(0.0);
        parameters.parse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseParametersEffectSizeHigh() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setEffectSize(1.1);
        parameters.parse();
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseParametersEffectSizeLow() throws Exception {
        Parameters parameters = new Parameters();
        parameters.setEffectSize(-1.1);
        parameters.parse();
    }
}
