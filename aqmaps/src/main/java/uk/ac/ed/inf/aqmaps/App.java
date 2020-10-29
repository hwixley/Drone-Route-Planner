package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;

import uk.ac.ed.inf.aqmaps.Objects.Point;
import uk.ac.ed.inf.aqmaps.Objects.Building;
import uk.ac.ed.inf.aqmaps.Objects.LineGraph;
import uk.ac.ed.inf.aqmaps.Objects.Sensor;
import uk.ac.ed.inf.aqmaps.Objects.Move;

public class App 
{
	//VARIABLES
	
	//Confinement area coordinates
    private static final double maxLat = 55.946233; 
    private static final double minLat = 55.942617;
    private static final double maxLng = -3.184319;
    private static final double minLng = -3.192473;
    
    //Constants
    private static double errorMargin = 0.0002;
    private static final double pathLength = 0.0003;
    private static double undefTan = Math.tan(Math.toRadians(90));
    
    //Global variables
    private static ArrayList<Building> buildings = new ArrayList<Building>();
    private static ArrayList<Sensor> sensors = new ArrayList<Sensor>();
    
    //Temporary variables
    private static Move lastMove = new Move();
    
    
    //METHODS
    
    //Find point
    private static Move findPoint(Point currPoint, Point nextPoint) {
		Double angle = calcAngle(currPoint, nextPoint);
		Double remainder = angle % 10;
		Move move = new Move();
		move.origin = currPoint;
		Move tempMove = new Move(move);
    	
		//Valid angle
		if ((remainder == 0) && isValid(currPoint, transformPoint(currPoint, angle))) {
			move.angle = angle;
			move.dest = transformPoint(currPoint, angle);

		} else { //Try floor and ceiling angles
			Double newAngle = angle - remainder;
			
			//Point with floored angle
			Point newPF = new Point(transformPoint(currPoint, newAngle));
			Double distF = calcDistance(nextPoint, newPF);
			
			//Point with ceilinged angle
			if (newAngle == 360) {
				newAngle = 10.0;
			} else {
				newAngle += 10;
			}
			Point newPC = new Point(transformPoint(currPoint, newAngle));
			Double distC = calcDistance(nextPoint, newPC);
			tempMove.angle = angle - remainder;
			tempMove.dest = newPF;
			
			if ((distF < distC) && isValid(currPoint,newPF) && !isStuck(tempMove)) {
				move.angle = angle - remainder;
				move.dest = newPF;
				
			} else {
				tempMove.angle = newAngle;
				tempMove.dest = newPC;
				
				if (isValid(currPoint,newPC) && !isStuck(tempMove)) {
					move.angle = newAngle;
					move.dest = newPC;
					
				} else {
					Double pcAngle = newAngle;
					Double pfAngle = angle - remainder;
					
					while (!isValid(currPoint, newPF)) {
						if (pfAngle == 360) {
							pfAngle = 10.0;
						} else {
							pfAngle += 10;
						}
						newPF = new Point(transformPoint(currPoint, pfAngle));
					}
					while (!isValid(currPoint, newPC)) {
						if (pcAngle == 0) {
							pcAngle = 350.0;
						} else {
							pcAngle -= 10;
						}
						newPC = new Point(transformPoint(currPoint,pcAngle));
					}
					distF = calcDistance(nextPoint, newPF);
					distC = calcDistance(nextPoint, newPC);
					
					tempMove.angle = pfAngle;
					tempMove.dest = newPF;
					
					if ((distF < distC) && !isStuck(tempMove)) {
						move.angle = pfAngle;
						move.dest = newPF;
					} else {
						tempMove.angle = pcAngle;
						tempMove.dest = newPC;
						
						if (!isStuck(tempMove)) {
							move.angle = pcAngle;
							move.dest = newPC;
						} else {
							move.angle = pfAngle;
							move.dest = newPF;
						}
					}
				}
			}
		}
		lastMove = move;
		return move;
    }
    
    //Checks if algorithm is stuck (checks if last and current moves are equivalent)
    private static Boolean isStuck(Move current) {

    	if (Move.isNull(lastMove)) {
    		return false;
    	} else {
    	
	    	if (Math.abs(lastMove.angle - current.angle) == 180) {
	    		return true;
	    	} else {
	    		return false;
	    	}
    	}
    }
    
    //Transform point
    private static Point transformPoint(Point origin, Double angle) {
    	Point out = new Point(origin);
    	angle = Math.toRadians(angle);
    	
    	out.lat += pathLength*Math.sin(angle);
    	out.lng += pathLength*Math.cos(angle);
    	
    	return out;
    }
    
    //Check if valid point
    private static Boolean checkPoint(Point destination, Point actual) {
    	
    	if (calcDistance(destination, actual) < errorMargin) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    //Calculate distance of route
	private static Double calcRouteCost(ArrayList<Point> points) {
    	Double cost = 0.0;
    	ArrayList<Point> unreadPoints = new ArrayList<Point>(points);
    	ArrayList<Point> route = new ArrayList<Point>();
    	route.add(points.get(0));
    	unreadPoints.remove(0);
    	
    	while (unreadPoints.size() > 0) {
    		//Point newP = findPoint(route.get(route.size()-1), unreadPoints.get(0)).dest;
    		
    		//if (checkPoint(unreadPoints.get(0),newP)) {
    		//	unreadPoints.remove(0);
    		//}
    		Double dist = calcDistance(route.get(route.size()-1),unreadPoints.get(0));
    		
    		if (!isValid(route.get(route.size()-1),unreadPoints.get(0))) {
    			dist = dist*10;
    		}
    		
			cost += dist;
			route.add(unreadPoints.get(0));
			unreadPoints.remove(0);
    	}
    	Double dist = calcDistance(route.get(route.size()-1),route.get(0));
    	
    	if (!isValid(route.get(route.size()-1),route.get(0))) {
    		dist = dist*10;
    	}
    	cost += dist;
    	
    	return cost;
    }
	
	//Returns true if point is valid (within appropriate areas)
	private static Boolean isValid(Point origin, Point dest) {
		
		if (checkConfinement(dest) && checkBuildings(origin, dest)) {
			return true;
		} else {
			return false;
		}
	}
	
	//Returns true if path between p1 and p2 does not pass through any buildings
	private static Boolean checkBuildings(Point p1, Point p2) {
		LineGraph path = new LineGraph(p1,p2);
		
		for (int i = 0; i < buildings.size(); i++) {
			Building building = new Building(buildings.get(i));
			
			for (int j=0; j < building.points.size(); j++) {
				Point next = new Point();
				
				if (j == building.points.size()-1) {
					next = building.points.get(0);
				} else {
					next = building.points.get(j+1);
				}
				LineGraph bound = new LineGraph(building.points.get(j), next);
				
				if (!checkBound(path,bound)) {
					return false;
				}
			}
				
		}
		return true;
	}
	
	//returns True if these do not intersect
	private static Boolean checkBound(LineGraph path, LineGraph bound) {
		Double netGrad = path.gradient - bound.gradient;
		Double netYint = bound.yint - path.yint;
		Double max_lat = bound.p1.lat;
		Double min_lat = bound.p1.lat;
		Double max_lng = bound.p1.lng;
		Double min_lng = bound.p1.lng;
		
		if (bound.p2.lat > bound.p1.lat) {
			max_lat = bound.p2.lat;
		} else {
			min_lat = bound.p2.lat;
		}
		if (bound.p2.lng > bound.p1.lng) {
			max_lng = bound.p2.lng;
		} else {
			min_lng = bound.p2.lng;
		}
		
		if ((path.gradient == Double.NEGATIVE_INFINITY) || (path.gradient == Double.POSITIVE_INFINITY)) {
			if ((path.p1.lng <= max_lng) && (path.p1.lng >= min_lng)) {
				return true;
			} else {
				return false;
			}
		} else {
			if (netGrad != 0) {
				Double icLng = netYint/netGrad;
				Double icLat = path.gradient*icLng + path.yint;
				
				if (((icLng <= max_lng) && (icLng >= min_lng)) || ((icLat <= max_lat) && (icLat >= min_lat))) {
					return false;
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
	}
	
	//Returns true if point is in confinement area
	private static Boolean checkConfinement(Point p) {
		if ((p.lat < maxLat) && (p.lat > minLat) && (p.lng < maxLng) && (p.lng > minLng)) {
			return true;
		} else {
			return false;
		}
	}
    
    //Returns the appropriate colour for a given air quality reading
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
    
   //Returns the appropriate symbol for a given air quality reading
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
	
    //Calculates distance between 2 points
    private static Double calcDistance(Point p1, Point p2) { 
    	Double lats = Math.pow(p1.lat - p2.lat,2);
    	Double lngs = Math.pow(p1.lng - p2.lng, 2);
    	
    	return Math.sqrt(lats + lngs);
    }
    
    //Calculates angle between 2 points
    private static Double calcAngle(Point origin, Point dest) {
    	Double grad = (dest.lat - origin.lat)/(dest.lng - origin.lng);
    	Double angle = Math.toDegrees(Math.atan(grad));
    	
    	if ((dest.lng > origin.lng) && (dest.lat < origin.lat)) {
    		angle += 360;
    		
    	} else if ((dest.lng < origin.lng) && (dest.lat > origin.lat)) {
    		angle += 180;

    	} else if ((dest.lng < origin.lng) && (dest.lat < origin.lat)) {
    		angle += 180;
    	}
    	
    	return angle;
    }
    
    
    
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
        @SuppressWarnings("unused")
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
		String dataGeojson = "{\"type\": \"FeatureCollection\",\n\t\"features\"\t: [";
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
				dataGeojson += "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Polygon\", \"coordinates\": [[";
				
				for (int p = 0; p < building.points.size(); p++) {
					Point pointP = building.points.get(p);
					
					dataGeojson += "[" + pointP.lng + ", " + pointP.lat + "],";
				}
				dataGeojson += "[" + building.points.get(0).lng + ", " + building.points.get(0).lat + "]]]},\n\t\t";
				dataGeojson += "\"properties\": {\"fill-opacity\": 0.5, \"fill\": \"#ff0000\"}},";
			}
		}/*
        LineGraph path = new LineGraph(new Point(55.944,-3.188), new Point(55.944487503,-3.187));
        LineGraph bound = new LineGraph(new Point(55.94448750356385,-3.1871804594993587),new Point(55.944193856370475,-3.187042325735092));
        System.out.println(path.gradient);
        System.out.println(path.yint);
        System.out.println(bound.gradient);
        System.out.println(bound.yint);
        System.out.println(checkBound(path,bound));
        System.out.println(checkBuildings(path.p1, path.p2));
        System.out.println(isValid(path.p1, path.p2));

        */
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
				for (int i = 0; i < j; i++) {
					Double oldCost = calcRouteCost(pointRoute);
					 
					Point iPoint = pointRoute.get(i);
					Point iPointP = new Point();
					if (i == 0) {
						iPointP = pointRoute.get(pointRoute.size()-1);
					} else {
						iPointP = pointRoute.get(i-1);
					}
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
		Sensor finishPoint = new Sensor();
		finishPoint.point = startPoint;
		finishPoint.location = "end";
		unreadSensors.add(finishPoint);
		String flightpathTxt = "";
		
        //PARSE SENSORS INTO GEOJSON MARKERS
		//Add Geo-JSON Polygon to represent confinement area
		dataGeojson += "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Polygon\", \"coordinates\": [[";
		dataGeojson += "[" + maxLng + ", " + maxLat + "], [" + maxLng + ", " + minLat + "], [" + minLng + ", " + minLat + "], [" + minLng + ", " + maxLat + "]]]},\n\t\t";
		dataGeojson += "\"properties\": {\"fill-opacity\": 0}},";
		//Geo-JSON marker point
		String markerGeojson = "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Point\", \"coordinates\": [";
		

		//FIND MOVES FOR CHOSEN ROUTE
		while ((unreadSensors.size() > 0) && (moves < 150)) {
			Sensor nextSensor = new Sensor(unreadSensors.get(0));
			Point currPoint = new Point(route.get(route.size()-1));
			
			Double dist = calcDistance(currPoint, nextSensor.point);
			
			if (nextSensor.location == "end") {
				errorMargin = 0.0003;
			}
			
			//Checks if current point is in range of next point
			if (dist < 0.0005) {
				Move move = findPoint(currPoint,nextSensor.point);
				Point newP = move.dest;
				Double angle = move.angle;
				
				route.add(newP);
				
				//Adds comma for further Geo-JSON object additions
				String location = "null";
				String comma = "";
				if ((unreadSensors.size() > 1) && (moves < 149)) {
					comma = ",";
				}
				
				//Checks if point is valid
				if (checkPoint(nextSensor.point, newP)) { 
					location = nextSensor.location;
					unreadSensors.remove(0);
					
					//Checks if it is the end point
					if (location != "end") {
						//Add Geo-JSON Point for each sensor
						dataGeojson += markerGeojson + nextSensor.point.lng.toString() + ", " + nextSensor.point.lat.toString() + "]},\n";
						dataGeojson += "\t\t\t\"properties\": {\"marker-size\": \"medium\", \"location\": \"" + nextSensor.location  + "\", \"rgb-string\": \"" + readingColour(nextSensor.reading) + "\", ";
						dataGeojson += "\"marker-color\": \"" + readingColour(nextSensor.reading) + "\", \"marker-symbol\": \"" + readingSymbol(nextSensor.reading) + "\"}\n\t\t\t},";
					}
				}
				
				//Writing to files
				flightpathTxt += (moves+1) + "," + currPoint.lng.toString() + "," + currPoint.lat.toString() + "," + angle.toString() + "," + newP.lng.toString() + "," + newP.lat.toString() + "," + location + "\n";
				dataGeojson += lineGeojson + "\n\t\t\t\t[" + currPoint.lng.toString() + ", " + currPoint.lat.toString() + "], [" + newP.lng.toString() + ", " + newP.lat.toString() + "]\n\t\t\t\t]\n\t\t\t},\"properties\":{\n\t\t}\n\t}" + comma + "\n\t\t";
						
				moves += 1;
				
			//Checks if the current point is not in range of the next point
			} else {
				Move move = findPoint(currPoint,nextSensor.point);
				Point newP = move.dest;
				Double angle = move.angle;
				
				route.add(newP);
				
				//Adds comma for further Geo-JSON object additions
				String comma = "";
				if (moves < 149) {
					comma = ",";
				}

				//Writing to files
				flightpathTxt += (moves+1) + "," + currPoint.lng.toString() + "," + currPoint.lat.toString() + "," + angle.toString() + "," + newP.lng.toString() + "," + newP.lat.toString() + ",null\n";
				dataGeojson += lineGeojson + "\n\t\t\t\t[" + currPoint.lng.toString() + ", " + currPoint.lat.toString() + "], [" + newP.lng.toString() + ", " + newP.lat.toString() + "]\n\t\t\t\t]\n\t\t\t},\"properties\":{\n\t\t}\n\t}" + comma + "\n\t\t";
						
				moves += 1;
			}
		}
		
		
		//Add the unread sensors as gray markers to the Geo-JSON map
		if (unreadSensors.size() > 0) {
			dataGeojson += ",";
			for (int s = 0; s < unreadSensors.size(); s++) {
				Sensor unreadSensor = new Sensor(unreadSensors.get(s));
				String comma = ",";
				if (s == unreadSensors.size()-2) {
					comma = "";
				}
				
				if (unreadSensor.location != "end") {
					//Add Geo-JSON Point for each sensor
					dataGeojson += markerGeojson + unreadSensor.point.lng.toString() + ", " + unreadSensor.point.lat.toString() + "]},\n";
					dataGeojson += "\t\t\t\"properties\": {\"marker-size\": \"medium\", \"location\": \"" + unreadSensor.location  + "\", \"rgb-string\": \"#aaaaaa\", ";
					dataGeojson += "\"marker-color\": \"#aaaaaa\"}\n\t\t\t}" + comma;
				}
			}
		}
		dataGeojson += "\n\t]\n}";
		
		System.out.println("# Moves: " + moves);
		System.out.println("# Unread sensors: " + unreadSensors.size());
		
		
        //OUTPUT OUR GEO-JSON AQMAPS FILE
        
        //Try write the code in the 'dataGeojson' String variable to a Geo-JSON file
        try {
        	String geojsonFilename = "/readings-" + dateDD + "-" + dateMM + "-" + dateYY + ".geojson"; 
        	FileWriter writer = new FileWriter(System.getProperty("user.dir") + geojsonFilename);
        	writer.write(dataGeojson); 
        	writer.close();
        	//Success writing to file 'readings-DD-MM-YYYY.geojson'
        	System.out.println("\nThe air quality sensors from " + dateDD + "-" + dateMM + "-" + dateYY + " have been read by the drone and formatted into a Geo-JSON map.\nGeo-JSON file path:   " + System.getProperty("user.dir") + geojsonFilename);
        	
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
        	System.out.println("\nAll the drone moves from " + dateDD + "-" + dateMM + "-" + dateYY + " have been logged into a text file.\nText file path:   " + System.getProperty("user.dir") + txtFilename);
        	
        } catch (IOException e) {
        	//Failure writing to file 'readings-DD-MM-YYYY.geojson'
        	e.printStackTrace();
        }/*
        lastMove.angle=180.0;
        Point p1 = new Point(0.0,1.0);
        Point p2 = new Point(1.0, 2.0);
        lastMove.dest = p1;
        lastMove.origin = p2;
        
        Move m = new Move(p1,p2,0.0);
        
        System.out.println(isStuck(m));*/
    }
}