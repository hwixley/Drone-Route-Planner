package uk.ac.ed.inf.aqmaps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;

import com.mapbox.geojson.*;

public class App 
{
	//Confinement area coordinates
    private static final double maxLat = 55.946233; 
    private static final double minLat = 55.942617;
    private static final double maxLng = -3.184319;
    private static final double minLng = -3.192473;
    
    //Constants
    private static final double errorMargin = 0.0002;
    private static final double pathLength = 0.0003;
    
    //OBJECT: lineGraph custom object
    private static class LineGraph {
    	Double gradient;
    	Double yint;
    	
    	//Constructor with input
    	public LineGraph(Point p1, Point p2) {
    		this.gradient = (p1.lat - p2.lat)/(p1.lng - p2.lng);
    		this.yint = -gradient*p1.lng + p1.lat;
    	}
    	
    	//Constructor with angle input
    	public LineGraph(Double angle, Point origin) {
    		this.gradient = Math.tan(angle);
    		this.yint = -gradient*origin.lat + origin.lng;
    	}
    	
    	//Default constructor
    	public LineGraph() {
    	}
    }
    
    //METHOD: transform point
    private static Point transformPoint(Point origin, Double angle) {
    	Point out = new Point(origin);
    	
    	out.lat += pathLength*Math.sin(angle);
    	out.lng += pathLength*Math.cos(angle);
    	
    	return out;
    }
    
    //METHOD: check if valid point
    private static Boolean checkPoint(Point destination, Point actual) {
    	
    	if (calcDistance(destination, actual) < errorMargin) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    //METHOD: calculate distance of route
	private static Double calcRouteCost(ArrayList<Point> points) {
    	Double cost = 0.0;
    	
    	for (int p = 0; p < points.size()-1; p++) {
    		cost += calcDistance(points.get(p),points.get(p+1));
    	}
    	return cost;
    }
    
    //METHOD: returns the appropriate colour for a given air quality reading
    @SuppressWarnings("unused")
	private static String readingColour(Double reading) {
		String colour = "#000000";
		
		//Classify the given 'reading' by returning it's appropriate rgb-string
		if (reading == Double.NaN) {
			colour = "#000000";
		} else if (reading < 32) {
			colour = "#00ff00";
		} else if (reading < 64) {
			colour = "#40ff00";
		} else if (reading < 96) {
			colour = "#80ff00";
		} else if (reading < 128) {
			colour = "#c0ff00";
		} else if (reading < 160) {
			colour = "#ffc000";
		} else if (reading < 192) {
			colour = "#ff8000";
		} else if (reading < 224) {
			colour = "#ff4000";
		} else if (reading < 256) {
			colour = "#ff0000";
		}
		
		return colour;
    }
    
   //METHOD: returns the appropriate symbol for a given air quality reading
    @SuppressWarnings("unused")
	private static String readingSymbol(Double reading) {
    	String symbol = "cross";
    	
    	if (reading == Double.NaN) {
    		symbol = "cross";
    	} else if ((reading < 128) && (reading >= 0)) {
    		symbol = "lighthouse";
    	} else if ((reading >= 128) && (reading < 256)) {
    		symbol = "danger";
    	}
    	
    	return symbol;
    }
    
    //OBJECT: Custom Sensor object
    private static class Sensor {
    	String location;
    	Double battery;
    	Double reading;
    	Point point;
    	
    	//Constructor created to clone custom objects effectively
    	public Sensor(Sensor another) {
    		this.location = another.location;
    		this.battery = another.battery;
    		this.reading = another.reading;
    		this.point = another.point;
    	}
    	
    	//Constructor with no arguments for default properties
		public Sensor() {
		}
    }
    
    //OBJECT: Custom Point object
    private static class Point {
    	Double lat = -1.0;
    	Double lng = -1.0;
    	
    	//Constructor created to clone custom objects effectively
    	public Point(Point another) {
    		this.lat = another.lat;
    		this.lng = another.lng;
    	}
    	
    	//Constructor with no arguments for default properties
    	public Point() {
    	}
    }
    
    //OBJECT: Custom building(no-fly-zone) object
    private static class Building {
    	ArrayList<Point> points = new ArrayList<Point>();
    	String name;
    	String fill;
    	
    	//Constructor created to clone custom objects effectively
    	public Building(Building another) {
    		this.points = another.points;
    		this.name = another.name;
    		this.fill = another.fill;
    	}
    	
    	//Constructor with no arguments for default properties
    	public Building() {
    	}
    }
	
    //METHOD: calculate distance between 2 points
    static Double calcDistance(Point p1, Point p2) { 
    	Double lats = Math.pow(p1.lat - p2.lat,2);
    	Double lngs = Math.pow(p1.lng - p2.lng, 2);
    	
    	return Math.sqrt(lats + lngs);
    }
    
    //METHOD: calculate angle between 2 points
    static Double calcAngle(Point origin, Point dest) {
    	Double grad = (dest.lng - origin.lng)/(dest.lat - origin.lat);
    	Double angle = Math.toDegrees(Math.atan(grad));
    	
    	if ((dest.lng > origin.lng) && (dest.lat < origin.lat)) {
    		angle += 180;
    		
    	} else if ((dest.lng < origin.lng) && (dest.lat > origin.lat)) {
    		angle += 360;

    	} else if ((dest.lng < origin.lng) && (dest.lat < origin.lat)) {
    		angle += 180;
    	}
    	
    	return angle;
    }
    
    
    @SuppressWarnings("unchecked")
	public static void main( String[] args ) throws IOException
    {    	
    	//SETUP
    	
    	//Storing command line arguments into appropriate variables
        String dateDD = args[0];
        String dateMM = args[1];
        String dateYY = args[2];
        Double startLat = Double.parseDouble(args[3]);
        Double startLng = Double.parseDouble(args[4]);
        Point startPoint = new Point();
        startPoint.lat = startLat;
        startPoint.lng = startLng;
        int randomSeed = Integer.parseInt(args[5]);
        String portNumber = args[6];
        
    	//Initialise WebServer
    	var client = HttpClient.newHttpClient();
    	var wsURL = "http://localhost:" + portNumber + "/";
    	var request = HttpRequest.newBuilder().uri(URI.create(wsURL)).build();
    	try { 
			var response = client.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				System.out.println("Successfully connected to the WebServer at port " + portNumber);
			} else {
				System.out.println("ERROR: unable to connect to the WebServer at port " + portNumber);
				System.exit(0);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}

    	
    	//GET THE AIR QUALITY DATA FOR THE GIVEN DATE
    	
    	//1) Retrieve file from the WebServer
        //Define maps filePath
        String mapsFilePath = wsURL + "maps/" + dateYY + "/" + dateMM + "/" + dateDD + "/air-quality-data.json";
        
    	//Read the '/YYYY/MM/DD/air-quality-data.json' file from the WebServer
        var mapsRequest = HttpRequest.newBuilder().uri(URI.create(mapsFilePath)).build();
        String mapsFile = "";
        try {
        	var response = client.send(mapsRequest, BodyHandlers.ofString());
        	if (response.statusCode() == 200) {
        		System.out.println("Successfully retrieved the maps json file");
        		mapsFile = response.body();
        	} else {
        		System.out.println("ERROR: this maps file does not exist. Path = " + mapsFilePath);
        		System.exit(0);
        	}
        } catch (IOException | InterruptedException e) {
        	e.printStackTrace();
        }
        
        //2) Parse this file into a list of Sensor objects
        //Create ArrayList to store the data for the 33 sensors from the '/YYYY/MM/DD/air-quality-data.json' file
        ArrayList<Sensor> sensors = new ArrayList<Sensor>();
        
        //Iterate through the lines of the '/YYYY/MM/DD/air-quality-data.json' file and store them as Sensors in the 'sensors' ArrayList
        Integer sensorIndex = 0;
        Sensor sens = new Sensor();
        String[]mapLines = mapsFile.split(System.getProperty("line.separator"));
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
        			sens.location = data;
        		} else if (sensorIndex == 1) {
        			sens.battery = Double.parseDouble(data);
        		} else if (sensorIndex == 2) {
        			
        			//If the battery is below 10% then set the sensor reading to NaN
        			if (sens.battery < 10) {
        				sens.reading = Double.NaN;
        			} else {
        				sens.reading = Double.parseDouble(data);
        			}
        		}
        		
        		sensorIndex += 1;
        		
        	//Else check if there is no more data for the given sensor
        	} else if (line.indexOf("}") != -1) {
        		sensors.add(new Sensor(sens));
        		sensorIndex = 0;
        	}
        }
        
        //3) Get the given coordinates of the W3W location
        //Get swPoint and nePoint for the given w3w location
        for (int i = 0; i < sensors.size(); i++) {
        	Sensor s = sensors.get(i);
        	
        	String w3w = s.location;
			String w1 = w3w.substring(0, w3w.indexOf("."));
			w3w = w3w.substring(w3w.indexOf(".") + 1);
			String w2 = w3w.substring(0, w3w.indexOf("."));
			String w3 = w3w.substring(w3w.indexOf(".") + 1);
			
			w3w = w1 + "/" + w2 + "/" + w3 + "/details.json";
    		
			//RETRIEVE W3W DATA FROM WEBSERVER
            //Define W3W filePath
            String w3wFilePath = wsURL + "words/" + w3w;
            
        	//Read the '/W1/W2/W3/details.json' file from the WebServer
            var w3wRequest = HttpRequest.newBuilder().uri(URI.create(w3wFilePath)).build();
            String w3wFile = "";
            try {
            	var response = client.send(w3wRequest, BodyHandlers.ofString());
            	if (response.statusCode() == 200) {
            		w3wFile = response.body();
            	} else {
            		System.out.println("ERROR: this W3W file does not exist. Path = " + w3wFilePath);
            		System.exit(0);
            	}
            } catch (IOException | InterruptedException e) {
            	e.printStackTrace();
            }
    		
            //PARSE W3W FILE AND APPEND DATA TO THE APPROPRIATE SENSOR OBJECTS
    		//Loop through file
    		Point point = new Point();
    		Integer stage = -20;
    		String[]linesW3W = w3wFile.split(System.getProperty("line.separator"));
    		for(String line : linesW3W) {
    			
    			if (line.indexOf("coordinates") != -1) {
    				stage = 1;
    			}
    			
    			//Parse the latitude and longitude values into doubles, and pass these into our 'point' object
    			if (stage == 2){
    				point.lng = Double.parseDouble(line.substring(line.indexOf(":") + 1, line.length() - 1));
    			} else if (stage == 3) {
    				point.lat = Double.parseDouble(line.substring(line.indexOf(":") + 1, line.length()));
    				s.point = new Point(point);
    				break;
    			}

    			stage += 1;
    		}
        }
        
		
        //PARSE SENSORS INTO GEOJSON MARKERS
		String dataGeojson = "{\"type\": \"FeatureCollection\",\n\t\"features\"\t: [";
		//Add geojson Polygon to represent confinement area
		dataGeojson += "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Polygon\", \"coordinates\": [[";
		dataGeojson += "[" + maxLng + ", " + maxLat + "], [" + maxLng + ", " + minLat + "], [" + minLng + ", " + minLat + "], [" + minLng + ", " + maxLat + "]]]},\n\t\t";
		dataGeojson += "\"properties\": {\"fill-opacity\": 0}},";
		//Geojson marker point
		String markerGeojson = "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Point\", \"coordinates\": [";
		
		for (int m = 0; m < sensors.size(); m++) {
			Sensor sensor = new Sensor(sensors.get(m));
			
			//Add geojson Point for each sensor
			dataGeojson += markerGeojson + sensor.point.lng.toString() + ", " + sensor.point.lat.toString() + "]},\n";
			dataGeojson += "\t\t\t\"properties\": {\"marker-size\": \"medium\", \"location\": \"" + sensor.location  + "\", \"rgb-string\": \"" + readingColour(sensor.reading) + "\", ";
			dataGeojson += "\"marker-color\": \"" + readingColour(sensor.reading) + "\", \"marker-symbol\": \"" + readingSymbol(sensor.reading) + "\"}\n\t\t\t},";
		}
        
        
        //GET THE NO-FLY-ZONE DATA
        
        //1) Retrieve files from the WebServer
        //Define no fly zones filePath
        String noflyzoneFilePath = wsURL + "buildings/no-fly-zones.geojson";
        
    	//Read the '/W1/W2/W3/details.json' file from the WebServer
        var noflyzoneRequest = HttpRequest.newBuilder().uri(URI.create(noflyzoneFilePath)).build();
        String noflyzoneFile = "";
        try {
        	var response = client.send(noflyzoneRequest, BodyHandlers.ofString());
        	if (response.statusCode() == 200) {
        		noflyzoneFile = response.body();
        		System.out.println("Successfully retrieved the no fly zones geojson file");
        	} else {
        		System.out.println("ERROR: this no fly zone file does not exist. Path = " + noflyzoneFilePath);
        		System.exit(0);
        	}
        } catch (IOException | InterruptedException e) {
        	e.printStackTrace();
        }
        
        //2) Parse these files into appropriate java Building objects
		//ArrayList to store building polygons
		ArrayList<Building> buildings = new ArrayList<Building>();
		
		//Iterate through the '/buildings/no-fly-zones.geojson' file
		Building building = new Building();
		Point polyPoint = new Point();
		Boolean buildingComplete = false;
        String[]noflyzoneLines = noflyzoneFile.split(System.getProperty("line.separator"));
        for(String line : noflyzoneLines) {
			
			//Check if line contains name property
			if (line.indexOf("name") != -1) {
				building.name = line.substring(line.indexOf(":") + 3, line.length() - 2);
				buildingComplete = false;
				building.points = new ArrayList<Point>();
			
			//Check if line contains fill property
			} else if (line.indexOf("fill") != -1) {
				building.fill = line.substring(line.indexOf(":") + 3, line.length() - 1);
			
			//Check if line contains longitude
			} else if ((line.indexOf("-3.") != -1)) {
				polyPoint.lng = Double.parseDouble(line.substring(line.indexOf("-"), line.length() -1));
				
			//Check if line contains latitude
			} else if (line.indexOf("55.") != -1) {
				polyPoint.lat = Double.parseDouble(line.substring(line.indexOf("55."), line.length()));
				building.points.add(new Point(polyPoint));
			
			//Check if line contains a closing square bracket (indicates end of a given polygon)
			} else if ((line.indexOf("]") != -1) && (line.indexOf("],") == -1) && !buildingComplete) {
				buildings.add(new Building(building));
				buildingComplete = true;
			}
		}
		
        
        //FIND OPTIMAL ROUTE
        
        //1) Use greedy algorithm to choose closest points
        ArrayList<Point> pointRoute = new ArrayList<Point>();
		ArrayList<Sensor> sensorRoute = new ArrayList<Sensor>();
		ArrayList<Sensor> unexploredSensors = new ArrayList<Sensor>(sensors);
		 
		for (int s = 0; s < sensors.size()+1; s++) {
			Point currPoint;
			if (s == 0) {
				currPoint = new Point(startPoint);
			} else {
				currPoint = pointRoute.get(s-1);
			}
			Double minDist = 100.0;
			Point minPoint = new Point();
			int minSensor = -1;
			 
			for (int u = 0; u < unexploredSensors.size(); u++) {
				Sensor nextSensor = unexploredSensors.get(u);
				 
				if (calcDistance(nextSensor.point, currPoint) < minDist) {
					minDist = calcDistance(nextSensor.point, currPoint);
					minPoint = new Point(nextSensor.point);
					minSensor = u;
				}
				 
			}
			if (unexploredSensors.size() > 0) {
		    	pointRoute.add(minPoint);
		    	sensorRoute.add(unexploredSensors.get(minSensor));
		    	unexploredSensors.remove(minSensor);
			}
		}
		unexploredSensors.clear();
		
		//2) Use 2-OPT heuristic algorithm to swap points around in the route to see if it produces a lower cost
		Boolean better = true;
		while (better) {
			better = false;
			 
			for (int j = 0; j < pointRoute.size()-1; j++) {
				for (int i = 1; i < j; i++) {
					Double oldCost = calcRouteCost(pointRoute);
					 
					Point iPoint = pointRoute.get(i);
					Point iPointP = pointRoute.get(i-1);
					Point jPoint = pointRoute.get(j);
					Point jPointP = pointRoute.get(j+1);
					 
					Double newCost = oldCost - calcDistance(iPointP, iPoint) - calcDistance(jPoint, jPointP) + calcDistance(iPointP, jPoint) + calcDistance(iPoint, jPointP);
					 
					if (newCost < oldCost) {
						ArrayList<Point> revPoints = new ArrayList<Point>();
						ArrayList<Sensor> revSensors = new ArrayList<Sensor>();
						 
						for (int v = 0; v < j-i+1; v++) {
							revPoints.add(pointRoute.get(i+v));
							revSensors.add(sensorRoute.get(i+v));
						}
						for (int z = 0; z < j-i+1; z++) {
							pointRoute.set(i+z, revPoints.get(j-i-z));
							sensorRoute.set(i+z, revSensors.get(j-i-z));
						}
						 
						better = true;
					}
				}
		 	}
		}
		pointRoute.clear();
		
		
		//Variables
		String lineGeojson = "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\": {\"type\": \"LineString\",\n\t\t\t\t\"coordinates\": [";
		ArrayList<Point> route = new ArrayList<Point>();
		route.add(startPoint);
		int moves = 0;
		sensorRoute.remove(0);
		ArrayList<Sensor> unreadSensors = new ArrayList<Sensor>(sensorRoute);
		String flightpathTxt = "";
		
		//FIND MOVES FOR CHOSEN ROUTE
		while ((unreadSensors.size() > 0) && (moves < 150)) {
			Sensor nextSensor = new Sensor(unreadSensors.get(0));
			Point currPoint = new Point(route.get(route.size()-1));
			
			Double dist = calcDistance(currPoint, nextSensor.point);
			
			if ((dist < 0.0005) && (dist > 0.0001)) { // valid length
				Double angle = calcAngle(currPoint, nextSensor.point);
				Double remainder = angle % 10;
				Point newP = new Point();
				
				//Valid angle
				if (remainder == 0) {
					newP = new Point(transformPoint(currPoint, angle));
					System.out.println("yes?");
				} else { //Try floor and ceiling angles
					angle -= remainder;
					
					//Point with floored angle
					newP = new Point(transformPoint(currPoint, angle));
					
					if (!checkPoint(nextSensor.point, newP)) { //Invalid floored angle point
						angle += 10;
						//Point with ceilinged angle
						newP = new Point(transformPoint(currPoint, angle));
					}
				}
				route.add(newP);
				String location = "null";
				String comma = "";
				
				if ((unreadSensors.size() > 1) || (moves < 149)) {
					comma = ",";
				}
				
				if (checkPoint(nextSensor.point, newP)) { //Checks if point is valid
					System.out.println("valid");
					location = nextSensor.location;
					System.out.println(location);
					unreadSensors.remove(0);
				}
				//Writing to files
				flightpathTxt += (moves+1) + "," + currPoint.lng.toString() + "," + currPoint.lat.toString() + "," + angle.toString() + "," + newP.lng.toString() + "," + newP.lat.toString() + "," + location + "\n";
				dataGeojson += lineGeojson + "\n\t\t\t\t[" + currPoint.lng.toString() + ", " + currPoint.lat.toString() + "], [" + newP.lng.toString() + ", " + newP.lat.toString() + "]\n\t\t\t\t]\n\t\t\t},\"properties\":{\n\t\t}\n\t}" + comma + "\n\t\t";
						
				moves += 1;
				
				
			} else if (dist >= 0.0005) { //zigzag
				Double angle = calcAngle(currPoint, nextSensor.point);
				Double remainder = angle % 10;
				Point newP = new Point();
				
				//Valid angle
				if (remainder == 0) {
					newP = new Point(transformPoint(currPoint, angle));
					
				} else { //Try floor and ceiling angles
					Double roundedAngle = angle - remainder;
					
					//Point with floored angle
					newP = new Point(transformPoint(currPoint, roundedAngle));
				}
				route.add(newP);
				String comma = "";
				
				if (moves < 149) {
					comma = ",";
				}

				//Writing to files
				System.out.println(moves);
				flightpathTxt += (moves+1) + "," + currPoint.lng.toString() + "," + currPoint.lat.toString() + "," + angle.toString() + "," + newP.lng.toString() + "," + newP.lat.toString() + ",null\n";
				dataGeojson += lineGeojson + "\n\t\t\t\t[" + currPoint.lng.toString() + ", " + currPoint.lat.toString() + "], [" + newP.lng.toString() + ", " + newP.lat.toString() + "]\n\t\t\t\t]\n\t\t\t},\"properties\":{\n\t\t}\n\t}" + comma + "\n\t\t";
						
				moves += 1;
			}
		}
		dataGeojson += "\n\t]\n}";
		
		
        //OUTPUT OUR GEO-JSON AQMAPS FILE
        
        //Try write the code in the 'dataGeojson' String variable to a Geo-JSON file
        try {
        	String geojsonFilename = "/readings-" + dateDD + "-" + dateMM + "-" + dateYY + ".geojson"; 
        	FileWriter writer = new FileWriter(System.getProperty("user.dir") + geojsonFilename);
        	writer.write(dataGeojson);
        	writer.close();
        	//Success writing to file 'readings-DD-MM-YYYY.geojson'
        	System.out.println("The air quality sensors from " + dateDD + "-" + dateMM + "-" + dateYY + " have been read by the drone and formatted into a Geo-JSON map.\nGeo-JSON file path:\t" + System.getProperty("user.dir") + geojsonFilename);
        	
        } catch (IOException e) {
        	//Failure writing to file 'readings-DD-MM-YYYY.geojson'
        	e.printStackTrace();
        }
        
        //Try write the code in the 'flightpathTxt' String variable to a .txt file
        try {
        	String txtFilename = "/flightpath-" + dateDD + "-" + dateMM + "-" + dateYY +".txt"; 
        	FileWriter writer = new FileWriter(System.getProperty("user.dir") + txtFilename);
        	writer.write(flightpathTxt);
        	writer.close();
        	//Success writing to file 'flightpath-DD-MM-YYYY.geojson'
        	System.out.println("All the drone moves from " + dateDD + "-" + dateMM + "-" + dateYY + " have been logged into a text file.\nText file path:\t" + System.getProperty("user.dir") + txtFilename);
        	
        } catch (IOException e) {
        	//Failure writing to file 'readings-DD-MM-YYYY.geojson'
        	e.printStackTrace();
        }
    }
}