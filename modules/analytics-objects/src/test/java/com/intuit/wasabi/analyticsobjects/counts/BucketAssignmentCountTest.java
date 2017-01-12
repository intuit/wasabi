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
package com.intuit.wasabi.analyticsobjects.counts;

import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * This class tests the {@link BucketAssignmentCount}.
 */
public class BucketAssignmentCountTest {

    @Test
    public void testBuilder() {
        Bucket.Label bucket = Bucket.Label.valueOf("TestBucket");
        long count = 10000;
        BucketAssignmentCount bucketAssignmentCounter = new BucketAssignmentCount.Builder()
                .withBucket(bucket).withCount(count).build();
        assertThat(bucketAssignmentCounter.getBucket(), equalTo(bucket));
        assertThat(bucketAssignmentCounter.getCount(), equalTo(count));
    }

    @Test
    public void testBuildWithOther() {
        Bucket.Label bucket = Bucket.Label.valueOf("TestBucket");
        long count = 10000;
        BucketAssignmentCount bucketAssignmentCounter = new BucketAssignmentCount.Builder()
                .withBucket(bucket).withCount(count).build();
        BucketAssignmentCount otherBucketAssignmentCounter = new BucketAssignmentCount.Builder(bucketAssignmentCounter).build();
        assertThat(otherBucketAssignmentCounter.getBucket(), equalTo(bucket));
        assertThat(otherBucketAssignmentCounter.getCount(), equalTo(count));
    }
}
