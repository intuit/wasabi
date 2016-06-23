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
package com.intuit.wasabi.api;

import org.junit.Test;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ComponentHealthTest {

    @Test
    public void componentHealth() {
        ComponentHealth health = new ComponentHealth("myComponent");

        health.setHealthy(true);
        health.setDetailedMessage("message");

        assertThat(health.getComponentName(), is("myComponent"));
        assertThat(health.isHealthy(), is(TRUE));
        assertThat(health.getDetailedMessage(), is("message"));
    }
}
