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

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the {@link PrioritizedExperiment}
 */
public class PrioritizedExperimentTest {

    private String[] tagsArray = {"ui", "mobile", "12&"};
    private Set<String> tags = new HashSet<>(Arrays.asList(tagsArray));

    private PrioritizedExperiment prioExp = PrioritizedExperiment.withID(Experiment.ID.newInstance())
            .withApplicationName(Application.Name.valueOf("appName"))
            .withDescription("suchDescription")
            .withLabel(Experiment.Label.valueOf("thisLabel"))
            .withSamplingPercent(0.2)
            .withState(Experiment.State.RUNNING)
            .withPriority(3)
            .withUserCap(42)
            .withIsRapidExperiment(false)
            .withTags(tags)
            .build();

    private Experiment exp = Experiment.withID(Experiment.ID.newInstance())
            .withApplicationName(Application.Name.valueOf("appName"))
            .withDescription("suchDescription")
            .withLabel(Experiment.Label.valueOf("thisLabel"))
            .withSamplingPercent(0.2)
            .withState(Experiment.State.RUNNING)
            .withIsRapidExperiment(true)
            .withUserCap(300)
            .withTags(tags)
            .build();

    @Test
    public void testBuilderWithId() {
        PrioritizedExperiment prioExp = PrioritizedExperiment.from(exp, 0).build();
        assertEquals(prioExp.getID(), exp.getID());
    }

    @Test
    public void testSettersOnObject() {
        PrioritizedExperiment prioExp = PrioritizedExperiment.from(exp, 0).build();
        assertEquals(prioExp.getID(), exp.getID());
        prioExp.setUserCap(5);
        assertEquals((Integer) 5, prioExp.getUserCap());
        prioExp.setIsRapidExperiment(false);
        assertEquals(false, prioExp.getIsRapidExperiment());
        prioExp.setCreatorID("c1");
        assertEquals("c1", prioExp.getCreatorID());
        assertEquals(prioExp.hashCode(), prioExp.hashCode());
    }

    @Test
    public void testBuilderFromOtherExperiment() {
        PrioritizedExperiment fromExp = PrioritizedExperiment.from(exp, prioExp.getPriority()).build();
        //assert that all the fields are copied correctly
        assertEquals(fromExp.getID(), exp.getID());
        assertEquals(fromExp.getCreationTime(), exp.getCreationTime());
        assertEquals(fromExp.getModificationTime(), exp.getModificationTime());
        assertEquals(fromExp.getDescription(), exp.getDescription());
        assertEquals(fromExp.getSamplingPercent(), exp.getSamplingPercent());
        assertEquals(fromExp.getStartTime(), exp.getStartTime());
        assertEquals(fromExp.getEndTime(), exp.getEndTime());
        assertEquals(fromExp.getState(), exp.getState());
        assertEquals(fromExp.getLabel(), exp.getLabel());
        assertEquals(fromExp.getApplicationName(), exp.getApplicationName());
        assertEquals(fromExp.getIsRapidExperiment(), exp.getIsRapidExperiment());
        assertEquals(fromExp.getTags(), exp.getTags());
    }

    @Test
    public void testBuilderMethods() {
        Date startTime = new Date();
        PrioritizedExperiment exp = PrioritizedExperiment.withID(Experiment.ID.newInstance())
                .withIsPersonalizationEnabled(true)
                .withModelName("m1")
                .withModelVersion("1")
                .withCreatorID("c1")
                .withStartTime(startTime)
                .withCreationTime(startTime)
                .withTags(tags)
                .build();

        assertEquals(startTime, exp.getCreationTime());
        assertEquals(startTime, exp.getStartTime());
        assertEquals("m1", exp.getModelName());
        assertEquals("1", exp.getModelVersion());
        assertEquals("c1", exp.getCreatorID());
        Arrays.sort(tagsArray);
        assertArrayEquals(tagsArray, exp.getTags().toArray());
    }

    @Test
    public void testClone() throws Exception {
        assertEquals(prioExp, prioExp.clone());
    }

    @Test
    public void testIsDeleted() throws Exception {
        assertFalse(prioExp.isDeleted());
        PrioritizedExperiment prioExp2 = prioExp.clone();
        prioExp2.setState(Experiment.State.DELETED);
        assertTrue(prioExp2.isDeleted());
    }

    @Test
    public void addNullTags() {
        prioExp.setTags(null);
        assertEquals(prioExp.getTags(), null);
    }


}
