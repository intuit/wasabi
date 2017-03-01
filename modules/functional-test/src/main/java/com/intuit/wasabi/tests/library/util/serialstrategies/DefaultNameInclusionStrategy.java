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

import com.google.gson.FieldAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implements an SerializationStrategy which only allows fields which have whitelisted names.
 */
public class DefaultNameInclusionStrategy implements SerializationStrategy {

    /**
     * the whitelist
     */
    private List<String> whitelist;

    /**
     * Whitelists method names.
     *
     * @param includes names to whitelist.
     */
    public DefaultNameInclusionStrategy(String... includes) {
        whitelist = new ArrayList<>(includes.length);
        add(includes);
    }

    /**
     * Skips all fields if their names are not contained in the whitelist.
     *
     * @param fieldAttributes the field attributes
     * @return false if the whitelist contains the field name
     */
    @Override
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return !whitelist.contains(fieldAttributes.getName());
    }

    /**
     * Skips no classes.
     *
     * @param aClass the class types
     * @return always false
     */
    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
        return false;
    }

    /**
     * Adds identifiers to the exclusion strategy.
     *
     * @param identifiers the modifiers to be added.
     */
    @Override
    public void add(String... identifiers) {
        whitelist.addAll(Arrays.asList(identifiers));
    }

    /**
     * Removes identifiers from the serialization strategy.
     *
     * @param identifiers the modifiers to be removed.
     */
    @Override
    public void remove(String... identifiers) {
        whitelist.removeAll(Arrays.asList(identifiers));
    }

    /**
     * Checks if an identifier is contained in this strategy.
     *
     * @param identifier the identifier to be checked.
     * @return true if identifier is in this strategy.
     */
    @Override
    public boolean contains(String identifier) {
        return whitelist.contains(identifier);
    }

    /**
     * Checks if the given identifier should be included.
     *
     * @param identifier the identifier to be checked.
     * @return true if identifier is whitelisted
     */
    @Override
    public boolean include(String identifier) {
        return whitelist.contains(identifier);
    }

    /**
     * Checks if the given identifier should be excluded.
     *
     * @param identifier the identifier to be checked.
     * @return true if identifier is not whitelisted
     */
    @Override
    public boolean exclude(String identifier) {
        return !whitelist.contains(identifier);
    }

    /**
     * Returns an explanation how this strategy works.
     *
     * @return this strategy in a human readable manner
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + ": Includes fields with these names: "
                + whitelist.toString();
    }
}
