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
package com.intuit.wasabi.tests.model;

import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;

/**
 * A very simple Application wrapper.
 */
public class Application extends ModelItem {

    /**
     * the application name
     */
    public String name;

    /**
     * The serialization strategy for comparisons and JSON serialization.
     */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();

    /**
     * Creates an application with a default name.
     */
    public Application() {
        this("");
    }

    /**
     * Creates an application with the given name.
     *
     * @param name the application name
     */
    public Application(String name) {
        this.name = name;
    }


    /**
     * Creates a deep copy of the {@code other} application.
     *
     * @param other an application to copy.
     */
    public Application(Application other) {
        update(other);
    }

    /**
     * Sets the name and returns this instance.
     *
     * @param name the application name
     * @return this
     */
    public Application setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the SerializationStrategy which is used for JSON serialization and
     * {@link Experiment#equals(Object)}.
     *
     * @param serializationStrategy the serialization strategy
     */
    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        Application.serializationStrategy = serializationStrategy;
    }

    /**
     * Returns the current SerializationStrategy.
     *
     * @return the current SerializationStategy.
     */
    @Override
    public SerializationStrategy getSerializationStrategy() {
        return Application.serializationStrategy;
    }

}
