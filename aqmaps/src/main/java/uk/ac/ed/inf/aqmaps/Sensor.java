package uk.ac.ed.inf.aqmaps;

/**
 * A custom class used to represent an air-quality sensor
 */

public class Sensor {
	
	private String location;
	private Double battery;
	private Double reading;
	private Point point;
	
	//CONSTRUCTORS
	
	//Constructor created to clone custom objects effectively
	public Sensor(Sensor another) {
		this.location = another.location;
		this.battery = another.battery;
		this.reading = another.reading;
		this.point = another.point;
	}
	
	//Constructor for single Point argument
	public Sensor(Point point) {
		this.point = point;
	}
	
	//Constructor with no arguments for default properties
	public Sensor() {
	}
	
	//AIR-QUALITY CLASSIFICATION METHODS
	
    //Returns the appropriate colour for a given air quality reading
	public String getReadingColour() {
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
	public String getReadingSymbol() {
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
	
	//GETTERS
	
	public String getLocation() {
		return location;
	}
	
	public Double getBattery() {
		return battery;
	}
	
	public Double getReading() {
		return reading;
	}
	
	public Point getPoint() {
		return point;
	}
	
	//SETTERS
	
	public void setLocation(String location) {
		this.location = location;
	}
	
	public void setBattery(Double battery) {
		this.battery = battery;
	}
	
	public void setReading(Double reading) {
		this.reading = reading;
	}
	
	public void setPoint(Point point) {
		this.point = point;
	}
}