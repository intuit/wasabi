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
package com.intuit.wasabi.export.rest.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRestEndPointTest {

    @Mock
    DefaultRestEndPointConfiguration config;

    @InjectMocks
    DefaultRestEndPoint rest;

    @Test
    public void testGetRestEndPointURI() {
        given(config.getScheme()).willReturn("http");
        given(config.getHost()).willReturn("host1");
        given(config.getPath()).willReturn("path1");
        given(config.getPort()).willReturn(5);

        URI uri = rest.getRestEndPointURI();

        then(uri.toString()).isEqualTo("http://host1:5path1");
    }

    @Test
    public void testUseProxy() {
        given(config.useProxy()).willReturn(true);

        then(rest.useProxy()).isEqualTo(true);

        given(config.useProxy()).willReturn(false);

        then(rest.useProxy()).isEqualTo(false);
    }

    @Test
    public void testGetRetries() {
        given(config.getRetries()).willReturn(2);

        then(rest.getRetries()).isEqualTo(2);
    }

}
