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
package com.intuit.wasabi.email;

/**
 * This enum holds the different fields that can be replaced in the email.
 */
public enum WasabiEmailFields {

    EXPERIMENT_ID("experiment_id"),
    EXPERIMENT_LABEL("experiment_label"),
    EXPERIMENT_STATE("experiment_state"),
    APPLICATION_NAME("application_name"),
    USER_NAME("user_name"),
    LINK("link"),
    FIELD_NAME("exp_field"),
    FIELD_BEFORE("exp_field_value_before"),
    FIELD_AFTER("exp_field_value_after"),
    BUCKET_NAME("bucket_label"),
    EMAIL_LINKS("emailLinks"),
    NO_SUCH_VALUE("");

    private final String emailValue;

    /*enum private*/ WasabiEmailFields(String value) {
        this.emailValue = value;
    }

    public static WasabiEmailFields getByEmailValue(String emailValue) {
        WasabiEmailFields result = NO_SUCH_VALUE;
        for (WasabiEmailFields val : WasabiEmailFields.values()) {
            if (val.emailValue.equalsIgnoreCase(emailValue)) {
                result = val;
                break;
            }
        }
        return result;
    }

    public String getEmailValue() {
        return emailValue;
    }

    @Override
    public String toString() {
        return emailValue;
    }
}
