package uk.ac.ed.inf.aqmaps;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;

public class App 
{
	//VARIABLES


	//Confinement area coordinates
    public static final double maxLat = 55.946233; 
    public static final double minLat = 55.942617;
    public static final double maxLng = -3.184319;
    public static final double minLng = -3.192473;
    
    //Constants
    private static double errorMargin = 0.0002; //Not left as final to cater for the 0.0003 error margin when returning to startPoint
    private static final double pathLength = 0.0003;
    
    //Global variables
    public static ArrayList<Building> buildings = new ArrayList<Building>();
    public static ArrayList<Sensor> sensors = new ArrayList<Sensor>();
    
    //Global argument variables
    private static String dateDD;
    private static String dateMM;
    private static String dateYY;
    public static Point startPoint;
    @SuppressWarnings("unused")
	private static int randomSeed;
    private static String portNumber;
    
    //Global WebServer variables
    private static String wsURL;
    private static final HttpClient client = HttpClient.newHttpClient();
    
    //Global output file strings
    private static String dataGeojson = "{\"type\": \"FeatureCollection\",\n\t\"features\"\t: [";
    private static String flightpathTxt = "";
    
    //findPoint method temporary variable
    private static Move lastMove = new Move();
    private static Point lastSensorPoint = new Point();
    
    //Variable to store the optimised sensor route
	private static ArrayList<Sensor> sensorRoute = new ArrayList<Sensor>();
    
	//Geo-JSON Feature syntax
	private static final String endFeatureCollectionGeojson = "\n\t\t\t\t]\n\t\t\t},\"properties\":{\n\t\t}\n\t}\n\t\t\n\t]\n}";
	private static final String startMarkerGeojson = "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\"\t: {\"type\": \"Point\", \"coordinates\": [";
	private static final String startLineStringGeojson = "\n\t{\"type\": \"Feature\",\n\t\t\t\"geometry\": {\"type\": \"LineString\",\n\t\t\t\t\"coordinates\": [\n\t\t\t\t";
	
	//Move finding variables
	private static int moves = 0;
	private static ArrayList<Point> route = new ArrayList<Point>();
	private static ArrayList<Sensor> unreadSensors = new ArrayList<Sensor>();
	
	
	
    //METHODS
	
	
	//INPUT ARGUMENT VALIDATION METHODS
	
	//Checks the date is valid and fixes any formatting issues (repairs single digit inputs)
	private static void checkDateIsValid(String day, String month, String year) {
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
		
		//Try to convert the input into a number
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
    private static Move findNextMove(Point currPoint, Point nextPoint) {
		Double angle = calcAngle(currPoint, nextPoint);
		Move move = new Move();
		move.setOrigin(currPoint);
		
		//Try floor and ceiling angles
		Double floorAngle = angle - (angle % 10);
		Double ceilAngle = floorAngle + 10;
		
		if (floorAngle == 350.0) {
			ceilAngle = 0.0;
		}
		
		Point floorPoint = new Point(transformPoint(currPoint,floorAngle));
		Point ceilPoint = new Point(transformPoint(currPoint,ceilAngle));
		
		//Iterate until valid floored angle point is found
		while (!isPathValid(currPoint, floorPoint)) {
			if (floorAngle == 0.0) {
				floorAngle = 350.0;
			} else {
				floorAngle -= 10.0;
			}

			floorPoint = new Point(transformPoint(currPoint, floorAngle));
		}
		
		//Iterate until valid ceilinged angle point is found
		while (!isPathValid(currPoint, ceilPoint)) {
			if (ceilAngle == 350.0) {
				ceilAngle = 0.0;
			} else {
				ceilAngle += 10.0;
			}

			ceilPoint = new Point(transformPoint(currPoint,ceilAngle));
		}

		//Calculate distances from the next sensor
		Double floorDist = calcDistance(nextPoint, floorPoint);
		Double ceilDist = calcDistance(nextPoint, ceilPoint);
		
		//Move variables for each angle option
		Move floorMove = new Move(floorAngle, floorPoint);
		Move ceilMove = new Move(ceilAngle, ceilPoint);
		
		//Check if the floored angle point is best and valid
		if ((floorDist < ceilDist) && (!isMoveRedundant(floorMove,nextPoint))) {
			move.setAngle(floorAngle);
			move.setDest(floorPoint);
		
		//Otherwise check if the ceilinged angle point is valid
		} else if (!isMoveRedundant(ceilMove,nextPoint)) {
			move.setAngle(ceilAngle);
			move.setDest(ceilPoint);
			
		//Otherwise use the floored angle point
		} else {
			move.setAngle(floorAngle);
			move.setDest(floorPoint);
		}
		
		lastMove = move;
		lastSensorPoint = new Point(nextPoint);
		return move;
    }
    
    
    //METHOD THAT CHECKS FOR REDUNDANT MOVES (indicates algorithm is stuck)
    
    //Checks if algorithm is stuck (checks if last and current moves are opposite) returns true if so
    private static Boolean isMoveRedundant(Move current, Point currSensorPoint) {
    	
    	//Returns false if no move has been made yet
    	if (lastMove.isNull()) {
    		return false;
    	} else {
    		
    		//Returns true if the last and current moves are opposite (angle difference of 180 degrees)
	    	if (((int)Math.abs(lastMove.getAngle() - current.getAngle()) == 180) && currSensorPoint.isEqual(lastSensorPoint)) {
	    		return true;
	    	} else {
	    		return false;
	    	}
    	}
    }
    
    
    //METHOD THAT CHECKS IF A POINT IS WITHIN RANGE OF THE DESTINATION SENSOR
    
    //Checks if valid point (within the range of a sensor)
    private static Boolean isPointInRange(Point destination, Point actual) {
    	
    	if (calcDistance(destination, actual) < errorMargin) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    
    //METHODS FOR CALCULATING ROUTE AND MOVE COST (used in route optimisation methods)
    
    //Calculate distance of route
	public static Double calcRouteCost(ArrayList<Sensor> sens) {
    	Double cost = 0.0;

    	ArrayList<Sensor> unreadSens = new ArrayList<Sensor>(sens);
    	ArrayList<Sensor> route = new ArrayList<Sensor>();
    	route.add(sens.get(0));
    	unreadSens.remove(0);
    	
    	//Iterates through the points in the route 
    	while (unreadSens.size() > 0) {
			cost += calcEdgeCost(route.get(route.size()-1).getPoint(),unreadSens.get(0).getPoint());
			
			route.add(unreadSens.get(0));
			unreadSens.remove(0);
    	}
    	//Adds last edge from the first and final sensor
    	cost += calcEdgeCost(route.get(route.size()-1).getPoint(),route.get(0).getPoint());
    	
    	return cost;
    }
	
	//Returns the estimated distance between two points
	public static Double calcEdgeCost(Point origin, Point dest) {
		Double dist = calcDistance(origin,dest);
		
		//If the path between adjacent points is not valid (intersects a building) we increase the added cost 
		if (!isPathValid(origin,dest)) {
			dist = calcActualDist(origin,dest);
		}
		
		return dist;
	}
	
	//Returns the sum of Euclidean distances for paths which pass through no fly zones (distance to go around the no fly zones)
    public static Double calcActualDist(Point origin, Point destination) {
    	Point currPoint = new Point(origin);
    	Double totDist = 0.0;
    	
    	//Iterates through moves until drone arrives at the sensor in order to calculate real distance
    	while (true) {
    		Double dist = calcDistance(currPoint, destination);
    		totDist += pathLength;
    		
    		//Checks if drone is in proximity of the specified sensor
    		if (dist < pathLength+errorMargin) {
    			break;
    		}
    		
    		//Find the next move
    		currPoint = new Point(findNextMove(currPoint, destination).getDest());
    	}
    	return totDist;
    }
	
	
	//METHODS FOR CHECKING FOR VALID MOVES (within confinement area and outside of no-fly-zone buildings)
	
	//Returns true if path is valid (within confinement and outside no-fly-zones)
	private static Boolean isPathValid(Point origin, Point dest) {
		
		if (dest.checkConfinement() && checkBuildings(origin, dest)) {
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
			for (int j=0; j < building.getPoints().size(); j++) {
				Point next = new Point();
				
				//Initialises value of next point
				if (j == building.getPoints().size()-1) {
					next = building.getPoints().get(0);
				} else {
					next = building.getPoints().get(j+1);
				}
				//Define the function for the given bound of the building
				LineGraph bound = new LineGraph(building.getPoints().get(j), next);
				
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
		Double netGrad = path.getGradient() - bound.getGradient();
		Double netYint = bound.getYint() - path.getYint();
		
		//Variables to define the bounds of latitude and longitude values for the given building boundary
		Double maxBoundLat = bound.getPoint1().getLat();
		Double minBoundLat = bound.getPoint2().getLat();
		Double maxBoundLng = bound.getPoint1().getLng();
		Double minBoundLng = bound.getPoint2().getLng();
		//Initialise bound boundary variables appropriately
		if (bound.getPoint2().getLat() > bound.getPoint1().getLat()) {
			maxBoundLat = bound.getPoint2().getLat();
			minBoundLat = bound.getPoint1().getLat();
		}
		if (bound.getPoint2().getLng() > bound.getPoint1().getLng()) {
			maxBoundLng = bound.getPoint2().getLng();
			minBoundLng = bound.getPoint1().getLng();
		}
		
		//Variables to define the bounds of latitude and longitude values for the given path
		Double maxPathLat = path.getPoint1().getLat();
		Double minPathLat = path.getPoint2().getLat();
		Double maxPathLng = path.getPoint1().getLng();
		Double minPathLng = path.getPoint2().getLng();
		//Initialise path boundary variables appropriately
		if (path.getPoint2().getLat() > path.getPoint1().getLat()) {
			maxPathLat = path.getPoint2().getLat();
			minPathLat = path.getPoint1().getLat();
		}
		if (path.getPoint2().getLng() > path.getPoint1().getLng()) {
			maxPathLng = path.getPoint2().getLng();
			minPathLng = path.getPoint1().getLng();
		}
		
		
		//Checks if the path is a vertical line (given when angle = 90/180)
		if ((path.getGradient() == Double.NEGATIVE_INFINITY) || (path.getGradient() == Double.POSITIVE_INFINITY)) {
			
			//Checks if the coordinates of the path is within the bounds of the given building boundary (meaning an intersection)
			if ((path.getPoint1().getLng() <= maxBoundLng) && (path.getPoint1().getLng() >= minBoundLng) && (minBoundLat <= maxPathLat) && (maxBoundLat >= minPathLat)) {
				return false;
			}
			
		//Checks if the bound is a vertical line
		} else if ((bound.getGradient() == Double.NEGATIVE_INFINITY) || (bound.getGradient() == Double.POSITIVE_INFINITY)) {
			
			//Checks if the coordinates of the bound is within the bounds of the given path (meaning an intersection)
			if ((bound.getPoint1().getLng() <= maxPathLng) && (bound.getPoint1().getLng() >= minPathLng) && (minPathLat <= maxBoundLat) && (maxPathLat >= minBoundLat)) {
				return false;
			}
			
		//Checks that the net gradient is not zero (meaning these lines are not parallel, thus an intersection at some point)
		} else if (netGrad != 0) {
			Double icLng = netYint/netGrad;
			Double icLat = path.getGradient()*icLng + path.getYint();
			
			//Checks whether the point of intersection is within the bounds of the given building boundary (meaning an intersection)
			if (((icLng <= maxBoundLng) && (icLng >= minBoundLng) && (icLng <= maxPathLng) && (icLng >= minPathLng)) || ((icLat <= maxBoundLat) && (icLat >= minBoundLat) && (icLat <= maxPathLat) && (icLat >= minPathLat))) {
				return false;
			}
		
		//Checks if these lines are the same and share points in the same bounds (meaning an intersection)
		} else if ((netYint == 0.0) && (minBoundLat <= maxPathLat) && (maxBoundLat >= minPathLat)) {
			return false;
		}
		
		return true;
	}
	
	
	//GEOMETRICAL CALCULATIONS
	
    //Calculates Euclidean distance between 2 points
    private static Double calcDistance(Point p1, Point p2) { 
    	Double lats = Math.pow(p1.getLat() - p2.getLat(),2);
    	Double lngs = Math.pow(p1.getLng() - p2.getLng(), 2);
    	
    	return Math.sqrt(lats + lngs);
    }
    
    //Calculates angle between 2 points
    private static Double calcAngle(Point origin, Point dest) {
    	Double grad = (dest.getLat() - origin.getLat())/(dest.getLng() - origin.getLng());
    	Double angle = Math.toDegrees(Math.atan(grad));
    	
    	if ((dest.getLng() > origin.getLng()) && (dest.getLat() < origin.getLat())) {
    		angle += 360;
    		
    	} else if ((dest.getLng() < origin.getLng()) && (dest.getLat() > origin.getLat())) {
    		angle += 180;

    	} else if ((dest.getLng() < origin.getLng()) && (dest.getLat() < origin.getLat())) {
    		angle += 180;
    	}
    	
    	return angle;
    }
    
    //Transform point (returns the transformed point by using the angle of the drone's movement)
    private static Point transformPoint(Point origin, Double angle) {
    	Point out = new Point(origin);
    	angle = Math.toRadians(angle);
    	
    	//Uses planar trigonometry to transform the current point given the angle of movement
    	out.setLat(out.getLat() + pathLength*Math.sin(angle));
    	out.setLng(out.getLng() + pathLength*Math.cos(angle));
    	
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
    private static ArrayList<Sensor> getSensorCoords(ArrayList<Sensor> inputSensors) {
    	
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
			String w3wFile = getWebServerFile("words/" + w3w);
            
            //2) Parse the W3W file and append the coordinate data to the appropriate sensor object
			s.setPoint(parseJsonW3Wtile(w3wFile));
        }
        return inputSensors;
    }
    
    //Retrieves the Sensor and air-quality data from the WebServer for the given date
    private static void getSensorData() {
    	
    	//1) Retrieve maps file from the WebServer (stored in 'mapsFile' global variable)
    	String mapsFile = getWebServerFile("maps/" + dateYY + "/" + dateMM + "/" + dateDD + "/air-quality-data.json");
        
        //2) Parse this maps file into a list of Sensor objects (stored in 'sensors' global variable)
        sensors = parseJsonSensors(mapsFile);
        
        //3) Get the given coordinates of the W3W location for each sensor (stored in 'sensors' global variable)
        sensors = getSensorCoords(sensors);
    }
    
    
    //RETRIEVING THE NO-FLY-ZONE DATA METHODS
    
    //Parses the no-fly-zones file as Building objects
    private static ArrayList<Building> parseNoflyzoneBuildings(String fileContents) {
    	
		ArrayList<Building> outputBuildings = new ArrayList<Building>();
		
		//Variables for iteration
		Building building = new Building();
		Point polyPoint = new Point();
		Boolean buildingComplete = false;
		ArrayList<Point> buildingVertices = new ArrayList<Point>();
		
		//Parsing points
		ArrayList<String> lngPrefix = new ArrayList<String>();
		lngPrefix.add(String.valueOf(maxLng).substring(0, String.valueOf(maxLng).indexOf(".")+1));
		lngPrefix.add(String.valueOf(minLng).substring(0, String.valueOf(minLng).indexOf(".")+1));
		
		ArrayList<String> latPrefix = new ArrayList<String>();
		latPrefix.add(String.valueOf(maxLat).substring(0, String.valueOf(maxLat).indexOf(".")+1));
		latPrefix.add(String.valueOf(minLat).substring(0, String.valueOf(minLat).indexOf(".")+1));

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
    
    //Get the no-fly-zone Geo-JSON data (stored in global 'buildings' variable)
    private static void getNoflyzoneData() {
    	
        //1) Retrieve files from the WebServer (stored in the 'noflyzoneFile' global variable)
    	String noflyzoneFile = getWebServerFile("buildings/no-fly-zones.geojson");
        
        //2) Parse these files into appropriate java Building objects (stored in 'buildings' global variable)
        buildings = parseNoflyzoneBuildings(noflyzoneFile);
    }
    
    
    //ROUTE OPTIMISATION METHODS
    
    //Method for finding the optimal route (route is stored in the global 'sensorRoute' variable)
    private static void findOptimalRoute() {
    	
    	//Adds the start point as a sensor so it can be accounted for in the route optimisation
		Sensor startPointSensor = new Sensor(startPoint);
		startPointSensor.setLocation("start");
		sensors.add(startPointSensor);
		
		//**NOTE: The numbered algorithms below can be swapped out for others if needed/wanted.
		
		//ALL ALGORITHM OPTIONS:
		//Initial route setting algorithms:  temperate(), greedy()
		//Route refinement algorithms:  twoOpt(App.sensorRoute), swap(App.sensorRoute)
		
    	//1) Use greedy algorithm to choose closest points
    	App.sensorRoute = Algorithms.greedy();
    	
		//2) Use 2-OPT heuristic algorithm to swap points around in the route to see if it produces a lower cost
    	App.sensorRoute = Algorithms.twoOpt(App.sensorRoute);
    }
    
    
    //WRITING GEOJSON FEATURES
    
    //Method that returns the Geo-JSON Point code for a sensor marker
    private static String getGeojsonMarker(Sensor sens, Boolean beenVisited) {
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
    private static String getGeojsonRoute(ArrayList<Point> points) {
    	String lineOutput = "";
    	
    	//Add the route as a single LineString Geo-JSON feature
		lineOutput += startLineStringGeojson;
		
		//Iterates through the points in our route
		for (int r = 0; r < points.size(); r++) {
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
    
    
    //MOVE FINDING METHOD
    
    //Method that finds valid moves for the drone to move along the optimised route
    private static void findMoves() {
    	
    	//1) Setup the route to be mapped and necessary variables
    	
		//Global ArrayList to store the sequential points in the route
		route.add(startPoint);

		//Remove sensor which represents the start/end point
		for (int s = 0; s < sensorRoute.size(); s++) {
			if (sensorRoute.get(s).getLocation() == "start") {

				if (s == sensorRoute.size()-1) {
					sensorRoute.remove(s);
					break;
				} else {
					ArrayList<Sensor> tempSensorRoute = new ArrayList<Sensor>();
					tempSensorRoute.addAll(sensorRoute.subList(s+1, sensorRoute.size()));
					tempSensorRoute.addAll(sensorRoute.subList(0, s));
					sensorRoute = tempSensorRoute;
				}
			}
		}
		
		//ArrayList to store the sensors the drone still needs to visit and read
		unreadSensors = new ArrayList<Sensor>(sensorRoute);
		
		//Add the start point to 'unreadSensors' so our drone finishes at this point
		Sensor finishPoint = new Sensor();
		finishPoint.setPoint(startPoint);
		finishPoint.setLocation("end");
		unreadSensors.add(finishPoint);
		

		//2) Find the moves for the chosen route
		
		//Continues to find points in our route while we have available moves and unread sensors
		while ((unreadSensors.size() > 0) && (moves < 150)) {
			Sensor nextSensor = new Sensor(unreadSensors.get(0));
			Point currPoint = new Point(route.get(route.size()-1));
			
			//Changes the error margin if the last Sensor represents the end point
			if (nextSensor.getLocation() == "end") {
				errorMargin = 0.0003;
			}
			
			//Variables to represent the given move
			Move move = findNextMove(currPoint,nextSensor.getPoint());
			Point newPoint = move.getDest();
			Double angle = move.getAngle();
			
			route.add(newPoint);
				
			//Adds location variable for paths which do not visit a sensor
			String location = "null";
			
			//Checks if point is in range of next sensor
			if (isPointInRange(nextSensor.getPoint(), newPoint)) {
				location = nextSensor.getLocation();
				unreadSensors.remove(0);
				
				//Checks it is not the end point
				if (location != "end") {
					//Adds Geo-JSON Point for the visited sensor
					dataGeojson += getGeojsonMarker(nextSensor, true);
				}
			}
			if (location == "end") {
				location = "null";
			}
			
			//Writing to our flight path text file
			flightpathTxt += (moves+1) + "," + currPoint.getLng().toString() + "," + currPoint.getLat().toString() + "," + String.valueOf(angle.intValue()) + "," + newPoint.getLng().toString() + "," + newPoint.getLat().toString() + "," + location + "\n";
					
			moves += 1;
		}
		
		//3) Add final features to our Geo-JSON string variable 'dataGeojson'
		
		//Add the unread sensors as gray markers to the Geo-JSON map
		if (unreadSensors.size() > 0) {
			for (int s = 0; s < unreadSensors.size(); s++) {
				Sensor unreadSensor = new Sensor(unreadSensors.get(s));
				
				//Checks it is not the end point
				if (unreadSensor.getLocation() != "end") {
					//Adds Geo-JSON Point for the unvisited sensor
					dataGeojson += getGeojsonMarker(unreadSensor, false);
				}
			}
		}
		
		//Add the route as a single LineString Geo-JSON feature
		dataGeojson += getGeojsonRoute(route);
		
		//Add the closing brackets to the Geo-JSON LineString Feature and FeatureCollection
		dataGeojson += endFeatureCollectionGeojson;
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
        checkDateIsValid(dateDD,dateMM,dateYY); //Checks date inputs are valid and repairs any single digit inputs
        
        startPoint = new Point(Double.parseDouble(args[3]), Double.parseDouble(args[4]));
		randomSeed = checkIsNumber(args[5],"random seed");
        portNumber = String.valueOf(checkIsNumber(args[6],"port number"));
        
        
    	//INITIALISE WEB SERVER (URL stored in global String 'wsURL')
        initWebserver();

    	
    	//GET THE SENSORS & AIR QUALITY DATA FOR THE GIVEN DATE (all sensors stored in global ArrayList of Sensor objects called 'sensors')
        getSensorData();
        
        
        //GET THE NO-FLY-ZONE DATA (all no-fly zones stored in global ArrayList of Building objects called 'buildings')
        getNoflyzoneData();

        
        //FIND OPTIMAL ROUTE (route stored in 'sensorRoute' global variable)
        findOptimalRoute();
		
		
		//FIND DRONE MOVEMENTS (sequence of points stored in 'route' global variable)
		findMoves();
		
		
		//OUTPUT FILES AND PERFORMANCE DATA
		
		//Print performance of our drone for the given day
		System.out.println("\nA drone route has been successfully found!");
		System.out.println("# Moves: " + moves);
		System.out.println("# Unread sensors: " + unreadSensors.size());
		System.out.println("# Read sensors: " + (sensors.size()-unreadSensors.size()-1));
		
		
		//Output our results to a 'aqmaps' and 'flightpath' file for the given date
		writeOutputFiles();
    }
}