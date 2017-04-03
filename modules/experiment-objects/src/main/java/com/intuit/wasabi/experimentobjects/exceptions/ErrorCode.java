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
package com.intuit.wasabi.experimentobjects.exceptions;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum ErrorCode {

    // Input validation errors
    ILLEGAL_ARGUMENT("4000", 400),
    INVALID_IDENTIFIER("4001", 400),
    INVALID_LIST_SIZE("4002", 400),
    INVALID_EXPERIMENT_PRIORITY("4003", 400),
    END_TIME_PASSED("4004", 404),
    DIFFERENT_APPLICATIONS("4005", 404),
    INVALID_PROFILE("4006", 404),
    INVALID_TIME_FORMAT("4007", 400),
    INVALID_TIME_ZONE("4008", 400),

    // Not found errors
    EXPERIMENT_NOT_FOUND("4101", 404),
    BUCKET_NOT_FOUND("4102", 404),
    ASSIGNMENT_NOT_FOUND("4103", 404),
    APPLICATION_NOT_FOUND("4104", 404),
    EXCLUSION_NOT_FOUND("4105", 404),

    // Constraint errors
    NULL_CONSTRAINT_VIOLATION("4201", 400),
    UNIQUE_CONSTRAINT_VIOLATION("4202", 400),
    TIME_CONSTRAINT_VIOLATION("4203", 400),
    APPLICATION_CONSTRAINT_VIOLATION("4204", 400),

    // Concurrency errors
    CONCURRENT_UPDATE("4301", 400),

    // Assignment update errors
    ASSIGNMENT_EXISTS_VIOLATION("4401", 409), //todo: is this the correct code?
    INVALID_ASSIGNMENT("4402", 409),

    // Not allowed errors
    AUTHENTICATION_FAILED("4501", 401),
    USER_TOOLS_FAILED("4502", 401),
    ASSIGNMENT_FAILED("4503", 401),
    EVENT_FAILED("4504", 401),
    ANALYTICS_FAILED("4505", 405),

    // Database errors
    DATABASE_ERROR("5001", 500),
    REPOSITORY_ERROR("5101", 500),

    // State errors
    INVALID_EXPERIMENT_STATE("6001", 400),
    INVALID_EXPERIMENT_STATE_TRANSITION("6002", 400),
    INVALID_BUCKET_STATE_TRANSITION("6003", 400),

    // application errors
    EMAIL_NOT_ACTIVE_ERROR("6042", 500),
    EMAIL_SERVICE_ERROR("6043", 500),
    EVENT_LOG_ERROR("6050", 500),
    AUDIT_LOG_ERROR("6051", 500),

    // Pagination parameter errors
    SORT_KEY_UNPROCESSABLE("7000", 400),
    FILTER_KEY_UNPROCESSABLE("7001", 400),

    // Other errors
    APPLICATION_MISMATCH_ERROR("9997", 400),
    __TEST_ERROR("9998", 400), // TODO: This is lame
    UNKNOWN_ERROR("9999", 500);

    ErrorCode(String partialCode, int responseCode) {
        this.code = "WASABI-" + partialCode;
        this.responseCode = responseCode;
    }

    @Override
    public String toString() {
        return code;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public static ErrorCode fromString(String value) {
        return errorMap.get(value);
    }

    private String code;
    private int responseCode;
    private static Map<String, ErrorCode> errorMap = new HashMap<>();

    static {
        // Map all the error codes
        EnumSet<ErrorCode> codes = EnumSet.allOf(ErrorCode.class);
        for (ErrorCode code : codes) {
            errorMap.put(code.toString(), code);
        }
    }
}
