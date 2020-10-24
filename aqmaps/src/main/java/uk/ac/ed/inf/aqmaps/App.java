package uk.ac.ed.inf.aqmaps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
    
    //OBJECT: Custom Sensor object
    private static class Sensor {
    	String location;
    	Double battery;
    	Double reading;
    	Point swPoint;
    	Point sePoint;
    	Point nePoint;
    	Point nwPoint;
    	
    	//Constructor created to clone custom objects effectively
    	public Sensor(Sensor another) {
    		this.location = another.location;
    		this.battery = another.battery;
    		this.reading = another.reading;
    		this.swPoint = another.swPoint;
    		this.sePoint = another.sePoint;
    		this.nePoint = another.nePoint;
    		this.nwPoint = another.nwPoint;
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
    	//Storing command line arguments into appropriate variables
        String dateDD = args[0];
        String dateMM = args[1];
        String dateYY = args[2];
        Double startLat = Double.parseDouble(args[3]);
        Double startLng = Double.parseDouble(args[4]);
        int randomSeed = Integer.parseInt(args[5]);
        String portNumber  = args[6];
        
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
        
        
        //Create ArrayList to store the data for the 33 sensors from the '/YYYY/MM/DD/air-quality-data.json' file
        ArrayList<Sensor> sensors = new ArrayList<Sensor>();
        
        //Iterate through the lines of the '/YYYY/MM/DD/air-quality-data.json' file and store them as Sensors in the 'sensors' ArrayList
        Boolean newSensor = true;
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
        		newSensor = true;
        		sensorIndex = 0;
        	}
        }
        
        
        //Get swPoint and nePoint for the given w3w location
        for (int i = 0; i < sensors.size(); i++) {
        	Sensor s = sensors.get(i);
        	
        	String w3w = s.location;
			String w1 = w3w.substring(0, w3w.indexOf("."));
			w3w = w3w.substring(w3w.indexOf(".") + 1);
			String w2 = w3w.substring(0, w3w.indexOf("."));
			String w3 = w3w.substring(w3w.indexOf(".") + 1);
			
			w3w = w1 + "/" + w2 + "/" + w3 + "/details.json";
    		
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
    		
    		//Loop through file
    		Point point = new Point();
    		Integer stage = -20;
    		String[]linesW3W = w3wFile.split(System.getProperty("line.separator"));
    		for(String line : linesW3W) {
    			
    			if (line.indexOf("southwest") != -1) {
    				stage = 1;
    			} else if (line.indexOf("northeast") != -1) {
    				stage = 4;
    			}
    			
    			//Parse the latitude and longitude values into doubles, and pass these into our 'point' object
    			if ((stage == 2) || (stage == 5)) {
    				point.lng = Double.parseDouble(line.substring(line.indexOf(":") + 1, line.length() - 1));
    			} else if ((stage == 3) || (stage == 6)) {
    				point.lat = Double.parseDouble(line.substring(line.indexOf(":") + 1, line.length()));
    			}
    			
    			//Pass the given Point object 'point' to the Sensor object 's'
    			if (stage == 3) {
    				s.swPoint = new Point(point);
    			} else if (stage == 6) {
    				s.nePoint = new Point(point);
    				Point se = new Point();
    				se.lat = s.swPoint.lat;
    				se.lng = s.nePoint.lng;
    				Point nw = new Point();
    				nw.lat = s.nePoint.lat;
    				nw.lng = s.swPoint.lng;
    				s.nwPoint = nw;
    				s.sePoint = se;
    			}
    			
    			stage += 1;
    		}
        }
        
        
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
		
		/*
		//Start mapping route
		String flightpathTxt = "";
		String readingsTxt = "";
		
		ArrayList<Sensor> unreadSensors = new ArrayList<Sensor>(sensors);
		Point lastPoint = null;
		Sensor lastSensor = new Sensor();
		int pathIndex = 1;
		
		while (unreadSensors.size() > 0) {
			
			Sensor nextSensor = new Sensor();
			
			if (lastPoint == null) {
				lastSensor = unreadSensors.get(0);
				unreadSensors.remove(0);
				lastPoint = lastSensor.nePoint;
			}
			
			Double minDist = 0.0;
			int minIndex = -1;
			int vertexNum = -1;
			
			//Find the closest sensor from the last point
			for (int g = 0; g < unreadSensors.size(); g++) {
				Double dist = calcDistance(unreadSensors.get(g).nePoint, lastPoint);
				int vNum = 1;
				
				if (calcDistance(unreadSensors.get(g).nwPoint, lastPoint) < dist) {
					vNum = 2;
				} else if (calcDistance(unreadSensors.get(g).swPoint, lastPoint) < dist) {
					vNum = 3;
				} else if (calcDistance(unreadSensors.get(g).sePoint, lastPoint) < dist) {
					vNum = 4;
				}
				
				if (dist < minDist) {
					minDist = dist;
					minIndex = g;
					vertexNum = vNum;
				}
			}
			
			nextSensor = unreadSensors.get(minIndex);
			unreadSensors.remove(minIndex);
			
			//Map route to this new sensor 's'
			double pathAngle = calcAngle(lastPoint, nextSensor.nePoint);
			
			if (pathAngle % 10 == 0) {
				flightpathTxt += pathIndex + "," + lastPoint.lng + "," + lastPoint.lat + "," + pathAngle + "," + nextSensor.nePoint.lng + "," + nextSensor.nePoint.lat + "," + nextSensor.location;
			} else {
				pathAngle = pathAngle - (pathAngle % 10);
			}
		}
		*/
    }
}