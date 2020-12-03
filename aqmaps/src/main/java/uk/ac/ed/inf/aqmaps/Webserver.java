package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

/**
 * Custom class used to store all web server methods
 */

public class Webserver {
	
	private static String wsURL = "http://localhost:";
    private static final HttpClient client = HttpClient.newHttpClient();
	
    //Initialise WebServer
    public static void initWebserver() {
    	
    	//Set up the HTTP Request, and URL variables
    	wsURL += App.portNumber + "/";
    	var request = HttpRequest.newBuilder().uri(URI.create(wsURL)).build();
    	
    	//Try connect to the WebServer at this URL
    	try { 
			var response = client.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				System.out.println("Successfully connected to the WebServer at port " + App.portNumber);
			
			//If the WebServer response is not successful then terminate the program
			} else {
				System.out.println("WEBSERVER CONNECTION ERROR: unable to connect to the WebServer at port " + App.portNumber);
				System.exit(0);
			}
			
		//If the WebServer response is not successful then terminate the program
		} catch (IOException | InterruptedException e) {
			System.out.println("WEBSERVER CONNECTION ERROR: unable to connect to the WebServer at port " + App.portNumber + ".\nEither the WebServer is not running or the port is incorrect.");
			System.exit(0);
		}
    }
    
    
    //Returns the file contents at the specified path of the WebServer
    public static String getWebServerFile(String path) {
    	
    	//Set up the HTTP Request variable
    	var request = HttpRequest.newBuilder().uri(URI.create(wsURL + path)).build();
    	
    	//Try read the file on the WebServer at this URL
    	try {
    		var response = client.send(request, BodyHandlers.ofString());
    		
    		if (response.statusCode() == 200) {
    			//System.out.println("Successfully retrieved the " + path + " file");
    			return response.body();
    		
    		//If the WebServer response is not successful (cannot locate the file) then terminate the program
    		} else {
    			System.out.println("FILE NOT FOUND ERROR: the file at the specified path does not exist on the WebServer. Path = " + wsURL + path);
        		System.exit(0);
    		}
    	
    	//If the WebServer response is not successful (cannot locate the file) then terminate the program
    	} catch (IOException | InterruptedException e) {
    		System.out.println("FILE NOT FOUND ERROR: the file at the specified path does not exist on the WebServer. Path = " + wsURL + path);
    		System.exit(0);
    	}
    	return "";
    }
	
}