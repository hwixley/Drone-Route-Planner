package uk.ac.ed.inf.aqmaps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

import com.mapbox.geojson.*;

public class App 
{
	//Confinement area coordinates
    private static final double maxLat = 55.946233; 
    private static final double minLat = 55.942617;
    private static final double maxLng = -3.184319;
    private static final double minLng = -3.192473;
    
    //Sensor object
    private class Sensor {
    	String location;
    	int battery;
    	Double reading;
    	Double lat = -1.0;
    	Double lng = -1.0;
    }
	
    public static void main( String[] args )
    {
    	//Storing command line arguments into appropriate variables
        String dateDD = args[0];
        String dateMM = args[1];
        String dateYY = args[2];
        Double startLat = Double.parseDouble(args[3]);
        Double startLng = Double.parseDouble(args[4]);
        int randomSeed = Integer.parseInt(args[5]);
        String portNumber  = args[6];
        
        String mapsFilePath = "~/Documents/Year3/ILP/WebServer/maps/" + dateYY + "/" + dateMM + "/" + dateDD + "/air-quality-data.json";
        
        
    	//Read the '/YYYY/MM/DD/air-quality-data.json' file using BufferedReader
        File mapsFile = new File(mapsFilePath);
        try {
			BufferedReader br = new BufferedReader(new FileReader(mapsFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        //Create ArrayLists to store the 'air-quality-data.json' data
        ArrayList<Sensor> sensors = new ArrayList<Sensor>();
        
        
        
    }
}