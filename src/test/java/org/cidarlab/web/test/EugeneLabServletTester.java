package org.cidarlab.web.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Lookup;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;

import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EugeneLabServletTester {

	/*
	 * EugeneLabServlet client and poster
	 */
	private static CloseableHttpClient eugene_client;
	private static HttpPost eugene_post;

	private static BasicHttpContext  context;
	private static CookieStore cookies;
	
	/*
	 * AuthenticationServlet client and poster
	 */
	private static CloseableHttpClient auth_client;
	private static HttpPost auth_post;
	
	@BeforeClass
	public static void setUpBeforeClass() 
			throws Exception {
		
		/*
		 * EugeneLabServlet client
		 */
		eugene_client = HttpClientBuilder.create().build();
		eugene_post = new HttpPost("http://localhost:8080/EugeneLab/EugeneLabServlet");
	    
		/*
		 * AuthenticationServlet client
		 */
		auth_client = HttpClients.createDefault();
		cookies = new BasicCookieStore();
		auth_post = new HttpPost("http://localhost:8080/EugeneLab/AuthenticationServlet");

		// do the login
		doLogin();
	}
	
	private static void doLogin()
			throws Exception {
		
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("command", "login"));
        nvps.add(new BasicNameValuePair("username", "test"));
        nvps.add(new BasicNameValuePair("password", "test"));

	    try {
	        auth_post.setEntity(
	        		new UrlEncodedFormEntity(
	        				nvps, 
	        				Consts.UTF_8));

	        context = new BasicHttpContext();
			context.setAttribute(HttpClientContext.COOKIE_STORE, cookies);

	        HttpResponse response = auth_client.execute(auth_post, context);
	    	
		    BufferedReader rd = new BufferedReader(
		    		new InputStreamReader(
		    				response.getEntity().getContent()));	
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }
	    

	}

	@AfterClass
	public static void tearDownAfterClass() 
			throws Exception {
	}

	@Before
	public void setUp() 
			throws Exception {
	}

	@After
	public void tearDown() 
			throws Exception {
	}

	@Test
	public void testInitServletConfig() {
		fail("Not yet implemented");
	}

	@Test
	public void testDoGetHttpServletRequestHttpServletResponse() {
		fail("Not yet implemented");
	}

	@Test
	public void testDoPostHttpServletRequestHttpServletResponse() {
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("command", "execute"));
        nvps.add(new BasicNameValuePair("script", "println(\"Hello World!\");"));
        
        eugene_post.setEntity(
        		new UrlEncodedFormEntity(
        				nvps, 
        				Consts.UTF_8));
		
	    try {
	    	HttpResponse response = eugene_client.execute(eugene_post, context);
	    	
		    BufferedReader rd = new BufferedReader(
		    		new InputStreamReader(
		    				response.getEntity().getContent()));	
		    
		    String json = EntityUtils.toString(response.getEntity());
		    System.out.println(json);
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }
	    
	}

}
