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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

import java.util.UUID;

/**
 * Accessor interface
 */
@Accessor
public interface StagingAccessor {
    @Query("insert into staging_v2(time, type, exep , msg) values(now(), ?, ? , ?)")
    ResultSet insertBy(String type, String exception, String message);

    @Query("insert into staging_v2(time, type, exep , msg) values(?, ?, ? , ?)")
    BoundStatement batchInsertBy(UUID time, String type, String exception, String message);

    @Query("select * from staging_v2 limit ?")
    ResultSet batchSelectBy(int batchSize);

    @Query("delete from staging_v2 where time = ?")
    ResultSet deleteBy(UUID timeUUID);

}