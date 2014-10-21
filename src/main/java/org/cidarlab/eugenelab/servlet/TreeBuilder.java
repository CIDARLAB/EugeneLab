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
import org.json.JSONArray;
import org.json.JSONObject;

public class TreeBuilder {

	public TreeBuilder() {}
	
	public JSONArray buildFileTree(String home) {
		
        File rootFolder = new File(home);
        File[] rootFiles = rootFolder.listFiles();
        
        JSONArray rootArray = new JSONArray();        
        if (null == rootFiles) {
            return rootArray;
        }
        for (int i = 0; i < rootFiles.length; i++) {
        	
            JSONObject toPut = new JSONObject();
            toPut.put("title", rootFiles[i].getName());
            if (rootFiles[i].isDirectory()) {
            	
            	/*
            	 * iterate over all children
            	 */
            	JSONArray childs = new JSONArray();
	            File[] subFiles = rootFiles[i].listFiles();
	            for (int j = 0; j < subFiles.length; j++) {
	            	JSONObject child = new JSONObject();
	            	child.put("title", subFiles[j].getName());
	            	
	                child.put("icon", this.getIcon(subFiles[j].getName().toLowerCase()));
	            	  
	            	childs.put(child);
	            }

            	toPut.put("isFolder", true);
            	toPut.put("children", childs);
            	
            } else {
                toPut.put("icon", this.getIcon(rootFiles[i].getName().toLowerCase()));
            }

            rootArray.put(toPut);
        }

        return rootArray;
	}
	
	private String getIcon(String filename) {
		if(filename.endsWith(".sbol") ||
				filename.endsWith(".xml")) {
			return "sbol.ico";
		} else if(filename.endsWith(".eug")) {
			return "eugene.jpg";
		} else if(filename.endsWith(".h")) {
			return "headerfile.png";
		} else if(filename.endsWith(".gb")) {
			return "genbank.png";
		}
		return null;
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
}
