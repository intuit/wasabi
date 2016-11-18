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

import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * This class tests the {@link AssignmentCounts}
 */
public class AssignmentCountsTest {

    @Test
    public void testBuilder() {
        Experiment.ID experimentID = Experiment.ID.newInstance();
        TotalUsers totalUsersCounter = new TotalUsers();
        List<BucketAssignmentCount> assignmentCountList = new ArrayList<BucketAssignmentCount>();
        AssignmentCounts counter = new AssignmentCounts.Builder().withExperimentID(experimentID)
                .withBucketAssignmentCount(assignmentCountList).withTotalUsers(totalUsersCounter)
                .build();
        assertThat(counter.getExperimentID(), equalTo(experimentID));
        assertThat(counter.getAssignments(), equalTo(assignmentCountList));
        assertThat(counter.getTotalUsers(), equalTo(totalUsersCounter));

        AssignmentCounts otherCounter = new AssignmentCounts.Builder(counter).build();
        assertThat(otherCounter.getExperimentID(), equalTo(experimentID));
        assertThat(otherCounter.getAssignments(), equalTo(assignmentCountList));
        assertThat(otherCounter.getTotalUsers(), equalTo(totalUsersCounter));
    }
}
