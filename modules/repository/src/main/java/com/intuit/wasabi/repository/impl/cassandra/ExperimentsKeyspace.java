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
package com.intuit.wasabi.repository.impl.cassandra;

import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.*;
import com.netflix.astyanax.annotations.Component;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.serializers.AnnotatedCompositeSerializer;

import java.util.Date;
import java.util.UUID;

/**
 * Keyspace definition for storing experiment data
 *
 */
public interface ExperimentsKeyspace extends RepositoryKeyspace {

    ColumnFamily<Application.Name,String> application_CF();

    ColumnFamily<Application.Name, String> applicationList_CF();

    /**
     * The Experiment column family definition
	 *
     * @return  The column family definition. Never null.
	 */
    ColumnFamily<Experiment.ID,String> experimentCF();
	/**
     * The Bucket column family definition
	 *
     * @return  The column family definition. Never null.
	 */
    ColumnFamily<Bucket.Label,String> bucketCF();
	/**
     * The index of app name + experiment label to experiment
	 *
     * @return  The column family definition. Never null.
	 */
    ColumnFamily<AppNameExperimentLabelComposite,String> experimentLabelIndexCF();
	/**
     * The index of states to experiments
	 *
     * @return  The column family definition. Never null.
	 */
    ColumnFamily<ExperimentStateIndexKey,Experiment.ID> stateExperimentIndexCF();
	/**
     * The user_assignment column family definition
	 *
     * @return  The column family definition. Never null.
	 */
    ColumnFamily<UserAssignmentComposite,String> userAssignmentCF();
	/**
     *
	 *
     * @return  The column family definition. Never null.
	 */
    ColumnFamily<Application.Name,String> userExperimentIndexCF();

    /**
     * The user_assignment_look_up column family definition
     *
     * @return The column family definition, Never null
     */
    ColumnFamily<User.ID,String> userAssignmentLookUp();
    /**
     * The user_assignment_export column family definition
     *
     * @return The column family definition, Never null
     */
    ColumnFamily<ExperimentIDDayHourComposite,String> userAssignmentExport();
	/**
     * 
	 *
     * @return  The column family definition. Never null.
	 */
    ColumnFamily<Experiment.ID,String> userBucketIndexCF();

    ColumnFamily<UserBucketComposite,String> bucket_audit_log_CF();

    ColumnFamily<Experiment.ID,String> experiment_audit_log_CF();

    ColumnFamily<Experiment.ID, Experiment.ID> exclusion_CF();

    ColumnFamily<User.ID, String> experimentUserIndexCF();

    ColumnFamily<AppNamePageComposite, String> page_experiment_index_CF();

    ColumnFamily<Experiment.ID, Page.Name> experiment_page_CF();

    ColumnFamily<Application.Name, String> app_page_index_CF();

    ColumnFamily<UserInfo.Username, String> userRolesCF();

    ColumnFamily<UserInfo.Username, String> userInfoCF();

    ColumnFamily<Application.Name, String> appRoleCF();

    ColumnFamily<UserInfo.Username, String> feedbackCF();

    ColumnFamily<UUID, String> stagingCF();

    ColumnFamily<Experiment.ID, String> bucketAssignmentCountsCF();

    /**
     * The {@code auditlog} column family definition
     *
     * @return The column family definition. Never null.
     */
    ColumnFamily<Application.Name, String> auditlogCF();

    /**
     * State-to-experiment indexes
     */
    enum ExperimentStateIndexKey {
        NOT_DELETED,
        DELETED
    }

    /**
     * Composite key for the experiment_label_index column family
     */
    class AppNameExperimentLabelComposite {
        @Component(ordinal = 0)
        Application.Name appName;
        @Component(ordinal = 1)
        Experiment.Label experimentLabel;

        public static class Serializer
                extends AnnotatedCompositeSerializer<AppNameExperimentLabelComposite> {

            private static final Serializer INSTANCE = new Serializer();

            public Serializer() {
                super(AppNameExperimentLabelComposite.class);
            }
            public static Serializer get() {
                return INSTANCE;
            }
        }
    }

    /**
     * Composite key for the user_assignment column family
     */

    class UserAssignmentComposite {
        @Component(ordinal = 0)
        Experiment.ID experimentID;
        @Component(ordinal = 1)
        User.ID userID;

        public static class Serializer
                extends AnnotatedCompositeSerializer<UserAssignmentComposite> {

            private static final Serializer INSTANCE = new Serializer();

            public Serializer() {
                super(UserAssignmentComposite.class);
            }
            public static Serializer get() {
                return INSTANCE;
            }
        }
    }

    /**
     * Composite key for the user_assignment_export column family
     *
     */
    public class ExperimentIDDayHourComposite {
        @Component(ordinal=0)
        Experiment.ID experimentID;
        @Component(ordinal=1)
        Date day_hour;

        public ExperimentIDDayHourComposite(){
        }

        public ExperimentIDDayHourComposite(Experiment.ID experimentID, Date day_hour){
            this.experimentID = experimentID;
            this.day_hour = day_hour;
        }

        public Experiment.ID getExperimentID() {
            return experimentID;
        }

        public Date getDayHour() {
            return day_hour;
        }

        public static class Serializer
                extends AnnotatedCompositeSerializer<ExperimentIDDayHourComposite> {

            public Serializer() {
                super(ExperimentIDDayHourComposite.class);
            }

            public static Serializer get() {
                return INSTANCE;
            }

            private static final Serializer INSTANCE=new Serializer();
        }
    }

    /**
     * Composite key for the user_bucket_index column family
     */
    class UserBucketComposite {
        @Component(ordinal = 0)
        Experiment.ID experimentID;
        @Component(ordinal = 1)
        Bucket.Label bucketLabel;

        public static class Serializer
                extends AnnotatedCompositeSerializer<UserBucketComposite> {

            private static final Serializer INSTANCE = new Serializer();

            public Serializer() {
                super(UserBucketComposite.class);
            }
            public static Serializer get() {
                return INSTANCE;
            }
        }
    }

    class AppNamePageComposite {
        @Component(ordinal = 0)
        Application.Name applicationName;
        @Component(ordinal = 1)
        Page.Name pageName;

        public AppNamePageComposite() {
        }

        public static class Serializer extends AnnotatedCompositeSerializer<AppNamePageComposite> {
            private static final Serializer INSTANCE = new Serializer();
            public Serializer() {
                super(AppNamePageComposite.class);
            }
            public static Serializer get() {
                return INSTANCE;
            }
        }
    }
}
