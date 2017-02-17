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
package com.intuit.wasabi.repository.cassandra.accessor.index;

import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.repository.cassandra.pojo.index.PageExperimentByAppNamePage;

import java.util.UUID;

/**
 * Interface for accessor
 */
@Accessor
public interface PageExperimentIndexAccessor {
    /**
     * Insert using attributes
     *
     * @param appName
     * @param page
     * @param experimentId
     * @param assign
     * @return Statement
     */
    @Query("insert into page_experiment_index(app_name, page, exp_id, assign) " +
            "values(?,?,?,?)")
    Statement insertBy(String appName, String page, UUID experimentId, boolean assign);

    /**
     * Delete by using attributes
     *
     * @param appName
     * @param page
     * @param uuid
     * @return Statement
     */
    @Query("delete from page_experiment_index where app_name = ? and page = ? and exp_id = ?")
    Statement deleteBy(String appName, String page, UUID uuid);

    /**
     * Get by attributes
     *
     * @param appName
     * @param page
     * @return result
     */
    @Query("select * from page_experiment_index where app_name = ? and page = ?")
    Result<PageExperimentByAppNamePage> selectBy(String appName, String page);


    /**
     * Asynchronously fetch experiments associated to given application and page.
     *
     * @param appName
     * @param page
     * @return result
     */
    @Query("select * from page_experiment_index where app_name = ? and page = ?")
    ListenableFuture<Result<PageExperimentByAppNamePage>> asyncSelectBy(String appName, String page);

}
