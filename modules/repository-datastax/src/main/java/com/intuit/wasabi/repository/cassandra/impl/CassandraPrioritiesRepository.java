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
import com.intuit.wasabi.experimentobjects.PrioritizedExperimentList;
import com.intuit.wasabi.repository.PrioritiesRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cassandra priorities repository implementation
 * 
 * @see PrioritiesRepository
 */
public class CassandraPrioritiesRepository implements PrioritiesRepository {

    private PrioritiesAccessor prioritiesAccessor;

    /*
     * ExperimentAccessor experimentAccessor; // TODO - Needs to be hooked up
     */

    /**
     * Logger for the class
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(CassandraPrioritiesRepository.class);

    @Inject
    public CassandraPrioritiesRepository(PrioritiesAccessor prioritiesAccessor /*, 
    	ExperimentAccessor experimentAccessor*/){
		
    	this.prioritiesAccessor = prioritiesAccessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrioritizedExperimentList getPriorities(Application.Name applicationName) {
        throw new UnsupportedOperationException("getPriorities not implemented until experimentAccessor is avaialble");
    	
        /**
        PrioritizedExperimentList prioritizedExperimentList = new PrioritizedExperimentList();

        List<UUID> priorityList = getPriorityList(applicationName);
        if (priorityList != null) {
            ExperimentList experimentList = experimentRepository.getExperiments(priorityList);
            int priorityValue = 1;
            for (Experiment experiment : experimentList.getExperiments()) {
                prioritizedExperimentList.
                        addPrioritizedExperiment(PrioritizedExperiment.from(experiment, priorityValue).build());
                priorityValue += 1;
            }
        }
        return prioritizedExperimentList;
        **/
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
    public void createPriorities(Application.Name applicationName, List<Experiment.ID> priorityIds) {
    	LOGGER.debug("Creating priority list for {} and ids {}", 
    			applicationName, priorityIds);

        if (priorityIds.isEmpty()) {

        	LOGGER.debug("Deleting priority list for {} and ids {}", 
        			applicationName, priorityIds);
            try {
            	prioritiesAccessor.deletePriorities(applicationName.toString());
            } catch (Exception e) {
            	LOGGER.error("Exception while deleting priority list for {} and ids {}", 
            			new Object [] {applicationName, priorityIds}, e);

            	throw new RepositoryException("Unable to delete the priority list for the application: \""
                        + applicationName.toString() + "\"" + e);
            }
            
        } else {

        	LOGGER.debug("Updating priority list for {} and ids {}", applicationName, priorityIds);
        	
        	List<UUID> experimentIds = new ArrayList<>();
        	for (Experiment.ID experimentId : priorityIds) {
        		experimentIds.add(experimentId.getRawID());
        	}
        	
            try {
            	prioritiesAccessor.
            		updatePriorities(experimentIds, applicationName.toString());
            } catch (Exception e) {
            	LOGGER.error("Exception while updating priority list for {} and ids {}", 
            			new Object [] {applicationName, priorityIds}, e);
            	
                throw new RepositoryException("Unable to modify the priority list " + experimentIds + " for application: \""
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
        	Result<com.intuit.wasabi.repository.cassandra.pojo.Application> priorities = prioritiesAccessor.getPriorities(applicationName.toString());
        	for (com.intuit.wasabi.repository.cassandra.pojo.Application priority : priorities.all()) {
        		for (UUID uuid : priority.getPriorities()) {
        			experimentIds.add(Experiment.ID.valueOf(uuid));
        		}
        	}

        } catch (Exception e) {
        	LOGGER.error("Exception while updating priority list for {} and ids {}", 
        			new Object [] {applicationName}, e);
            throw new RepositoryException("Unable to retrieve the priority list for application: \""
                    + applicationName.toString() + "\"" + e);
        }
        
        return experimentIds;
    }
}
