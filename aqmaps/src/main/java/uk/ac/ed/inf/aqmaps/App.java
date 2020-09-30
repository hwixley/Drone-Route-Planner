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
    
    //Custom Sensor object
    private static class Sensor {
    	String location;
    	Double battery;
    	Double reading;
    	Point swPoint;
    	Point nePoint;
    	
    	//Constructor created to clone objects effectively
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
    
    //Custom Point object
    private static class Point {
    	Double lat = -1.0;
    	Double lng = -1.0;
    }
	
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
        
        String mapsFilePath = "/home/hwixley/Documents/Year3/ILP/WebServer/maps/" + dateYY + "/" + dateMM + "/" + dateDD + "/air-quality-data.json";
        
        
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
        			sens.reading = Double.parseDouble(data);
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
        
        
        for (int i = 0; i < sensors.size(); i++) {
        	Sensor s = sensors.get(i);
        	
        	String w3w = s.location;
			String w1 = w3w.substring(0, w3w.indexOf("."));
			w3w = w3w.substring(w3w.indexOf(".") + 1);
			String w2 = w3w.substring(0, w3w.indexOf("."));
			String w3 = w3w.substring(w3w.indexOf(".") + 1);
			
			w3w = "/" + w1 + "/" + w2 + "/" + w3 + "/details.json";
			System.out.println(w3w);
        	
        	//Read the '/YYYY/MM/DD/details.json' file using BufferedReader
            //File mapsFile = new File(mapsFilePath);
    		//BufferedReader br = new BufferedReader(new FileReader(mapsFile));
        }
    }
}