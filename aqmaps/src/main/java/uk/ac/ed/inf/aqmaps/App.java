package uk.ac.ed.inf.aqmaps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class App 
{
	//VARIABLES

	//Confinement area coordinates
    public static final double maxLat = 55.946233; 
    public static final double minLat = 55.942617;
    public static final double maxLng = -3.184319;
    public static final double minLng = -3.192473;
    
    //Constants
    public static double errorMargin = 0.0002; //Not left as final to cater for the 0.0003 error margin when returning to startPoint
    public static final double pathLength = 0.0003;
    
    //Global variables
    public static ArrayList<Building> buildings = new ArrayList<Building>();
    public static ArrayList<Sensor> sensors = new ArrayList<Sensor>();
    
    //Global argument variables
    public static String dateDD;
    public static String dateMM;
    public static String dateYY;
    public static Point startPoint;
    @SuppressWarnings("unused")
	private static int randomSeed;
    public static String portNumber;

    //Variable to store the optimised sensor route
	private static ArrayList<Sensor> sensorRoute = new ArrayList<Sensor>();

	
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
    
    //Retrieves the Sensor and air-quality data from the WebServer for the given date
    private static void getSensorData() {
    	
    	//1) Retrieve maps file from the WebServer (stored in 'mapsFile' global variable)
    	String mapsFile = Webserver.getWebServerFile("maps/" + dateYY + "/" + dateMM + "/" + dateDD + "/air-quality-data.json");
        
        //2) Parse this maps file into a list of Sensor objects (stored in 'sensors' global variable)
        App.sensors = FileReading.parseJsonSensors(mapsFile);
        
        //3) Get the given coordinates of the W3W location for each sensor (stored in 'sensors' global variable)
        App.sensors = FileReading.getSensorCoords(App.sensors);
    }

    
    //Get the no-fly-zone Geo-JSON data (stored in global 'buildings' variable)
    private static void getNoflyzoneData() {
    	
        //1) Retrieve files from the WebServer (stored in the 'noflyzoneFile' global variable)
    	String noflyzoneFile = Webserver.getWebServerFile("buildings/no-fly-zones.geojson");
        
        //2) Parse these files into appropriate java Building objects (stored in 'buildings' global variable)
        buildings = FileReading.parseNoflyzoneBuildings(noflyzoneFile);
    }
    
    
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
    
    
    //Output our 'aqmaps' (.geojson) and 'flightpath' (.txt) files
    private static void writeOutputFiles(Route route) {
    	
    	//1) Output our 'aqmaps' Geo-JSON file
    	FileWriting.writeToFile("/readings-" + dateDD + "-" + dateMM + "-" + dateYY + ".geojson", route.getGeojsonData());
    	
    	//2) Output our 'flightpath' text file
    	FileWriting.writeToFile("/flightpath-" + dateDD + "-" + dateMM + "-" + dateYY +".txt", route.getFlightData());
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
        Webserver.initWebserver();

    	
    	//GET THE SENSORS & AIR QUALITY DATA FOR THE GIVEN DATE (all sensors stored in global ArrayList of Sensor objects called 'sensors')
        getSensorData();
        
        
        //GET THE NO-FLY-ZONE DATA (all no-fly zones stored in global ArrayList of Building objects called 'buildings')
        getNoflyzoneData();

        
        //FIND OPTIMAL ROUTE (route stored in 'sensorRoute' global variable)
        findOptimalRoute();
		
		
		//FIND DRONE MOVEMENTS (sequence of points stored in 'route' global variable)
		Route route = new Route(App.sensorRoute);		
		
		//OUTPUT FILES AND PERFORMANCE DATA
		
		//Print performance of our drone for the given day
		System.out.println("\nA drone route has been successfully found!");
		System.out.println("# Moves: " + route.getMoves());
		System.out.println("# Unread sensors: " + (App.sensors.size()-1 - route.getNumberOfReadSensors()));
		System.out.println("# Read sensors: " + (route.getNumberOfReadSensors()));
		
		
		//Output our results to a 'aqmaps' and 'flightpath' file for the given date
		writeOutputFiles(route);
    }
}