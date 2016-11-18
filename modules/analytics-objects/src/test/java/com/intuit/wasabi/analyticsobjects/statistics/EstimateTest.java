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

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests the {@link Estimate}.
 */
public class EstimateTest {

    private Double estimate;
    private Double lowerBound;
    private Double upperBound;
    private Estimate estimator;

    @Before
    public void setup() {
        estimate = 0.5;
        lowerBound = 0.45;
        upperBound = 0.55;
        estimator = new Estimate.Builder().withEstimate(estimate).withLowerBound(lowerBound)
                .withUpperBound(upperBound).build();
    }

    @Test
    public void testBuilder() {
        assertThat(estimator.getEstimate(), is(estimate));
        assertThat(estimator.getLowerBound(), is(lowerBound));
        assertThat(estimator.getUpperBound(), is(upperBound));

        assertThat(estimator.hashCode(), is(estimator.clone().hashCode()));

        String est = estimator.toString();
        assertThat(est, containsString(String.valueOf(estimate)));
        assertThat(est, containsString(String.valueOf(lowerBound)));
        assertThat(est, containsString(String.valueOf(upperBound)));

        assertThat(estimator, equalTo(estimator));
        assertThat(estimator, not(equalTo(null)));
        assertThat(estimator, not(equalTo(upperBound)));
    }

    @Test
    public void testSettersandGetters() {
        estimator.setEstimate(0.0);
        estimator.setLowerBound(0.0);
        estimator.setUpperBound(0.0);

        Estimate otherEstimator = new Estimate(0.0, 0.0, 0.0);
        assertThat(estimator, equalTo(otherEstimator));
    }
}
