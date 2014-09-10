package org.cidarlab.eugenelab.servlet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.cidarlab.eugene.Eugene;
import org.cidarlab.eugene.data.pigeon.Pigeonizer;
import org.cidarlab.eugene.dom.Component;
import org.cidarlab.eugene.dom.NamedElement;
import org.cidarlab.eugene.dom.imp.container.EugeneCollection;
import org.cidarlab.eugene.exception.EugeneException;
import org.json.JSONObject;

/**
 *
 * @author Ernst Oberortner
 */
public class EugeneLabServlet 
		extends HttpServlet {

	private static final long serialVersionUID = -5373818164273289666L;

	private DiskFileItemFactory factory;
	private Eugene eugene;
	private TreeBuilder treeBuilder;
	
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

    	/*
    	 * do some session handling
    	 */
    	HttpSession session = request.getSession();
    	if(null == this.eugene) {
    		try {
    			this.eugene = new Eugene(session.getId());
    		} catch(EugeneException ee) {
    			throw new ServletException(ee.toString());
    		}
    	}
    	
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

        //we're returning a JSON object
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String command = request.getParameter("command");
        try {
            if ("getFileList".equalsIgnoreCase(command)) {
            	// OK
                out.write(this.getFiles());

            } else if ("read".equalsIgnoreCase(command)) {

            } else if ("getLibrary".equalsIgnoreCase(command)) {
            	
            	out.write(this.buildLibraryTree());
            	
            } else if ("loadFile".equalsIgnoreCase(command)) {
                response.setContentType("text/html;charset=UTF-8");
                String fileName = request.getParameter("fileName");
                String toReturn = loadFile(fileName);
                out.write(toReturn);
            } else if (command.equals("test")) {
                out.write("{\"response\":\"test response\"}");
            }
        } catch (Exception e) {
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws IOException {

    	/*
    	 * do some session handling
    	 */
    	HttpSession session = request.getSession();
    	if(null == this.eugene) {
    		try {
    			this.eugene = new Eugene(session.getId());
    		} catch(EugeneException ee) {
    			throw new IOException(ee.toString());
    		}
    	}

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
                
	        } else {
	        	/*
	        	 * read the command of the request
	        	 */
	        	String command = request.getParameter("command");
	        	if("login".equalsIgnoreCase(command)) {
	        		
	        	} else if("createFile".equalsIgnoreCase(command)) {
	        		this.createFile(request.getParameter("folder"), request.getParameter("filename"));
	        	} else if("deleteFile".equalsIgnoreCase(command)) {
	        		this.deleteFile(request.getParameter("folder"), request.getParameter("filename"));
	        	} else if("execute".equalsIgnoreCase(command)) {
	        		jsonResponse = this.executeEugene(request.getParameter("script"));
	        	}

	        }

    		jsonResponse.put("status", "good");
    		
    	} catch(Exception e) {
    		jsonResponse.put("status", "exception");
    		jsonResponse.put("result", e.toString());
    	}

        /*
         * RESPONSE
         */
        response.setContentType("application/json");
        
        PrintWriter writer = response.getWriter();
        writer.write(jsonResponse.toString());
        writer.flush();
        writer.close();

    }

    private String getCurrentUser() {
        return "testuser";
    }


    // Returns a JSON Array (represented as String) 
    // with the name of a file/directory and if it is a file
    // {"title": name, "isFolder": true/false}
    private String getFiles() {
        String home = Paths.get(
        		this.getServletContext().getRealPath(""), 
        		"data", 
        		getCurrentUser()).toString();
        
        // Lazy loading
		if(null == this.treeBuilder) {
			this.treeBuilder = new TreeBuilder();
		}

		return this.treeBuilder.buildFileTree(home).toString();
    }
    
    private String buildLibraryTree() 
    		throws EugeneException {
    	
		if(null == this.treeBuilder) {
			this.treeBuilder = new TreeBuilder();
		}
		
    	Collection<Component> library = null;
        if(null != this.eugene) {
        	try {
        		library = this.eugene.getLibrary();
        	} catch(EugeneException ee) {
        		
        	}
        }
        
   		return this.treeBuilder.buildLibraryTree(library).toString();
    }

    private String loadFile(String fileName) 
    		throws Exception {
       	return new String(Files.readAllBytes(
       						Paths.get(this.getServletContext().getRealPath(""), "data", getCurrentUser(), fileName)));
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
    
    
    /**
     * The executeEugene/1 method executes the Eugene script 
     * that the user typed into the large textarea.
     * 
     * @param script
     * @return a JSONObject containing the for the web-interface relevant information
     * @throws EugeneException
     */
    private JSONObject executeEugene(String script) 
    		throws EugeneException {
    	
    	if(null == this.eugene) {
    		this.eugene = new Eugene();
    	}
    	
    	JSONObject jsonResponse = new JSONObject();
    	
    	try {
    		Collection<Component> components = this.eugene.executeScript(script);
    		
    		System.out.println("components:" + components);
    		
    		/*
    		 * process the collection
    		 */
    		EugeneCollection col = new EugeneCollection(UUID.randomUUID().toString());
    		col.getElements().addAll(components);
    		URI pigeon = new Pigeonizer().pigeonize(col);
    		System.out.println(pigeon);
    		jsonResponse.put("pigeon-uri", pigeon);
    		
    	} catch(Exception e) {
    		throw new EugeneException(e.toString());
    	}
    	
    	return jsonResponse;
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
