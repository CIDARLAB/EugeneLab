package org.cidarlab.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;

/**
 *
 * @author Ernst Oberortner
 */
public class AuthenticationServlet 
	extends HttpServlet {

	private static final long serialVersionUID = -1579220291590687064L;
	
	private static final String USER_DB_NAME = "CIDAR";
	private static org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("AuthenticationServlet");
	
	// a reference to an instance 
	// of the CIDAR authenticator
	private Authenticator auth;
	
	@Override
	public void init(ServletConfig config) 
			throws ServletException {
		
	    super.init(config);
	    
	    this.auth = new Authenticator(USER_DB_NAME);
	    
	    // set a system property such that Simple Logger will include timestamp
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        // set a system property such that Simple Logger will include timestamp in the given format
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "dd-MM-yy HH:mm:ss");

        // set minimum log level for SLF4J Simple Logger at warn
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        
        LOGGER.warn("[AuthenticationServlet] loaded!");	    
	}
	
    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	JSONObject jsonResponse = new JSONObject();
    	
        try {
        	
        	// get the username and password parameter values 
        	// from the request
        	String command = request.getParameter("command");
        	String username = request.getParameter("username");
        	String password = request.getParameter("password");
        	
        	/*
        	 * SIGNUP Request
        	 */
        	if("signup".equals(command)) {
        		
        		LOGGER.warn("SIGNUP! user: " + username +", password: " + password);

        		// register the user
            	this.auth.register(username, password);
            	
            	// we automatically login the user, 
            	// i.e. we do some session management 
            	this.login(request, response, username);

                // send the user to the eugenelab.html, i.e. the main IDE site
//                response.sendRedirect("eugenelab.html");

            /*
             * LOGIN Request
             */
        	} else if("login".equals(command)) {
        		
        		LOGGER.warn("LOGIN! user: " + username +", password: " + password);

        		// first, we check if the user exists and 
        		// if the passwords match up
        		boolean bLogin = this.auth.login(username, password);
        		
        		if(bLogin) {
            		LOGGER.warn("LOGIN VALID!");
            		
            		// login the user including session management
                	this.login(request, response, username);
                	
                    // send the user to the eugenelab.html, i.e. the main IDE site
//                    response.sendRedirect("eugenelab.html");
        		}
        		
        	/*
        	 *  LOGOUT Request
        	 */
        	} else if("logout".equals(command)) {
        		
        		// TODO:
        		LOGGER.warn("LOGOUT");
        		
        		HttpSession session = request.getSession(false);
        		if (session != null) {
        			// the session expires immediately
        			session.setMaxInactiveInterval(1);
        			// we remove the user information
        			session.removeAttribute("user");
        			// and invalidate the session
        		    session.invalidate();
        		    
//        		    response.sendRedirect("index.html");
        		}
        		
        	/*
        	 * Invalid Request	
        	 */
            } else {
            	LOGGER.warn("Invalid login! user: " + username + ", password: " + password);
            	throw new AuthenticationException("Invalid Request!");
            }
        	
        	jsonResponse.put("status", "good");
        	
        } catch(Exception e) {
        	
    		LOGGER.warn(e.getLocalizedMessage());
    		
    		jsonResponse.put("status", "exception");
    		jsonResponse.put("result", e.getLocalizedMessage());

		    //response.sendRedirect("index.html");
        } 

        /*
         * write the response
         */
    	PrintWriter out = response.getWriter();
    	response.setContentType("application/json");
        
    	out.write(jsonResponse.toString());
    	
    	out.flush();
        out.close();
    }
    
    private void login(HttpServletRequest request, HttpServletResponse response, String user) {

		/*-------------------------------
		 * VALID AUTHENTICATION 
		 *-------------------------------*/  
		
		// we create a session
		HttpSession session = request.getSession();
		
		// put the username into it
        session.setAttribute("user", user);

        // a session expires after 60 mins
        session.setMaxInactiveInterval(60 * 60);
        
        // also, we set two cookies
        // - the first cookie indicates of the user is authenticated
        // - the second cookie contains user information
        Cookie authenticateCookie = new Cookie("eugenelab", "authenticated");
        Cookie userCookie = new Cookie("user", user);
        authenticateCookie.setMaxAge(60 * 60); //cookie lasts for an hour
        
        
        // add both cookies to the response
        response.addCookie(authenticateCookie);
        response.addCookie(userCookie);
        
//do we need to set a cookie if authentication failed?	                
//	} else {
//		
//		/*
//		 * INVALID AUTHENTICATION 
//		 */
//        Cookie authenticateCookie = new Cookie("eugenelab", "failed");
//        authenticateCookie.setMaxAge(60 * 60); //cookie lasts for an hour
//        response.addCookie(authenticateCookie);
//        response.sendRedirect("login.html");
    }
    	


    /**
     * Processes requests for HTTP
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processGetRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    	
        response.setContentType("text/html;charset=UTF-8");
        response.sendRedirect("login.html");
        PrintWriter out = response.getWriter();
        try {
        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
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
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }

}