package org.cidarlab.web.test;

import static org.junit.Assert.*;

import org.cidarlab.web.AuthenticationException;
import org.cidarlab.web.Authenticator;
import org.junit.BeforeClass;
import org.junit.Test;

public class AuthenticationTester {

	private static final String USER_DB_NAME = "TEST";
	
	@BeforeClass
	public static void instantiate() {
		System.out.println("*** ONLY ONCE ***");
	}
	
	@Test
	public void testRegister() {
		Authenticator auth = new Authenticator(USER_DB_NAME);

		try {
			auth.register("ernst", "s3cr3t");
		} catch(AuthenticationException ae) {
			ae.printStackTrace();
		}
	}
	
	@Test
	public void testValidLogin() {
		Authenticator auth = new Authenticator(USER_DB_NAME);
		
		try {
			auth.login("ernst", "s3cr3t");
			
			assertTrue(
					auth.login("ernst", "s3cr3t"));
		} catch(AuthenticationException ae) {
			//ae.printStackTrace();
		}
		
	}

	@Test
	public void testInvalidLogin() {
		Authenticator auth = new Authenticator(USER_DB_NAME);
		try {
			auth.login("me not", "regIster3d");
		} catch(AuthenticationException ae) {
			assertEquals("Invalid Login!", ae.getMessage());
		}
	}
}
