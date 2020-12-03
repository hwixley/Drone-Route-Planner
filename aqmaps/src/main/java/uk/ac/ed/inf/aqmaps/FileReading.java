package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

public class FileReading {
	
    //RETRIEVING THE SENSOR AND AIR-QUALITY DATA METHODS
	
    //Parse Maps file into a list of Sensor objects
    public static ArrayList<Sensor> parseJsonSensors(String fileContents) {
    	
    	//Method output variable
    	ArrayList<Sensor> totalSensors = new ArrayList<Sensor>();
    	
    	//Iteration variables
        Integer sensorIndex = 0;
        Sensor sens = new Sensor();
        String[]mapLines = fileContents.split(System.getProperty("line.separator"));
        
        //Iterate through the lines of the '/YYYY/MM/DD/air-quality-data.json' file and store them as Sensors in the 'sensors' ArrayList
        for(String line : mapLines){
        	
        	//Check if the given line contains sensor data
        	if ((line.indexOf("[") == -1) && (line.indexOf("]") == -1) && (line.indexOf("{") == -1) && (line.indexOf("}") == -1)) {
        		
        		//Index offset variables for retrieving the correct substring of data for each given line
        		int startIndexOffset = 3;
        		int endIndexOffset = 1;
        		
        		if (sensorIndex == 1) {
        			startIndexOffset = 2;
        		} else if (sensorIndex == 0) {
        			endIndexOffset = 2;
        		}
        		
        		//Data retrieved as a substring from 'line'
        		String data = line.substring(line.indexOf(":") + startIndexOffset, line.length() - endIndexOffset);
        		
        		//Initialise the properties for the given sensor
        		if (sensorIndex == 0) {
        			sens.setLocation(data);
        		} else if (sensorIndex == 1) {
        			sens.setBattery(Double.parseDouble(data));
        		} else if (sensorIndex == 2) {
        			
        			//If the battery is below 10% then set the sensor reading to NaN
        			if (sens.getBattery() < 10) {
        				sens.setReading(Double.NaN);
        			} else {
        				sens.setReading(Double.parseDouble(data));
        			}
        		}
        		
        		sensorIndex += 1;
        		
        	//Else check if there is no more data for the given sensor
        	} else if (line.indexOf("}") != -1) {
        		totalSensors.add(new Sensor(sens));
        		sensorIndex = 0;
        	}
        }
        return totalSensors;
    }
    
    //Parses the .json file from a given What3Words tile into a Point (representing the centre of this tile)
    public static Point parseJsonW3Wtile(String fileContents) {
    	
    	//Output variable
    	Point point = new Point();
    	
    	//Iteration variables
		Integer stage = -20;
		String[]linesW3W = fileContents.split(System.getProperty("line.separator"));
		
		//Iterate over the lines in the W3W file 
		for(String line : linesW3W) {
			
			if (line.indexOf("coordinates") != -1) {
				stage = 1;
			}
			
			//Parse the latitude and longitude values into doubles, and pass these into our 'point' object
			if (stage == 2){
				point.setLng(Double.parseDouble(line.substring(line.indexOf(":") + 1, line.length() - 1)));
			} else if (stage == 3) {
				point.setLat(Double.parseDouble(line.substring(line.indexOf(":") + 1, line.length())));
				return point;
			}

			stage += 1;
		}
		return point;
    }
    
    //Adds the central coordinates of each sensor by parsing each W3W location
    public static ArrayList<Sensor> getSensorCoords(ArrayList<Sensor> inputSensors) {
    	
        //Get the central coordinates of the W3W tile for each sensor
        for (int i = 0; i < inputSensors.size(); i++) {
        	Sensor s = inputSensors.get(i);
        	
        	//Get the file path for the W3W file on the WebServer
        	String w3w = s.getLocation();
			String w1 = w3w.substring(0, w3w.indexOf("."));
			w3w = w3w.substring(w3w.indexOf(".") + 1);
			String w2 = w3w.substring(0, w3w.indexOf("."));
			String w3 = w3w.substring(w3w.indexOf(".") + 1);
			
			w3w = w1 + "/" + w2 + "/" + w3 + "/details.json";
    		
			
			//1) Retrieve W3W data from the WebServer
			String w3wFile = Webserver.getWebServerFile("words/" + w3w);
            
            //2) Parse the W3W file and append the coordinate data to the appropriate sensor object
			s.setPoint(parseJsonW3Wtile(w3wFile));
        }
        return inputSensors;
    }
    
    
    //RETRIEVING THE NO-FLY-ZONE DATA METHODS
    
    //Parses the no-fly-zones file as Building objects
    public static ArrayList<Building> parseNoflyzoneBuildings(String fileContents) {
    	
		ArrayList<Building> outputBuildings = new ArrayList<Building>();
		
		//Variables for iteration
		Building building = new Building();
		Point polyPoint = new Point();
		Boolean buildingComplete = false;
		ArrayList<Point> buildingVertices = new ArrayList<Point>();
		
		//Parsing points
		ArrayList<String> lngPrefix = new ArrayList<String>();
		lngPrefix.add(String.valueOf(App.maxLng).substring(0, String.valueOf(App.maxLng).indexOf(".")+1));
		lngPrefix.add(String.valueOf(App.minLng).substring(0, String.valueOf(App.minLng).indexOf(".")+1));
		
		ArrayList<String> latPrefix = new ArrayList<String>();
		latPrefix.add(String.valueOf(App.maxLat).substring(0, String.valueOf(App.maxLat).indexOf(".")+1));
		latPrefix.add(String.valueOf(App.minLat).substring(0, String.valueOf(App.minLat).indexOf(".")+1));

		//List of lines in the file
        String[]noflyzoneLines = fileContents.split(System.getProperty("line.separator"));
        
        //Iterate through the '/buildings/no-fly-zones.geojson' file
        for(String line : noflyzoneLines) {
			
			//Check if line contains name property
			if (line.indexOf("name") != -1) {
				buildingComplete = false;
				buildingVertices = new ArrayList<Point>();
			
			//Check if line contains longitude
			} else if (line.indexOf(lngPrefix.get(0)) != -1) {
				polyPoint.setLng(Double.parseDouble(line.substring(line.indexOf(lngPrefix.get(0)), line.length() -1)));
			
			} else if (line.indexOf(lngPrefix.get(1)) != -1) {
				polyPoint.setLng(Double.parseDouble(line.substring(line.indexOf(lngPrefix.get(1)), line.length() -1)));
				
			//Check if line contains latitude
			} else if (line.indexOf(latPrefix.get(0)) != -1) {
				polyPoint.setLat(Double.parseDouble(line.substring(line.indexOf(latPrefix.get(0)), line.length())));
				buildingVertices.add(new Point(polyPoint));
				building.setPoints(buildingVertices);
				
			} else if (line.indexOf(latPrefix.get(1)) != -1) {
				polyPoint.setLat(Double.parseDouble(line.substring(line.indexOf(latPrefix.get(1)), line.length())));
				buildingVertices.add(new Point(polyPoint));
				building.setPoints(buildingVertices);
			
			//Check if line contains a closing square bracket (indicates end of a given polygon)
			} else if ((line.indexOf("]") != -1) && (line.indexOf("],") == -1) && !buildingComplete) {
				outputBuildings.add(new Building(building));
				buildingComplete = true;
			}
		}
        return outputBuildings;
    }
}