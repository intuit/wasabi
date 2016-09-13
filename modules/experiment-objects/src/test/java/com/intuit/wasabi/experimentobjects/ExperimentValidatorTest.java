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
package com.intuit.wasabi.experimentobjects;

import java.util.Date;

import org.junit.Test;

public class ExperimentValidatorTest {

	ExperimentValidator experimentValidator = new ExperimentValidator();
	
	@Test(expected=IllegalArgumentException.class)
	public void testExperimentLabelStartEndNull() {
		Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).build();
		experiment.setState(Experiment.State.DRAFT);
		experimentValidator.validateExperiment(experiment);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNewExperimentLabelStartEndNull() {
		NewExperiment newExperiment = NewExperiment.withID(Experiment.ID.newInstance()).build();

		experimentValidator.validateNewExperiment(newExperiment);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExperimentStartEndNull() {
		Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).
				withLabel(Experiment.Label.valueOf("l1")).build();
		experiment.setState(Experiment.State.DRAFT);

		experimentValidator.validateExperiment(experiment);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNewExperimentStartNull() {
		NewExperiment newExperiment = NewExperiment.withID(Experiment.ID.newInstance()).
				withLabel(Experiment.Label.valueOf("l1")).build();

		experimentValidator.validateNewExperiment(newExperiment);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExperimentEndNull() {
		Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).
				withLabel(Experiment.Label.valueOf("l1")).withStartTime(new Date()).build();
		experiment.setState(Experiment.State.DRAFT);

		experimentValidator.validateExperiment(experiment);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNewExperimentEndNull() {
		NewExperiment newExperiment = NewExperiment.withID(Experiment.ID.newInstance()).
				withLabel(Experiment.Label.valueOf("l1")).withStartTime(new Date()).build();;
		experimentValidator.validateNewExperiment(newExperiment);
	}

	@Test
	public void testExperimentSuccess() {
		Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).
				withLabel(Experiment.Label.valueOf("l1")).withStartTime(new Date()).
				withSamplingPercent(1.0d).
				withEndTime(new Date()).build();
		experiment.setState(Experiment.State.DRAFT);

		experimentValidator.validateExperiment(experiment);
	}

	@Test
	public void testNewExperimentSuccess() {
		NewExperiment newExperiment = NewExperiment.withID(Experiment.ID.newInstance()).
				withLabel(Experiment.Label.valueOf("l1")).withStartTime(new Date()).
				withSamplingPercent(1.0d).
				withEndTime(new Date()).build();

		experimentValidator.validateNewExperiment(newExperiment);
	}
}
