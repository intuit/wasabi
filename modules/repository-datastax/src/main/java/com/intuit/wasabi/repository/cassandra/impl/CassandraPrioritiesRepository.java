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
package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.mapping.Result;
import com.google.inject.Inject;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.PrioritizedExperiment;
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.PrioritiesRepository;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cassandra priorities repository implementation
 * 
 * @see PrioritiesRepository
 */
public class CassandraPrioritiesRepository implements PrioritiesRepository {

	private PrioritiesAccessor prioritiesAccessor;

	private ExperimentAccessor experimentAccessor;

	/**
	 * Logger for the class
	 */
	protected static final Logger LOGGER = LoggerFactory
			.getLogger(CassandraPrioritiesRepository.class);

	@Inject
	public CassandraPrioritiesRepository(PrioritiesAccessor prioritiesAccessor,
			ExperimentAccessor experimentAccessor) {

		this.prioritiesAccessor = prioritiesAccessor;
		this.experimentAccessor = experimentAccessor;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PrioritizedExperimentList getPriorities(
			Application.Name applicationName) {

		LOGGER.debug("Getting priorities for {} ", applicationName);

		PrioritizedExperimentList prioritizedExperimentList = new PrioritizedExperimentList();

		try {
			List<ID> priorityList = getPriorityList(applicationName);

			LOGGER.debug("Received priorities list {} for {} ", new Object[] {
					priorityList, applicationName });

			if (priorityList != null) {

				List<UUID> priorityUUIDs = priorityList.stream()
						.map(id -> id.getRawID()).collect(Collectors.toList());

				Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experimentsResult = 
						experimentAccessor.getExperiments(priorityUUIDs);

				List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experimentPojos = 
						experimentsResult.all();

				LOGGER.debug("Received experimentPojos {} for priorityUUIDs {}",
						new Object[] { experimentPojos, priorityUUIDs });

				int priorityValue = 1;
				for (com.intuit.wasabi.repository.cassandra.pojo.Experiment experimentPojo : experimentPojos) {
					prioritizedExperimentList
							.addPrioritizedExperiment(PrioritizedExperiment
									.from(ExperimentHelper
											.makeExperiment(experimentPojo),
											priorityValue).build());

					priorityValue += 1;
				}
			}

		} catch (Exception e) {
			LOGGER.error("Exception while getting priority list for {} ",
					new Object[] { applicationName }, e);
			throw new RepositoryException(
					"Unable to retrieve the priority list for application: \""
							+ applicationName.toString() + "\"" + e);

		}
		
		LOGGER.debug("Returning prioritizedExperimentList {} ",prioritizedExperimentList);

		return prioritizedExperimentList;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPriorityListLength(Application.Name applicationName) {

		LOGGER.debug("Getting priority list length for {}", applicationName);

		return getPriorityList(applicationName).size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void createPriorities(Application.Name applicationName,
			List<Experiment.ID> priorityIds) {
		
		LOGGER.debug("Creating priority list for {} and ids {}",
				applicationName, priorityIds);

		if (priorityIds.isEmpty()) {

			LOGGER.debug("Deleting priority list for {} and ids {}",
					applicationName, priorityIds);
			try {
				prioritiesAccessor.deletePriorities(applicationName.toString());
			} catch (Exception e) {
				LOGGER.error(
						"Exception while deleting priority list for {} and ids {}",
						new Object[] { applicationName, priorityIds }, e);

				throw new RepositoryException(
						"Unable to delete the priority list for the application: \""
								+ applicationName.toString() + "\"" + e);
			}

		} else {

			LOGGER.debug("Updating priority list for {} and ids {}",
					applicationName, priorityIds);

			List<UUID> experimentIds = new ArrayList<>();
			for (Experiment.ID experimentId : priorityIds) {
				experimentIds.add(experimentId.getRawID());
			}

			try {
				prioritiesAccessor.updatePriorities(experimentIds,
						applicationName.toString());
			} catch (Exception e) {
				LOGGER.error(
						"Exception while updating priority list for {} and ids {}",
						new Object[] { applicationName, priorityIds }, e);

				throw new RepositoryException(
						"Unable to modify the priority list " + experimentIds
								+ " for application: \""
								+ applicationName.toString() + "\"" + e);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Experiment.ID> getPriorityList(Application.Name applicationName) {

		LOGGER.debug("Getting priority list  for {} ", applicationName);

		List<Experiment.ID> experimentIds = new ArrayList<>();
		try {
			Result<com.intuit.wasabi.repository.cassandra.pojo.Application> priorities = prioritiesAccessor
					.getPriorities(applicationName.toString());
			for (com.intuit.wasabi.repository.cassandra.pojo.Application priority : priorities
					.all()) {
				for (UUID uuid : priority.getPriorities()) {
					experimentIds.add(Experiment.ID.valueOf(uuid));
				}
			}

		} catch (Exception e) {
			LOGGER.error("Exception while getting priority list for {} ",
					new Object[] { applicationName }, e);
			throw new RepositoryException(
					"Unable to retrieve the priority list for application: \""
							+ applicationName.toString() + "\"" + e);
		}

		return experimentIds;
	}
}
