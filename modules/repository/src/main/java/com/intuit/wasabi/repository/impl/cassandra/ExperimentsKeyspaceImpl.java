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
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Page;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ApplicationNameSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.BucketLabelSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentIDSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentStateIndexKeySerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.PageNameSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.UserIDSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.UsernameSerializer;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.serializers.TimeUUIDSerializer;

import java.util.UUID;

/**
 * ExperimentKeyspace implementation
 *
 * @see ExperimentsKeyspace
 */
public class ExperimentsKeyspaceImpl implements ExperimentsKeyspace {
    private final ColumnFamily<Experiment.ID, String> EXPERIMENT_CF =
            ColumnFamily.newColumnFamily(
                    "experiment",
                    ExperimentIDSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<Bucket.Label, String> BUCKET_CF =
            ColumnFamily.newColumnFamily(
                    "bucket",
                    BucketLabelSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<AppNameExperimentLabelComposite, String>
            EXPERIMENT_LABEL_INDEX_CF =
            ColumnFamily.newColumnFamily(
                    "experiment_label_index",
                    AppNameExperimentLabelComposite.Serializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<ExperimentStateIndexKey, Experiment.ID>
            STATE_EXPERIMENT_CF =
            ColumnFamily.newColumnFamily(
                    "state_experiment_index",
                    ExperimentStateIndexKeySerializer.get(),
                    ExperimentIDSerializer.get());
    private final ColumnFamily<UserAssignmentComposite, String>
            USER_ASSIGNMENT_CF =
            ColumnFamily.newColumnFamily(
                    "user_assignment",
                    UserAssignmentComposite.Serializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<User.ID, String> EXPERIMENT_USER_INDEX_CF =
            ColumnFamily.newColumnFamily("experiment_user_index", UserIDSerializer.get(), StringSerializer.get());
    private final ColumnFamily<Application.Name, String>
            USER_EXPERIMENT_INDEX_CF =
            ColumnFamily.newColumnFamily(
                    "user_experiment_index",
                    ApplicationNameSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<Experiment.ID, String>
            USER_BUCKET_INDEX_CF =
            ColumnFamily.newColumnFamily(
                    "user_bucket_index",
                    ExperimentIDSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<UserBucketComposite, String>
            BUCKET_AUDIT_LOG_CF =
            ColumnFamily.newColumnFamily(
                    "bucket_audit_log",
                    UserBucketComposite.Serializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<Experiment.ID, String>
            EXPERIMENT_AUDIT_LOG_CF =
            ColumnFamily.newColumnFamily(
                    "experiment_audit_log",
                    ExperimentIDSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<Experiment.ID, Experiment.ID>
            EXCLUSION_CF =
            ColumnFamily.newColumnFamily(
                    "exclusion",
                    ExperimentIDSerializer.get(),
                    ExperimentIDSerializer.get());
    private final ColumnFamily<Application.Name, String>
            APPLICATION_CF =
            ColumnFamily.newColumnFamily(
                    "application",
                    ApplicationNameSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<Application.Name, String>
            APPLICATIONLIST_CF =
            ColumnFamily.newColumnFamily(
                    "applicationList",
                    ApplicationNameSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<AppNamePageComposite, String>
            PAGE_EXPERIMENT_INDEX_CF =
            ColumnFamily.newColumnFamily("page_experiment_index",
                    AppNamePageComposite.Serializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<Experiment.ID, Page.Name>
            EXPERIMENT_PAGE_CF =
            ColumnFamily.newColumnFamily("experiment_page",
                    ExperimentIDSerializer.get(),
                    PageNameSerializer.get());
    private final ColumnFamily<Application.Name, String>
            APP_PAGE_INDEX_CF =
            ColumnFamily.newColumnFamily("app_page_index",
                    ApplicationNameSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<UserInfo.Username, String>
            USER_ROLES_CF =
            ColumnFamily.newColumnFamily("user_roles",
                    UsernameSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<Application.Name, String>
            APP_ROLE_CF =
            ColumnFamily.newColumnFamily("app_role",
                    ApplicationNameSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<UserInfo.Username, String>
            USER_INFO_CF =
            ColumnFamily.newColumnFamily("user_info",
                    UsernameSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<ExperimentIDDayHourComposite, String>
            USER_ASSIGNMENT_EXPORT_CF =
            ColumnFamily.newColumnFamily("user_assignment_export", ExperimentIDDayHourComposite.Serializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<User.ID, String>
            USER_ASSIGNMENT_LOOK_UP =
            ColumnFamily.newColumnFamily("user_assignment_look_up", UserIDSerializer.get(),
                    StringSerializer.get());
    private final ColumnFamily<UserInfo.Username, String> FEEDBACK_CF = ColumnFamily.newColumnFamily("user_feedback",
            UsernameSerializer.get(), StringSerializer.get());
    private ColumnFamily<UUID, String> STAGING_CF = ColumnFamily.newColumnFamily("staging", TimeUUIDSerializer.get(),
            StringSerializer.get());
    private ColumnFamily<Experiment.ID, String> BUCKET_ASSIGNMENT_COUNTS_CF = ColumnFamily
            .newColumnFamily("bucket_assignment_counts", ExperimentIDSerializer.get(),
                    StringSerializer.get());
    private ColumnFamily<Application.Name, String> AUDITLOG_CF = ColumnFamily
            .newColumnFamily("auditlog", ApplicationNameSerializer.get(), StringSerializer.get());
    private ColumnFamily<ExperimentIDContextBasicISODateComposite, String> EXPERIMENT_ASSIGNMENT_COUNT_BY_DAY = ColumnFamily
            .newColumnFamily("experiment_assignments_per_day", ExperimentIDContextBasicISODateComposite.Serializer.get(),
                    StringSerializer.get());

    @Override
    public ColumnFamily<UserInfo.Username, String> feedbackCF() {
        return FEEDBACK_CF;
    }

    @Override
    public ColumnFamily<Application.Name, String> application_CF() {
        return APPLICATION_CF;
    }

    @Override
    public ColumnFamily<Application.Name, String> applicationList_CF() {
        return APPLICATIONLIST_CF;
    }

    @Override
    public ColumnFamily<Experiment.ID, String> experimentCF() {
        return EXPERIMENT_CF;
    }

    @Override
    public ColumnFamily<Bucket.Label, String> bucketCF() {
        return BUCKET_CF;
    }

    @Override
    public ColumnFamily<AppNameExperimentLabelComposite, String> experimentLabelIndexCF() {
        return EXPERIMENT_LABEL_INDEX_CF;
    }

    @Override
    public ColumnFamily<ExperimentStateIndexKey, Experiment.ID> stateExperimentIndexCF() {
        return STATE_EXPERIMENT_CF;
    }

    @Override
    public ColumnFamily<UserInfo.Username, String> userRolesCF() {
        return USER_ROLES_CF;
    }

    @Override
    public ColumnFamily<UserInfo.Username, String> userInfoCF() {
        return USER_INFO_CF;
    }

    @Override
    public ColumnFamily<Application.Name, String> appRoleCF() {
        return APP_ROLE_CF;
    }

    @Override
    public ColumnFamily<UserAssignmentComposite, String> userAssignmentCF() {
        return USER_ASSIGNMENT_CF;
    }

    @Override
    public ColumnFamily<Application.Name, String> userExperimentIndexCF() {
        return USER_EXPERIMENT_INDEX_CF;
    }

    @Override
    public ColumnFamily<User.ID, String> userAssignmentLookUp() {
        return USER_ASSIGNMENT_LOOK_UP;
    }

    @Override
    public ColumnFamily<ExperimentIDDayHourComposite, String> userAssignmentExport() {
        return USER_ASSIGNMENT_EXPORT_CF;
    }

    @Override
    public ColumnFamily<Experiment.ID, String> userBucketIndexCF() {
        return USER_BUCKET_INDEX_CF;
    }

    @Override
    public ColumnFamily<UserBucketComposite, String> bucket_audit_log_CF() {
        return BUCKET_AUDIT_LOG_CF;
    }

    @Override
    public ColumnFamily<Experiment.ID, String> experiment_audit_log_CF() {
        return EXPERIMENT_AUDIT_LOG_CF;
    }

    @Override
    public ColumnFamily<Experiment.ID, Experiment.ID> exclusion_CF() {
        return EXCLUSION_CF;
    }

    @Override
    public ColumnFamily<User.ID, String> experimentUserIndexCF() {
        return EXPERIMENT_USER_INDEX_CF;
    }

    @Override
    public ColumnFamily<AppNamePageComposite, String> page_experiment_index_CF() {
        return PAGE_EXPERIMENT_INDEX_CF;
    }

    @Override
    public ColumnFamily<Experiment.ID, Page.Name> experiment_page_CF() {
        return EXPERIMENT_PAGE_CF;
    }

    @Override
    public ColumnFamily<Application.Name, String> app_page_index_CF() {
        return APP_PAGE_INDEX_CF;
    }

    @Override
    public ColumnFamily<UUID, String> stagingCF() {
        return STAGING_CF;
    }

    @Override
    public ColumnFamily<Experiment.ID, String> bucketAssignmentCountsCF() {
        return BUCKET_ASSIGNMENT_COUNTS_CF;
    }

    @Override
    public ColumnFamily<Application.Name, String> auditlogCF() {
        return AUDITLOG_CF;
    }

    @Override
    public ColumnFamily<ExperimentIDContextBasicISODateComposite, String> experimentAssignmentCountByDay() {
        return EXPERIMENT_ASSIGNMENT_COUNT_BY_DAY;
    }
}
