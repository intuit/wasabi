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
import com.google.common.util.concurrent.ListenableFuture;
import com.intuit.wasabi.repository.cassandra.pojo.Exclusion;

import java.util.UUID;

/**
 * Interface to exclusion table
 */
@Accessor
public interface ExclusionAccessor {

    /**
     * Get exclusion for the base experiment
     *
     * @param base experiment id
     * @return exclusion instances
     */
    @Query("select * from exclusion where base = ?")
    Result<Exclusion> getExclusions(UUID base);

    @Query("select * from exclusion where base = ?")
    ListenableFuture<Result<Exclusion>> asyncGetExclusions(UUID base);

    /**
     * Delete exclusion pair
     *
     * @param base expriment id
     * @param pair exclusion experiment id
     * @return Statement object
     */
    @Query("delete from exclusion where base = ? and pair = ?")
    Statement deleteExclusion(UUID base, UUID pair);

    /**
     * Create exclusion pair
     *
     * @param base the base experiment id
     * @param pair the exclusion experiment id
     * @return Statement object
     */
    @Query("insert into exclusion(base, pair) values(?, ?)")
    Statement createExclusion(UUID base, UUID pair);
}
