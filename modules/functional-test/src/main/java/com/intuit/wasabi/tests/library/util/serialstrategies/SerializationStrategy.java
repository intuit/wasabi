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
package com.intuit.wasabi.tests.library.util.serialstrategies;

import com.google.gson.ExclusionStrategy;

/**
 * SerializationStrategies are used by the model classes for Gsonserialization.
 */
public interface SerializationStrategy extends ExclusionStrategy {

    /**
     * Adds identifiers to the serialization strategy.
     *
     * @param identifiers the identifiers to be added.
     */
    void add(String... identifiers);

    /**
     * Removes identifiers from the serialization strategy.
     *
     * @param identifiers the identifiers to be removed.
     */
    void remove(String... identifiers);

    /**
     * Checks if an identifier is contained in this strategy.
     *
     * @param identifier the identifier to be checked.
     * @return true if identifier is in this strategy.
     */
    boolean contains(String identifier);

    /**
     * Checks if the given identifier should be included.
     *
     * @param identifier the identifier to be checked.
     * @return true if the identifier should be includeds
     */
    boolean include(String identifier);

    /**
     * Checks if the given identifier should be excluded.
     *
     * @param identifier the identifier to be checked.
     * @return true if the identifier should be excluded.
     */
    boolean exclude(String identifier);

}
