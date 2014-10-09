package org.cidarlab.eugenelab.servlet;

import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
import org.cidarlab.eugene.data.sbol.SBOLExporter;
import org.cidarlab.eugene.dom.Component;
import org.cidarlab.eugene.dom.Device;
import org.cidarlab.eugene.dom.NamedElement;
import org.cidarlab.eugene.dom.imp.container.EugeneCollection;
import org.cidarlab.eugene.exception.EugeneException;
import org.json.JSONObject;
import org.sbolstandard.core.SBOLDocument;
import org.sbolstandard.core.SBOLFactory;

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
	private String TMP_DIRECTORY;
	private static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("EugenLabServlet");
	private static final String USER_HOMES = "home";
	private static final int MAX_VISUAL_COMPONENTS = 10;
	
	/*
	 * a writer that is being used for writing
	 * all Eugene outputs
	 * mainly being used for print and println
	 */
	private BufferedWriter writer;
	private ByteArrayOutputStream baos;
	
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
        this.TMP_DIRECTORY = config.getInitParameter("TMP_DIRECTORY");
		File tmp = new File(this.TMP_DIRECTORY);
		if(!tmp.exists()) {
			tmp.mkdir();
			tmp.mkdirs();
		}
		
        /*
         * initialize Pigeon
         */
        this.pigeon = new Pigeonizer();

        /*
         * initialize the Clotho connection
         * WILL THIS COME SOON?
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
        
        /*
         * initialize the writer
         */
        try {        
        	this.baos = new ByteArrayOutputStream();
            this.writer = new BufferedWriter(
            		new OutputStreamWriter(baos), 
            		1024);
        } catch(Exception e) {
        	throw new ServletException(e.getLocalizedMessage());
        }

        LOGGER.warn("[EugeneLabServlet] loaded!");	          
    }
	
	private void initEugene(HttpSession session) 
		throws EugeneException {
		
		try {
			this.eugene = new Eugene(
					session.getId(), 
					this.writer);
		} catch(EugeneException ee) {
			throw new EugeneException(ee.toString());
		}
	}

    @Override
    public void destroy() {
    	// TODO: persist all running instances of Sparrow 
    	
    	this.writer = null;
    	this.pigeon = null;
    	this.eugene = null;
    	
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

    	/*
    	 * do some session handling
    	 */
    	HttpSession session = request.getSession();
    	if(null == this.eugene) {
    		try {
    			this.initEugene(session);
    		} catch(EugeneException ee) {
    			throw new ServletException(ee.getLocalizedMessage());
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
        
//        LOGGER.warn("[processGetRequest] -> " + command + ", " + username);
        
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

    	LOGGER.warn("[doPost] session: " + request.getSession());

    	/*
    	 * do some session handling
    	 */
    	HttpSession session = request.getSession();
    	if(null == this.eugene) {
    		try {
    			this.initEugene(session);
    		} catch(EugeneException ee) {
    			throw new ServletException(ee.getLocalizedMessage());
    		}
    	}

    	LOGGER.warn("[doPost] session: " + request.getSession());

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

    			this.uploadFile(request);
                response.sendRedirect("eugenelab.html");
                
	        } else {
	    		// retrieve the username from the session information
	            String username = this.getUsername(request.getCookies());
	            // get the command
	        	String command = request.getParameter("command");

	        	LOGGER.warn("[processPostRequest] username: " + username+", command: "+ command);
	        	
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
       						Paths.get(
       								this.getServletContext().getRealPath(""), 
       								USER_HOMES, 
       								username, 
       								fileName)));
    }
    
    /**
     * 
     * @param request
     * @throws Exception
     */
    private void uploadFile(HttpServletRequest request) 
    	throws Exception {
    	
    	// get the current user
        String username = this.getUsername(request.getCookies());
    	
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
    	String uploadFilePath = Paths.get(
    			this.getServletContext().getRealPath(""), 
    			USER_HOMES, 
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
    	
    	LOGGER.warn("[deleteFile] -> " + username+", "+filename);
    	
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
    	
    	JSONObject jsonResponse = new JSONObject();
    	
    	try {
    		
    		this.baos.reset();
    		
    		Collection<Component> components = this.eugene.executeScript(script);
    		
    		// visualize the outcome using SBOL visual compliant glyphs
    		// therefore, we use Pigeon
    		if(null != components && !components.isEmpty()) {
    			
    			/*
    			 * first, we convert the Collection into a EugeneCollection
    			 * 
    	    	 * whoever reads those lines, please enhance this!
    	    	 * ie Eugene should return a EugeneCollection already!
    			 */
        		EugeneCollection col = new EugeneCollection(UUID.randomUUID().toString());
        		col.getElements().addAll(components);
        		
    			
	    		/*
	    		 * pigeonize the collection and 
	    		 * return the URI of the generated pigeon image
	    		 */
    			jsonResponse.put("pigeon-uri", this.pigeonize(col));
    			
    			/*
    			 * SBOL XML/RDF serialization
    			 */
    			jsonResponse.put("sbol-xml-rdf", this.SBOLize(col));
    		}
    		
    		jsonResponse.put("eugene-output", 
    				this.baos.toString().replaceAll("\\n", "<br/>"));
    	} catch(Exception e) {
    		throw new EugeneException(e.toString());
    	}
    	
    	return jsonResponse;
    }
    
    private Collection<Component> getRandomComponents(Collection<Component> components, int MAX) {
    	Collection<Component> lst = new ArrayList<Component>(MAX);
    	Iterator<Component> it = components.iterator();
    	for(int i=0; i<MAX; i++) {
    		lst.add(it.next());
    	}
    	return lst;
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
					uris.add(this.pigeon.pigeonizeSingle((Device)ne, null));
				}
				
				// we only pigeonize MAX_VISUAL_COMPONENTS
				if(uris.size() > MAX_VISUAL_COMPONENTS) {
					break;
				}
			}
			
			if(uris.isEmpty()) {
				return null;
			}
			
			// do some file/directory management
			// arghhh
			String pictureName = col.getName() + ".png";
			String imgFilename = "." + this.TMP_DIRECTORY + "/" + pictureName;
			
			// merge all images (created above) 
			// into a single big image
			RenderedImage img = this.pigeon.toMergedImage(uris);
			
			// serialize the image to the filesystem
			this.writeToFile(img, imgFilename);

			// if everything went fine so far, 
			// then we return the relative URL of
			// the resulting image
			return URI.create("/tmp/" + pictureName);
				// how can we get rid of the tmp w/o
				// sophisticated string operations?
    	} catch(Exception ee) {
    		// something went wrong, i.e.
    		// throw an exception
    		throw new EugeneException(ee.getLocalizedMessage());
    	}
		
    }
    
    /*-------------------------------
     * SBOL XML/RDF serialization
     *-------------------------------*/
    private String SBOLize(EugeneCollection col) 
    		throws EugeneException {
    	
		String sbolFilename = col.getName() + ".sbol";
		String sbolPath = "." + this.TMP_DIRECTORY + "/" + sbolFilename;

    	SBOLDocument doc = SBOLExporter.toSBOLDocument(col);

    	/*
    	 * now, we persist the SBOLDocument object
    	 * to a file  
    	 */
    	this.serializeSBOL(doc, sbolPath);
    	
    	return "Download the SBOL file <a href=\"/EugeneLab/tmp/"+sbolFilename+"\">here</a>";    	
    }
    
    
	private void serializeSBOL(SBOLDocument document, String file) 
			throws EugeneException {

		try {
			// open a file stream
			FileOutputStream fos;
			File f = new File(file);
			if(!f.getParentFile().exists()) {
				f.getParentFile().mkdir();
				f.getParentFile().mkdirs();
			}
			if (!f.exists()) {
				f.createNewFile();
			}
			fos = new FileOutputStream(f);
	
			// write the document to the file stream
			// using the SBOLFactory
			SBOLFactory.write(document, fos);
	
			// flush and close the stream
			fos.flush();
			fos.close();
		} catch(Exception e) {
			// if something went wrong, throw an exception
			throw new EugeneException(e.toString());
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
        if(null != cookies) {
	    	for(Cookie c : cookies) {
	    		if("user".equalsIgnoreCase(c.getName())) {
	    			return c.getValue();
	    		}
	    	}
        }

    	return username;
    }
    
    
    private String getDefaultUser() {
        return DEFAULT_FREE_USER;
    }


}
