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
package com.intuit.wasabi.experimentobjects.filter;

/**
 * Holds a list of properties to be able to search through or sort by which are relevant for the UI.
 */
public enum ExperimentProperty {

    APP_NAME("app_name"),
    EXP_NAME("experiment_name"),
    CREATE_BY("created_by"),
    SAMPLING_PERC("sampling_perc"),
    START_DATE("start_date"),
    END_DATE("end_date"),
    MOD_DATE("mod_date"),
    STATUS("status");

    private final String key;

    /**
     * Constructor.
     *
     * @param key the key to be used
     */
    ExperimentProperty(String key) {
        this.key = key;
    }

    /**
     * Returns the key for the search string.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the key list.
     *
     * @return the key list.
     */
    public static String[] keys() {
        String[] keys = new String[values().length];
        for (int i = 0; i < keys.length; ++i) {
            keys[i] = values()[i].getKey();
        }
        return keys;
    }

    /**
     * Returns the property for the given key.
     *
     * @param key the key
     * @return the property
     */
    public static ExperimentProperty forKey(String key) {
        for (ExperimentProperty property : values()) {
            if (property.getKey().equalsIgnoreCase(key)) {
                return property;
            }
        }
        return null;
    }
}