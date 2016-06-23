package com.intuit.wasabi.experimentobjects;

import static org.junit.Assert.*;

import org.junit.Test;

public class BucketListTest {

	@Test
	public void testEquals() {
		BucketList bl = new BucketList(2);
		assertFalse(bl.equals(null));
		assertFalse(bl.equals("c2"));
	}

}
