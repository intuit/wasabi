/*
  *****************************************************************************
  Copyright 2017 Intuit

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.intuit.wasabi.assignment.impl;

import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import com.intuit.wasabi.assignment.cache.impl.NoopAssignmentsMetadataCacheImpl;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Page;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

/**
 * Class to test expected behavior of Noop implementation of AssignmentsMetadataCache
 */
public class NoopAssignmentsMetadataCacheImplTest {

    @Test
    public void validateNoopImplementation() {
        AssignmentsMetadataCache cache = new NoopAssignmentsMetadataCacheImpl();
        Experiment.ID expId = Experiment.ID.newInstance();

        Application.Name appName = Application.Name.valueOf("TestApp");
        Page.Name pageName = Page.Name.valueOf("TestPage");

        Assert.assertThat(cache.clear(), is(false));
        Assert.assertThat(cache.refresh(), is(false));
        Assert.assertNull(cache.getLastRefreshTime());
        Assert.assertNull(cache.getDetails());

        Assert.assertNull(cache.getBucketList(expId));
        Assert.assertNull(cache.getExclusionList(expId));
        Assert.assertNull(cache.getExperimentsByAppName(appName));
        Assert.assertNull(cache.getPageExperiments(appName, pageName));
        Assert.assertThat(cache.getExperimentById(expId).isPresent(), is(false));
        Assert.assertThat(cache.getPrioritizedExperimentListMap(appName).isPresent(), is(false));
    }
}
