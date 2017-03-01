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

import io.swagger.annotations.ApiModelProperty;

import java.util.List;

public class ComponentHealthList {

    @ApiModelProperty(required = true)
    private List<ComponentHealth> componentHealths;
    @ApiModelProperty(required = false)
    private String version;

    public ComponentHealthList(List<ComponentHealth> componentHealths) {
        this.componentHealths = componentHealths;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ComponentHealth> getComponentHealths() {
        return componentHealths;
    }
}
