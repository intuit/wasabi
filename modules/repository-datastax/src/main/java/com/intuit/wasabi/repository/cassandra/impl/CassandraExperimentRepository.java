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

import static org.slf4j.LoggerFactory.getLogger;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketAssignmentCount;
import com.intuit.wasabi.analyticsobjects.counts.TotalUsers;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.exceptions.ConstraintViolationException;
import com.intuit.wasabi.exceptions.ExperimentNotFoundException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.BucketAuditInfo;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ExperimentAuditInfo;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.Experiment.State;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.audit.BucketAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.ExperimentAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentState;
import com.intuit.wasabi.repository.cassandra.accessor.ApplicationListAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.BucketAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentLabelIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.StateExperimentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.UserBucketIndexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentByAppNameLabel;
import com.intuit.wasabi.repository.cassandra.pojo.index.StateExperimentIndex;

import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cassandra experiment repository
 * 
 * @see ExperimentRepository
 */
class CassandraExperimentRepository implements ExperimentRepository {

	private final ExperimentValidator validator;

	private CassandraDriver driver;

	private ExperimentAccessor experimentAccessor;

	private ExperimentLabelIndexAccessor experimentLabelIndexAccessor;

	private UserBucketIndexAccessor userBucketIndexAccessor;

	private BucketAccessor bucketAccessor;

	private ApplicationListAccessor applicationListAccessor;
	
	private StateExperimentIndexAccessor stateExperimentIndexAccessor;

	private BucketAuditLogAccessor bucketAuditLogAccessor;

	private ExperimentAuditLogAccessor experimentAuditLogAccessor;

	/**
	 * Logger for this class
	 */
	private static final Logger LOGGER = getLogger(CassandraExperimentRepository.class);

	@Inject
	public CassandraExperimentRepository(CassandraDriver driver,
			ExperimentAccessor experimentAccessor,
			ExperimentLabelIndexAccessor experimentLabelIndexAccessor,
			UserBucketIndexAccessor userBucketIndexAccessor,
			BucketAccessor bucketAccessor,
			ApplicationListAccessor applicationListAccessor,
			BucketAuditLogAccessor bucketAuditLogAccessor, 
			ExperimentAuditLogAccessor experimentAuditLogAccessor, 
			StateExperimentIndexAccessor stateExperimentIndexAccessor,
			ExperimentValidator validator) {
		this.driver = driver;
		this.experimentAccessor = experimentAccessor;
		this.experimentLabelIndexAccessor = experimentLabelIndexAccessor;
		this.userBucketIndexAccessor = userBucketIndexAccessor;
		this.bucketAccessor = bucketAccessor;
		this.applicationListAccessor = applicationListAccessor;
		this.stateExperimentIndexAccessor = stateExperimentIndexAccessor;
		this.bucketAuditLogAccessor = bucketAuditLogAccessor;
		this.experimentAuditLogAccessor = experimentAuditLogAccessor;
		this.validator = validator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Experiment getExperiment(Experiment.ID experimentID) {
		LOGGER.debug("Getting experiment", experimentID);

		return internalGetExperiment(experimentID);
	}

	/**
	 * Get experiment
	 * 
	 * @param experimentID
	 *            experiment id
	 * @param consistency
	 *            cassandra consistency level
	 * @return the experiment @ repository failure
	 */
	protected Experiment internalGetExperiment(Experiment.ID experimentID) {

		LOGGER.debug("Getting experiment {}", experimentID);

		Preconditions.checkNotNull(experimentID, "Parameter \"experimentID\" cannot be null");

		try {

			com.intuit.wasabi.repository.cassandra.pojo.Experiment experimentPojo = experimentAccessor
					.getExperimentById(experimentID.getRawID()).one();

			LOGGER.debug("Experiment retrieved {}", experimentPojo);

			if (experimentPojo == null)
				return null;
			
			if (State.DELETED.name().equals(experimentPojo.getState()))
				return null;

			return ExperimentHelper.makeExperiment(experimentPojo);
		} catch (Exception e) {
			LOGGER.error("Exception while getting experiment {}", experimentID);
			throw new RepositoryException("Could not retrieve experiment with ID \"" + experimentID + "\"", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Experiment getExperiment(Application.Name appName,
			Experiment.Label experimentLabel) {

		LOGGER.debug("Getting App {} with label {}", new Object[] { appName,experimentLabel });

		return internalGetExperiment(appName, experimentLabel);
	}

	/**
	 * Get experiment by params
	 * 
	 * @param appName
	 *            application name
	 * @param experimentLabel
	 *            experiment label
	 * @param consistency
	 *            cassandra consistency level
	 * @return the experiment
	 */
	protected Experiment internalGetExperiment(Application.Name appName,
			Experiment.Label experimentLabel) {

		LOGGER.debug("Getting experiment by app {} with label {} ",	new Object[] { appName, experimentLabel });

		Preconditions.checkNotNull(appName,"Parameter \"appName\" cannot be null");
		Preconditions.checkNotNull(experimentLabel,"Parameter \"experimentLabel\" cannot be null");

		Experiment experiment = null;
		
		try {
			ExperimentByAppNameLabel experimentAppLabel = experimentLabelIndexAccessor
					.getExperimentBy(appName.toString(),
							experimentLabel.toString()).one();

			if (experimentAppLabel != null) {
				com.intuit.wasabi.repository.cassandra.pojo.Experiment experimentPojo = experimentAccessor
						.getExperimentById(experimentAppLabel.getId()).one();
				experiment = ExperimentHelper.makeExperiment(experimentPojo);
			} 
			
		} catch (Exception e) {
			LOGGER.error("Error while getting experiment by app {} with label {} ",
					new Object[] { appName, experimentLabel }, e);

			throw new RepositoryException("Could not retrieve experiment \""
					+ appName + "\".\"" + experimentLabel + "\"", e);
		}

		LOGGER.debug("Returning experiment {}", experiment);

		return experiment;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Experiment.ID createExperiment(NewExperiment newExperiment) {

		validator.validateNewExperiment(newExperiment);

		// Ensure no other experiment is using the label
		Experiment existingExperiment = getExperiment(
				newExperiment.getApplicationName(), newExperiment.getLabel());

		if (existingExperiment != null) {
			throw new ConstraintViolationException(
					ConstraintViolationException.Reason.UNIQUE_CONSTRAINT_VIOLATION,
					"An active experiment with label \""
							+ newExperiment.getApplicationName() + "\".\""
							+ newExperiment.getLabel()
							+ "\" already exists (id = "
							+ existingExperiment.getID() + ")", null);
		}

		// Yes, there is a race condition here, where two experiments
		// being created with the same app name/label could result in one being
		// clobbered. In practice, this should never happen, but...
		// TODO: Implement a transactional recipe

		final Date NOW = new Date();
		final Experiment.State DRAFT = State.DRAFT;

		try {
			experimentAccessor.insertExperiment(
				newExperiment.getId().getRawID(),
				(newExperiment.getDescription() != null) ? newExperiment
						.getDescription() : "",
				(newExperiment.getRule() != null) ? newExperiment.getRule()
						: "",
				newExperiment.getSamplingPercent(),
				newExperiment.getStartTime(),
				newExperiment.getEndTime(),
				DRAFT.name(),
				newExperiment.getLabel().toString(),
				newExperiment.getApplicationName().toString(),
				NOW,
				NOW,
				newExperiment.getIsPersonalizationEnabled(),
				newExperiment.getModelName(),
				newExperiment.getModelVersion(),
				newExperiment.getIsRapidExperiment(),
				newExperiment.getUserCap(),
				(newExperiment.getCreatorID() != null) ? newExperiment
						.getCreatorID() : "");

			// TODO - Do we need to create an application while creating a new experiment ?
			createApplication(newExperiment.getApplicationName());
		}
		catch(Exception e) {
			LOGGER.error("Error while creating experiment {}", newExperiment, e);
			throw new RepositoryException("Exception while creating experiment " + newExperiment + " message " + e, e);
		}
		return newExperiment.getId();

	}

	/**
	 * Create indices for new experiment
	 *
	 * @param newExperiment
	 *            the new experiment object
	 * 
	 *            TODO: Need more clarification
	 */
    // TODO - Why is this method is on the interface - if the client does not call it, the indices will be inconsistent ?	
	@Override
	public void createIndicesForNewExperiment(NewExperiment newExperiment) {
		// Point the experiment index to this experiment
		LOGGER.debug("Create indices for new experiment Experiment {}",newExperiment);

			updateExperimentLabelIndex(newExperiment.getID(),
				newExperiment.getApplicationName(), newExperiment.getLabel(),
				newExperiment.getStartTime(), newExperiment.getEndTime(),
				State.DRAFT);

		try {
			 updateStateIndex( newExperiment.getID(), ExperimentState.NOT_DELETED );
		} catch (Exception e) {
			LOGGER.error("Create indices for new experiment Experiment {} failed",newExperiment, e);
			// remove the created ExperimentLabelIndex
			removeExperimentLabelIndex(newExperiment.getApplicationName(),
					newExperiment.getLabel());
			throw new RepositoryException("Could not update indices for experiment \""
							+ newExperiment + "\"", e);
		}
	}

	/**
	 * Get the summary of assignments delivered for each experiment
	 */
	@Override
	public AssignmentCounts getAssignmentCounts(Experiment.ID experimentID,
			Context context) {
		
		LOGGER.debug("Get Assignment Counts for Experiment {} and context {}",
				new Object[] { experimentID, context });

		List<Bucket> bucketList = getBuckets(experimentID).getBuckets();

		AssignmentCounts.Builder builder = new AssignmentCounts.Builder();
		builder.withExperimentID(experimentID);

		List<BucketAssignmentCount> bucketAssignmentCountList = new ArrayList<>(
				bucketList.size() + 1);
		long bucketAssignmentsCount = 0, nullAssignmentsCount = 0;

		for (Bucket bucket : bucketList) {

			try {
				ResultSet counts = userBucketIndexAccessor.countUserBy(
						experimentID.getRawID(), context.toString(), bucket
								.getLabel().toString());
				Long count = counts.one().get(0, Long.class);
				bucketAssignmentCountList
						.add(new BucketAssignmentCount.Builder()
								.withBucket(bucket.getLabel()).withCount(count)
								.build());
				bucketAssignmentsCount += count;

			} catch (Exception e) {
				LOGGER.error(
						"Get Assignment Counts for Experiment {} and context {} failed",
						new Object[] { experimentID, context }, e);
				throw new RepositoryException("Could not fetch assignmentCounts for experiment "
								+ "with ID \"" + experimentID + "\"", e);
			}

		}

		// Checking the count for null assignments
		try {
			ResultSet counts = userBucketIndexAccessor.countUserBy(
					experimentID.getRawID(), context.toString(), "");
			nullAssignmentsCount = counts.one().get(0, Long.class);
			bucketAssignmentCountList.add(new BucketAssignmentCount.Builder()
					.withBucket(null).withCount(nullAssignmentsCount).build());
		} catch (Exception e) {
			LOGGER.error("Get Assignment Counts for Experiment {} and context {} failed",
					new Object[] { experimentID, context }, e);
			throw new RepositoryException("Could not fetch assignmentCounts for experiment "
							+ "with ID \"" + experimentID + "\"", e);
		}

		return builder
				.withBucketAssignmentCount(bucketAssignmentCountList)
				.withTotalUsers(
						new TotalUsers.Builder()
								.withTotal(
										bucketAssignmentsCount
												+ nullAssignmentsCount)
								.withBucketAssignments(bucketAssignmentsCount)
								.withNullAssignments(nullAssignmentsCount)
								.build()).build();

	}

	/**
	 * Get a bucket list for a list of Experiments using a single cassandra call
	 */
	@Override
	public Map<Experiment.ID, BucketList> getBucketList(Collection<Experiment.ID> experimentIDCollection) {

		LOGGER.debug("Getting buckets list by experimentId {}", experimentIDCollection);

		List<UUID> experimentIds = ExperimentHelper.makeUUIDs(experimentIDCollection);

		try {
			Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket> buckets = 
					bucketAccessor.getBucketByExperimentIds(experimentIds);

			Map<Experiment.ID, BucketList> result = new HashMap<>();

			for (com.intuit.wasabi.repository.cassandra.pojo.Bucket bucketPojo : buckets
					.all()) {

				Bucket bucket = BucketHelper.makeBucket(bucketPojo);

				BucketList bucketList = result.get(bucket.getExperimentID());
				if (bucketList == null) {
					bucketList = new BucketList();
					bucketList.addBucket(bucket);
				} else {
					bucketList.addBucket(bucket);
				}
				
				result.put(bucket.getExperimentID(), bucketList);
			}
			
			LOGGER.debug("Returning result {}", result);
			
			return result;
			
		} catch (Exception e) {
			LOGGER.error("getBucketList for {} failed", experimentIDCollection, e);
			throw new RepositoryException("Could not fetch buckets for the list of experiments", e);
		}

	}

	/**
	 * Get the list of buckets for an experiment
	 *
	 * @param experimentID
	 *            experiment id
	 * @return a list of buckets
	 */
	@Override
	public BucketList getBucketList(Experiment.ID experimentID) {
		LOGGER.debug("Getting buckets list by one experimentId {}",experimentID);

		BucketList bucketList = new BucketList();

		try {
			Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucketPojos = bucketAccessor
					.getBucketByExperimentId(experimentID.getRawID());

			for (com.intuit.wasabi.repository.cassandra.pojo.Bucket bucketPojo : bucketPojos
					.all()) {
				bucketList.addBucket(BucketHelper.makeBucket(bucketPojo));
			}

		} catch (Exception e) {
			LOGGER.error("Getting bucket list by one experiment id {} failed", experimentID, e);
			throw new RepositoryException("Could not fetch buckets for experiment \"" + experimentID
							+ "\" ", e);
		}

		LOGGER.debug("Returning buckets list by one experimentId {} bucket {}",
				new Object[] { experimentID, bucketList });

		return bucketList;
	}

	@Override
    public Experiment updateExperiment(Experiment experiment) {

		LOGGER.debug("Updating experiment  {}",experiment);
		
		validator.validateExperiment(experiment);
		
		try {
		   // Note that this timestamp gets serialized as mulliseconds from
		   // the epoch, so timezone is irrelevant
		   final Date NOW = new Date();
	
		   experimentAccessor.updateExperiment(
				   experiment.getDescription() != null ? experiment.getDescription() : "", 
                   experiment.getRule() != null ? experiment.getRule() : "", 
                   experiment.getSamplingPercent(), 
                   experiment.getStartTime(), 
                   experiment.getEndTime(), 
                   experiment.getState().name(), 
                   experiment.getLabel().toString(), 
                   experiment.getApplicationName().toString(), 
                   NOW, 
                   experiment.getIsPersonalizationEnabled(), 
                   experiment.getModelName(), 
                   experiment.getModelVersion(), 
                   experiment.getIsRapidExperiment(), 
                   experiment.getUserCap(), 
                   experiment.getID().getRawID());
		
		   // Point the experiment index to this experiment
		   updateExperimentLabelIndex(experiment.getID(), experiment.getApplicationName(), experiment.getLabel(),
		           experiment.getStartTime(), experiment.getEndTime(), experiment.getState());
		
		   updateStateIndex(experiment);
		
		} catch (Exception e) {
			LOGGER.error("Error while experiment updating experiment  {}",experiment,e);
		   throw new RepositoryException("Could not update experiment with ID \"" + experiment.getID() + "\"", e);
		}
		
		return experiment;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public Experiment updateExperimentState(Experiment experiment, State state){

		LOGGER.debug("Updating experiment  {} state {} ", new Object [] {experiment, state});

		validator.validateExperiment(experiment);
		
		try {
		   // Note that this timestamp gets serialized as mulliseconds from
		   // the epoch, so timezone is irrelevant
		   final Date NOW = new Date();

		   experimentAccessor.updateExperiment(state.name(), NOW, experiment.getID().getRawID());
		   
		   experiment = Experiment.from(experiment).withState(state).build();
		
		   // Point the experiment index to this experiment
		   updateExperimentLabelIndex(experiment.getID(), experiment.getApplicationName(), experiment.getLabel(),
		           experiment.getStartTime(), experiment.getEndTime(), experiment.getState());
		
		   updateStateIndex(experiment);
		
		} catch (Exception e) {
			LOGGER.error("Error while updating experiment  {} state {} ",new Object [] {experiment, state}, e);
		    throw new RepositoryException("Could not update experiment with ID \""
		           + experiment.getID() + "\"" + " to state " + state.toString(), e);
		}
		
		return experiment;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public List<Experiment.ID> getExperiments() {
		LOGGER.debug("Getting experiment ids which are live {}", ExperimentState.NOT_DELETED);

		try {
            // Get all experiments that are live
        	Result<StateExperimentIndex> ids = stateExperimentIndexAccessor
        			.selectByKey(ExperimentState.NOT_DELETED.name());
        	List<ID> experimentIds = ids.all().stream().map(
        			sei -> Experiment.ID.valueOf(sei.getExperimentId())).collect(Collectors.toList());
        	
        	return experimentIds;
        } catch (Exception e) {
    		LOGGER.error("Error while getting experiment ids which are live {}", ExperimentState.NOT_DELETED, e);
            throw new RepositoryException("Could not retrieve experiments", e);
        }
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Experiment> getExperiments(Application.Name appName) {
		LOGGER.debug("Getting experiments for application {}", appName);

		try { 
			Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experimentPojos = 
					experimentAccessor.getExperimentByAppName(appName.toString());
			
			List<Experiment> experiments = experimentPojos.all().stream().filter(experiment -> 
				(experiment.getState() != null) &&
				(! experiment.getState().equals(Experiment.State.TERMINATED.name())) &&
				(! experiment.getState().equals(Experiment.State.DELETED.name())))
				.map(experimentPojo -> ExperimentHelper.makeExperiment(experimentPojo))
						.collect(Collectors.toList());
			
			return experiments;
		}
		catch(Exception e) {
    		LOGGER.error("Error while getting experiments for app {}", appName, e);
            throw new RepositoryException("Could not retrieve experiments for app " + appName, e);
		}
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteExperiment(NewExperiment newExperiment) {
		LOGGER.debug("Deleting experiment {}", newExperiment);
		
		try {
			experimentAccessor.deleteExperiment(newExperiment.getID().getRawID());
		} catch (Exception e) {
			LOGGER.debug("Error while deleting experiment {}", newExperiment, e);
			throw new RepositoryException("Could not delete experiment "
					+ "with id \"" + newExperiment.getId() + "\"", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public ExperimentList getExperiments(Collection<Experiment.ID> experimentIDs) {
		
		LOGGER.debug("Getting experiments {}", experimentIDs);
        
		ExperimentList result = new ExperimentList();
        
		try {
            if (!experimentIDs.isEmpty()) {
            	List<UUID> uuids = experimentIDs.stream().map(Experiment.ID::getRawID).collect(Collectors.toList());
            	Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experimentPojos = 
            			experimentAccessor.getExperiments(uuids);
            	
                List<Experiment> experiments = experimentPojos.all().stream().filter(experiment -> 
                	! experiment.getState().equals(Experiment.State.DELETED))
                	.map(ExperimentHelper::makeExperiment).collect(Collectors.toList());
                
                result.setExperiments(experiments);
            }
        } catch (Exception e) {
    		LOGGER.error("Errro while getting experiments {}", experimentIDs, e);
            throw new RepositoryException("Could not retrieve the experiments for the collection of experimentIDs", e);
        }
        
		return result;
    }

	/**
	 * Get the experiments for an Application
	 */
	@Override
    public Table<Experiment.ID, Experiment.Label, Experiment> getExperimentList(Application.Name appName) {

        try {
			List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experimentPojos = 
					experimentAccessor.getExperimentByAppName(appName.toString()).all();
			
	        Table<Experiment.ID, Experiment.Label, Experiment> result = HashBasedTable.create();
			for (com.intuit.wasabi.repository.cassandra.pojo.Experiment experimentPojo : 
				experimentPojos ) {
				Experiment experiment = ExperimentHelper.makeExperiment(experimentPojo);
				result.put(experiment.getID(), experiment.getLabel(), experiment);
			}
			
	        return result;
        } catch (Exception e) {
            throw new RepositoryException("Could not retrieve experiment list " + appName.toString(), e);
        }
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public List<Application.Name> getApplicationsList() {

        List<Application.Name> result = new ArrayList<>();

        try {
        	result = applicationListAccessor.getUniqueAppName().all().stream()
        		.map(app -> Application.Name.valueOf(app.getAppName())).collect(Collectors.toList());
        	
        	
        } catch (Exception e) {
            throw new RepositoryException("Could not retrieve the application names", e);
        }
        return result;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
    public Bucket getBucket(Experiment.ID experimentID, Bucket.Label bucketLabel) {

        Preconditions.checkNotNull(experimentID, "Parameter \"experimentID\" cannot be null");
        Preconditions.checkNotNull(bucketLabel, "Parameter \"bucketLabel\" cannot be null");

        try {
        	List<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucket = 
        			bucketAccessor.getBucketByExperimentIdAndBucket(
        					experimentID.getRawID(), bucketLabel.toString()).all();

        	if ( bucket.size() > 1 )
        		throw new RepositoryException("More than one row found for experiment ID " 
        				+ experimentID + " and label " + bucketLabel);

        	Bucket result = BucketHelper.makeBucket(bucket.get(0));


            return result;
        } catch (Exception e) {
            throw new RepositoryException("Could not retrieve bucket \"" +
                    bucketLabel + "\" in experiment \"" + experimentID + "\"", e);
        }
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void createBucket(Bucket newBucket) {

		LOGGER.debug("Creating bucket {}", newBucket);
		
		Preconditions.checkNotNull(newBucket,"Parameter \"newBucket\" cannot be null");

		final Bucket.State STATE = Bucket.State.OPEN;

		try {
			bucketAccessor.insert(newBucket.getExperimentID().getRawID(), 
				newBucket.getLabel().toString(), 
				newBucket.getDescription(), 
				newBucket.getAllocationPercent(), 
				newBucket.isControl(), 
				newBucket.getPayload(), 
				STATE.name());
			
		} catch (Exception e) {
			LOGGER.error("Error creating bucket {}", newBucket, e);
			throw new RepositoryException("Could not create bucket \"" + newBucket + "\"", e); 
	    }
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public Bucket updateBucket(Bucket bucket) {

        // Note, if the bucket doesn't already exist, it will be created
        // because Cassandra always does upserts

        /*
        * First update the is_control column to false for all the buckets of an experiment
        * Then make the is_control to true only for the one requested by the user
        * */


        if (bucket.isControl()) {

            try {

            	List<Bucket> buckets = bucketAccessor.getBucketByExperimentId(bucket.getExperimentID()
						.getRawID()).all().stream().map(BucketHelper::makeBucket)
						.collect(Collectors.toList());
            	
                buckets.forEach(currentBucket ->
                    		bucketAccessor.updateControl(false, 
                    				currentBucket.getExperimentID().getRawID(), 
                    				currentBucket.getLabel().toString()));
                    
            } catch (Exception e) {
                throw new RepositoryException("Could not update buckets", e);
            }
        }

        try {
        	bucketAccessor.updateBucket(
        			bucket.getDescription() != null ? bucket.getDescription() : "",
                    bucket.getAllocationPercent(),
                    bucket.isControl(),
                    bucket.getPayload() != null ? bucket.getPayload() : "",
                    bucket.getExperimentID().getRawID(),
                    bucket.getLabel().toString());
        	

        } catch (Exception e) {
            throw new RepositoryException("Could not update bucket \"" + bucket.getExperimentID() + "\".\"" + bucket.getLabel() + "\"", e);
        }
        
        return bucket;
    }

	@Override
    public Bucket updateBucketAllocationPercentage(Bucket bucket, Double desiredAllocationPercentage) {

        try {
        	bucketAccessor.updateAllocation(desiredAllocationPercentage, bucket.getExperimentID().getRawID(),
        			bucket.getLabel().toString());
        } catch (Exception e) {
            throw new RepositoryException("Could not update bucket allocation percentage \"" +
                    bucket.getExperimentID() + "\".\"" + bucket.getLabel() + "\"", e);
        }

        // return the bucket with the updated values
        bucket = getBucket(bucket.getExperimentID(), bucket.getLabel());

        return bucket;
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bucket updateBucketState(Bucket bucket, Bucket.State desiredState) {

		LOGGER.debug("Updating bucket {} state {}", new Object[] { bucket, desiredState});
		
		try {
			bucketAccessor.updateState(desiredState.name(), bucket
					.getExperimentID().getRawID(), bucket.getLabel().toString());
	
			Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucketPojo = 
					bucketAccessor.getBucketByExperimentIdAndBucket(bucket.getExperimentID()
							.getRawID(), bucket.getLabel().toString());
	
			return BucketHelper.makeBucket(bucketPojo.one());
		}
		catch(Exception e) {
			LOGGER.error("Error while updating bucket {} state {}", new Object[] { bucket, desiredState}, e);
			throw new RepositoryException("Exception while updating bucket state " + 
					bucket + " state " + desiredState, e);
		}
	}

	/**
	 * Update bucket batch
	 * 
	 * @param experimentID
	 *            the experiment id
	 * @param bucketList
	 *            the bucket list
	 * 
	 * @return BucketList
	 */
	@Override
	public BucketList updateBucketBatch(Experiment.ID experimentID,
			BucketList bucketList) {

		LOGGER.debug("bucket update {} for experiment id {}", new Object [] { bucketList, experimentID});

		ArrayList<Object> args = new ArrayList<>();
        String CQL = "BEGIN BATCH ";
        for (int i = 0; i < bucketList.getBuckets().size(); i++) {
            Bucket b = bucketList.getBuckets().get(i);
            CQL += "UPDATE bucket SET ";
            if (b.getState() != null) {
                CQL += "state = ?,";
                args.add(b.getState().name());
            }
            if (b.getAllocationPercent() != null) {
                CQL += "allocation = ?,";
                args.add(b.getAllocationPercent());
            }
            if (b.getDescription() != null) {
                CQL += "description = ?,";
                args.add(b.getDescription());
            }
            if (b.isControl() != null) {
                CQL += "is_control = ?,";
                args.add(b.isControl());
            }
            if (b.getPayload() != null) {
                CQL += "payload = ?,";
                args.add(b.getPayload());
            }
            if (",".equals(CQL.substring(CQL.length() - 1, CQL.length()))) {
                CQL = CQL.substring(0, CQL.length() - 1);
            }
            CQL += " where experiment_id = ? and label = ?;";
            args.add(experimentID.getRawID());
            args.add(b.getLabel().toString());
        }
        CQL += "APPLY BATCH;";

        LOGGER.debug("bucket update {} for experiment id {} statement{} with args {}", 
        		new Object [] { bucketList, experimentID, CQL, args});
        try {
        	PreparedStatement preparedStatement = driver.getSession().prepare(CQL);
        	BoundStatement boundStatement = new BoundStatement(preparedStatement);
        	boundStatement.bind(args.toArray());
        	driver.getSession().execute(boundStatement);
        } catch (Exception e) {
            throw new RepositoryException("Could not update bucket for experiment \"" + experimentID + "\"", e);
        }

        // return the bucket with the updated values
        BucketList buckets;
        buckets = getBuckets(experimentID);
        return buckets;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteBucket(Experiment.ID experimentID, Bucket.Label bucketLabel) {

		Preconditions.checkNotNull(experimentID, "Parameter \"experimentID\" cannot be null");
		Preconditions.checkNotNull(bucketLabel,	"Parameter \"bucketLabel\" cannot be null");
		try {

			bucketAccessor.deleteByExperimentIdAndLabel(experimentID.getRawID(), bucketLabel.toString());

		} catch (Exception e) {
			throw new RepositoryException("Could not delete bucket \""	+ bucketLabel + "\" from experiment with ID \""
					+ experimentID + "\"", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void logBucketChanges(Experiment.ID experimentID, Bucket.Label bucketLabel,
            List<BucketAuditInfo> changeList)  {

		final Date NOW = new Date();

		try {
			for (BucketAuditInfo changeData : changeList) {
				bucketAuditLogAccessor.insertBy(experimentID.getRawID(), 
						bucketLabel.toString(), NOW, changeData.getAttributeName(), 
						(changeData.getOldValue() != null) ? changeData.getOldValue() : "", 
						(changeData.getNewValue() != null) ? changeData.getNewValue() : "");
				
			}
		} catch (Exception e) {
			throw new RepositoryException("Could not log bucket changes \"" + experimentID + " : " + bucketLabel + " " +"\"", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
    public void logExperimentChanges(Experiment.ID experimentID, List<ExperimentAuditInfo> changeList) {
		final Date NOW = new Date();
        try {

            for (ExperimentAuditInfo changeData : changeList) {
            	experimentAuditLogAccessor.insertBy(experimentID.getRawID(), 
            			NOW, changeData.getAttributeName(), 
            			(changeData.getOldValue() != null) ? changeData.getOldValue() : "", 
                        (changeData.getNewValue() != null) ? changeData.getNewValue() : "");
            }
        } catch (Exception e) {
            throw new RepositoryException("Could not log experiment changes for experimentID \"" +
                    experimentID + "\"", e);
        }
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	public BucketList getBuckets(Experiment.ID experimentID) {

		LOGGER.debug("Getting buckets for {}", experimentID);
		
		Preconditions.checkNotNull(experimentID,
				"Parameter \"experimentID\" cannot be null");

		try {
			// Check if the experiment is live
			Experiment experiment = getExperiment(experimentID);
			if (experiment == null) {
				throw new ExperimentNotFoundException(experimentID);
			}
	
			 Result<com.intuit.wasabi.repository.cassandra.pojo.Bucket> bucketPojos = 
					 bucketAccessor.getBucketByExperimentId(experimentID.getRawID());
			 
			 List<Bucket> buckets = bucketPojos.all().stream().map(BucketHelper::makeBucket).collect(Collectors.toList());
			 
			 BucketList bucketList = new BucketList();
			 bucketList.setBuckets(buckets);
			 
			 LOGGER.debug("Returning buckets {} for experiment {}", new Object[] {
					 bucketList, experimentID});
			 
			 return bucketList;
		}
		catch(Exception e) {
			LOGGER.error("Error while getting buckets for {}", experimentID,e);
			throw new RepositoryException("Unable to get buckets for " + experimentID, e);
		}
	}

	protected void updateExperimentLabelIndex(Experiment.ID experimentID,
			Application.Name appName, Experiment.Label experimentLabel,
			Date startTime, Date endTime, Experiment.State state) {

		LOGGER.debug("update experiment label index experiment id {} app {} label {} "
				+ " start time {} end time {} state {} ", 
				new Object [] {experimentID, appName, experimentLabel, startTime, endTime, state});

		if (state == Experiment.State.TERMINATED
				|| state == Experiment.State.DELETED) {
			removeExperimentLabelIndex(appName, experimentLabel);
			return;
		}

		try {
			// Note that this timestamp gets serialized as mulliseconds from
			// the epoch, so timezone is irrelevant
			final Date NOW = new Date();

			experimentLabelIndexAccessor.updateBy(experimentID.getRawID(), NOW,
					startTime, endTime, state.name(), appName.toString(),
					experimentLabel.toString());

		} catch (Exception e) {
			LOGGER.debug("Error while updating experiment label index experiment id {} app {} label {} "
					+ " start time {} end time {} state {} ", 
					new Object [] {experimentID, appName, experimentLabel, startTime, endTime, state}, e);
			throw new RepositoryException("Could not index experiment \"" + experimentID + "\"", e);
		}

	}

	protected void removeExperimentLabelIndex(Application.Name appName, Experiment.Label experimentLabel) {
		LOGGER.debug("Removing experiment label index for app {}, label {} ", new Object[] {
				appName, experimentLabel});
		
		try {
			experimentLabelIndexAccessor.deleteBy(appName.toString(),
				experimentLabel.toString());
		}
		catch (Exception e) {
			LOGGER.error("Error while removing experiment label index for app {}, label {} ", new Object[] {
					appName, experimentLabel},e);
			 throw new RepositoryException("Could not remove index for " +
					 "experiment \"" + appName + "\".\"" + experimentLabel + "\"", e); 
		}
	}

	/**
	 * Update state index
	 *
	 * @param experiment
	 *            the experiment object
	 *
	 */
	public void updateStateIndex(Experiment experiment) {
		LOGGER.debug("update state index experiment {} ", experiment);
		
		try {
			 updateStateIndex(experiment.getID(),
					 experiment.getState() != State.DELETED
					 ? ExperimentState.NOT_DELETED
							 : ExperimentState.DELETED);
		 } catch (Exception e) {
			 LOGGER.error("update state index experiment {} ", experiment, e);
			 throw new RepositoryException("Exception while updating state index: " + e, e);
		 }
	}

	protected void updateStateIndex(Experiment.ID experimentID, ExperimentState state) throws Exception {

		LOGGER.debug("update state index experiment id {} state {} ", new Object[] { experimentID, state});
		
		try {
			switch(state) {
				case DELETED:
					LOGGER.debug("update state index insert experiment id {} state {} ", 
							new Object[] { experimentID, ExperimentState.DELETED.name()});
					
					BatchStatement batch1 = new BatchStatement();
					Statement insertStatement1 = stateExperimentIndexAccessor.insert(ExperimentState.DELETED.name(), 
							experimentID.getRawID(), ByteBuffer.wrap("".getBytes("")));
					batch1.add(insertStatement1);
					
					LOGGER.debug("update state index delete experiment id {} state {} ", 
							new Object[] { experimentID, ExperimentState.NOT_DELETED.name()});
					
					Statement deleteStatement1 = stateExperimentIndexAccessor.deleteBy(ExperimentState.NOT_DELETED.name(), 
							experimentID.getRawID());
					batch1.add(deleteStatement1);
					
					driver.getSession().execute(batch1);
					
					break;
					
				case NOT_DELETED:
					LOGGER.debug("update state index insert experiment id {} state {} ", 
							new Object[] { experimentID, ExperimentState.NOT_DELETED.name()});
					
					BatchStatement batch2 = new BatchStatement();
					Statement insert2 = stateExperimentIndexAccessor.insert(ExperimentState.NOT_DELETED.name(), 
							experimentID.getRawID(), ByteBuffer.wrap("".getBytes()));
					batch2.add(insert2);
					
					LOGGER.debug("update state index delete experiment id {} state {} ", 
							new Object[] { experimentID, ExperimentState.DELETED.name()});
					
					Statement delete2 = stateExperimentIndexAccessor.deleteBy(ExperimentState.DELETED.name(), 
							experimentID.getRawID());
					batch2.add(delete2);
					
					driver.getSession().execute(batch2);
					
					break;
				default:
					throw new RepositoryException("Unknown experiment state: " + state);
			}
		}
		catch(Exception e) {
			LOGGER.error("Error while updating state index experiment id {} state {} ", 
					new Object[] { experimentID, state}, e);
			throw new RepositoryException("Unable to update experiment " + experimentID + " with state " + state);
		}
		
	}

	/**
	 * Creates an application at top level
	 * 
	 * @param applicationName
	 *            Application Name
	 */
	@Override
	public void createApplication(Application.Name applicationName) {

		LOGGER.debug("Creating application {}", applicationName);

		try {
			applicationListAccessor.insert(applicationName.toString());
		} catch (Exception e) {
			LOGGER.error("Error while creating application {}", applicationName, e);
			throw new RepositoryException("Unable to insert into top level application list: \""
							+ applicationName.toString() + "\"" + e);
		}
	}

}
