/*******************************************************************************
 * Copyright 2017 Intuit
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
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentTagsByApplication;

import java.util.Collection;
import java.util.Set;

/**
 * Accessor for the tags associated with Experiments.
 */
@Accessor
public interface ExperimentTagAccessor {

    @Query("select * from experiment_tag where app_name IN ? ")
    Result<ExperimentTagsByApplication> getExperimentTags(Collection<Application.Name> applicationNames);
    
    @Query("insert into experiment_tag(app_name, tags) values(?,?)")
    Statement insert(Application.Name appName, Set<String> tags);

    @Query("update experiment_tag set tags = tags + ? WHERE app_name = ?;")
    Statement update(Set<String> tags, Application.Name appName);

    @Query("update experiment_tag set tags = tags - ? WHERE app_name = ?;")
    Statement remove(Set<String> tags, Application.Name appName);

    @Query("delete tags from experiment_tag WHERE app_name = ?;")
    Statement removeAll(Application.Name appName);
}
