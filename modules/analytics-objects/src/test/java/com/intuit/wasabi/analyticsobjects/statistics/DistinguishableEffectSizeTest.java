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
 * Test for the {@link DistinguishableEffectSize}.
 */
public class DistinguishableEffectSizeTest {

    private Double negativeEffectSize;
    private Double positiveEffectSize;
    private DistinguishableEffectSize effectSize;

    @Before
    public void setup() {
        negativeEffectSize = 0.5;
        positiveEffectSize = 0.5;
        effectSize = new DistinguishableEffectSize.Builder().withNegativeEffectSize(negativeEffectSize)
                .withPositiveEffectSize(positiveEffectSize).build();
    }

    @Test
    public void testBuilder() {
        assertThat(effectSize.getNegativeEffectSize(), is(negativeEffectSize));
        assertThat(effectSize.getPositiveEffectSize(), is(positiveEffectSize));

        DistinguishableEffectSize otherEffectSize = new DistinguishableEffectSize(negativeEffectSize, positiveEffectSize);

        assertThat(effectSize.hashCode(), equalTo(effectSize.clone().hashCode()));

        String efSize = effectSize.toString();
        assertThat(efSize, containsString(String.valueOf(negativeEffectSize)));
        assertThat(efSize, containsString(String.valueOf(positiveEffectSize)));

        assertThat(effectSize, equalTo(otherEffectSize));
        assertThat(effectSize, equalTo(effectSize));
        assertThat(effectSize, not(equalTo(null)));
        assertThat(effectSize, not(equalTo(negativeEffectSize)));
    }

    @Test
    public void testSettersandGetters() {
        negativeEffectSize = 0.0;
        positiveEffectSize = 0.0;
        effectSize.setNegativeEffectSize(negativeEffectSize);
        assertThat(effectSize.getNegativeEffectSize(), is(negativeEffectSize));
        effectSize.setPositiveEffectSize(positiveEffectSize);
        assertThat(effectSize.getPositiveEffectSize(), is(positiveEffectSize));
    }
}
