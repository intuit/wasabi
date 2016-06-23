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
package com.intuit.wasabi.export.rest;

import java.net.URI;

/**
 * Interface for capturing rest endpoint related attributes
 */
public interface RestEndPoint {

	/**
	 * Get rest endpoint uri
	 * @return the uri
	 */
    URI getRestEndPointURI();
    
    /**
     * Should use proxy
     * @return whether to use proxy
     */
    Boolean useProxy();
    
    /**
     * Number of retries
     * @return retries
     */
    int getRetries();

    /**
     * Configuration parames for rest endpoint
     */
    interface Configuration{
    	
    	/**
    	 * Get the scheme
    	 * @return scheme
    	 */
        String getScheme();
        
        /**
         * Get host
         * @return host
         */
        String getHost();
        
        /**
         * Get port
         * @return port
         */
        int getPort();
        
        /**
         * Get path
         * @return path
         */
        String getPath();
        
        /**
         * Should a proxy be used
         * @return to use proxy or not
         */
        Boolean useProxy();
        
        /**
         * Get retries
         * @return retries
         */
        int getRetries();
    }
}
