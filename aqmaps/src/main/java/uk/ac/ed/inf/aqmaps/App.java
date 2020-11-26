package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;

import uk.ac.ed.inf.aqmaps.Objects.Point;
import uk.ac.ed.inf.aqmaps.Objects.Building;
import uk.ac.ed.inf.aqmaps.Objects.LineGraph;
import uk.ac.ed.inf.aqmaps.Objects.Sensor;
import uk.ac.ed.inf.aqmaps.Objects.Move;
import uk.ac.ed.inf.aqmaps.Objects.Fragment;

public class App 
{
	//VARIABLES
	
	
	//Confinement area coordinates
    private static final double maxLat = 55.946233; 
    private static final double minLat = 55.942617;
    private static final double maxLng = -3.184319;
    private static final double minLng = -3.192473;
    
    //Constants
    private static double errorMargin = 0.0002; //Not left as final to cater for the 0.0003 error margin when returning to startPoint
    private static final double pathLength = 0.0003;
    
    //Global variables
    private static ArrayList<Building> buildings = new ArrayList<Building>();
    private static ArrayList<Sensor> sensors = new ArrayList<Sensor>();
    
    //Global argument variables
    private static String dateDD;
    private static String dateMM;
    private static String dateYY;
    private static Point startPoint;
    @SuppressWarnings("unused")
	private static int randomSeed;
    private static String portNumber;
    
    //Global WebServer variables
    private static String wsURL;
    private static final HttpClient client = HttpClient.newHttpClient();;
    
    //Global WebServer file strings
    private static String mapsFile;
    private static String noflyzoneFile;
    
    //Global output file strings
    private static String dataGeojson = "";
    private static String flightpathTxt = "";
    
    //findPoint method temporary variable
    private static Move lastMove = new Move();
    
    //Variable to store the optimized sensor route
	private static ArrayList<Sensor> sensorRoute = new ArrayList<Sensor>();
    
	//Geo-JSON Feature syntax
	private static final String markerGeojson = "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Point\", \"coordinates\": [";
	private static final String lineGeojson = "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\": {\"type\": \"LineString\",\n\t\t\t\t\"coordinates\": [";
	
	//Move finding variables
	private static int moves = 0;
	private static ArrayList<Point> route = new ArrayList<Point>();
	private static ArrayList<Sensor> unreadSensors = new ArrayList<Sensor>();
	
	
	
    //METHODS
	
	
	//INPUT ARGUMENT VALIDATION METHODS
	
	//Checks the date is valid and fixes any formatting issues (repairs single digit inputs)
	private static void checkDate(String day, String month, String year) {
		int dayVal = checkIsNumber(day,"day");
		int monthVal = checkIsNumber(month,"month");
		int yearVal = checkIsNumber(year,"year");
		
		//Array describing valid number of days for each month
		ArrayList<Integer> monthDays = new ArrayList<Integer>(Arrays.asList(31,28,31,30,31,30,31,31,30,31,30,31));
		
		//Account for leap years
		if (yearVal % 4 == 0) {
			monthDays.set(1, 29);
		}
		
		//Checks if the month is valid
		if ((monthVal < 1) || (monthVal > 12)) {
			System.out.println("INPUT ERROR: " + month + " is not a valid entry for the month. This entry must in the range [1,12].");
			System.exit(0);
		}
		
		//Checks if the day is valid for the respective month
		if ((dayVal < 1) || (dayVal > monthDays.get(monthVal-1))) {
			System.out.println("INPUT ERROR: " + day + " is not a valid entry for the day in month " + month + " of year " + year + ". This entry must in the range [1," + monthDays.get(monthVal-1).toString() + "].");
			System.exit(0);
		}
		
		if (dayVal < 10) {
			dateDD = "0" + String.valueOf(dayVal);
		}
		if (monthVal < 10) {
			dateMM = "0" + String.valueOf(monthVal);
		}
	}
	
	//Checks if the given date input argument is an integer 
	private static Integer checkIsNumber(String date, String name) {

		try {
			int val = Integer.parseInt(date);
			
			if (val < 0) {
				System.out.println("INPUT ERROR: " + date + " is not a valid entry for the " + name + ". This entry must be a positive integer.");
				System.exit(0);
			}
			return val;
			
		} catch (NumberFormatException e) {
			System.out.println("INPUT ERROR: " + date + " is not a valid entry for the " + name + ". This entry must be a positive integer.");
			System.exit(0);
		}
		return -1;
	}
	
	
	//FIND NEXT POINT METHOD 
    
    //Find next valid point to move to given the current and destination sensors
    private static Move findPoint(Point currPoint, Point nextPoint) {
    	
		Double angle = calcAngle(currPoint, nextPoint);
		Double remainder = angle % 10;
		Move move = new Move();
		move.origin = currPoint;
		
		//This Move variable is used to determine if the given move is the opposite as last (prevents infinite loops)
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
			
			//Temporary Move variable initialisation
			tempMove.angle = angle - remainder;
			tempMove.dest = newPF;
			
			//Check if the floored angle point is best and valid
			if ((distF < distC) && isValid(currPoint,newPF) && !isStuck(tempMove)) {
				move.angle = angle - remainder;
				move.dest = newPF;
			
			} else {
				tempMove.angle = newAngle;
				tempMove.dest = newPC;
				
				//Check if the ceilinged angle point is valid
				if (isValid(currPoint,newPC) && !isStuck(tempMove)) {
					move.angle = newAngle;
					move.dest = newPC;
				
				//Choose next best valid angle point
				} else {
					Double pcAngle = newAngle;
					Double pfAngle = angle - remainder;
					
					//Iterate until valid floored angle point is found
					while (!isValid(currPoint, newPF)) {
						if (pfAngle == 360) {
							pfAngle = 10.0;
						} else {
							pfAngle += 10;
						}
						newPF = new Point(transformPoint(currPoint, pfAngle));
					}
					//Iterate until valid ceilinged angle point is found
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
					
					//Check if the floored angle point is best and valid
					if ((distF < distC) && !isStuck(tempMove)) {
						move.angle = pfAngle;
						move.dest = newPF;
					} else {
						tempMove.angle = pcAngle;
						tempMove.dest = newPC;
						
						//Check if the ceilinged angle point is valid
						if (!isStuck(tempMove)) {
							move.angle = pcAngle;
							move.dest = newPC;
							
						//Else use the floored angle point
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
    
    
    //METHOD THAT CHECKS FOR REDUNDANT MOVES (indicates algorithm is stuck)
    
    //Checks if algorithm is stuck (checks if last and current moves are opposite)
    private static Boolean isStuck(Move current) {
    	
    	//Returns false if no move has been made yet
    	if (Move.isNull(lastMove)) {
    		return false;
    	} else {
    		
    		//Returns true if the last and current moves are opposite (angle difference of 180 degrees)
	    	if (Math.abs(lastMove.angle - current.angle) == 180) {
	    		return true;
	    	} else {
	    		return false;
	    	}
    	}
    }
    
    
    //METHOD THAT CHECKS IF A POINT IS WITHIN RANGE OF THE DESTINATION SENSOR
    
    //Checks if valid point (within the range of a sensor)
    private static Boolean checkPoint(Point destination, Point actual) {
    	
    	if (calcDistance(destination, actual) < errorMargin) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    
    //METHODS FOR CALCULATING ROUTE AND MOVE COST (used in route optimisation methods)
    
    //Calculate distance of route
	private static Double calcRouteCost(ArrayList<Sensor> sens) {
    	Double cost = 0.0;

    	ArrayList<Sensor> unreadSens = new ArrayList<Sensor>(sens);
    	ArrayList<Sensor> route = new ArrayList<Sensor>();
    	route.add(sens.get(0));
    	unreadSens.remove(0);
    	
    	//Iterates through the points in the route 
    	while (unreadSens.size() > 0) {
			cost += calcEdgeCost(route.get(route.size()-1).point,unreadSens.get(0).point);
			
			route.add(unreadSens.get(0));
			unreadSens.remove(0);
    	}
    	cost += calcEdgeCost(route.get(route.size()-1).point,route.get(0).point);
    	
    	return cost;
    }
	
	//Returns the estimated distance between two points
	private static Double calcEdgeCost(Point origin, Point dest) {
		Double dist = calcDistance(origin,dest);
		
		//If the path between adjacent points is not valid (intersects a building) we increase the added cost 
		if (!isValid(origin,dest)) {
			dist = dist*2;
		}
		
		return dist;
	}
	
	
	//METHODS FOR CHECKING FOR VALID MOVES (within confinement area and outside of no-fly-zone buildings)
	
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
		
		//Define the function for our given path
		LineGraph path = new LineGraph(p1,p2);
		
		//Iterates through the no-fly-zone buildings
		for (int i = 0; i < buildings.size(); i++) {
			Building building = new Building(buildings.get(i));
			
			//Iterates through the bounds of a given building
			for (int j=0; j < building.points.size(); j++) {
				Point next = new Point();
				
				//Initialises value of next point
				if (j == building.points.size()-1) {
					next = building.points.get(0);
				} else {
					next = building.points.get(j+1);
				}
				//Define the function for the given bound of the building
				LineGraph bound = new LineGraph(building.points.get(j), next);
				
				//Checks if the path intersects the given bound (if so then returns false)
				if (!checkBound(path,bound)) {
					return false;
				}
			}
				
		}
		return true;
	}
	
	//Returns True if these lines do not intersect
	private static Boolean checkBound(LineGraph path, LineGraph bound) {
		
		//Variables to determine point of intersection between the functions
		Double netGrad = path.gradient - bound.gradient;
		Double netYint = bound.yint - path.yint;
		//Variables to define the bounds of latitude and longitude values for the given building boundary
		Double max_lat = bound.p1.lat;
		Double min_lat = bound.p1.lat;
		Double max_lng = bound.p1.lng;
		Double min_lng = bound.p1.lng;
		
		//Initialise bound variables appropriately
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
		
		//Checks if the path is a vertical line (given when angle = 90/180)
		if ((path.gradient == Double.NEGATIVE_INFINITY) || (path.gradient == Double.POSITIVE_INFINITY)) {
			
			//Checks if the longitude of the path is within the bounds of the given building boundary (meaning an intersection)
			if ((path.p1.lng <= max_lng) && (path.p1.lng >= min_lng)) {
				return true;
			} else {
				return false;
			}
		} else {
			
			//Checks that the net gradient is not zero (this means no intersection)
			if (netGrad != 0) {
				Double icLng = netYint/netGrad;
				Double icLat = path.gradient*icLng + path.yint;
				
				//Checks whether the point of intersection is within the bounds of the given building boundary (meaning an intersection)
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
    
	
	//AIR-QUALITY CLASSIFICATION METHODS
	
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
	
	
	//GEOMETRICAL CALCULATOINS
	
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
    
    //Transform point (returns the transformed point by using the angle of the drone's movement)
    private static Point transformPoint(Point origin, Double angle) {
    	Point out = new Point(origin);
    	angle = Math.toRadians(angle);
    	
    	//Uses planar trigonometry to transform the current point given the angle of movement
    	out.lat += pathLength*Math.sin(angle);
    	out.lng += pathLength*Math.cos(angle);
    	
    	return out;
    }
    
    
    //WEBSERVER METHODS
    
    //Initialise WebServer
    private static void initWebserver() {
    	
    	//Set up the HTTP Request, and URL variables
    	wsURL = "http://localhost:" + portNumber + "/";
    	var request = HttpRequest.newBuilder().uri(URI.create(wsURL)).build();
    	
    	//Try connect to the WebServer at this URL
    	try { 
			var response = client.send(request, BodyHandlers.ofString());
			if (response.statusCode() == 200) {
				System.out.println("Successfully connected to the WebServer at port " + portNumber);
			
			//If the WebServer response is not successful then terminate the program
			} else {
				System.out.println("WEBSERVER CONNECTION ERROR: unable to connect to the WebServer at port " + portNumber);
				System.exit(0);
			}
			
		//If the WebServer response is not successful then terminate the program
		} catch (IOException | InterruptedException e) {
			System.out.println("WEBSERVER CONNECTION ERROR: unable to connect to the WebServer at port " + portNumber + ".\nEither the WebServer is not running or the port is incorrect.");
			System.exit(0);
		}
    }
    
    //Returns the file contents at the specified path of the WebServer
    private static String getWebServerFile(String path) {
    	
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
    
    
    //RETRIEVING THE SENSOR AND AIR-QUALITY DATA METHODS
    
    //Parse Maps file into a list of Sensor objects
    private static ArrayList<Sensor> parseJsonSensors(String fileContents) {
    	
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
        		totalSensors.add(new Sensor(sens));
        		sensorIndex = 0;
        	}
        }
        return totalSensors;
    }
    
    private static Point parseJsonW3Wtile(String fileContents) {
    	
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
				point.lng = Double.parseDouble(line.substring(line.indexOf(":") + 1, line.length() - 1));
			} else if (stage == 3) {
				point.lat = Double.parseDouble(line.substring(line.indexOf(":") + 1, line.length()));
				return point;
			}

			stage += 1;
		}
		return point;
    }
    
    //Adds the central coordinates of each sensor by parsing each W3W location
    private static ArrayList<Sensor> getSensorCoords(ArrayList<Sensor> inputSensors) {
    	
        //Get the central coordinates of the W3W tile for each sensor
        for (int i = 0; i < inputSensors.size(); i++) {
        	Sensor s = inputSensors.get(i);
        	
        	//Get the file path for the W3W file on the WebServer
        	String w3w = s.location;
			String w1 = w3w.substring(0, w3w.indexOf("."));
			w3w = w3w.substring(w3w.indexOf(".") + 1);
			String w2 = w3w.substring(0, w3w.indexOf("."));
			String w3 = w3w.substring(w3w.indexOf(".") + 1);
			
			w3w = w1 + "/" + w2 + "/" + w3 + "/details.json";
    		
			
			//1) Retrieve W3W data from the WebServer
			String w3wFile = getWebServerFile("words/" + w3w);
            
            //2) Parse the W3W file and append the coordinate data to the appropriate sensor object
			s.point = parseJsonW3Wtile(w3wFile);
        }
        return inputSensors;
    }
    
    //Retrieves the Sensor and air-quality data from the WebServer for the given date
    private static void getSensorData() {
    	
    	//1) Retrieve maps file from the WebServer (stored in 'mapsFile' global variable)
    	mapsFile = getWebServerFile("maps/" + dateYY + "/" + dateMM + "/" + dateDD + "/air-quality-data.json");
        
        //2) Parse this maps file into a list of Sensor objects (stored in 'sensors' global variable)
        sensors = parseJsonSensors(mapsFile);
        
        //3) Get the given coordinates of the W3W location for each sensor (stored in 'sensors' global variable)
        sensors = getSensorCoords(sensors);
    }
    
    
    //RETRIEVING THE NO-FLY-ZONE DATA METHODS
    
    //Parses the no-fly-zones file as Building objects
    private static void parseNoflyzoneBuildings() {
    	
		dataGeojson = "{\"type\": \"FeatureCollection\",\n\t\"features\"\t: [";
		
		//Variables for iteration
		Building building = new Building();
		Point polyPoint = new Point();
		Boolean buildingComplete = false;
        String[]noflyzoneLines = noflyzoneFile.split(System.getProperty("line.separator"));
        
        //Iterate through the '/buildings/no-fly-zones.geojson' file
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
				
				//DELETE: BUILDING GEOJSON
				dataGeojson += "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Polygon\", \"coordinates\": [[";
				
				for (int p = 0; p < building.points.size(); p++) {
					Point pointP = building.points.get(p);
					
					dataGeojson += "[" + pointP.lng + ", " + pointP.lat + "],";
				}
				dataGeojson += "[" + building.points.get(0).lng + ", " + building.points.get(0).lat + "]]]},\n\t\t";
				dataGeojson += "\"properties\": {\"fill-opacity\": 0.5, \"fill\": \"#ff0000\"}},";
			}
		}
    }
    
    //Get the no-fly-zone Geo-JSON data (stored in global 'buildings' variable)
    private static void getNoflyzoneData() {
    	
        //1) Retrieve files from the WebServer (stored in the 'noflyzoneFile' global variable)
    	noflyzoneFile = getWebServerFile("buildings/no-fly-zones.geojson");
        
        //2) Parse these files into appropriate java Building objects (stored in 'buildings' global variable)
        parseNoflyzoneBuildings();
    }
    
    
    //ROUTE OPTIMISATION METHODS
    
    //Return closest sensor
    private static Sensor closestSensor(Sensor sens) {
    	Double minDist = 10000.0;
    	int minIndex = -1;
    	
    	for (int s = 0; s < sensors.size(); s++) {
    		Sensor next = sensors.get(s);
    		
    		if ((sensorRoute.indexOf(next) != -1) || (next.equals(sens))) {
    			continue;
    		} else {
    			Double dist = calcEdgeCost(sens.point,next.point);
    			
    			if (dist < minDist) {
    				minDist = dist;
    				minIndex = s;
    			}
    		}
    	}
    	
    	return sensors.get(minIndex);
    }
    
    //Custom 'Temperate' route optimisation algorithm
    private static void temperate() {
    	
    	ArrayList<Double> avgDistances = new ArrayList<Double>();
    	ArrayList<Fragment> bestFrags = new ArrayList<Fragment>();
    	
    	//Calculate average distance for each sensor
    	for (int s = 0; s < sensors.size(); s++) {
    		Sensor sens = sensors.get(s);
    		Double avg = 0.0;
    				
    		for (int t = 0; t < sensors.size(); t++) {
    			if (t != s) {
    				avg += calcEdgeCost(sens.point,sensors.get(t).point);
    			}
    		}
    		bestFrags.add(new Fragment(sens,avg));
    		avgDistances.add(avg);
    	}

    	//Order fragments by AvgDistance (descending)
    	for (int i = 0; i < sensors.size(); i++) {
    		
    		if ( i < sensors.size()-1) {
	    		Double maxDist = Collections.max(avgDistances.subList(i, sensors.size()-1));
	    		int maxIndex = avgDistances.indexOf(maxDist);
	    		
	    		Fragment oldHead = bestFrags.get(i);
	    		bestFrags.set(i, bestFrags.get(maxIndex));
	    		bestFrags.set(maxIndex, oldHead);
    		}
    	}

    	//Calculate best transitions for each sensor
    	for (int r = 0; r < sensors.size(); r++) {
    		Fragment frag = bestFrags.get(r);
    		frag.bestDestSensor = closestSensor(frag.sensor);
    		bestFrags.set(r, frag);
    	}
    	
    	sensorRoute.add(bestFrags.get(0).sensor);
    	sensorRoute.add(bestFrags.get(0).bestDestSensor);
    	bestFrags.remove(0);
    	
    	//Calculate route
    	while (sensorRoute.size() < 33) {
    		Sensor lastSens = sensorRoute.get(sensorRoute.size()-1);
    		ArrayList<Integer> redundancies = new ArrayList<Integer>();
    		
    		for (int b = 0; b < bestFrags.size(); b++) {
    			Fragment frag = bestFrags.get(b);
    			
    			if (((frag.sensor.equals(lastSens)) && (sensorRoute.indexOf(frag.bestDestSensor) == -1)) || ((frag.bestDestSensor.equals(lastSens)) && (sensorRoute.indexOf(frag.sensor) == -1))) {
    				if (frag.sensor.equals(lastSens)) {
    					sensorRoute.add(frag.bestDestSensor);
    				} else {
    					sensorRoute.add(frag.sensor);
    				}
    				bestFrags.remove(b);
    				break;
    				
    			} else if ((frag.sensor.equals(lastSens)) || (frag.bestDestSensor.equals(lastSens))) {
    				redundancies.add(b);
    			}
    		}
    		
    		//Remove redundant edges 
    		for (int r = 0; r < redundancies.size(); r++) {
    			bestFrags.remove(redundancies.get(r));
    		}
    		
    		if (lastSens.equals(sensorRoute.get(sensorRoute.size()-1))) {
    			sensorRoute.add(closestSensor(lastSens));
    		}
    	}
    }
    
    //Greedy route optimisation algorithm
    private static void greedy() {
		ArrayList<Sensor> unexploredSensors = new ArrayList<Sensor>(sensors);
		 
		for (int s = 0; s < sensors.size()+1; s++) {
			Point currPoint;
			if (s == 0) {
				currPoint = new Point(startPoint);
			} else {
				currPoint = sensorRoute.get(s-1).point;
			}
			Double minDist = 100.0;
			int minSensor = -1;
			 
			for (int u = 0; u < unexploredSensors.size(); u++) {
				Sensor nextSensor = unexploredSensors.get(u);
				 
				if (calcEdgeCost(nextSensor.point, currPoint) < minDist) {
					minDist = calcEdgeCost(nextSensor.point, currPoint);
					minSensor = u;
				}
				 
			}
			if (unexploredSensors.size() > 0) {
		    	sensorRoute.add(unexploredSensors.get(minSensor));
		    	unexploredSensors.remove(minSensor);
			}
		}
    }
    
    //Swap heuristic route optimisation algorithm
    private static void swap() {
    	Boolean better = true;
    	
		if (sensorRoute.isEmpty()) {
			sensorRoute = new ArrayList<Sensor>(sensors);
		}
		
		while (better) {
			better = false;
			
			for (int i = 0; i < sensorRoute.size(); i++) {
				Double oldCost = calcRouteCost(sensorRoute);
				
				int indexI2 = i+1;
				if (i+1 == sensorRoute.size()) {
					indexI2 = 0;
				}
				
				Sensor newI = sensorRoute.get(indexI2);
				Sensor newI2 = sensorRoute.get(i);
				sensorRoute.set(indexI2, newI2);
				sensorRoute.set(i, newI);
				
				Double newCost = calcRouteCost(sensorRoute);
				
				if (newCost < oldCost) {
					better = true;
				} else {
					sensorRoute.set(i, newI2);
					sensorRoute.set(indexI2, newI);
				}
			}
		}
    }
    
    //2-Opt heuristic route optimisation algorithm
    private static void twoOpt() {
		Boolean better = true;
		int indexTwoOp = 0;
		
		if (sensorRoute.isEmpty()) {
			sensorRoute = new ArrayList<Sensor>(sensors);
		}
		
		while (better) {
			better = false; 
			
			for (int j = 0; j < sensorRoute.size()-1; j++) {
				for (int i = 0; i < j; i++) {
					Double oldCost = calcRouteCost(sensorRoute);
					indexTwoOp += 1;
					 
					Point iPoint = sensorRoute.get(i).point;
					Point iPointP = new Point();
					if (i == 0) {
						iPointP = sensorRoute.get(sensorRoute.size()-1).point;
					} else {
						iPointP = sensorRoute.get(i-1).point;
					}
					Point jPoint = sensorRoute.get(j).point;
					Point jPointP = sensorRoute.get(j+1).point;
					 
					Double newCost = oldCost - calcDistance(iPointP, iPoint) - calcDistance(jPoint, jPointP) + calcDistance(iPointP, jPoint) + calcDistance(iPoint, jPointP);
					
					if (indexTwoOp <= 2000) {
						if (newCost < oldCost) {
							ArrayList<Sensor> revSensors = new ArrayList<Sensor>();
							 
							for (int v = 0; v < j-i+1; v++) {
								revSensors.add(sensorRoute.get(i+v));
							}
							for (int z = 0; z < j-i+1; z++) {
								sensorRoute.set(i+z, revSensors.get(j-i-z));
							}
							 
							better = true;
						}
					}
				}
		 	}
		}
    }
    
    //Method for finding the optimal route
    private static void findOptimalRoute() {
    	//Route stored in sensorRoute variable
    	
    	//1) Use greedy algorithm to choose closest points
    	greedy();
    	
    	swap();
    	
		//2) Use 2-OPT heuristic algorithm to swap points around in the route to see if it produces a lower cost
    	twoOpt();
    }
    
    
    //MOVE FINDING METHOD
    
    //Method that finds valid moves for the drone to move along the optimised route
    private static void findMoves() {
    	
		//ArrayList to store the sequential points in the route
		route.add(startPoint);
		
		//ArrayList to store the sensors the drone still needs to visit and read
		unreadSensors = new ArrayList<Sensor>(sensorRoute);
		
		//Add the start point to 'unreadSensors' so our drone finishes at this point
		Sensor finishPoint = new Sensor();
		finishPoint.point = startPoint;
		finishPoint.location = "end";
		unreadSensors.add(finishPoint);
		

		//FIND MOVES FOR CHOSEN ROUTE
		
		//Continues to find points in our route while we have available moves and unread sensors
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
				
				//Adds location variable for paths which do not visit a sensor
				String location = "null";
				
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
				if (location == "end") {
					location = "null";
				}
				
				//Writing to our flight path text file
				flightpathTxt += (moves+1) + "," + currPoint.lng.toString() + "," + currPoint.lat.toString() + "," + String.valueOf(angle.intValue()) + "," + newP.lng.toString() + "," + newP.lat.toString() + "," + location + "\n";
						
				moves += 1;
				
				
			//Checks if the current point is not in range of the next point
			} else {
				Move move = findPoint(currPoint,nextSensor.point);
				Point newP = move.dest;
				Double angle = move.angle;
				
				route.add(newP);

				//Writing to our flight path text file
				flightpathTxt += (moves+1) + "," + currPoint.lng.toString() + "," + currPoint.lat.toString() + "," + String.valueOf(angle.intValue()) + "," + newP.lng.toString() + "," + newP.lat.toString() + ",null\n";
						
				moves += 1;
			}
		}
		
		//ADD FINAL FEATURES TO OUR GEO-JSON TEXT VARIABLE 'dataGeojson'
		
		//Add the unread sensors as gray markers to the Geo-JSON map
		if (unreadSensors.size() > 0) {
			dataGeojson += ",";
			for (int s = 0; s < unreadSensors.size(); s++) {
				Sensor unreadSensor = new Sensor(unreadSensors.get(s));
				
				if (unreadSensor.location != "end") {
					//Add Geo-JSON Point for each sensor
					dataGeojson += markerGeojson + unreadSensor.point.lng.toString() + ", " + unreadSensor.point.lat.toString() + "]},\n";
					dataGeojson += "\t\t\t\"properties\": {\"marker-size\": \"medium\", \"location\": \"" + unreadSensor.location  + "\", \"rgb-string\": \"#aaaaaa\", ";
					dataGeojson += "\"marker-color\": \"#aaaaaa\"}\n\t\t\t},";
				}
			}
		}
		
		//Add the route as a single LineString Geo-JSON feature
		dataGeojson += lineGeojson + "\n\t\t\t\t";
		for (int r = 0; r < route.size(); r++) {
			Point point = route.get(r);
			
			//Add a comma to appropriately separate points
			String comma = ",";
			if (r == route.size()-1) {
				comma = "";
			}
			
			dataGeojson += "[" + point.lng.toString() + ", " + point.lat.toString() + "]" + comma;
		}
		//Add the closing brackets to the Geo-JSON LineString Feature and FeatureCollection
		dataGeojson += "\n\t\t\t\t]\n\t\t\t},\"properties\":{\n\t\t}\n\t}\n\t\t\n\t]\n}";
    }
    
    
    //FILE OUTPUT METHODS:
    
    //Write to a file given the specified path and contents
    private static void writeToFile(String filePath, String fileContents) {
    	
        //Try write the code in the 'fileContents' String variable to the file path 'filPath'
        try {
        	FileWriter writer = new FileWriter(System.getProperty("user.dir") + filePath);
        	writer.write(fileContents);
        	writer.close();
        	//Success writing to file
        	System.out.println("\nFile for " + dateDD + "-" + dateMM + "-" + dateYY + " outputted successfully.\nFile path:   " + System.getProperty("user.dir") + filePath);
        	
        //Failure writing to file
        } catch (IOException e) {
        	System.out.println("FILE OUTPUT ERROR: unable to write to the file for " + dateDD +"-" + dateMM + "-" + dateYY + ". Attempted file path: " + System.getProperty("user.dir") + filePath);
        	System.exit(0);
        }
    }
    
    //Output our 'aqmaps' (.geojson) and 'flightpath' (.txt) files
    private static void writeOutputFiles() {
    	
    	//1) Output our 'aqmaps' Geo-JSON file
    	writeToFile("/readings-" + dateDD + "-" + dateMM + "-" + dateYY + ".geojson", dataGeojson);
    	
    	//2) Output our 'flightpath' text file
    	writeToFile("/flightpath-" + dateDD + "-" + dateMM + "-" + dateYY +".txt", flightpathTxt);
    }
    
    
    
    
    public static void main( String[] args ) throws IOException
    {    	
    	//SETUP
    	
    	//Storing command line arguments into appropriate variables
        dateDD = args[0];
        dateMM = args[1];
        dateYY = args[2];
        checkDate(dateDD,dateMM,dateYY); //Checks date inputs are valid and repairs any single digit inputs
        
        startPoint = new Point(Double.parseDouble(args[3]), Double.parseDouble(args[4]));
		randomSeed = checkIsNumber(args[5],"random seed");
        portNumber = String.valueOf(checkIsNumber(args[6],"port number"));
        
        ArrayList<Integer> monthDays = new ArrayList<Integer>(Arrays.asList(31,29,31,30,31,30,31,31,30,31,30,31));
        
    	//Initialise WebServer
        initWebserver();
        
        //GET THE NO-FLY-ZONE DATA
        getNoflyzoneData();
        
        String fileText ="";
        String dateText = "";
        
        for (int y = 0; y < 2; y++) {
    		if (y==1) {
    			monthDays.set(1,28);
    		}
        	for (int m = 0; m < 12; m++) {
        		for (int d = 0; d < monthDays.get(m); d++) {
        			
        			dateDD = String.valueOf(d+1);
        			dateMM = String.valueOf(m+1);
        			dateYY = String.valueOf(2020+y);
        			checkDate(String.valueOf(d+1),String.valueOf(m+1),String.valueOf(2020+y));
        			
        			
        	    	//GET THE SENSORS & AIR QUALITY DATA FOR THE GIVEN DATE
        	        getSensorData();

        	        
        	        //FIND OPTIMAL ROUTE (stored in 'sensorRoute' global variable)
        	        //findOptimalRoute();
        	        temperate();
        	        //greedy();
        	        swap();
        	        twoOpt();
        	        //swap();
        	        
        			//DELETE: CONFINEMENT AREA GEOJSON
        			//dataGeojson += "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Polygon\", \"coordinates\": [[";
        			//dataGeojson += "[" + maxLng + ", " + maxLat + "], [" + maxLng + ", " + minLat + "], [" + minLng + ", " + minLat + "], [" + minLng + ", " + maxLat + "]]]},\n\t\t";
        			//dataGeojson += "\"properties\": {\"fill-opacity\": 0}},";
        			
        			
        			//FIND DRONE MOVEMENTS (sequence of points stored in 'route' global variable)
        			findMoves();
        			
        			System.out.println(dateDD + "/" + dateMM + "/" + dateYY + ": " + String.valueOf(moves));
        			fileText += String.valueOf(moves) + "\n";
        			dateText += dateDD + "/" + dateMM + "/" + dateYY + "\n";
        			
        			route.clear();
        			sensorRoute.clear();
        			unreadSensors.clear();
        			sensors.clear();
        			lastMove = new Move();
        			moves = 0;
        			errorMargin = 0.0002;
        			dataGeojson="";
        			flightpathTxt="";
        		}
        	}
        }
        writeToFile("CSTMoves.txt",fileText);
        //writeToFile("Dates.txt",dateText);
    	//Initialise WebServer
        //initWebserver();

    	
    	//GET THE SENSORS & AIR QUALITY DATA FOR THE GIVEN DATE
        //getSensorData();
        
        
        //GET THE NO-FLY-ZONE DATA
        //getNoflyzoneData();

        
        //FIND OPTIMAL ROUTE (stored in 'sensorRoute' global variable)
        //findOptimalRoute();
        
        
		//DELETE: CONFINEMENT AREA GEOJSON
		//dataGeojson += "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Polygon\", \"coordinates\": [[";
		//dataGeojson += "[" + maxLng + ", " + maxLat + "], [" + maxLng + ", " + minLat + "], [" + minLng + ", " + minLat + "], [" + minLng + ", " + maxLat + "]]]},\n\t\t";
		//dataGeojson += "\"properties\": {\"fill-opacity\": 0}},";
		
		
		//FIND DRONE MOVEMENTS (sequence of points stored in 'route' global variable)
		//findMoves();
		
		
		//OUTPUT FILES AND PERFORMANCE DATA
		
		//Print performance of our drone for the given day
		//System.out.println("\nA drone route has been successfully found!");
		//System.out.println("# Moves: " + moves);
		//System.out.println("# Unread sensors: " + unreadSensors.size());
		//System.out.println("# Read sensors: " + sensorRoute.size());
		
		
		//Output our results to a 'aqmaps' and 'flightpath' file for the given date
		//writeOutputFiles();
    }
}