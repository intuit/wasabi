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
package com.intuit.wasabi.assignment;

import com.intuit.wasabi.experimentobjects.Experiment;

import java.io.UnsupportedEncodingException;
import java.net.URI;

/**
 * Decorator for creating uri from experiment
 */
public interface AssignmentDecorator {


    /**
     * Given an experiment, derive the materialized URI for calling other resources on this experiment
     *
     * @param experiment {@link com.intuit.wasabi.experimentobjects.Experiment} current experiment
     * @return URI contains the materialized URI for experiment
     * @throws UnsupportedEncodingException
     */
    URI materializeUri(Experiment experiment) throws UnsupportedEncodingException;
}
