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

import org.hamcrest.number.IsCloseTo;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class DecisionAdjusterTest {

    private double precision = 0.00001;

    @Test
    public void scaledZ() throws Exception {
        Double output = DecisionAdjuster.scaledZ(0.95, 23);
        assertThat("test 1", output, IsCloseTo.closeTo(3.0653832, precision));

        output = DecisionAdjuster.scaledZ(0.99, 23);
        assertThat("test 2", output, IsCloseTo.closeTo(3.5180182, precision));

        output = DecisionAdjuster.scaledZ(0.80, 23);
        assertThat("test 3", output, IsCloseTo.closeTo(2.6237934, precision));

        output = DecisionAdjuster.scaledZ(0.95, 18);
        assertThat("test 4", output, IsCloseTo.closeTo(2.9913161, precision));

        output = DecisionAdjuster.scaledZ(0.95, 33);
        assertThat("test 5", output, IsCloseTo.closeTo(3.1717658, precision));
    }

    @Test(expected = IllegalArgumentException.class)
    public void scaledZInputs() throws Exception {
        DecisionAdjuster.scaledZ(0.95, 0);
    }

    @Test
    public void calcMaxDecisions() throws Exception {
        Double output = DecisionAdjuster.calcMaxDecisions((long) 1, (long) Math.pow(10, 10));
        assertThat("log(10^10/1) = 23", output, IsCloseTo.closeTo(23.0258509, precision));

        output = DecisionAdjuster.calcMaxDecisions((long) 100, (long) Math.pow(10, 10));
        assertThat("log(10^10/100) = 18", output, IsCloseTo.closeTo(18.4206807, precision));

        output = DecisionAdjuster.calcMaxDecisions((long) 1, (long) Math.pow(10, 11));
        assertThat("log(10^11/1) = 25", output, IsCloseTo.closeTo(25.328436, precision));
    }
}
