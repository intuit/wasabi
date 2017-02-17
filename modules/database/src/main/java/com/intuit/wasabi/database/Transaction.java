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
package com.intuit.wasabi.database;

import java.util.List;

/**
 * Represents a database transaction
 */
public interface Transaction {

    <S> S attach(Class<S> transactional);

    /**
     * Executes an insert database query
     *
     * @param query  Database query
     * @param params Dynamic parameters
     * @throws Exception in case of errors
     */
    void insert(String query, Object... params) throws Exception;

    /**
     * Executes an insert database query and returns unique row key
     *
     * @param query  Database query
     * @param params Dynamic parameters
     * @return unique key for the row
     */
    Integer insertAndReturnKey(String query, Object... params);

    /**
     * Executes a select database query and return selected rows
     *
     * @param query  Database query
     * @param params Dynamic parameters
     * @return list of selected rows
     */
    List select(String query, Object... params);

    /**
     * Returns the transaction object given a block
     *
     * @param block Block
     * @return transaction object
     */
    Object transaction(Block block);

    /**
     * Executes an update query and returns unique row key
     *
     * @param query  Database query
     * @param params Dynamic parameters
     * @return unique row key
     */
    Integer update(String query, Object... params);

    interface Block {

        /**
         * Returns an object given the transaction
         *
         * @param transaction Transaction
         * @return Object
         */
        Object value(Transaction transaction);
    }
}
