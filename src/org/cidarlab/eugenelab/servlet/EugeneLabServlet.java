package org.cidarlab.eugenelab.servlet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Ernst Oberortner
 */
public class EugeneLabServlet 
		extends HttpServlet {

	private static final long serialVersionUID = -5373818164273289666L;

	
	private DiskFileItemFactory factory;

	@Override
    public void init()
            throws ServletException {

        super.init();

        this.factory = null;
        
        //this.clotho = ClothoFactory.getAPI("ws://localhost:8080/websocket");
    }

    @Override
    public void destroy() {
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processGetRequest(request, response);
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processGetRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	System.out.println("[EugeneLabServlet.processGet] -> "+request.getParameter("command"));
    	
        //I'm returning a JSON object and not a string
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String command = request.getParameter("command");

        try {
            if ("getFileList".equalsIgnoreCase(command)) {
            	// OK
                out.write(this.getFiles());

            } else if (command.equalsIgnoreCase("read")) {

            } else if ("loadFile".equalsIgnoreCase(command)) {
                response.setContentType("text/html;charset=UTF-8");
                String fileName = request.getParameter("fileName");
                String toReturn = loadFile(fileName);
                out.write(toReturn);
            } else if (command.equals("test")) {
                out.write("{\"response\":\"test response\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            String exceptionAsString = stringWriter.toString().replaceAll("[\r\n\t]+", "<br/>");
            out.println("{\"result\":\"" + exceptionAsString + "\",\"status\":\"exception\"}");
        } finally {
            out.flush();
            out.close();
        }
    }


    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	processPostRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }
    // </editor-fold>

    protected void processPostRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

    	JSONObject jsonResponse = new JSONObject();
    	
    	try {
    		
    		if (ServletFileUpload.isMultipartContent(request)) {

	        	/*
	        	 * FILE UPLOAD
	        	 */
            	if(null == this.factory) {
            		this.factory = new DiskFileItemFactory();
            		ServletContext servletContext = this.getServletConfig().getServletContext();
            		File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
            		factory.setRepository(repository);
            	}
            	
            	ServletFileUpload uploadHandler = new ServletFileUpload(this.factory);
                
            	/*
            	 * check if the user's directory exists already
            	 */
            	String uploadFilePath = Paths.get(this.getServletContext().getRealPath(""), "data", getCurrentUser()).toString();
                File userDir = new File(uploadFilePath);
                if(!userDir.exists()) {
                	userDir.mkdirs();
                }
                
                /*
                 * store the file in the user's directory
                 */
                List<FileItem> items = uploadHandler.parseRequest(request);
                List<File> toLoad = new ArrayList<File>();
                for (FileItem item : items) {
                    File file;
                    if (!item.isFormField()) {
                        String fileName = item.getName();
                        
                        if (fileName.equals("")) {
                            System.out.println("You forgot to choose a file.");
                        }

                        if (fileName.lastIndexOf("\\") >= 0) {
                            file = new File(uploadFilePath + "/" + fileName.substring(fileName.lastIndexOf("\\")));
                        } else {
                            file = new File(uploadFilePath + "/" + fileName.substring(fileName.lastIndexOf("\\") + 1));
                        }
                        
                        item.write(file);
                        toLoad.add(file);
                    }
                }
                
                jsonResponse.put("status", "good");
	        } else {
	        	/*
	        	 * read the command of the request
	        	 */
	        	String command = request.getParameter("command");
	        	if("createFile".equalsIgnoreCase(command)) {
	        		this.createFile(request.getParameter("folder"), request.getParameter("filename"));
	        	} else if("deleteFile".equalsIgnoreCase(command)) {
	        		this.deleteFile(request.getParameter("folder"), request.getParameter("filename"));
	        	}

	        	
	        	System.out.println("[doPost] -> "+command);
	        }
    	} catch(Exception e) {
    		jsonResponse.put("status", "exception");
    		jsonResponse.put("reason", e.toString());
    	}

        /*
         * RESPONSE
         */
        PrintWriter writer = response.getWriter();
        response.setContentType("application/json");
        response.sendRedirect("eugenelab.html");
        writer.write(jsonResponse.toString());
        writer.flush();
        writer.close();

    }

    private String getCurrentUser() {
        return "testuser";
    }


    // Returns a JSON Array with the name of a file/directory and if it is a file
    // {"name": name, "isFile", isFile}
    private String getFiles() {
        //String currentFolderExtension = this.getServletContext().getRealPath("/") + "/data/" + getCurrentUser() + "/";
        String currentFolderExtension = Paths.get(this.getServletContext().getRealPath(""), "data", getCurrentUser()).toString();

        File rootFolder = new File(currentFolderExtension);
        List<File> queue = new ArrayList<File>();
        List<JSONArray> folders = new ArrayList<JSONArray>();
        List<Integer> folderSizes = new ArrayList<Integer>();
        File[] rootFiles = rootFolder.listFiles();
        if (null == rootFiles) {
            return "";
        }
        for (int i = 0; i < rootFiles.length; i++) {
            queue.add(rootFiles[i]);
        }

        JSONArray rootArray = new JSONArray();
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
        return rootArray.toString();
    }

    private String loadFile(String fileName) {
        BufferedReader br = null;
        try {
            String currentFileExtension = getFileExtension(fileName, true);
            File file = new File(currentFileExtension);
            br = new BufferedReader(new FileReader(file.getAbsolutePath()));
            String toReturn = "";
            String line = br.readLine();
            while (line != null) {
                toReturn = toReturn + "\n\r" + line;
                line = br.readLine();
            }
            br.close();
            return toReturn;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "the bads";
        }
    }
    
    /**
     * 
     * @param folder
     * @param filename
     * @throws IOException
     */
    private void createFile(String folder, String filename) 
    		throws IOException {
    	
    	Files.createFile(
    			Paths.get(
    					this.getServletContext().getRealPath(""), 
    					"data", 
    					getCurrentUser(), 
    					folder, 
    					filename));
    	
    }

    /**
     * 
     * @param folder
     * @param filename
     * @throws IOException
     */
    private void deleteFile(String folder, String filename) 
    		throws IOException {
    	
    	Files.deleteIfExists(
    			Paths.get(
    					this.getServletContext().getRealPath(""), 
    					"data", 
    					getCurrentUser(), 
    					folder, 
    					filename));
    	
    }

    private void saveFile(String fileName, String fileContent) throws IOException {
        String currentFileExtension = getFileExtension("/" + fileName, true);
        File file = new File(currentFileExtension);
        BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
        bw.write(fileContent);
        bw.close();
    }

    private String getFileExtension(String localExtension, boolean isFile) {
        //String extension = this.getServletContext().getRealPath("/") + "/data/" + getCurrentUser() + "/" + localExtension;
        String extension = Paths.get(this.getServletContext().getRealPath(""), "data", getCurrentUser(), localExtension).toString();
        if (!isFile) {
            extension += "/";
        }
        return extension;
    }
}
