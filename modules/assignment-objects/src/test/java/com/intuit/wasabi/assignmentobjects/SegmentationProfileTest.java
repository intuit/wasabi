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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class SegmentationProfileTest {

    private HashMap<String, Object> profile;
    private SegmentationProfile segProfile;

    @Before
    public void setUp() throws Exception {
        profile = new HashMap<>();
        segProfile = SegmentationProfile.from(profile).build();
    }

    @Test
    public void testSegmentationProfile() {
        Object val1 = new Object();
        segProfile.addAttribute("test1", val1);
        assertTrue(segProfile.hasAttribute("test1"));
        assertSame(val1, segProfile.getAttribute("test1"));

    }

    @Test
    public void testSegmentationProfileFromOther() {
        SegmentationProfile segProfileOther = SegmentationProfile.from(segProfile).build();

        assertEquals(segProfileOther.getProfile(), segProfile.getProfile());
        assertEquals(segProfileOther.toString(), segProfile.toString());
        assertEquals(segProfileOther.hashCode(), segProfileOther.hashCode());

        assertEquals(segProfile, segProfileOther);
        assertEquals(segProfile.toJSONProfile(), segProfileOther.toJSONProfile());

    }

}
