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
package com.intuit.wasabi.analyticsobjects.metrics;

import com.intuit.wasabi.analyticsobjects.statistics.DistinguishableEffectSize;
import com.intuit.wasabi.analyticsobjects.statistics.Estimate;
import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NormalApproxMetricTest {

    private double precision = 0.00001;

    NormalApproxMetric testStats95 = new NormalApproxMetric(0.95, 1.0);
    NormalApproxMetric testStats99 = new NormalApproxMetric(0.99, 1.0);


    @Test(expected = IllegalArgumentException.class)
    public void constructorInputs1() throws Exception {
        new NormalApproxMetric(0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorInputs2() throws Exception {
        new NormalApproxMetric(1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorInputs3() throws Exception {
        new NormalApproxMetric(0.95, 0.99);
    }

    @Test
    public void estimateRate() throws Exception {
        Estimate output = testStats95.estimateRate(5, 2);
        assertThat("2/5 = 0.4", output.getEstimate(), IsCloseTo.closeTo(0.4, precision));
        assertThat("check lower bound", output.getLowerBound(), IsCloseTo.closeTo(0.1159866, precision));
        assertThat("check upper bound", output.getUpperBound(), IsCloseTo.closeTo(0.7709098, precision));

        output = testStats95.estimateRate(0, 0);
        assertThat("0 impressions returns NaN", output.getEstimate(), is(NaN));
        assertThat("check lower bound for 0 impressions", output.getLowerBound(), IsCloseTo.closeTo(0.0, precision));
        assertThat("check upper bound for 0 impressions", output.getUpperBound(), IsCloseTo.closeTo(1.0, precision));
    }

    @Test(expected = IllegalArgumentException.class)
    public void estimateRateInputs1() throws Exception {
        testStats95.estimateRate(-1, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void estimateRateInputs2() throws Exception {
        testStats95.estimateRate(5, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void estimateRateInputs3() throws Exception {
        testStats95.estimateRate(5, 6);
    }

    @Test
    public void estimateRateDifference() throws Exception {
        Estimate output = testStats95.estimateRateDifference(5, 2, 4, 3);
        assertThat("2/5 - 3/4 = -0.35", output.getEstimate(), IsCloseTo.closeTo(-0.35, precision));
        assertThat("check lower bound", output.getLowerBound(), IsCloseTo.closeTo(-0.9537039, precision));
        assertThat("check upper bound", output.getUpperBound(), IsCloseTo.closeTo(0.2537039, precision));

        output = testStats95.estimateRateDifference(500, 200, 400, 300);
        assertThat("200/500 - 300/400 = -0.35", output.getEstimate(), IsCloseTo.closeTo(-0.35, precision));
        assertThat("check lower bound", output.getLowerBound(), IsCloseTo.closeTo(-0.4103704, precision));
        assertThat("check upper bound", output.getUpperBound(), IsCloseTo.closeTo(-0.2896296, precision));

        output = testStats95.estimateRateDifference(1, 1, 0, 0);
        assertThat("0 impressions returns NaN", output.getEstimate(), is(NaN));
        assertThat("0 impressions returns NaN", output.getLowerBound(), is(NaN));
        assertThat("0 impressions returns NaN", output.getUpperBound(), is(NaN));

        output = testStats95.estimateRateDifference(0, 0, 1, 1);
        assertThat("0 impressions returns NaN", output.getEstimate(), is(NaN));
        assertThat("0 impressions returns NaN", output.getLowerBound(), is(NaN));
        assertThat("0 impressions returns NaN", output.getUpperBound(), is(NaN));
    }

    @Test
    public void distinguishableEffectSizes() throws Exception {
        DistinguishableEffectSize output = testStats95.distinguishableEffectSizes(5, 2, 4, 0);
        assertThat("check lower bound", output.getNegativeEffectSize(), IsCloseTo.closeTo(-0.5116258, precision));
        assertThat("check upper bound", output.getPositiveEffectSize(), IsCloseTo.closeTo(0.4136477, precision));
        DistinguishableEffectSize output2 = testStats95.distinguishableEffectSizes(5, 2, 4, 3);
        assertThat("independent of actions_2", output2, equalTo(output));

        output = testStats95.distinguishableEffectSizes(500, 200, 400, 0);
        assertThat("check lower bound", output.getNegativeEffectSize(), IsCloseTo.closeTo(-0.0650622, precision));
        assertThat("check upper bound", output.getPositiveEffectSize(), IsCloseTo.closeTo(0.0631597, precision));
        output2 = testStats95.distinguishableEffectSizes(500, 200, 400, 300);
        assertThat("independent of actions_2", output2, equalTo(output));

        output = testStats95.distinguishableEffectSizes(0, 0, 1, 0);
        assertThat("0 impressions returns NaN", output.getNegativeEffectSize(), is(NaN));
        assertThat("0 impressions returns NaN", output.getPositiveEffectSize(), is(NaN));
        output = testStats95.distinguishableEffectSizes(1, 1, 0, 0);
        assertThat("0 impressions returns NaN", output.getNegativeEffectSize(), is(NaN));
        assertThat("0 impressions returns NaN", output.getPositiveEffectSize(), is(NaN));
    }

    @Test
    public void fractionOfData() throws Exception {
        Double output = testStats95.fractionOfData(5, 2, 4, 0, -0.5);
        assertThat("lower bound: need more data", output, IsCloseTo.closeTo(0.9231127, precision));
        Double output2 = testStats95.fractionOfData(5, 2, 4, 3, -0.5);
        assertThat("independent of actions_2", output2, equalTo(output));

        output = testStats95.fractionOfData(5, 2, 4, 0, -0.7);
        assertThat("lower bound: have enough data", output, IsCloseTo.closeTo(6.2222297, precision));
        output2 = testStats95.fractionOfData(5, 2, 4, 3, -0.7);
        assertThat("independent of actions_2", output2, equalTo(output));

        output = testStats95.fractionOfData(5, 2, 4, 0, 0.2);
        assertThat("upper bound: need more data", output, IsCloseTo.closeTo(0.1183263, precision));
        output2 = testStats95.fractionOfData(5, 2, 4, 3, 0.2);
        assertThat("independent of actions_2", output2, equalTo(output));

        output = testStats95.fractionOfData(5, 2, 4, 0, 0.5);
        assertThat("upper bound: have enough data", output, IsCloseTo.closeTo(3.174607, precision));
        output2 = testStats95.fractionOfData(5, 2, 4, 3, 0.5);
        assertThat("independent of actions_2", output2, equalTo(output));

        DistinguishableEffectSize output3 = testStats95.distinguishableEffectSizes(5, 2, 4, 0);
        output = testStats95.fractionOfData(5, 2, 4, 0, output3.getNegativeEffectSize());
        assertThat("lower bound: boundary", output, IsCloseTo.closeTo(1.0, precision));
        output = testStats95.fractionOfData(5, 2, 4, 0, output3.getPositiveEffectSize());
        assertThat("upper bound: boundary", output, IsCloseTo.closeTo(1.0, precision));
        output = testStats95.fractionOfData(0, 0, 1, 0, 0.1);
        assertThat("0 impressions returns NaN", output, is(NaN));
        output = testStats95.fractionOfData(1, 1, 0, 0, 0.1);
        assertThat("0 impressions returns 0", output, IsCloseTo.closeTo(0.0, precision));
        output = testStats95.fractionOfData(0, 0, 0, 0, 0.1);
        assertThat("0 impressions returns NaN", output, is(NaN));
    }

    @Test(expected = IllegalArgumentException.class)
    public void fractionOfDataInputs1() throws Exception {
        testStats95.fractionOfData(5, 2, 4, 0, -1.1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fractionOfDataInputs2() throws Exception {
        testStats95.fractionOfData(5, 2, 4, 0, 1.1);
    }

    @Test
    public void predictedDataNeeded() throws Exception {
        Long output = testStats95.predictedDataNeeded(0.5, 0.5, 0.5, 0.05);
        assertThat("test 1", output, equalTo(new Long(1537)));

        output = testStats95.predictedDataNeeded(0.5, 0.25, 0.25, 0.01);
        assertThat("test 2", output, equalTo(new Long(76829)));

        output = testStats95.predictedDataNeeded(0.1, 0.3, 0.2, 0.03);
        assertThat("test 3", output, equalTo(new Long(3201)));

        output = testStats99.predictedDataNeeded(0.01, 0.7, 0.3, 0.005);
        assertThat("test 4", output, equalTo(new Long(12512)));

        Long output2 = testStats99.predictedDataNeeded(0.01, 0.3, 0.7, 0.005);
        assertThat("symmetric in allocation percentage", output, equalTo(output2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void predictedDataNeededInputs1() throws Exception {
        testStats95.predictedDataNeeded(-0.01, 0.5, 0.5, 0.05);
    }

    @Test(expected = IllegalArgumentException.class)
    public void predictedDataNeededInputs2() throws Exception {
        testStats95.predictedDataNeeded(1.01, 0.5, 0.5, 0.05);
    }

    @Test(expected = IllegalArgumentException.class)
    public void predictedDataNeededInputs3() throws Exception {
        testStats95.predictedDataNeeded(0.5, 0.0, 0.5, 0.05);
    }

    @Test(expected = IllegalArgumentException.class)
    public void predictedDataNeededInputs4() throws Exception {
        testStats95.predictedDataNeeded(0.5, 1.0, 0.5, 0.05);
    }

    @Test(expected = IllegalArgumentException.class)
    public void predictedDataNeededInputs5() throws Exception {
        testStats95.predictedDataNeeded(0.5, 0.5, 0.5, 0.0);
    }
}
