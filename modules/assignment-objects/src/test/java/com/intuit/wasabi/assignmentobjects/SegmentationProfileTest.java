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
package com.intuit.wasabi.assignmentobjects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class SegmentationProfileTest{

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
        SegmentationProfile segProfile1 = SegmentationProfile.newInstance().build();
        segProfile1.setProfile(profile);
        SegmentationProfile segProfileOther = SegmentationProfile.from(segProfile1).build();

        assertEquals(segProfile1, segProfileOther);
    }

}
