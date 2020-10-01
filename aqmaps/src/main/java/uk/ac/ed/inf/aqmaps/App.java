package uk.ac.ed.inf.aqmaps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.mapbox.geojson.*;

public class App 
{
	//Confinement area coordinates
    private static final double maxLat = 55.946233; 
    private static final double minLat = 55.942617;
    private static final double maxLng = -3.184319;
    private static final double minLng = -3.192473;
    
    //*TEMPORARY; local webserver directory path
    private static final String wsPath = "/home/hwixley/Documents/Year3/ILP/WebServer/";
    
    //OBJECT: Custom Sensor object
    private static class Sensor {
    	String location;
    	Double battery;
    	Double reading;
    	Point swPoint;
    	Point nePoint;
    	
    	//Constructor created to clone custom objects effectively
    	public Sensor(Sensor another) {
    		this.location = another.location;
    		this.battery = another.battery;
    		this.reading = another.reading;
    		this.swPoint = another.swPoint;
    		this.nePoint = another.nePoint;
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
        
        String mapsFilePath = wsPath + "maps/" + dateYY + "/" + dateMM + "/" + dateDD + "/air-quality-data.json";
        
        
    	//Read the '/YYYY/MM/DD/air-quality-data.json' file using BufferedReader
        File mapsFile = new File(mapsFilePath);
		BufferedReader br = new BufferedReader(new FileReader(mapsFile));
        
        //Create ArrayList to store the data for the 33 sensors from the '/YYYY/MM/DD/air-quality-data.json' file
        ArrayList<Sensor> sensors = new ArrayList<Sensor>();
        
        
        //Iterate through the lines of the '/YYYY/MM/DD/air-quality-data.json' file and store them as Sensors in the 'sensors' ArrayList
        String line;
        Boolean newSensor = true;
        Integer sensorIndex = 0;
        Sensor sens = new Sensor();
        while ((line = br.readLine()) != null) {
        	
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
        //Close the buffered reader
        br.close();
        
        
        //Get swPoint and nePoint for the given w3w location
        for (int i = 0; i < sensors.size(); i++) {
        	Sensor s = sensors.get(i);
        	
        	String w3w = s.location;
			String w1 = w3w.substring(0, w3w.indexOf("."));
			w3w = w3w.substring(w3w.indexOf(".") + 1);
			String w2 = w3w.substring(0, w3w.indexOf("."));
			String w3 = w3w.substring(w3w.indexOf(".") + 1);
			
			w3w = w1 + "/" + w2 + "/" + w3 + "/details.json";
        	
        	//Read the '/words/w1/w2/w3/details.json' file using BufferedReader
            File w3wFile = new File(wsPath + "words/" + w3w);
    		BufferedReader br2 = new BufferedReader(new FileReader(w3wFile));
    		
    		//Loop through file
    		String w3wLine;
    		Point point = new Point();
    		Integer stage = -20;
    		while ((w3wLine = br2.readLine()) != null) {
    			
    			if (w3wLine.indexOf("southwest") != -1) {
    				stage = 1;
    			} else if (w3wLine.indexOf("northeast") != -1) {
    				stage = 4;
    			}
    			
    			//Parse the latitude and longitude values into doubles, and pass these into our 'point' object
    			if ((stage == 2) || (stage == 5)) {
    				point.lng = Double.parseDouble(w3wLine.substring(w3wLine.indexOf(":") + 1, w3wLine.length() - 1));
    			} else if ((stage == 3) || (stage == 6)) {
    				point.lat = Double.parseDouble(w3wLine.substring(w3wLine.indexOf(":") + 1, w3wLine.length()));
    			}
    			
    			//Pass the given Point object 'point' to the Sensor object 's'
    			if (stage == 3) {
    				s.swPoint = new Point(point);
    			} else if (stage == 6) {
    				s.nePoint = new Point(point);
    			}
    			
    			stage += 1;
    		}
    		//Close the buffered reader
    		br2.close();
        }
        
        
		//Parse the no fly zone data
		File noflyzoneFilePath = new File(wsPath + "buildings/no-fly-zones.geojson");
		BufferedReader br3 = new BufferedReader(new FileReader(noflyzoneFilePath));
		
		//ArrayList to store building polygons
		ArrayList<Building> buildings = new ArrayList<Building>();
		
		
		//Iterate through the '/buildings/no-fly-zones.geojson' file
		String buildingsLine;
		Building building = new Building();
		Point polyPoint = new Point();
		Boolean buildingComplete = false;
		while ((buildingsLine = br3.readLine()) != null) {
			
			//Check if line contains name property
			if (buildingsLine.indexOf("name") != -1) {
				building.name = buildingsLine.substring(buildingsLine.indexOf(":") + 3, buildingsLine.length() - 2);
				buildingComplete = false;
				building.points = new ArrayList<Point>();
			
			//Check if line contains fill property
			} else if (buildingsLine.indexOf("fill") != -1) {
				building.fill = buildingsLine.substring(buildingsLine.indexOf(":") + 3, buildingsLine.length() - 1);
			
			//Check if line contains longitude
			} else if ((buildingsLine.indexOf("-3.") != -1)) {
				polyPoint.lng = Double.parseDouble(buildingsLine.substring(buildingsLine.indexOf("-"), buildingsLine.length() -1));
				
			//Check if line contains latitude
			} else if (buildingsLine.indexOf("55.") != -1) {
				polyPoint.lat = Double.parseDouble(buildingsLine.substring(buildingsLine.indexOf("55."), buildingsLine.length()));
				building.points.add(new Point(polyPoint));
			
			//Check if line contains a closing square bracket (indicates end of a given polygon)
			} else if ((buildingsLine.indexOf("]") != -1) && (buildingsLine.indexOf("],") == -1) && !buildingComplete) {
				buildings.add(new Building(building));
				buildingComplete = true;
			}
		}
		//Close the BufferedReader
		br3.close();
		
		for (int k = 0; k < buildings.size(); k++) {
			System.out.println(buildings.get(k).name);
			System.out.println(buildings.get(k).points.get(0).lng);
		}
		
		
    }
}