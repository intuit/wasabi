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
import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import static java.lang.Double.NaN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NormalApproxSymMetricTest {

    private double precision = 0.00001;

    NormalApproxSymMetric testStats95 = new NormalApproxSymMetric(0.95, 1.0);


    @Test
    public void distinguishableEffectSizes() throws Exception {
        DistinguishableEffectSize output = testStats95.distinguishableEffectSizes(5, 2, 4, 3);
        assertThat("check lower bound", output.getNegativeEffectSize(), IsCloseTo.closeTo(-0.6533213, precision));
        assertThat("check upper bound", output.getPositiveEffectSize(), IsCloseTo.closeTo(0.6533213, precision));

        output = testStats95.distinguishableEffectSizes(500, 200, 400, 300);
        assertThat("check lower bound", output.getNegativeEffectSize(), IsCloseTo.closeTo(-0.0653321, precision));
        assertThat("check upper bound", output.getPositiveEffectSize(), IsCloseTo.closeTo(0.0653321, precision));

        output = testStats95.distinguishableEffectSizes(0, 0, 2, 1);
        assertThat("0 impressions returns Infinity", output.getNegativeEffectSize().isInfinite());
        assertThat("0 impressions returns Infinity", output.getPositiveEffectSize().isInfinite());

        output = testStats95.distinguishableEffectSizes(2, 1, 0, 0);
        assertThat("0 impressions returns Infinity", output.getNegativeEffectSize().isInfinite());
        assertThat("0 impressions returns Infinity", output.getPositiveEffectSize().isInfinite());

        output = testStats95.distinguishableEffectSizes(0, 0, 0, 0);
        assertThat("0 impressions returns NaN", output.getNegativeEffectSize(), is(NaN));
        assertThat("0 impressions returns NaN", output.getPositiveEffectSize(), is(NaN));
    }

    @Test
    public void fractionOfData() throws Exception {
        Double output = testStats95.fractionOfData(5, 2, 4, 3, -0.5);
        assertThat("lower bound: need more data", output, IsCloseTo.closeTo(0.585715, precision));
        output = testStats95.fractionOfData(5, 2, 4, 3, -0.7);
        assertThat("lower bound: have enough data", output, IsCloseTo.closeTo(1.148001, precision));
        output = testStats95.fractionOfData(5, 2, 4, 3, 0.2);
        assertThat("upper bound: need more data", output, IsCloseTo.closeTo(0.0937144, precision));
        output = testStats95.fractionOfData(5, 2, 4, 3, 0.8);
        assertThat("upper bound: have enough data", output, IsCloseTo.closeTo(1.49943, precision));

        DistinguishableEffectSize output2 = testStats95.distinguishableEffectSizes(5, 2, 4, 3);
        output = testStats95.fractionOfData(5, 2, 4, 3, output2.getNegativeEffectSize());
        assertThat("lower bound: boundary", output, IsCloseTo.closeTo(1.0, precision));
        output = testStats95.fractionOfData(5, 2, 4, 3, output2.getPositiveEffectSize());
        assertThat("upper bound: boundary", output, IsCloseTo.closeTo(1.0, precision));
        output = testStats95.fractionOfData(0, 0, 2, 1, 0.1);
        assertThat("0 impressions returns 0", output, IsCloseTo.closeTo(0.0, precision));
        output = testStats95.fractionOfData(2, 1, 0, 0, 0.1);
        assertThat("0 impressions returns 0", output, IsCloseTo.closeTo(0.0, precision));
        output = testStats95.fractionOfData(0, 0, 0, 0, 0.1);
        assertThat("0 impressions returns NaN", output, is(NaN));
    }
}
