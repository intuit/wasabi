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
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.index.PageExperimentByAppNamePage;

import java.util.UUID;

/**
 * Accessor interface
 */
@Accessor
public interface ExperimentPageAccessor {
    @Query("insert into experiment_page(page, exp_id, assign) " +
            "values(?,?,?)")
    Statement insertBy(String page, UUID experimentId, boolean assign);

    @Query("delete from experiment_page where page = ? and exp_id = ?")
    Statement deleteBy(String page, UUID experimentId);

    @Query("select * from experiment_page where exp_id = ? and page = ?")
    Result<PageExperimentByAppNamePage> selectBy(UUID experimentId, String page);

    @Query("select * from experiment_page where exp_id = ?")
    Result<PageExperimentByAppNamePage> selectBy(UUID experimentId);

}
