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
package com.intuit.wasabi.auditlog.impl;

import com.intuit.wasabi.auditlog.AuditLog;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 *
 */
public class NoopAuditLogImplTest {

    @Test
    public void testNoopAuditLogImplTest() throws Exception {
        AuditLog auditLog = new NoopAuditLogImpl();
        Assert.assertEquals("", Collections.emptyList(), auditLog.getAuditLogs(null));
        Assert.assertEquals("", Collections.emptyList(), auditLog.getAuditLogs(null));
    }
}
