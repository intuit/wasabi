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
 * A very simple Page wrapper.
 */
public class Page extends ModelItem {
    /**
     * the page name
     */
    public String name;

    /**
     * are new assignments allowed?
     */
    public boolean allowNewAssignment;

    /**
     * The serialization strategy for comparisons and JSON serialization.
     */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();

    /**
     * Copies a page.
     *
     * @param other the page to copy.
     */
    public Page(Page other) {
        update(other);
    }

    /**
     * Creates a page with a name and an assignment allowance status.
     *
     * @param name               page name
     * @param allowNewAssignment assignment allowance
     */
    public Page(String name, boolean allowNewAssignment) {
        this.setName(name);
        this.setAllowNewAssignment(allowNewAssignment);
    }

    /**
     * Sets the name for this page (prefixed).
     *
     * @param name the name
     * @return this
     */
    public Page setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the new assignment allowance.
     *
     * @param allowNewAssignment the allowance
     * @return this
     */
    public Page setAllowNewAssignment(boolean allowNewAssignment) {
        this.allowNewAssignment = allowNewAssignment;
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
        Page.serializationStrategy = serializationStrategy;
    }

    /**
     * Returns the current SerializationStrategy.
     *
     * @return the current SerializationStategy.
     */
    @Override
    public SerializationStrategy getSerializationStrategy() {
        return Page.serializationStrategy;
    }
}
