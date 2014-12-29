package org.cidarlab.eugenelab.servlet;

import java.io.File;
import java.io.FilenameFilter;

/**
 * The EugeneLabFilenameFilter class implements 
 * the java.io.FilenameFilter class and serves 
 * for checking if a file should be displayed 
 * in the EugeneLab File Tree. Hence, it overwrites 
 * the accept(File, String) method of the 
 * java.io.FilenameFilter interface.
 * 
 * @author Ernst Oberortner
 *
 */
public class EugeneLabFilenameFilter 
		implements FilenameFilter {

	private boolean isLoggedIn;
	
	/**
	 * The constructor requires a flag indicating 
	 * if the current user is logged in or not.
	 * 
	 * @param isLoggedIn   true ... the user is logged in
	 *                    false ... otherwise
	 */
	public EugeneLabFilenameFilter(boolean isLoggedIn) {
		super();
		
		this.isLoggedIn = isLoggedIn;
	}
	
	/**
	 * In EugeneLab we have the following rules for displaying 
	 * files.
	 * We don't display hidden files.
	 * If the user is logged in, then we display the "exports" directory 
	 * which contains SBOL and Pigeon images.
	 * If the user is not logged in, then we don't display the "exports" directory.
	 * 
	 * @param dir   ... the file
	 * @param name  ... the filename
	 * 
	 * @return
	 */
	@Override
	public boolean accept(File dir, String name) {
		
		// if the user is logged in, then 
		// we only do not display hidden files
		if(isLoggedIn) {
			return !(name.startsWith("."));
		}
		
		// if the user is not logged in, then 
		// we don't display hidden files nor 
		// the exports directory.
		return !(name.startsWith(".")) && !(name.equals("exports"));
	}

}
