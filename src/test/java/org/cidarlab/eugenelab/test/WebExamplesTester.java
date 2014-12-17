package org.cidarlab.eugenelab.test;

import static org.junit.Assert.*;

import java.io.File;

import org.cidarlab.eugene.Eugene;
import org.cidarlab.eugene.exception.EugeneException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebExamplesTester {

	@BeforeClass
	public static void setUpBeforeClass() 
			throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() 
			throws Exception {
	}

	@Before
	public void setUp() 
			throws Exception {
		
	}

	@After
	public void tearDown() 
			throws Exception {
		
	}

	@Test
	public void testLibraryCreation() {
		try {
			this.test("./src/main/webapp/home/no_name_user/sequence-examples/random-sequences");
			this.test("./src/main/webapp/home/no_name_user/sequence-examples/reverse-complement");
			this.test("./src/main/webapp/home/no_name_user/sequence-examples/sequence-homology");
		} catch(EugeneException ee) {
			
		}
	}

	public void test(String file) 
			throws EugeneException {
		try {
			long t1 = System.nanoTime();
			
			new Eugene().executeFile(new File(file));

			long tProcessing = System.nanoTime() - t1;
			
			System.out.println("[TestSuite.test] full processing time: "+tProcessing*Math.pow(10, -9)+"sec");
		} catch(Exception e) {
			e.printStackTrace();
			throw new EugeneException(e.getMessage());
		}
	}

}
