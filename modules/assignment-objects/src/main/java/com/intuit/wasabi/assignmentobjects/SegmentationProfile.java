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
package com.intuit.wasabi.assignmentobjects;

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to holds a map of key values (user attributes) for segmentation with Hyrule
 */
//TODO: remove this class. it does nothing other than a wrapper over Map<String, Object>
public class SegmentationProfile {

    @ApiModelProperty(value = "a map of key/value user attributes")
    private Map<String, Object> profile = new HashMap<>();

    protected SegmentationProfile() {
        super();
    }

    public static Builder newInstance() {
        return new Builder();
    }

    public static Builder from(SegmentationProfile profile) {
        return new Builder(profile);
    }

    public static Builder from(Map<String, Object> profile) {
        return new Builder(profile);
    }

    public static class Builder {
        private Builder() {
            instance = new SegmentationProfile();
        }

        private Builder(SegmentationProfile other) {
            this();
            instance.profile = other.profile;
        }

        public Builder(Map<String, Object> profile) {
            this();
            instance.profile = profile;
        }

        public SegmentationProfile build() {
            SegmentationProfile result = instance;
            instance = null;
            return result;
        }

        private SegmentationProfile instance;
    }

    public Map<String, Object> getProfile() {
        return profile;
    }

    public JSONObject toJSONProfile() {
        JSONObject profileJson = new JSONObject(profile);
        return profileJson;
    }

    public void setProfile(Map<String, Object> profile) {
        this.profile = profile;
    }

    public void addAttribute(String key, Object value) {
        if (profile != null && key != null && value != null) {
            profile.put(key, value);
        }
    }

    public Object getAttribute(String key) {
        return profile.get(key);
    }

    public boolean hasAttribute(String key) {
        return profile != null && profile.containsKey(key);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
