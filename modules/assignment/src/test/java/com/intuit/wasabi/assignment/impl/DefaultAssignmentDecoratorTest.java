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
package com.intuit.wasabi.assignment.impl;

import com.intuit.wasabi.assignmentobjects.SegmentationProfile;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Created on 3/2/16.
 */
public class DefaultAssignmentDecoratorTest {

    @Test
    public void constructorTest() {
        DefaultAssignmentDecorator defaultAssignmentDecorator = new DefaultAssignmentDecorator();
        assertThat(defaultAssignmentDecorator, is(not(nullValue())));
    }

    @Test
    public void getBucketListTest() throws UnsupportedEncodingException {
        Experiment experiment = mock(Experiment.class);

        DefaultAssignmentDecorator defaultAssignmentDecorator = new DefaultAssignmentDecorator();
        BucketList bucketList = defaultAssignmentDecorator.getBucketList(experiment, mock(User.ID.class), mock(SegmentationProfile.class));

        assertThat(bucketList, is(nullValue()));
    }
}
