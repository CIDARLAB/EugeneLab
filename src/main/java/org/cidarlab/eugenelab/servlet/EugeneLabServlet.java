package org.cidarlab.eugenelab.servlet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
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
import org.cidarlab.eugene.dom.Component;
import org.cidarlab.eugene.dom.imp.container.EugeneCollection;
import org.cidarlab.eugene.dom.imp.container.EugeneReturnCollection;
import org.cidarlab.eugene.exception.EugeneException;
import org.cidarlab.web.AuthenticationConstants;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * The EugeneLabServlet represents the Back-End of the EugeneLab Web Application.
 * It processes HTTP GET and POST requests. The data exchange is based on JSON 
 * notation.
 * 
 * @author Ernst Oberortner
 */
public class EugeneLabServlet 
		extends HttpServlet {

	private static final long serialVersionUID = -5373818164273289666L;

	private DiskFileItemFactory factory;
	private TreeBuilder treeBuilder;
	
	private static String USER_HOMES_DIRECTORY;
	public static String TMP_DIRECTORY;
	
	private static org.slf4j.Logger LOGGER = 
			org.slf4j.LoggerFactory.getLogger("EugenLabServlet");
	private static final int MAX_VISUAL_COMPONENTS = 10;
	
	/*
	 * In the EugeneLabServlet, every sessions gets an instance
	 * to a EugeneAdapter. Therefore, the eugeneInstances hash map 
	 * holds for all existing sessions a reference to the session's
	 * EugeneAdapter instance.
	 * 
	 * key ... username
	 */
	private Map<String, EugeneAdapter> eugeneInstances;
	
	@Override
    public void init(ServletConfig config)
            throws ServletException {

        super.init(config);

        this.factory = null;
        
        /*
         * read the temporary directory information from the 
         * web.xml configuration file
         * 
         * in this directory we store all generated SBOL and Pigeon 
         * images (for the time being (and maybe until the end of the computer area, i.e. forever))
         */
        TMP_DIRECTORY = config.getInitParameter("TMP_DIRECTORY");
		File tmp = new File(
					this.getServletContext().getRealPath("") + "/" +
					TMP_DIRECTORY);
		if(!tmp.exists()) {
			tmp.mkdirs();
		}

		// USER_HOMES directory
        USER_HOMES_DIRECTORY = config.getInitParameter("USER_HOMES_DIRECTORY");
		File home = new File(USER_HOMES_DIRECTORY);
		if(!home.exists()) {
			home.mkdirs();
		}

		/*
		 * we also initialize the hashmap of eugene instances
		 */
		this.eugeneInstances = new HashMap<String, EugeneAdapter>();
		
        /*
         * initialize Clotho connection here!
         */
        //this.clotho = ClothoFactory.getAPI("ws://localhost:8080/websocket");
        
        /*
         * initialize the logger
         */
	    // set a system property such that Simple Logger will include timestamp
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        // set a system property such that Simple Logger will include timestamp in the given format
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "dd-MM-yy HH:mm:ss");
        // set minimum log level for SLF4J Simple Logger at warn
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        
        LOGGER.warn("[EugeneLabServlet] loaded!");	          
    }
	
	public String getUserHomesDirectory() {
		return Paths.get(
				//this.getServletContext().getRealPath(""),
				USER_HOMES_DIRECTORY).toString();
	}
	
	public String getTmpImageDirectory() {
		return Paths.get(
				this.getServletContext().getRealPath(""),
				TMP_DIRECTORY).toString();
	}
	
    @Override
    public void destroy() {
    	
    	if(null != this.eugeneInstances) {
    		for(String session : this.eugeneInstances.keySet()) {
    			this.eugeneInstances.get(session).persistAndCleanUp();
    		}
    	}

    	LOGGER.warn("[EugeneLabServlet] destroyed!");	    
    }

    /**
     * Handles HTTP <code>GET</code> requests
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	// we check if the request is coming from an authenticated user
    	// i.e. a user who contacted the AuthenticationServlet first
    	if(hasUserCookie(request)) {

    		// if the request is coming from an authenticated user, 
    		// then we set an authentication flag in the request and response
    		// only if it is not set
    		if(!isAuthenticated(request)) {
    			authenticate(request, response);
    		}
    	}
    	
    	// then, we process the request
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

        // retrieve the username from the session information
        String username = this.getUsername(request);
        
        try {
            if ("getFileList".equalsIgnoreCase(command)) {
            	
            	// we build a tree that contains all files (as nodes) 
            	// of the user's HOME directory.
            	out.write(this.buildFileTree(username, isAuthenticated(request)));

            } else if ("read".equalsIgnoreCase(command)) {

            } else if ("getLibrary".equalsIgnoreCase(command)) {
            	
            	// we build a tree that contains all files (as nodes) 
            	// of the current user's library.
            	out.write(this.buildLibraryTree(username, request.getSession().getId()));
            	
            } else if ("loadFile".equalsIgnoreCase(command)) {
            	
            	// we read the requested files content and
            	// return it's content as string encapsulated 
            	// in a JSON object
                response.setContentType("text/html;charset=UTF-8");
                String fileName = request.getParameter("fileName");
                String toReturn = loadFile(username, fileName);
                
                out.write(toReturn);
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
     * Handles HTTP <code>POST</code> requests.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException {

    	// we check if the request is coming from an authenticated user
    	// i.e. a user who contacted the AuthenticationServlet first
    	if(this.hasUserCookie(request)) {
    		
    		// if the request is coming from an authenticated user, 
    		// then we set an authentication flag in the request and response
    		// only if it is not set
    		if(!this.isAuthenticated(request)) {
    			this.authenticate(request, response);
    		}
    	}
    	
    	// then, we process the request
		this.processPostRequest(request, response);
    }
    
    /**
     * The isUserAuthenticated method evaluates if the current request 
     * is coming from a user who is authenticated. Therefore it iterates 
     * over all cookies of the request and checks if the USER cookie
     * --- which is set by the AuthenticationServlet --- exists. 
     * 
     * @param request
     * 
     * @return ... true if the user is logged in
     */
    private boolean hasUserCookie(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
            	if(AuthenticationConstants.USER_COOKIE.equals(cookies[i].getName())) {
            		
            		// check if the value is non-empty
            		if(null != cookies[i].getValue() && !cookies[i].getValue().isEmpty()) {
            			return true;
            		}
            	}
            }
        }
        
        return false;
    }
    
    private void authenticate(HttpServletRequest request, HttpServletResponse response) {
    	request.getSession().setAttribute(
    			EugeneLabConstants.APPLICATION_NAME, EugeneLabConstants.AUTHENTICATED);
    	
    	response.addCookie(
    			new Cookie(EugeneLabConstants.APPLICATION_NAME, EugeneLabConstants.AUTHENTICATED));
    }
    

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "This is the EugeneLabServlet. The Back-End of the EugeneLab Web Application.";
    }

    protected void processPostRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

    	JSONObject jsonResponse = new JSONObject();
    	
    	try {

    		if (ServletFileUpload.isMultipartContent(request)) {

    			this.uploadFile(request);
                response.sendRedirect("eugenelab.html");
                
	        } else {
	    		// retrieve the username from the session information
	            String username = this.getUsername(request);
	            // get the command
	        	String command = request.getParameter("command");

	        	/*---------------------------------
	        	 *  FILE HANDLING requests
	        	 *--------------------------------*/
	        	if("createFile".equalsIgnoreCase(command)) {
	        		this.createFile(username, request.getParameter("filename"));
	        	} else if("saveFile".equalsIgnoreCase(command)) {
	        		this.saveFile(username, request.getParameter("filename"), request.getParameter("content"));
	        	} else if("deleteFile".equalsIgnoreCase(command)) {
	        		this.deleteFile(username, request.getParameter("filename"));
	        	
		        /*---------------------------------
		         *  EUGENE-related requests
		         *--------------------------------*/	        	
	        	} else if("execute".equalsIgnoreCase(command)) {
	        		jsonResponse = this.executeEugene(username, request.getParameter("script"), request.getSession());
	        	} else {
	        		throw new EugeneException("Invalid request!");
	        	}

	        }

    		jsonResponse.put("status", "good");
    		
    	} catch(Exception e) {
    		jsonResponse.put("status", "exception");
    		jsonResponse.put("result", e.toString());
    		
    		e.printStackTrace();
    		LOGGER.warn(e.toString());
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

    /**
     * The buildFileTree/2 method builds the dynatree JSON array
     * in order to display all library elements (i.e. parts and 
     * devices) on the Front-End.
     * 
     * @param username   ... the current username
     * @param sessionId  ... the user's session
     * 
     * @return   ... a String that represents the dynatree JSON array
     * 
     * @throws EugeneException
     */
    private String buildFileTree(String username, boolean isLoggedIn) {
    	
        String home = Paths.get(
        		//this.getServletContext().getRealPath(""), 
        		USER_HOMES_DIRECTORY, 
        		username).toString();
        
        // if the user does not have a "home"-directory,
        // then we create one
        // this could also be done at the SignUp stage 
        // in the AuthenticationServlet. At the time being,
        // it's unclear how the AuthenticationServlet will 
        // be used for the CIDAR Lab Web of Apps
        if(!new File(home).exists()) {
        	new File(home).mkdir();
        	new File(home).mkdir();
        }
        
        // Lazy loading of the TreeBuilder
		if(null == this.treeBuilder) {
			this.treeBuilder = new TreeBuilder();
		}

		JSONArray jArray = this.treeBuilder.buildFileTree(home, isLoggedIn);
		if(null != jArray) {
			return jArray.toString();
		} 
		return null;
    }
    
    /**
     * The buildLibraryTree/2 method builds the dynatree JSON array
     * in order to display all library elements (i.e. parts and 
     * devices) on the Front-End.
     * 
     * @param username   ... the current username
     * @param sessionId  ... the user's session
     * 
     * @return   ... a String that represents the dynatree JSON array
     * 
     * @throws EugeneException
     */
    private String buildLibraryTree(String username, String sessionId) 
    		throws EugeneException {
    	
		if(null == this.treeBuilder) {
			this.treeBuilder = new TreeBuilder();
		}
		
    	Collection<Component> library = null;
    	EugeneAdapter ea = this.getEugeneAdapter(username, sessionId);
        if(null != ea) {
        	try {
        		library = ea.getLibrary();
        	} catch(EugeneException ee) {
        		
        	}
        }
		
		JSONArray jArray = this.treeBuilder.buildLibraryTree(library);
		if(null != jArray) {
			return jArray.toString();
		} 
		
		return null;
    }

    
    /**
     * The getEugeneAdapter/2 method returns the EugeneAdapter object of 
     * the current user. Therefore, it checks the eugeneInstances hash map 
     * if the current user has a EugeneAdapter object already. If a 
     * EugeneAdapter object for the current user exist, then it returns it.
     * Otherwise, it creates a new EugeneAdapter object, stores it in the 
     * eugeneInstances hash map, and returns it.
     * 
     * @param username   ... the username of the current user
     * @param sessionId  ... the sessionID of the current user's session
     * 
     * @return  ... an instance of the EugeneAdapter class
     * 
     * @throws EugeneException
     */
    private EugeneAdapter getEugeneAdapter(String username, String sessionId) 
    		throws EugeneException {

    	EugeneAdapter ea = this.eugeneInstances.get(username);
    	
    	// if the current user has created a EugeneAdapter object already,
    	// then we return it
    	if(null != ea) {
    		return ea;
    	}
    	
    	// if no EugeneAdapter exists, then we create one
    	ea = new EugeneAdapter(username, 
    			sessionId, 
    			this.getUserHomesDirectory(), 
    			this.getServletContext().getRealPath(""));
    	
    	// store it in the eugeneInstances hash map
    	this.eugeneInstances.put(username, ea);
    	// and return it
    	return ea;
    }
    
    /**
     * The executeEugene/1 method executes the Eugene script 
     * that the user typed into the large textarea.
     * 
     * @param username  ... the current user logged in
     * @param script    ... the Eugene script
     * @param session   ... the HTTP session object
     * 
     * @return  a JSONObject containing all relevant display data
     *          for the EugeneLab Front-End
     *          
     * @throws EugeneException
     */
    private JSONObject executeEugene(String username, String script, HttpSession session) 
    		throws EugeneException {
    	
    	JSONObject jsonResponse = new JSONObject();
    	
    	try {
    		EugeneAdapter ea = this.getEugeneAdapter(username, session.getId());
    		
    		EugeneCollection eugeneReturn = ea.executeScript(script);

    		// visualize the outcome using SBOL visual compliant glyphs
    		// therefore, we use Pigeon
    		if(null != eugeneReturn && !eugeneReturn.getElements().isEmpty()) {
    			
	    		/*
	    		 * pigeonize the collection and 
	    		 * return the URI of the generated pigeon image
	    		 */
    			
    			System.out.println(((EugeneReturnCollection)eugeneReturn).getImages());
    			jsonResponse.put("pigeon-uri", ea.pigeonize(eugeneReturn, MAX_VISUAL_COMPONENTS));
    			
    			/*
    			 * SBOL XML/RDF serialization
    			 */
    			jsonResponse.put("sbol-xml-rdf", ea.SBOLize(eugeneReturn));
    		}
    		
    		jsonResponse.put("eugene-output", ea.getEugeneOutput());

    	} catch(Exception e) {
    		e.printStackTrace();
    		throw new EugeneException(e.toString());
    	}
    	
    	return jsonResponse;
    }
    
    /*------------------------------
     * SESSION MANAGEMENT 
     *-----------------------------*/

    private String getUsername(HttpServletRequest request) {
    	
        String username = this.getDefaultUser();
        
        // we first check if the user is authenticated
        if(this.isAuthenticated(request)) {
        	
        	// if the user is authenticated, then we retrieve 
        	// the username from the USER cookie
	        if(null != request.getCookies()) {
		    	for(Cookie c : request.getCookies()) {
		    		if(AuthenticationConstants.USER_COOKIE.equalsIgnoreCase(c.getName())) {
		    			return c.getValue();
		    		}
		    	}
	        }
        }

        // the user is not authenticated, i.e. DEFAULT USER
    	return username;
    }
    
    private boolean isAuthenticated(HttpServletRequest request) {
    	return EugeneLabConstants.AUTHENTICATED.equals(
    			request.getSession().getAttribute(EugeneLabConstants.APPLICATION_NAME));
    }
    
    // the DEFAULT_FREE_USER denotes the username 
    // if the current EugeneLab client hits the 
    // "Try it for free!" button
    private static final String DEFAULT_FREE_USER = "no_name_user";    
    private String getDefaultUser() {
        return DEFAULT_FREE_USER;
    }


    /*-------------------------------------------------
     * FILE HANDLING METHODS
     *-------------------------------------------------*/
    
    /**
     * The saveFile/3 method saves the provided fileContent into the 
     * specified file of the current user.
     * 
     * @param username   ... the username of the current user
     * @param fileName   ... the filename of the file to be overwritten
     * @param fileContent  ... the new content of the file
     * 
     * @throws EugeneException
     */
    private void saveFile(String username, String fileName, String fileContent) 
    		throws EugeneException {
        String currentFileExtension = getFileExtension(username, "/" + fileName, true);
        File file = new File(currentFileExtension);
        try {
	        BufferedWriter bw = new BufferedWriter(new FileWriter(file.getAbsoluteFile()));
	        bw.write(fileContent);
	        bw.flush();
	        bw.close();
        } catch(Exception e) {
        	throw new EugeneException(e.getLocalizedMessage());
        }
    }

    /**
     * The loadFile/2 method reads the content of the specified 
     * fileName of the current user's HOME directory. It returns 
     * the file content as a String.
     * 
     * @param username  ... the current user
     * @param fileName  ... the name of the file to be loaded
     * @return ... the content of the file encapsulated in a String object
     * @throws Exception
     */
    private String loadFile(String username, String fileName) 
    		throws Exception {
    	
       	return new String(Files.readAllBytes(
       						Paths.get(
       								//this.getServletContext().getRealPath(""), 
       								USER_HOMES_DIRECTORY, 
       								username, 
       								fileName)));
    }
    
    /**
     * The uploadFile/1 method serves for processing file upload
     * requests. It stores the uploaded file in the 
     * current user's HOME directory
     * 
     * @param request  ... the request that contains the file to upload
     * 
     * @throws Exception
     */
    private void uploadFile(HttpServletRequest request) 
    	throws Exception {
    	
    	// get the current user
        String username = this.getUsername(request);
    	
    	/*
    	 * we do the file upload using 
    	 * Java7's File IO libraries. therefore, we 
    	 * need to initialize them first.
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
    	String uploadFilePath = Paths.get(
    			//this.getServletContext().getRealPath(""), 
    			USER_HOMES_DIRECTORY, 
    			username).toString();
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
                    LOGGER.warn("You forgot to choose a file.");
                }

                if (fileName.lastIndexOf("\\") >= 0) {
                    file = new File(uploadFilePath + "/" + 
                    		fileName.substring(fileName.lastIndexOf("\\")));
                } else {
                    file = new File(uploadFilePath + "/" + 
                    		fileName.substring(fileName.lastIndexOf("\\") + 1));
                }
                
                item.write(file);
                toLoad.add(file);
            }
        }
    }

    /**
     * The createFile/2 method serves for processing createFile 
     * request. It receives the current user's username, and 
     * the desired filename. It creates the new file in the 
     * user's HOME directory 
     * 
     * @param username  ... the current user's username
     * @param filename  ... the desired filename
     * 
     * @throws IOException
     */
    private void createFile(String username, String filename) 
    		throws IOException {
    	
    	Files.createFile(
    			Paths.get(
    					USER_HOMES_DIRECTORY, 
    					username, 
    					filename));
    }

    /**
     * The deleteFile/2 method serves for processing deleteFile 
     * requests. It receives the current user's username (in order 
     * to detect the user's HOME directory) and the filename 
     * of the file that should be deleted.
     * 
     * @param username  ... the current user
     * @param filename  ... the filename of the file to delete
     * 
     * @throws IOException
     */
    private void deleteFile(String username, String filename) 
    		throws IOException {
    	
    	Files.deleteIfExists(
    			Paths.get(
    					USER_HOMES_DIRECTORY, 
    					username, 
    					filename));
    	
    }

    private String getFileExtension(String username, String localExtension, boolean isFile) {
        String extension = Paths.get(
        		USER_HOMES_DIRECTORY, 
        		username, 
        		localExtension).toString();
        
        if (!isFile) {
            extension += "/";
        }
        
        return extension;
    }
}
