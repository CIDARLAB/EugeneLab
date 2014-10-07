package org.cidarlab.web.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
	AuthenticationTester.class, 
	EugeneLabServletTester.class})
public class AllTests {

}
