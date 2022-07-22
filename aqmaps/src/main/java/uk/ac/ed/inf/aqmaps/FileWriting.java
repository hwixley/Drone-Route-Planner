package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Custom class used to store all file writing methods
 */

public class FileWriting {
	
	//Geo-JSON Feature syntax
	private static final String endFeatureCollectionGeojson = "\n\t\t\t\t]\n\t\t\t},\"properties\":{\n\t\t}\n\t}\n\t\t\n\t]\n}";
	private static final String startMarkerGeojson = "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Point\", \"coordinates\": [";
	private static final String startLineStringGeojson = "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\": {\"type\": \"LineString\",\n\t\t\t\t\"coordinates\": [\n\t\t\t\t";
	
	
    //WRITING GEOJSON FEATURES
    
    //Method that returns the Geo-JSON Point code for a sensor marker
    public static String getGeojsonMarker(Sensor sens, Boolean beenVisited) {
    	String markerOutput = "";
    	markerOutput += startMarkerGeojson + sens.getPoint().getLng().toString() + ", " + sens.getPoint().getLat().toString() + "]},\n";
    	
    	//Checks if this Sensor has been visited (so we can give it a colour and symbol)
    	if (beenVisited) {
			markerOutput += "\t\t\t\"properties\": {\"marker-size\": \"medium\", \"location\": \"" + sens.getLocation()  + "\", \"rgb-string\": \"" + sens.getReadingColour() + "\", \"marker-color\": \"" + sens.getReadingColour() + "\", \"marker-symbol\": \"" + sens.getReadingSymbol() + "\"}\n\t\t\t},";
    	
    	} else {
			markerOutput += "\t\t\t\"properties\": {\"marker-size\": \"medium\", \"location\": \"" + sens.getLocation()  + "\", \"rgb-string\": \"#aaaaaa\", \"marker-color\": \"#aaaaaa\"}\n\t\t\t},";
    	}
		return markerOutput;
    }
    
    //Method that returns the Geo-JSON LineString code for the drone route
    public static String getGeojsonRoute(ArrayList<Point> route) {
    	String lineOutput = "";
    	
    	//Add the route as a single LineString Geo-JSON feature
		lineOutput += startLineStringGeojson;
		
		//Iterates through the points in our route
		for (int r = 0; r < route.size(); r++) {
			Point point = route.get(r);
			
			//Add a comma to appropriately separate points
			String comma = ",";
			if (r == route.size()-1) {
				comma = "";
			}
			
			lineOutput += "[" + point.getLng().toString() + ", " + point.getLat().toString() + "]" + comma;
		}
		return lineOutput;
    }
    
    public static String getGeojsonFeatureCollectionSuffix() {
    	return endFeatureCollectionGeojson;
    }
    
    //WRITING TO FILES:
    
    //Write to a file given the specified path and contents
    public static void writeToFile(String filePath, String fileContents) {
    	
        //Try write the code in the 'fileContents' String variable to the file path 'filPath'
        try {
        	FileWriter writer = new FileWriter(System.getProperty("user.dir") + filePath);
        	writer.write(fileContents);
        	writer.close();
        	//Success writing to file
        	System.out.println("\nFile for " + App.dateDD + "-" + App.dateMM + "-" + App.dateYY + " outputted successfully.\nFile path:   " + System.getProperty("user.dir") + filePath);
        	
        //Failure writing to file
        } catch (IOException e) {
        	System.out.println("FILE OUTPUT ERROR: unable to write to the file for " + App.dateDD +"-" + App.dateMM + "-" + App.dateYY + ". Attempted file path: " + System.getProperty("user.dir") + filePath);
        	System.exit(0);
        }
    }
}