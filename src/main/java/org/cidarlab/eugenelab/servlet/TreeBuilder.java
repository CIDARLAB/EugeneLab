package org.cidarlab.eugenelab.servlet;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cidarlab.eugene.dom.Component;
import org.cidarlab.eugene.dom.Device;
import org.cidarlab.eugene.dom.Part;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The TreeBuilder class provides methods to read 
 * the content of a EugeneLab user's HOME directory and
 * the part/design library of the EugeneLab user. 
 * The corresponding methods are buildFileTree and buildLibraryTree
 * respectively.
 *  
 * @author Ernst Oberortner
 */
public class TreeBuilder {

	/**
	 * The buildFileTree method recursively reads the files 
	 * from a given directory (i.e. the EugeneLab user's HOME_DIRECTORY) and  
	 * builds a JSONArray that represents the file- and 
	 * directory structure. 
	 * 
	 * The JSONArray is important for the Front-End's dynatree JS library 
	 * to display the content of a EugenLab user's HOME_DIRECTORY in 
	 * a File-Tree format.
	 * 
	 * @param home ... the directory
	 * 
	 * @return a JSONArray representing the directory's recursive content 
	 */
	public JSONArray buildFileTree(String home, boolean isLoggedIn) {
		
        File rootFolder = new File(home);

        // we read all files and directories 
        // excluding hidden files/directories ('.') and 
        // the "exports" directory which is the default directory 
        // for all SBOL files (visual and textual).
		FilenameFilter ff = new EugeneLabFilenameFilter(isLoggedIn);
        File[] rootFiles = rootFolder.listFiles(ff);
        
        
        JSONArray rootArray = new JSONArray();        
        if (null == rootFiles) {
            return rootArray;
        }
        
        // we iterate over all the files/directories 
        // in the given directory
        // and build the JSONArray's content.
        for (int i = 0; i < rootFiles.length; i++) {
        	
            JSONObject toPut = new JSONObject();
            toPut.put("title", rootFiles[i].getName());
            
            // if it's a directory
            if (rootFiles[i].isDirectory()) {

            	// recursion
            	JSONArray childs = 
            			this.buildFileTree(rootFiles[i].toString(), isLoggedIn);

            	toPut.put("isFolder", true);
            	toPut.put("children", childs);
            	
            } else {
                toPut.put("icon", 
                		this.getIcon(rootFiles[i].getName().toLowerCase()));
            }

            rootArray.put(toPut);
        }

        return rootArray;
	}
	
//	private JSONArray processDirectory(File dir) {
//		
//    	JSONArray childs = new JSONArray();
//        File[] subFiles = dir.listFiles();
//        for (int j = 0; j < subFiles.length; j++) {
//        	JSONObject child = new JSONObject();
//        	child.put("title", subFiles[j].getName());
//        	
//        	// we also try to find a nice icon for 
//        	// the file depending on its extension
//            child.put("icon", this.getIcon(
//            		subFiles[j].getName().toLowerCase()));
//        	  
//        	childs.put(child);
//        }
//        
//        return childs;
//	}
	
	public JSONArray buildLibraryTree(Collection<Component> library) {
    	
        JSONArray rootArray = new JSONArray();

        JSONObject jsonParts = new JSONObject();
        jsonParts.put("title", "Parts");
        jsonParts.put("isFolder", true);
        rootArray.put(jsonParts);

        JSONObject jsonDevices = new JSONObject();
        jsonDevices.put("title", "Devices");
        jsonDevices.put("isFolder", true);
        rootArray.put(jsonDevices);

		if(null != library && !library.isEmpty()) {
			Map<String, Set<Part>> parts = new HashMap<String, Set<Part>>();
	    	Set<Device> devices = new HashSet<Device>();
	    	
			/*
	    	 * let's iterate over the library
	    	 */
	    	for(Component c : library) {
	    		if(c instanceof Device) {
	    			devices.add((Device)c);
	    		} else if(c instanceof Part) {
	    			if(!parts.containsKey(((Part) c).getType().getName())) {
	    				parts.put(((Part) c).getType().getName(), new HashSet<Part>());
	    			}
	    			
	    			if(!parts.get(((Part) c).getType().getName()).contains(c)) {
	    				parts.get(((Part) c).getType().getName()).add((Part)c);
	    			}
	    		}
	    	}
		
	        /*
	         * process the parts and part types
	         */
	        if(null != parts && !parts.isEmpty()) {
	        	JSONArray jsonPartTypes = new JSONArray();
	        	for(String pt : parts.keySet()) {
	        		JSONObject jsonPartType = new JSONObject();
	        		jsonPartType.put("title", pt);
	        		jsonPartType.put("isFolder", true);
	        		
	        		/*
	        		 * iterate over the part type's parts
	        		 */
	        		JSONArray childs = new JSONArray();
	        		for(Part p : parts.get(pt)) {
	        			 
	        			JSONObject jsonPart = new JSONObject();        			
	        			jsonPart.put("title", p.getName());
	        			
	        			/*
	        			 * iterate over the part's properties and property values
	        			 */
	        			if(null != p.getPropertyValues() && !p.getPropertyValues().isEmpty()) {
	        				jsonPart.put("isFolder", true);
	        				
	        				JSONArray jsonProps = new JSONArray();
	        				for(String prop : p.getPropertyValues().keySet()) {
	        					JSONObject jsonPropVal = new JSONObject();
	        					jsonPropVal.put("title", prop+": "+p.getPropertyValue(prop));
	        					jsonProps.put(jsonPropVal);
	        				}
	        				
	        				jsonPart.put("children", jsonProps);
	        			}
	        			
	        			/*
	        			 * iterate over the part's properties
	        			 */
	        			
	        			childs.put(jsonPart);
	        		}
	        		
	        		/*
	        		 * and add the parts to the part type's node
	        		 */
	        		jsonPartType.put("children", childs);   
	        		jsonPartTypes.put(jsonPartType);
	        	}
	        	
	        	jsonParts.put("children", jsonPartTypes);
	        }
	
	
	        /*
	         * process the devices
	         */
	        if(null != devices && !devices.isEmpty()) {
	            JSONArray array = new JSONArray();
	            for(Device d : devices) {
	            	JSONObject obj = new JSONObject();
	            	obj.put("title", d.getName());
	            	obj.put("isFolder", true);            	
	            	array.put(obj);
	            }
	            jsonDevices.put("children", array);
	        }
		}

        return rootArray;
	}
	
	/**
	 * The private getIcon method returns an icon 
	 * corresponding to the filename's suffix.
	 * all icons are located in the images/icons directory.
	 * 
	 * @param filename ... the filename of that the icon should be figured out
	 * 
	 * @return the name of the icon
	 */
	private String getIcon(String filename) {
		if(filename.endsWith(".sbol") ||
				filename.endsWith(".xml")) {
			// SBOL 
			return "sbol.png";
		} else if(filename.endsWith(".eug")) {
			// Eugene
			return "eugene.jpg";
		} else if(filename.endsWith(".h")) {
			// Header files
			return "headerfile.png";
		} else if(filename.endsWith(".gb")) {
			// Genbank
			return "genbank.png";
		}
		
		// unknown extension of the filename, 
		// i.e. no icon.
		return null;
	} 
	
	
}
