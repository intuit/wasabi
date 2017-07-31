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
package com.intuit.wasabi.experimentobjects;


import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * This class is testing the functionality of the {@link ExperimentBatch}
 */
public class ExperimentBatchTest {

    private Set<Experiment.Label> labels = Sets.newHashSet(Experiment.Label.valueOf("a"),
            Experiment.Label.valueOf("b"), Experiment.Label.valueOf("c"));

    private static Map<String, Object> profile;

    private static Map<String, Object> personalizationParameters;

    static {
        profile = new HashMap<>();
        profile.put("a.b.c", 42);
        profile.put("d.e.f", 84);

        personalizationParameters = new HashMap<>();
        personalizationParameters.put("g.h.i", 42);
        personalizationParameters.put("j.k.l", 84);
    }

    private ExperimentBatch expBatch = ExperimentBatch.newInstance()
            .withLabels(labels)
            .withProfile(profile)
            .withPersonalizationParameters(personalizationParameters)
            .build();


    @Test
    public void testBuilder() {
        ExperimentBatch expBatchCopy = ExperimentBatch.from(expBatch).build();
        assertEquals(expBatch, expBatchCopy);

        assertEquals(expBatch.getLabels(), labels);
        assertEquals(expBatch.getProfile(), profile);
        assertEquals(expBatch.getPersonalizationParameters(), personalizationParameters);
    }


    @Test
    public void testEquals() {
        ExperimentBatch expBatchCopy = ExperimentBatch.from(expBatch).build();
        expBatchCopy.getProfile().remove("a.b.c");

        assertNotEquals(expBatch, expBatchCopy);
        assertNotEquals(expBatch, null);
        assertNotEquals(expBatch, 42);

        assertEquals(expBatch, expBatch);
    }

    @Test
    public void testHashCode() {
        ExperimentBatch expBatchCopy = ExperimentBatch.from(expBatch).build();

        assertEquals(expBatch.hashCode(), expBatchCopy.hashCode());
        expBatchCopy.setProfile(new HashMap<String, Object>());
        assertNotEquals(expBatch.hashCode(), expBatchCopy.hashCode());

    }

    @Test
    public void testToString() {
        String out = "ExperimentBatch labels=" + labels
                + ", profile=" + profile
                + ", personalizationParameters=" + personalizationParameters;
        assertEquals(expBatch.toString(), out);


    }


}
