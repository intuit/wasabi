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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for the {@link NewExperiment} class.
 */
public class NewExperimentTest {

    @Test
    public void testBuilderCreation() {
        Experiment.ID id = Experiment.ID.newInstance();
        Date date = new Date();
        String[] tagsArray = {"ui", "mobile", "12&"};
        Set<String> tags = new HashSet<>(Arrays.asList(tagsArray));
        NewExperiment.Builder builder = NewExperiment.withID(id);
        NewExperiment newExperiment = builder.withDescription("desc")
                .withIsPersonalizationEnabled(true)
                .withModelName("m1")
                .withModelVersion("v1")
                .withRule("r1==1")
                .withSamplingPercent(0.5)
                .withStartTime(date)
                .withEndTime(date)
                .withLabel(Experiment.Label.valueOf("label"))
                .withAppName(Application.Name.valueOf("app"))
                .withIsRapidExperiment(true)
                .withUserCap(1000)
                .withCreatorID("c1")
                .withTags(tags)
                .build();

        assertThat(newExperiment.getApplicationName(), is(Application.Name.valueOf("app")));
        assertThat(newExperiment.getIsRapidExperiment(), is(true));
        assertThat(newExperiment.getUserCap(), is(1000));
        assertThat(newExperiment.getModelVersion(), is("v1"));
        assertThat(newExperiment.getState(), is(Experiment.State.DRAFT));
        assertThat(newExperiment.getCreatorID(), is("c1"));
        assertThat(newExperiment.getID(), is(id));
        assertThat(newExperiment.getDescription(), is("desc"));
        assertThat(newExperiment.getLabel(), is(Experiment.Label.valueOf("label")));
        assertThat(newExperiment.getTags().size(), is(3));
        Arrays.sort(tagsArray); // the tags should be sorted
        assertThat(newExperiment.getTags().toArray(), is(tagsArray));


        newExperiment.setApplicationName(Application.Name.valueOf("NewApp"));
        newExperiment.setCreatorID("c2");
        assertThat(newExperiment.getApplicationName(), is(Application.Name.valueOf("NewApp")));
        assertThat(newExperiment.getCreatorID(), is("c2"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuildBadSamplePercentage() {
        NewExperiment newExperiment = NewExperiment.withID(Experiment.ID.newInstance())
                .withDescription("desc")
                .withIsPersonalizationEnabled(true)
                .withModelName("m1")
                .withModelVersion("v1")
                .withRule("r1")
                .withSamplingPercent(null) //<-- this is what causes the exception
                .withLabel(Experiment.Label.valueOf("label"))
                .withAppName(Application.Name.valueOf("app"))
                .withIsRapidExperiment(true)
                .withUserCap(1000)
                .withCreatorID("c1")
                .build();
        fail();
    }

    // New Experiment build should throw IllegalArgumentException when
    // personalization is enabled and model name is not specified
    @Test(expected = IllegalArgumentException.class)
    public void testExperimentPartialPersonalizationInfo() {
        NewExperiment.withID(Experiment.ID.newInstance())
                .withAppName(Application.Name.valueOf("app"))
                .withLabel(Experiment.Label.valueOf("label"))
                .withIsPersonalizationEnabled(true)
                .withSamplingPercent(0.5)
                .withStartTime(new Date())
                .withEndTime(new Date())
                .withDescription("test").build();
    }
}
