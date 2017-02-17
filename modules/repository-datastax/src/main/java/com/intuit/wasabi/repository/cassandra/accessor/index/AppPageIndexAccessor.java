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
import com.intuit.wasabi.repository.cassandra.pojo.AppPage;

/**
 * Interface for accessor
 */
@Accessor
public interface AppPageIndexAccessor {
    /**
     * Insert records
     *
     * @param appName
     * @param page
     * @return statement
     */
    @Query("insert into app_page_index(app_name, page) " +
            "values(?,?)")
    Statement insertBy(String appName, String page);

    /**
     * Delete by attributes
     *
     * @param page
     * @param appName
     * @return statement
     */
    @Query("delete from app_page_index where page = ? and app_name = ?")
    Statement deleteBy(String page, String appName);

    /**
     * Get by attributes
     *
     * @param page
     * @param appName
     * @return result
     */
    @Query("select * from app_page_index where page = ? and app_name = ?")
    Result<AppPage> selectBy(String page, String appName);

    /**
     * Get by attributes
     *
     * @param appName
     * @return result
     */
    @Query("select * from app_page_index where app_name = ?")
    Result<AppPage> selectBy(String appName);

}
