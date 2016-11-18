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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * This class tests the {@link TotalUsers}.
 */
public class TotalUsersTest {

    @Test
    public void testBuilder() {
        long total = 1000;
        long bucketAssignments = 10;
        long nullAssignments = 10;
        TotalUsers totalUserCount = new TotalUsers.Builder().withTotal(total)
                .withBucketAssignments(bucketAssignments)
                .withNullAssignments(nullAssignments).build();
        assertThat(totalUserCount.getTotal(), equalTo(total));
        assertThat(totalUserCount.getBucketAssignments(), equalTo(bucketAssignments));
        assertThat(totalUserCount.getNullAssignments(), equalTo(nullAssignments));
    }

    @Test
    public void testBuildWithOther() {
        long total = 1000;
        long bucketAssignments = 10;
        long nullAssignments = 10;
        TotalUsers totalUserCount = new TotalUsers.Builder().withTotal(total)
                .withBucketAssignments(bucketAssignments)
                .withNullAssignments(nullAssignments).build();
        TotalUsers otherTotalUserCount = new TotalUsers.Builder(totalUserCount).build();
        assertThat(otherTotalUserCount.getTotal(), equalTo(total));
        assertThat(otherTotalUserCount.getBucketAssignments(), equalTo(bucketAssignments));
        assertThat(otherTotalUserCount.getNullAssignments(), equalTo(nullAssignments));
    }
}

