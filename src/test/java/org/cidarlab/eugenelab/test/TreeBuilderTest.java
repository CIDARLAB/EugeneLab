package org.cidarlab.eugenelab.test;

import org.cidarlab.eugenelab.servlet.TreeBuilder;
import org.json.JSONArray;
import org.junit.Test;

public class TreeBuilderTest {

	@Test
	public void testOnlyFiles() {
		TreeBuilder tb = new TreeBuilder();
		
		JSONArray testDir = tb.buildFileTree("./src/test/java/org/cidarlab/eugenelab/test/", true);
		
		assert(testDir != null);
		
		// no folder involved
		assert(!testDir.toString().contains("\"isFolder\":true"));
	}

	@Test
	public void testOneRecursion() {
		TreeBuilder tb = new TreeBuilder();
		
		JSONArray testDir = tb.buildFileTree("./src/test/java/org/cidarlab/eugenelab/", true);
		
		assert(testDir != null);
		assert(testDir.toString().contains("\"isFolder\":true"));
	}

	@Test
	public void testMultipleRecursions() {
		TreeBuilder tb = new TreeBuilder();
		
		JSONArray testDir = tb.buildFileTree("./src/main/webapp/", true);

		assert(testDir != null);
		assert(testDir.toString().contains("\"isFolder\":true"));
	}

}
