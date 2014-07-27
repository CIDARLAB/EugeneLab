package org.cidarlab.eugenelab.servlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

import org.cidarlab.eugene.dom.Component;
import org.cidarlab.eugene.dom.Device;
import org.cidarlab.eugene.dom.Part;
import org.cidarlab.eugene.dom.PartType;
import org.cidarlab.eugene.dom.Property;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TreeBuilder {

	public TreeBuilder() {}
	
	public JSONArray buildFileTree(String home) {
        File rootFolder = new File(home);
        List<File> queue = new ArrayList<File>();
        List<JSONArray> folders = new ArrayList<JSONArray>();
        List<Integer> folderSizes = new ArrayList<Integer>();
        File[] rootFiles = rootFolder.listFiles();
        
        JSONArray rootArray = new JSONArray();        
        if (null == rootFiles) {
            return rootArray;
        }
        for (int i = 0; i < rootFiles.length; i++) {
            queue.add(rootFiles[i]);
        }

        JSONArray currentArray = rootArray;
        boolean switchFolder = false;
        int currentFolderSize = rootFolder.listFiles().length;
        int counter = 1;
        while (!queue.isEmpty()) {
            try {
                File currentFile = queue.get(0);
                queue.remove(0);
//                System.out.println(switchFolder + " " + counter + " | " + currentFolderSize + " " + currentFile.getName());

                if (!switchFolder) {
                    switchFolder = false;
                }
                JSONObject toPut = new JSONObject();
                toPut.put("title", currentFile.getName());
                currentArray.put(toPut);
                if (currentFile.isDirectory()) {
                    switchFolder = true;
                    toPut.put("children", new JSONArray());
                    toPut.put("isFolder", true);
                    if (currentFile.listFiles().length > 0) {
                        folderSizes.add(currentFile.listFiles().length);
                        folders.add(toPut.getJSONArray("children"));
                    }
                    File[] subFiles = currentFile.listFiles();
                    for (int i = 0; i < subFiles.length; i++) {
                        queue.add(subFiles[i]);
                    }
                }
                if (switchFolder && counter == currentFolderSize) {
//                    System.out.println("switching");
//                    System.out.println(folderSizes);
                    currentArray = folders.get(0);
                    currentFolderSize = folderSizes.get(0);
                    folderSizes.remove(0);
                    folders.remove(0);
                    counter = 0;
                    switchFolder = false;
                }
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
            counter++;
        }
        
        return rootArray;
	}
	
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
	    		} else if(c instanceof PartType) {
	    			if(!parts.containsKey(c.getName())) {
	    				parts.put(c.getName(), new HashSet<Part>());
	    			}
	    		} else if(c instanceof Part) {
	    			if(!parts.containsKey(((Part) c).getPartType().getName())) {
	    				parts.put(((Part) c).getPartType().getName(), new HashSet<Part>());
	    			}
	    			
	    			if(!parts.get(((Part) c).getPartType().getName()).contains(c)) {
	    				parts.get(((Part) c).getPartType().getName()).add((Part)c);
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
}
