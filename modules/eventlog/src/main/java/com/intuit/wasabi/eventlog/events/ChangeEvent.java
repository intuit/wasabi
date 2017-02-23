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
package com.intuit.wasabi.eventlog.events;

/**
 * Denotes a state transition of a single field of an item.
 */
public interface ChangeEvent extends EventLogEvent {

    /**
     * Returns the name of the changed property.
     *
     * @return The name of the property
     */
    String getPropertyName();

    /**
     * Returns a string representation of the state before the property change.
     *
     * Can be {@code null} if it was not determined.
     *
     * @return the old property value
     */
    String getBefore();

    /**
     * Returns a string representation of the state after the property change.
     *
     * @return the new property value
     */
    String getAfter();

}
