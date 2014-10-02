package org.cidarlab.eugenelab.servlet;

import java.awt.image.RenderedImage;
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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
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
import org.cidarlab.eugene.Eugene;
import org.cidarlab.eugene.data.pigeon.Pigeonizer;
import org.cidarlab.eugene.dom.Component;
import org.cidarlab.eugene.dom.Device;
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
	
	private Pigeonizer pigeon;
	private String IMAGE_DIRECTORY;
	private static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("EugenLabServlet");
	private static final String USER_HOMES = "home";
	

	@Override
    public void init(ServletConfig config)
            throws ServletException {

        super.init(config);

        this.factory = null;
        
        // initialize a Pigeonizer instance
        this.pigeon = new Pigeonizer();

        this.IMAGE_DIRECTORY = config.getInitParameter("IMAGE_DIRECTORY");
        //this.clotho = ClothoFactory.getAPI("ws://localhost:8080/websocket");
        
	    // set a system property such that Simple Logger will include timestamp
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        // set a system property such that Simple Logger will include timestamp in the given format
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "dd-MM-yy HH:mm:ss");

        // set minimum log level for SLF4J Simple Logger at warn
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        
        LOGGER.warn("[EugeneLabServlet] loaded!");	    
    }

    @Override
    public void destroy() {
    	// TODO: persist all running instances of Sparrow 
    	
        LOGGER.warn("[EugeneLabServlet] destroying eugene servlet...");	    
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

        // retrieve the username from the session information
        String username = this.getUsername(request.getCookies());
        
        LOGGER.warn("[processGetRequest] -> " + command + ", " + username);
        
        try {
            if ("getFileList".equalsIgnoreCase(command)) {
            	// OK
                out.write(this.getFiles(username));

            } else if ("read".equalsIgnoreCase(command)) {

            } else if ("getLibrary".equalsIgnoreCase(command)) {
            	
            	out.write(this.buildLibraryTree(username));
            	
            } else if ("loadFile".equalsIgnoreCase(command)) {
                response.setContentType("text/html;charset=UTF-8");
                String fileName = request.getParameter("fileName");
                String toReturn = loadFile(username, fileName);
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

    		// retrieve the username from the session information
            String username = this.getUsername(request.getCookies());

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
            	String uploadFilePath = Paths.get(this.getServletContext().getRealPath(""), USER_HOMES, username).toString();
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
                            file = new File(uploadFilePath + "/" + fileName.substring(fileName.lastIndexOf("\\")));
                        } else {
                            file = new File(uploadFilePath + "/" + fileName.substring(fileName.lastIndexOf("\\") + 1));
                        }
                        
                        item.write(file);
                        toLoad.add(file);
                    }
                }
                
	        } else {
	        	String command = request.getParameter("command");

	        	if("createFile".equalsIgnoreCase(command)) {
	        		this.createFile(username, request.getParameter("filename"));
	        	} else if("saveFile".equalsIgnoreCase(command)) {
	        		this.saveFile(username, request.getParameter("filename"), request.getParameter("content"));
	        	} else if("deleteFile".equalsIgnoreCase(command)) {
	        		this.deleteFile(username, request.getParameter("filename"));
	        	} else if("execute".equalsIgnoreCase(command)) {
	        		jsonResponse = this.executeEugene(request.getParameter("script"));
	        	} else {
	        		throw new EugeneException("Invalid request!");
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

    // Returns a JSON Array (represented as String) 
    // with the name of a file/directory and if it is a file
    // {"title": name, "isFolder": true/false}
    private String getFiles(String username) {
    	
        String home = Paths.get(
        		this.getServletContext().getRealPath(""), 
        		USER_HOMES, 
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
        
        // Lazy loading
		if(null == this.treeBuilder) {
			this.treeBuilder = new TreeBuilder();
		}

		return this.treeBuilder.buildFileTree(home).toString();
    }
    
    private String buildLibraryTree(String username) 
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

    private String loadFile(String username, String fileName) 
    		throws Exception {
       	return new String(Files.readAllBytes(
       						Paths.get(this.getServletContext().getRealPath(""), USER_HOMES, username, fileName)));
    }
    
    /**
     * 
     * @param folder
     * @param filename
     * @throws IOException
     */
    private void createFile(String username, String filename) 
    		throws IOException {
    	
    	Files.createFile(
    			Paths.get(
    					this.getServletContext().getRealPath(""), 
    					USER_HOMES, 
    					username, 
    					filename));
    	
    }

    /**
     * 
     * @param folder
     * @param filename
     * @throws IOException
     */
    private void deleteFile(String username, String filename) 
    		throws IOException {
    	
    	Files.deleteIfExists(
    			Paths.get(
    					this.getServletContext().getRealPath(""), 
    					USER_HOMES, 
    					username, 
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
    		
    		/*
    		 * process the collection
    		 */
    		EugeneCollection col = new EugeneCollection(UUID.randomUUID().toString());
    		col.getElements().addAll(components);

    		/*
    		 * visualize the components using Pigeon
    		 * (i.e. SBOL Visual compliant symbols)
    		 */
    		URI pigeon = this.pigeonize(col);
    		
    		jsonResponse.put("pigeon-uri", pigeon);
    		
    	} catch(Exception e) {
    		throw new EugeneException(e.toString());
    	}
    	
    	return jsonResponse;
    }
    
    /*
     * PIGEON
     */
    private URI pigeonize(EugeneCollection col) 
    		throws EugeneException {
		
    	try {
			
    		// visualize every single device of the collection
    		List<URI> uris = new ArrayList<URI>();
			for(NamedElement ne : col.getElements()) {
				if(ne instanceof Device) {
					uris.add(
						this.pigeon.pigeonizeSingle((Device)ne, null));
				}
			}
			
			// do some file/directory management
			// arghhh
			String pictureName = UUID.randomUUID() + ".png";
			String imgFilename = "." + this.IMAGE_DIRECTORY + "/" + pictureName;			
			File imgFile = new File(imgFilename);
			if(!imgFile.getParentFile().exists()) {
				imgFile.getParentFile().mkdir();
				imgFile.getParentFile().mkdirs();
			}
			
			// merge all images (created above) 
			// into a single big image
			RenderedImage img = this.pigeon.toMergedImage(uris);
			
			// serialize the image to the filesystem
			this.writeToFile(img, imgFilename);

			// if everything went fine so far, 
			// then we return the relative URL of
			// the resulting image
			return URI.create("/tmp/" + pictureName);
			
    	} catch(Exception ee) {
    		// something went wrong, i.e.
    		// throw an exception
    		throw new EugeneException(ee.getMessage());
    	}
		
    }
    
    public void writeToFile(RenderedImage buff, String savePath) 
    		throws EugeneException {

    	try {
//            Logger.warn("got image : " + buff.toString());
            Iterator iter = ImageIO.getImageWritersByFormatName("png");
            ImageWriter writer = (ImageWriter)iter.next();
            ImageWriteParam iwp = writer.getDefaultWriteParam();

            File file = new File(savePath);
            FileImageOutputStream output = new FileImageOutputStream(file);
            writer.setOutput(output);
            IIOImage image = new IIOImage(buff, null, null);
            writer.write(null, image, iwp);
            writer.dispose();

        } catch (Exception e) {
            throw new EugeneException(e.getMessage());
        }
    }

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

    private String getFileExtension(String username, String localExtension, boolean isFile) {
        //String extension = this.getServletContext().getRealPath("/") + "/data/" + getCurrentUser() + "/" + localExtension;
        String extension = Paths.get(this.getServletContext().getRealPath(""), USER_HOMES, username, localExtension).toString();
        if (!isFile) {
            extension += "/";
        }
        return extension;
    }
    
    
    
    /*------------------------------
     * Session Management methods
     * 
     *  not sure yet if we should introduce a separate 
     *  class for this?!
     *-----------------------------*/

    // the DEFAULT_FREE_USER denotes the username 
    // if the current EugeneLab client hits the 
    // "Try it for free!" button
    private static final String DEFAULT_FREE_USER = "no_name_user";
    
    private String getUsername(Cookie[] cookies) {
    	
        String username = this.getDefaultUser();
    	for(Cookie c : cookies) {
    		if("user".equalsIgnoreCase(c.getName())) {
    			return c.getValue();
    		}
    	}

    	return username;
    }
    
    
    private String getDefaultUser() {
        return DEFAULT_FREE_USER;
    }


}
