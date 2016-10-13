/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.experimentobjects.exception;

import com.intuit.wasabi.exceptions.WasabiClientException;
import com.intuit.wasabi.experimentobjects.Bucket;

import static com.intuit.wasabi.exceptions.ErrorCode.BUCKET_NOT_FOUND;

/**
 * Indicates a specified bucket wasn't found
 */
public class BucketNotFoundException extends WasabiClientException {

    private static final long serialVersionUID = -879695048442782438L;

    public BucketNotFoundException(Bucket.Label bucketLabel) {
		this(bucketLabel,null);
	}

    //todo: add context of experiment ID (or experiment label and app)
	public BucketNotFoundException(Bucket.Label bucketLabel,
            Throwable rootCause) {
		super(BUCKET_NOT_FOUND,
            "Bucket \""+bucketLabel+"\" not found",
            rootCause);
	}
}
