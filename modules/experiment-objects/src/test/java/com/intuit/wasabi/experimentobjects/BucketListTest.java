package com.intuit.wasabi.experimentobjects;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class BucketListTest {

    @Test
    public void testEquals() {
        BucketList bl = new BucketList(2);
        assertFalse(bl.equals(null));
        assertFalse(bl.equals("c2"));
    }
}