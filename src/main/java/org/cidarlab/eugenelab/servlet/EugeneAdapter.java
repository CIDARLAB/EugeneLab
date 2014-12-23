package org.cidarlab.eugenelab.servlet;

import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import org.cidarlab.eugene.Eugene;
import org.cidarlab.eugene.data.pigeon.Pigeonizer;
import org.cidarlab.eugene.data.sbol.SBOLExporter;
import org.cidarlab.eugene.dom.Component;
import org.cidarlab.eugene.dom.Device;
import org.cidarlab.eugene.dom.NamedElement;
import org.cidarlab.eugene.dom.imp.container.EugeneCollection;
import org.cidarlab.eugene.exception.EugeneException;
import org.sbolstandard.core.SBOLDocument;
import org.sbolstandard.core.SBOLFactory;

/**
 * The EugeneAdapter class serves as an adapter between 
 * the EugeneLabServlet and Eugene. Objects of this class 
 * hold all relevant information for Eugene, such as 
 * Session-ID, the user's HOME directory, or the username itself.
 * 
 * @author Ernst Oberortner
 *
 */
public class EugeneAdapter {

	private String sessionID;
	private String user;
	
	private String HOME_DIRECTORY;
	private String IMAGE_DIRECTORY;
	
	private Eugene eugene;
	private Pigeonizer pigeon;
	
	/*
	 * a writer that is being used for writing
	 * all Eugene outputs
	 * mainly being used for print and println
	 */
	private ByteArrayOutputStream baos;
	private BufferedWriter writer;
	
	
	public EugeneAdapter(String user, String sessionId,
			String HOME_DIRECTORY, String IMAGE_DIRECTORY) 
			throws EugeneException {
		
		this.user = user;
		this.sessionID = sessionId;
		
		this.HOME_DIRECTORY = HOME_DIRECTORY;
		this.IMAGE_DIRECTORY = IMAGE_DIRECTORY;
		
        /*
         * initialize Pigeon
         */
        this.pigeon = new Pigeonizer();

        this.initWriter();
        this.initEugene();
	}
	
	/**
	 * The initWriter/0 method initialized the output writer 
	 * for Eugene. I.e. Eugene writes all its outputs to 
	 * a given output writer.
	 * 
	 * @throws EugeneException
	 */
	private void initWriter() 
			throws EugeneException {
		
        try {        
        	this.baos = new ByteArrayOutputStream();
            this.writer = new BufferedWriter(
            		new OutputStreamWriter(baos), 
            		1024);
        } catch(Exception e) {
        	throw new EugeneException(e.getLocalizedMessage());
        }
	}
	
	/**
	 * The getWriter/0 method returns a reference 
	 * to the Output writer.
	 * 
	 * @return a BufferedWriter object representing 
	 *         the output writer
	 */
	private BufferedWriter getWriter() {
		return this.writer;
	}
	
	/**
	 * The initEugene/0 method instantiates Eugene
	 * with the session-specific data.
	 * 
	 * @throws EugeneException
	 */
	private void initEugene() 
			throws EugeneException {
		try {
			this.eugene = new Eugene(
					this.getSessionId(), 
					this.getWriter());
			
			// we also set Eugene's root directory
			// to the current user's home directory
			this.eugene.setRootDirectory(
					this.getUserHomeDirectory());
		} catch(EugeneException ee) {
			throw new EugeneException(ee.toString());
		}
	}
	
	private String getUserHomeDirectory() {
		return this.HOME_DIRECTORY+"/"+this.getUser();
	}
	
	
	/**
	 * The executeScript/1 method executes a given 
	 * Eugene script.  
	 * 
	 * @param script ... The Eugene script represented as a String
	 * 
	 * @throws EugeneException
	 */
	public EugeneCollection executeScript(String script) 
			throws EugeneException {
		
		if(null != this.eugene) {
			
    		this.baos.reset();

    		// we also set Eugene's root directory
			// to the current user's home directory
			this.eugene.setRootDirectory(
					this.getUserHomeDirectory());

    		return this.eugene.executeScript(script);
		}
		
		throw new EugeneException("Something went wrong!");
	}
	
	/**
	 * The getLibrary/0 method returns a Collection of components 
	 * that represent the current library.
	 * 
	 * @return ... the current library as a collection of components
	 * 
	 * @throws EugeneException
	 */
	public Collection<Component> getLibrary() 
			throws EugeneException {
		try {
			return this.eugene.getLibrary();
		} catch(EugeneException ee) {
			throw new EugeneException(ee.getLocalizedMessage());
		}
	}
	
	public String getUser() {
		return this.user;
	}
	
	public String getSessionId() {
		return this.sessionID;
	}
	

    /*
     * PIGEON
     */
    public URI pigeonize(EugeneCollection col, int MAX_VISUAL_COMPONENTS) 
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
			String pictureName = col.getName() + ".png";
			String imgFilename = IMAGE_DIRECTORY + "/" + pictureName;
			
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
    public String SBOLize(EugeneCollection col) 
    		throws EugeneException {
    	
		String sbolFilename = col.getName() + ".sbol";
		String sbolPath = IMAGE_DIRECTORY + "/" + sbolFilename;

		try {
			// convert the EugeneCollection into an in-memory
			// SBOLDocument
	    	SBOLDocument doc = SBOLExporter.toSBOLDocument(col);
	
	    	// then, we persist the SBOLDocument object
	    	// to a file  
	    	this.serializeSBOL(doc, sbolPath);
	    	
		} catch(EugeneException ee) {
			
			ee.printStackTrace();
			
			throw new EugeneException(ee.getLocalizedMessage());
		}
		
    	return "Download the SBOL file <a href=\"/EugeneLab/tmp/"+sbolFilename+"\">here</a>";    	
    }
    
    /**
     * The serializeSBOL(SBOLDocument, String) method serializes an SBOLDocument object 
     * into a file, specified by the String.  
     * 
     * @param document   ... The SBOLDocument object that contains all data in-memory regarding SBOL-compliant
     * format.
     * @param file  ... The name of the file (including path information) into that the in-memory data 
     * should be persisted.
     *  
     * @throws EugeneException
     */
	private void serializeSBOL(SBOLDocument document, String file) 
			throws EugeneException {

		try {
			File f = new File(file);
			
			// create directories
			if(!f.getParentFile().exists()) {
				f.getParentFile().mkdir();
				f.getParentFile().mkdirs();
			}
			
			// create the file
			if (!f.exists()) {
				f.createNewFile();
			}
			
			// open a file stream
			FileOutputStream fos = new FileOutputStream(f);
			
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

    private void writeToFile(RenderedImage buff, String savePath) 
    		throws EugeneException {

    	try {
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("png");
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

    /*
     * EUGENE OUTPUT
     */
    public String getEugeneOutput() {
    	return this.baos.toString();
    }
	
	/**
	 * The persistAndCleanUp/0 method persists the Eugene session
	 * and cleans up all reserved memory. 
	 */
	public void persistAndCleanUp() {
		// persist
		if(null != this.eugene) {
			// TODO: add a persist method to Eugene.
		}
		
		// cleanUp
		this.eugene = null;
		this.writer = null;
		this.pigeon = null;
	}
}
