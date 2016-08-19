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

import com.google.inject.Inject;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.repository.MutexRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.MutexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.Exclusion;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Result;

import java.io.IOException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mutax repo cassandra implementation
 * 
 * @see MutexRepository
 */
public class CassandraMutexRepository implements MutexRepository {

	/**
	 * Logger for the class
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraMutexRepository.class);

    /**
     * TODO - Hook up experiment accessor
     */
    private final ExperimentAccessor experimentAccessor;
	private final MutexAccessor mutexAccessor;

	private Session session;

    @Inject
    public CassandraMutexRepository(
            ExperimentAccessor experimentAccessor,
            MutexAccessor mutexAccessor,
            CassandraDriver driver)
            throws IOException, ConnectionException {
        this.experimentAccessor = experimentAccessor;
        this.session = driver.getSession();
        this.mutexAccessor = mutexAccessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Experiment.ID> getExclusionList(Experiment.ID experimentID) {
    	LOGGER.debug("Getting exclusions for {}", experimentID);
    	
    	List<Experiment.ID> exclusionIds = new ArrayList<>();
    	
        try {
        	Result<Exclusion> exclusions = mutexAccessor.getExclusions(experimentID.getRawID());
        	
        	for (Exclusion exclusion : exclusions.all()) {
        		exclusionIds.add(Experiment.ID.valueOf(exclusion.getPair()));
        	}
        	
        	
        } catch (Exception e) {
            throw new RepositoryException("Could not fetch exclusions for experiment \"" + experimentID + "\" ", e);
        }
        
    	LOGGER.debug("Getting exclusions list of size {}", exclusionIds);

    	return exclusionIds;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExclusion(Experiment.ID base, Experiment.ID pair) throws RepositoryException {
    	LOGGER.debug("Deleting exclusions for base {} pair {}", new Object[] {base, pair});

        try {
        	BatchStatement batchStatement = new BatchStatement();
        	batchStatement.add(mutexAccessor.deleteExclusion(base.getRawID(), 
        			pair.getRawID()));
        	batchStatement.add(mutexAccessor.deleteExclusion(pair.getRawID(), 
        			base.getRawID()));
        	session.execute(batchStatement);
        } catch (Exception e) {
            throw new RepositoryException("Could not delete the exclusion \"" + base + "\", \"" + pair + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createExclusion(Experiment.ID baseID, Experiment.ID pairID) throws RepositoryException {

    	LOGGER.debug("Create exclusions for {}", new Object [] {baseID, pairID});
        try {
        	BatchStatement batchStatement = new BatchStatement();
        	batchStatement.add(mutexAccessor.createExclusion(baseID.getRawID(), 
        			pairID.getRawID()));
        	batchStatement.add(mutexAccessor.createExclusion(pairID.getRawID(), 
        			baseID.getRawID()));
        	session.execute(batchStatement);
        } catch (Exception e) {
            throw new RepositoryException("Could not insert the exclusion \"" + baseID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    // TODO - Hook up to experiment accessor
    @Override
    public ExperimentList getExclusions(Experiment.ID base) {
    	throw new UnsupportedOperationException("Not implemented yet");
 /*
    	try {
            Collection<Experiment.ID> pairIDs = driver.getKeyspace().prepareQuery(keyspace.exclusion_CF())
                    .getKey(base).execute().getResult().getColumnNames();

            return experimentRepository.getExperiments(new ArrayList<>(pairIDs));
        	return null;
        } catch (Exception e) {
            throw new RepositoryException("Could not retrieve the exclusions for \"" + base + "\"", e);
        }
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    // TODO - Hook up to experiment accessor
    public ExperimentList getNotExclusions(Experiment.ID base) {
    	throw new UnsupportedOperationException("Not implemented yet");
    	/*
        try {
            Collection<Experiment.ID> pairIDs = driver.getKeyspace().prepareQuery(keyspace.exclusion_CF())
                    .getKey(base).execute().getResult().getColumnNames();

            //get info about the experiment
            Experiment experiment = experimentRepository.getExperiment(base);
            //get the application name
            Application.Name appName = experiment.getApplicationName();
            //get all experiments with this application name
            List<Experiment> ExpList = experimentRepository.getExperiments(appName);
            List<Experiment> notMutex = new ArrayList<>();
            for (Experiment exp : ExpList) {
                if (!pairIDs.contains(exp.getID()) && !exp.getID().equals(base)) {
                    notMutex.add(exp);
                }
            }

            ExperimentList result = new ExperimentList();
            result.setExperiments(notMutex);
            return result;
        	return null;
        } catch (Exception e) {
            throw new RepositoryException("Could not retrieve the exclusions for \"" + base + "\"", e);
        }
            */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Experiment.ID, List<Experiment.ID>> getExclusivesList(
    		Collection<Experiment.ID> experimentIDCollection) {
    	LOGGER.debug("Getting exclusions for {}", experimentIDCollection);
    	
        Map<Experiment.ID, List<Experiment.ID>> result = new HashMap<>(experimentIDCollection.size());

        try {
        	for (Experiment.ID experimentId : experimentIDCollection) {
        		result.put(experimentId, getExclusionList(experimentId));
        	}
        } catch (Exception e) {
            throw new RepositoryException("Could not fetch mutually exclusive experiments for the list of experiments"
                    , e);
        }
        
    	LOGGER.debug("Returning exclusions map {}", result);
    	
        return result;
    }
}
