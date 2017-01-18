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
package com.intuit.wasabi.repository.impl.cassandra;

import com.intuit.wasabi.repository.impl.cassandra.serializer.ApplicationNameSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.BucketLabelSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentIDSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentStateIndexKeySerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.PageNameSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.UserIDSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.UsernameSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.serializers.TimeUUIDSerializer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link ExperimentsKeyspace}.
 */
public class ExperimentsKeyspaceImplTest {

    private final ExperimentsKeyspace ek = new ExperimentsKeyspaceImpl();

    @Test
    public void testApplication_CF() throws Exception {
        Assert.assertEquals("application", ek.application_CF().getName());
        Assert.assertEquals(ApplicationNameSerializer.get(), ek.application_CF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.application_CF().getColumnSerializer());
    }

    @Test
    public void testExperimentCF() throws Exception {
        Assert.assertEquals("experiment", ek.experimentCF().getName());
        Assert.assertEquals(ExperimentIDSerializer.get(), ek.experimentCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.experimentCF().getColumnSerializer());
    }

    @Test
    public void testBucketCF() throws Exception {
        Assert.assertEquals("bucket", ek.bucketCF().getName());
        Assert.assertEquals(BucketLabelSerializer.get(), ek.bucketCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.bucketCF().getColumnSerializer());
    }

    @Test
    public void testExperimentLabelIndexCF() throws Exception {
        Assert.assertEquals("experiment_label_index", ek.experimentLabelIndexCF().getName());
        Assert.assertEquals(ExperimentsKeyspace.AppNameExperimentLabelComposite.Serializer.get(), ek.experimentLabelIndexCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.experimentLabelIndexCF().getColumnSerializer());
    }

    @Test
    public void testStateExperimentIndexCF() throws Exception {
        Assert.assertEquals("state_experiment_index", ek.stateExperimentIndexCF().getName());
        Assert.assertEquals(ExperimentStateIndexKeySerializer.get(), ek.stateExperimentIndexCF().getKeySerializer());
        Assert.assertEquals(ExperimentIDSerializer.get(), ek.stateExperimentIndexCF().getColumnSerializer());
    }

    @Test
    public void testUserRolesCF() throws Exception {
        Assert.assertEquals("user_roles", ek.userRolesCF().getName());
        Assert.assertEquals(UsernameSerializer.get(), ek.userRolesCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.userRolesCF().getColumnSerializer());
    }

    @Test
    public void testUserInfoCF() throws Exception {
        Assert.assertEquals("user_info", ek.userInfoCF().getName());
        Assert.assertEquals(UsernameSerializer.get(), ek.userInfoCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.userInfoCF().getColumnSerializer());
    }

    @Test
    public void testAppRoleCF() throws Exception {
        Assert.assertEquals("app_role", ek.appRoleCF().getName());
        Assert.assertEquals(ApplicationNameSerializer.get(), ek.appRoleCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.appRoleCF().getColumnSerializer());
    }

    @Test
    public void testUserAssignmentCF() throws Exception {
        Assert.assertEquals("user_assignment", ek.userAssignmentCF().getName());
        Assert.assertEquals(ExperimentsKeyspace.UserAssignmentComposite.Serializer.get(), ek.userAssignmentCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.userAssignmentCF().getColumnSerializer());
    }

    @Test
    public void testUserExperimentIndexCF() throws Exception {
        Assert.assertEquals("user_experiment_index", ek.userExperimentIndexCF().getName());
        Assert.assertEquals(ApplicationNameSerializer.get(), ek.userExperimentIndexCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.userExperimentIndexCF().getColumnSerializer());
    }

    @Test
    public void testUserBucketIndexCF() throws Exception {
        Assert.assertEquals("user_bucket_index", ek.userBucketIndexCF().getName());
        Assert.assertEquals(ExperimentIDSerializer.get(), ek.userBucketIndexCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.userBucketIndexCF().getColumnSerializer());
    }

    @Test
    public void testBucket_audit_log_CF() throws Exception {
        Assert.assertEquals("bucket_audit_log", ek.bucket_audit_log_CF().getName());
        Assert.assertEquals(ExperimentsKeyspace.UserBucketComposite.Serializer.get(), ek.bucket_audit_log_CF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.bucket_audit_log_CF().getColumnSerializer());
    }

    @Test
    public void testExperiment_audit_log_CF() throws Exception {
        Assert.assertEquals("experiment_audit_log", ek.experiment_audit_log_CF().getName());
        Assert.assertEquals(ExperimentIDSerializer.get(), ek.experiment_audit_log_CF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.experiment_audit_log_CF().getColumnSerializer());
    }

    @Test
    public void testExclusion_CF() throws Exception {
        Assert.assertEquals("exclusion", ek.exclusion_CF().getName());
        Assert.assertEquals(ExperimentIDSerializer.get(), ek.exclusion_CF().getKeySerializer());
        Assert.assertEquals(ExperimentIDSerializer.get(), ek.exclusion_CF().getColumnSerializer());
    }

    @Test
    public void testExperimentUserIndexCF() throws Exception {
        Assert.assertEquals("experiment_user_index", ek.experimentUserIndexCF().getName());
        Assert.assertEquals(UserIDSerializer.get(), ek.experimentUserIndexCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.experimentUserIndexCF().getColumnSerializer());
    }

    @Test
    public void testPage_experiment_index_CF() throws Exception {
        Assert.assertEquals("page_experiment_index", ek.page_experiment_index_CF().getName());
        Assert.assertEquals(ExperimentsKeyspace.AppNamePageComposite.Serializer.get(), ek.page_experiment_index_CF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.page_experiment_index_CF().getColumnSerializer());
    }

    @Test
    public void testExperiment_page_CF() throws Exception {
        Assert.assertEquals("experiment_page", ek.experiment_page_CF().getName());
        Assert.assertEquals(ExperimentIDSerializer.get(), ek.experiment_page_CF().getKeySerializer());
        Assert.assertEquals(PageNameSerializer.get(), ek.experiment_page_CF().getColumnSerializer());
    }

    @Test
    public void testApp_page_index_CF() throws Exception {
        Assert.assertEquals("app_page_index", ek.app_page_index_CF().getName());
        Assert.assertEquals(ApplicationNameSerializer.get(), ek.app_page_index_CF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.app_page_index_CF().getColumnSerializer());
    }

    @Test
    public void testFeedbackCF() throws Exception {
        Assert.assertEquals("user_feedback", ek.feedbackCF().getName());
        Assert.assertEquals(UsernameSerializer.get(), ek.feedbackCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.feedbackCF().getColumnSerializer());
    }

    @Test
    public void testStagingCF() throws Exception {
        Assert.assertEquals("staging", ek.stagingCF().getName());
        Assert.assertEquals(TimeUUIDSerializer.get(), ek.stagingCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.stagingCF().getColumnSerializer());
    }

    @Test
    public void testBucketAssignmentCountsCF() throws Exception {
        Assert.assertEquals("bucket_assignment_counts", ek.bucketAssignmentCountsCF().getName());
        Assert.assertEquals(ExperimentIDSerializer.get(), ek.bucketAssignmentCountsCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.bucketAssignmentCountsCF().getColumnSerializer());
    }

    @Test
    public void testAuditlogCF() throws Exception {
        Assert.assertEquals("auditlog", ek.auditlogCF().getName());
        Assert.assertEquals(ApplicationNameSerializer.get(), ek.auditlogCF().getKeySerializer());
        Assert.assertEquals(StringSerializer.get(), ek.auditlogCF().getColumnSerializer());
    }
}
