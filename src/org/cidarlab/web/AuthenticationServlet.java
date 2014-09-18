package org.cidarlab.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

/**
 *
 * @author CIDAR's Beer League
 */
public class AuthenticationServlet 
	extends HttpServlet {

	private static final long serialVersionUID = -1579220291590687064L;
	
	/**
     * Processes requests for HTTP
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processPostRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	JSONObject jsonResponse = new JSONObject();

        try {
        	
        	String user = request.getParameter("username");
        	String password = request.getParameter("password");
        	
        	/*
        	 * read the command of the request
        	 */
        	String command = request.getParameter("command");
        	if("register".equalsIgnoreCase(command)) {
        		
        		Authenticator.register(
        				user, password);
        		
        	} else if("login".equalsIgnoreCase(command)) {
        		
        		if(Authenticator.login(
        				user, password)) {

        			/*
        			 * VALID AUTHENTICATION 
        			 */
	                Cookie authenticateCookie = new Cookie("authenticate", "authenticated");
	                Cookie userCookie = new Cookie("user", user);
	                authenticateCookie.setMaxAge(60 * 60); //cookie lasts for an hour
	                response.addCookie(authenticateCookie);
	                response.addCookie(userCookie);
	                response.sendRedirect("index.html");
	                
        		} else {
        			
        			/*
        			 * INVALID AUTHENTICATION 
        			 */
	                Cookie authenticateCookie = new Cookie("authenticate", "failed");
	                authenticateCookie.setMaxAge(60 * 60); //cookie lasts for an hour
	                response.addCookie(authenticateCookie);
	                response.sendRedirect("login.html");
        		}
            }
        } catch(Exception e) {
        	
    		jsonResponse.put("status", "exception");
    		jsonResponse.put("result", e.toString());
        
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

}