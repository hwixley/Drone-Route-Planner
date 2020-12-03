package uk.ac.ed.inf.aqmaps;

/**
 * Custom class used to represent a given drone route
 */

import java.util.ArrayList;

public class Route {
	
	//Route variables
	private ArrayList<Sensor> sensorRoute = new ArrayList<Sensor>();
	private ArrayList<Point> pointRoute = new ArrayList<Point>();
	private ArrayList<Sensor> unreadSensors = new ArrayList<Sensor>();
	private int moves = 0;
	
    //Strings to store the output file data
    private String dataGeojson = "{\"type\": \"FeatureCollection\",\n\t\"features\"\t: [";
    private String flightpathTxt = "";
	
    
	//CONSTRUCTOR
	public Route(ArrayList<Sensor> sensorRoute) {
		this.sensorRoute = sensorRoute;
		findMoves();
	}
	
	
    //MOVE FINDING METHOD
	
    //Method that finds valid moves for the drone to move along the optimised route
    public void findMoves() {
    	
    	//1) Setup the route to be mapped and necessary variables
    	
		//Global ArrayList to store the sequential points in the route
		pointRoute.add(App.startPoint);

		//Remove sensor which represents the start/end point
		for (int s = 0; s < sensorRoute.size(); s++) {
			if (sensorRoute.get(s).getLocation() == "start" || sensorRoute.get(s).getReading() == null) {

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
		finishPoint.setPoint(App.startPoint);
		finishPoint.setLocation("end");
		unreadSensors.add(finishPoint);
		

		//2) Find the moves for the chosen route
		
		//Continues to find points in our route while we have available moves and unread sensors
		while ((unreadSensors.size() > 0) && (moves < 150)) {
			Sensor nextSensor = new Sensor(unreadSensors.get(0));
			Point currPoint = new Point(pointRoute.get(pointRoute.size()-1));
			
			//Changes the error margin if the last Sensor represents the end point
			if (nextSensor.getLocation() == "end") {
				App.errorMargin = 0.0003;
			}
			
			//Variables to represent the given move
			Move move = MoveCalcs.findNextMove(currPoint,nextSensor.getPoint());
			Point newPoint = move.getDest();
			Double angle = move.getAngle();
			
			pointRoute.add(newPoint);
				
			//Adds location variable for paths which do not visit a sensor
			String location = "null";
			
			//Checks if point is in range of next sensor
			if (MoveCalcs.isPointInRange(nextSensor.getPoint(), newPoint)) {
				location = nextSensor.getLocation();
				unreadSensors.remove(0);
				
				//Checks it is not the end point
				if (location != "end") {
					//Adds Geo-JSON Point for the visited sensor
					dataGeojson += FileWriting.getGeojsonMarker(nextSensor, true);
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
					dataGeojson += FileWriting.getGeojsonMarker(unreadSensor, false);
				}
			}
		}
		
		//Add the route as a single LineString Geo-JSON feature
		dataGeojson += FileWriting.getGeojsonRoute(pointRoute);
		
		//Add the closing brackets to the Geo-JSON LineString Feature and FeatureCollection
		dataGeojson += FileWriting.getGeojsonFeatureCollectionSuffix();
    }
    
    
    //FILE OUTPUTTING METHOD
    
    //Output our 'aqmaps' (.geojson) and 'flightpath' (.txt) files
    public void writeOutputFiles() {
    	
    	//1) Output our 'aqmaps' Geo-JSON file
    	FileWriting.writeToFile("/readings-" + App.dateDD + "-" + App.dateMM + "-" + App.dateYY + ".geojson", dataGeojson);
    	
    	//2) Output our 'flightpath' text file
    	FileWriting.writeToFile("/flightpath-" + App.dateDD + "-" + App.dateMM + "-" + App.dateYY +".txt", flightpathTxt);
    }
    
    
    //GETTERS
    
    public int getMoves() {
    	return moves;
    }
    
    public int getNumberOfReadSensors() {
    	return sensorRoute.size() - unreadSensors.size();
    }
    
    public void addData(String data) {
    	dataGeojson += data;
    }
}