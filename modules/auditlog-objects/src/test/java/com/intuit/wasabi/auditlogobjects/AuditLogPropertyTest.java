package com.intuit.wasabi.auditlogobjects;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class AuditLogPropertyTest {

    @Test
    public void testKeys() throws Exception {
        Assert.assertEquals(AuditLogProperty.values().length, AuditLogProperty.keys().length);

        List<String> keyList = Arrays.asList(AuditLogProperty.keys());
        for (AuditLogProperty property : AuditLogProperty.values()) {
            Assert.assertTrue("Property " + property + " not in list.", keyList.contains(property.getKey()));
        }
    }

    @Test
    public void testForKey() throws Exception {
        AuditLogProperty propertyNull = AuditLogProperty.forKey("invalidKey");
        Assert.assertNull(propertyNull);

        AuditLogProperty property = AuditLogProperty.forKey(AuditLogProperty.APP.getKey());
        Assert.assertEquals(AuditLogProperty.APP, property);
    }
}
