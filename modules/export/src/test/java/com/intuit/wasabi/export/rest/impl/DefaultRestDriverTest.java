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
package com.intuit.wasabi.export.rest.impl;

import static org.mockito.BDDMockito.*;

import org.apache.http.impl.client.CloseableHttpClient;
import org.assertj.core.api.BDDAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.intuit.wasabi.export.rest.Driver;

@RunWith(MockitoJUnitRunner.class)
public class DefaultRestDriverTest {

	@Mock
	Driver.Configuration configuration;
	
	DefaultRestDriver driver;
	
	@Before
	public void setUp () {
		driver = new DefaultRestDriver(configuration, "proxyHost", 50);
	}

	@Test
	public void testGetCloseableWithProxyDifferentFromNonProxy() {
		given(configuration.getConnectionTimeout()).willReturn(2);
		given(configuration.getSocketTimeout()).willReturn(5);
		
		CloseableHttpClient httpProxy = driver.getCloseableHttpClient(true);
		CloseableHttpClient httpNoProxy = driver.getCloseableHttpClient(false);

		BDDAssertions.then(httpProxy).isNotEqualTo(httpNoProxy);
	}

}
