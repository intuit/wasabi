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

import static org.junit.Assert.*;

public class DistinguishableEffectSizeTest {
    Double negativeEffectSize;
    Double positiveEffectSize;
    DistinguishableEffectSize effectSize;

    @Before
    public void setup() {
        negativeEffectSize = 0.5;
        positiveEffectSize = 0.5;
        effectSize = new DistinguishableEffectSize.Builder().withNegativeEffectSize(negativeEffectSize)
                .withPositiveEffectSize(positiveEffectSize).build();
    }

    @Test
    public void testBuilder() {
        assertEquals(effectSize.getNegativeEffectSize(), negativeEffectSize);
        assertEquals(effectSize.getPositiveEffectSize(), positiveEffectSize);

        DistinguishableEffectSize otherEffectSize = new DistinguishableEffectSize(negativeEffectSize, positiveEffectSize);

        assertNotNull(effectSize.hashCode());
        assertNotNull(effectSize.toString());
        assertNotNull(effectSize.clone());

        assertTrue(effectSize.equals(otherEffectSize));
        assertTrue(effectSize.equals(effectSize));
        assertFalse(effectSize.equals(null));
        assertFalse(effectSize.equals(negativeEffectSize));
    }

    @Test
    public void testSettersandGetters() {
        negativeEffectSize = 0.0;
        positiveEffectSize = 0.0;
        effectSize.setNegativeEffectSize(negativeEffectSize);
        assertEquals(effectSize.getNegativeEffectSize(), negativeEffectSize);
        effectSize.setPositiveEffectSize(positiveEffectSize);
        assertEquals(effectSize.getPositiveEffectSize(), positiveEffectSize);
    }
}
