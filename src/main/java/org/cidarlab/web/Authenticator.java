package org.cidarlab.web;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * The Authenticator class represents the interface 
 * between the AuthenticationServlet and the 
 * persisted objects.
 * 
 * @author Ernst Oberortner
 *
 */
public class Authenticator {

    private EntityManager entityManager;
	
	public Authenticator(String db) {
        
		try {
			EntityManagerFactory emf = 
					Persistence.createEntityManagerFactory( db );
			
			this.entityManager = emf.createEntityManager();

		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean login(String user, String password) 
			throws AuthenticationException {

		System.out.println("[Login] user: " + user);
		System.out.println("[Login] ui : " + this.entityManager.find(UserInformation.class, user));
		
		UserInformation ui = this.entityManager.find(UserInformation.class, user);
		if(null == ui) {
			throw new AuthenticationException("Invalid Login!");
		}

		
		/*
		 * hash & salt the password
		 */		
		byte[] received_password = getEncryptedPassword(
				password, ui.getSalt());
	
		
		/*
		 * now, the passwords must match
		 */
		if(Arrays.equals(received_password, ui.getEncryptedPassword())) {
			return true;
		}
		
		return false;
	}

	public void register(String user, String password) 
			throws AuthenticationException {
		
		if(null != this.entityManager.find(UserInformation.class, user)) {
			throw new AuthenticationException("The user exists already!");
		}
		
		
		/*
		 * hash & salt the password
		 */
		byte[] salt = generateSalt(); 
		byte[] encrypted_password = getEncryptedPassword(
				password, salt);
		
		/*
		 * then, we store username and password into 
		 * out database
		 */
		this.persist(
				new UserInformation(user, salt, encrypted_password));
	}
	
	private void persist(UserInformation ui) {
		this.entityManager.getTransaction().begin();
		this.entityManager.persist(ui);
		this.entityManager.getTransaction().commit();
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
	private byte[] getEncryptedPassword(String password, byte[] salt) {
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
	private byte[] generateSalt() {
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
