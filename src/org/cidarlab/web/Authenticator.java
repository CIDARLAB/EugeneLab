package org.cidarlab.web;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Authenticator {

	public static boolean login(String user, String password) {
		
		/*
		 * hash & salt the password
		 */
		byte[] encrypted_password = getEncryptedPassword(
				password, generateSalt());
	
		/*
		 * then, we store username and password into 
		 * out database
		 */
		
		System.out.println("user: " + user);
		System.out.println("encrypted password: " + encrypted_password);
		
		return false;
	}

	public static void register(String user, String password) {
		
		/*
		 * hash & salt the password
		 */
		byte[] encrypted_password = getEncryptedPassword(
				password, generateSalt());
	
		/*
		 * then, we store username and password into 
		 * out database
		 */
		
		System.out.println("user: " + user);
		System.out.println("encrypted password: " + encrypted_password);
	}
	
	
	/**
	 * Generates a hash from the supplied password and salt (see
	 * {@link PasswordEncryptionService#generateSalt()}) using the PBKDF2 with
	 * SHA-1 algorithm
	 * 
	 * @param password
	 *            - Password to hash
	 * @param salt
	 *            - Salt to use for the hashing
	 * @return - Password hash
	 */
	private static byte[] getEncryptedPassword(String password, byte[] salt) {
		try {
			// PBKDF2 with SHA-1 as the hashing algorithm. Note that the NIST
			// specifically names SHA-1 as an acceptable hashing algorithm for
			// PBKDF2
			String algorithm = "PBKDF2WithHmacSHA1";
			// SHA-1 generates 160 bit hashes, so that's what makes sense here
			int derivedKeyLength = 160;
			// Pick an iteration count that works for you. The NIST recommends
			// at
			// least 1,000 iterations:
			// http://csrc.nist.gov/publications/nistpubs/800-132/nist-sp800-132.pdf
			// iOS 4.x reportedly uses 10,000:
			// http://blog.crackpassword.com/2010/09/smartphone-forensics-cracking-blackberry-backup-passwords/
			int iterations = 20000;
 
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt,
					iterations, derivedKeyLength);
 
			SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);
 
			return f.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			/*
			 * This should only happen when running this code on a new
			 * environment. It will never happen unpredictably and therefore is
			 * caught here so that utilizing code doesn't require a try/catch
			 * block.
			 */
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * SALT generator
	 */
	private static byte[] generateSalt() {
		try {
			// we're using SecureRandom instead of just Random
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
 
			// Generate a 8 byte (64 bit) salt as recommended by RSA PKCS5
			byte[] salt = new byte[8];
			random.nextBytes(salt);
 
			return salt;
		} catch (NoSuchAlgorithmException e) {
			/*
			 * This should only happen when running this code on a new
			 * environment. It will never happen unpredictably and therefore is
			 * caught here so that utilizing code doesn't require a try/catch
			 * block.
			 */
			e.printStackTrace();
		}
		return null;
	}
	
}
