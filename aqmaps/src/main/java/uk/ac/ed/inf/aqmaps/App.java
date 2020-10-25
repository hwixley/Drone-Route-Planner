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
    
    //METHOD: calculate distance of route
    @SuppressWarnings("unused")
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
		String colour = "";
		
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
    	String symbol = "";
    	
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
    	Point swPoint;
    	Point sePoint;
    	Point nePoint;
    	Point nwPoint;
    	
    	//Method to return array of points
    	private static ArrayList<Point> getPoints(Sensor s) {
    		ArrayList<Point> out = new ArrayList<Point>();
    		out.add(s.swPoint);
    		out.add(s.sePoint);
    		out.add(s.nePoint);
    		out.add(s.nwPoint);
    		
    		return out;
    	}
    	
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
    	//SETUP
    	
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
         pointRoute.add(sensors.get(0).nePoint);
         sensorRoute.add(sensors.get(0));
         unexploredSensors.remove(0);
         
         for (int s = 0; s < sensors.size(); s++) {
        	 Point currPoint = pointRoute.get(s);
        	 Double minDist = 100.0;
        	 Point minPoint = new Point();
        	 int minSensor = -1;
        	 
        	 for (int u = 0; u < unexploredSensors.size(); u++) {
        		 ArrayList<Point> nextSensorPoints = Sensor.getPoints(unexploredSensors.get(u));
        		 
        		 for (int v = 0; v < 4; v++) {
        			 if (calcDistance(nextSensorPoints.get(v), currPoint) < minDist) {
        				 minDist = calcDistance(nextSensorPoints.get(v), currPoint);
        				 minPoint = new Point(nextSensorPoints.get(v));
        				 minSensor = u;
        			 }
        		 }
        		 
        	 }
        	 if (unexploredSensors.size() > 0) {
		    	 pointRoute.add(minPoint);
		    	 sensorRoute.add(unexploredSensors.get(minSensor));
		    	 unexploredSensors.remove(minSensor);
        	 }
         }
         System.out.println(calcRouteCost(pointRoute));
         for (int r = 0; r < sensorRoute.size(); r++) {
        	System.out.println(sensorRoute.get(r).location);
         }
         
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
         
         System.out.println(calcRouteCost(pointRoute));
         for (int r = 0; r < sensorRoute.size(); r++) {
        	System.out.println(sensorRoute.get(r).location);
         }
        
		/*
		//Start mapping route
		String flightpathTxt = "";
		String readingsGeojson = "{\"type\"\t: \"FeatureCollection\",\n\t\"features\"\t: [";
		String cellGeojson = "\n\t{\"type\"\t\t: \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\" : \"Point\",\n\t\t\t\t\"coordinates\" : [";
		
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
			Point closestPoint = new Point();
			
			if (vertexNum == 1) {
				closestPoint = new Point(nextSensor.nePoint);
			} else if (vertexNum == 2) {
				closestPoint = new Point(nextSensor.nwPoint);
			} else if (vertexNum == 3) {
				closestPoint = new Point(nextSensor.swPoint);
			} else if (vertexNum == 4) {
				closestPoint = new Point(nextSensor.sePoint);
			}
			
			//Map route to this new sensor 's'
			double pathAngle = calcAngle(lastPoint, closestPoint);
			
			if (pathAngle % 10 == 0) {
				flightpathTxt += pathIndex + "," + lastPoint.lng + "," + lastPoint.lat + "," + pathAngle + "," + closestPoint.lng + "," + closestPoint.lat + "," + nextSensor.location + "\n";
				readingsGeojson += cellGeojson + closestPoint.lng.toString() + ", " + closestPoint.lat.toString() + "]\n";
				readingsGeojson += "\t\t\t\"properties\"\t: {\"marker-size\": \"medium\", \"location\": \"" + nextSensor.location  + "\", \"rgb-string\": \"" + readingColour(nextSensor.reading) + "\", ";
				readingsGeojson += "\"marker-color\": \"" + readingColour(nextSensor.reading) + "\", \"marker-symbol\": \"" + readingSymbol(nextSensor.reading) + "\"}}";
				
				if (unreadSensors.size() > 1) {
					readingsGeojson += ",";
				}
				pathIndex += 1;
				
			} else { // First try all possible angles for a straight line by varying latitude and longitude whilst still remaining in the w3w tile
				if (vertexNum == 1) {
					//for (int latOff = 0; latOff < 0.000001 0.0000001)
				} else if (vertexNum == 2) {
					
				} else if (vertexNum == 3) {
					
				} else if (vertexNum == 4) {
					
				}
				
				//for (int latOff = 0.0)
			}
		}
		
        //Add the closing brackets for our FeatureCollection in our Geo-JSON code variable ('readingsGeojson')
        readingsGeojson += "\n\t]\n}";
        
        
        
        //OUTPUT OUR GEO-JSON AQMAPS FILE
        
        //Try write the code in the 'geojsonText' String variable to a Geo-JSON file ('heatmap.geojson')
        try {
        	FileWriter writer = new FileWriter(System.getProperty("user.dir") + "/readings-" + dateDD + "-" + dateMM + "-" + dateYY +".geojson");
        	writer.write(readingsGeojson);
        	writer.close();
        	//Success writing to file 'readings-DD-MM-YYYY.geojson'
        	System.out.println("The air quality sensors from " + dateDD + "-" + dateMM + "-" + dateYY + " have been read by the drone and formatted into a Geo-JSON map.\nGeo-JSON file path:\t" + System.getProperty("user.dir") + "/heatmap.geojson");
        	
        } catch (IOException e) {
        	//Failure writing to file 'readings-DD-MM-YYYY.geojson'
        	e.printStackTrace();
        }
        */
    }
}