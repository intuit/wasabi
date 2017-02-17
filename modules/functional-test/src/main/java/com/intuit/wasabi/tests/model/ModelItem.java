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

import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Superclass for the model items to provide common methods.
 */
public abstract class ModelItem {

    protected static final Logger LOGGER = getLogger(ModelItem.class);

    /**
     * Updates this item with the values of the other item.
     *
     * @param other the item supplying the values.
     */
    public void update(ModelItem other) {
        if (!this.getClass().isInstance(other)) {
            LOGGER.warn("Tried to update " + this + " with " + other + "! No change was done.");
            return;
        }

        for (Field field : this.getClass().getFields()) {
            try {
                field.set(this, field.get(other));
            } catch (IllegalAccessException ignored) {
                // do nothing, just skip this field
            }
        }
    }

    /**
     * Returns a String representing this instance.
     *
     * @return a String
     */
    @Override
    public String toString() {
        return new GsonBuilder().create().toJson(this);
    }

    /**
     * Returns a JSON String representing this instance. Applies the SerializationStrategy.
     *
     * @return a JSON String
     */
    public String toJSONString() {
        return new GsonBuilder().addSerializationExclusionStrategy(this.getSerializationStrategy())
                .create().toJson(this);
    }

    /**
     * Returns the current SerializationStrategy.
     *
     * @return the current SerializationStategy.
     */
    public abstract SerializationStrategy getSerializationStrategy();

    /**
     * Sets the SerializationStrategy which is used for JSON serialization and
     * {@link #equals(Object)}.
     *
     * @param serializationStrategy the serialization strategy
     */
    public abstract void setSerializationStrategy(SerializationStrategy serializationStrategy);

    /**
     * Implements an equals method to compare this instance to other objects.
     * Two instances are considered iff they are both of the same type and their
     * members are equal. That also means that members of both experiments can be
     * {@code null}, as long as the specific member is {@code null} for both instances
     * and not just one.
     * <p>
     * However some tests might need two instances to be equal in all but a few attributes, for example
     * two experiments can be equal except for their {@code modificationTime}.
     * <p>
     * In that case you can specify the fields to be excluded from the equality tests by
     * setting the {@link SerializationStrategy} accordingly.
     * <p>
     * Note that this slightly breaks the contract with the consistency of equals and hashCode!
     *
     * @param other another object
     * @return true iff both are objects are of the same type and all but their excluded member variables are equal.
     */
    @Override
    public boolean equals(Object other) {
        if (!this.getClass().isInstance(other)) {
            return false;
        }

        boolean equal = true;
        for (Field field : this.getClass().getFields()) {
            if (!this.getSerializationStrategy().shouldSkipField(new FieldAttributes(field))) {
                try {
                    boolean thisFieldEquals = Objects.equals(field.get(this), field.get(other));
                    if (!thisFieldEquals) {
                        LOGGER.debug("Field " + field.getName() + " not equal.");
                    }
                    equal &= thisFieldEquals;
                } catch (IllegalAccessException ignored) {
                    // do nothing, just skip this field
                }
            }
        }
        return equal;
    }

    /**
     * See {@link Object#hashCode()}}.
     * <p>
     * Uses {@link HashCodeBuilder}.
     *
     * @return the hashcode for this instance
     */
    @Override
    public int hashCode() {
        HashCodeBuilder hcb = new HashCodeBuilder();
        for (Field field : this.getClass().getFields()) {
            try {
                hcb.append(field.get(this));
            } catch (IllegalAccessException ignored) {
                // do nothing, just skip this field
            }
        }
        return hcb.hashCode();
    }
}
