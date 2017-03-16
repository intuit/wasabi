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
package com.intuit.wasabi.exceptions;

import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.exceptions.WasabiException;

import static com.intuit.wasabi.experimentobjects.exceptions.ErrorCode.BUCKET_NOT_FOUND;

/**
 * Created by smuralidharan2 on 7/31/15.
 */
public class BucketDistributionNotFetchableException extends WasabiException {
    public BucketDistributionNotFetchableException(User.ID userID,
                                                   Application.Name applicationName, Experiment.Label experimentLabel) {
        this(userID, applicationName, experimentLabel, null);
    }

    public BucketDistributionNotFetchableException(User.ID userID,
                                                   Application.Name applicationName, Experiment.Label experimentLabel,
                                                   Throwable rootCause) {
        super(BUCKET_NOT_FOUND,
                "Bucket distributions not found for userID \"" + userID + "\", applicationLabel \"" +
                        applicationName + "\", and experimentLabel \"" +
                        experimentLabel + "\" ",
                rootCause);
    }
}
