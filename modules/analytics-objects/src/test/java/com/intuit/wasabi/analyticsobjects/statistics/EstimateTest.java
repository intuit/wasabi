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
package com.intuit.wasabi.analyticsobjects.statistics;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EstimateTest {
    Double estimate;
    Double lowerBound;
    Double upperBound;
    Estimate estimator;

    @Before
    public void setup(){
        estimate = 0.5;
        lowerBound = 0.45;
        upperBound = 0.55;
        estimator = new Estimate.Builder().withEstimate(estimate).withLowerBound(lowerBound)
                    .withUpperBound(upperBound).build();
    }

    @Test
    public void testBuilder(){
        assertEquals(estimator.getEstimate(), estimate);
        assertEquals(estimator.getLowerBound(), lowerBound);
        assertEquals(estimator.getUpperBound(), upperBound);

        assertNotNull(estimator.hashCode());
        assertNotNull(estimator.toString());
        assertNotNull(estimator.clone());
        assertTrue(estimator.equals(estimator));
        assertFalse(estimator.equals(null));
        assertFalse(estimator.equals(upperBound));
    }

    @Test
    public void testSettersandGetters(){
        estimator.setEstimate(0.0);
        estimator.setLowerBound(0.0);
        estimator.setUpperBound(0.0);

        Estimate otherEstimator = new Estimate(0.0, 0.0, 0.0);
        assertTrue(estimator.equals(otherEstimator));
    }
}
