package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

public class Objects {
	
    //OBJECT: Custom Point object
    public static class Point {
    	Double lat = -1.0;
    	Double lng = -1.0;
    	
    	//Constructor created to clone custom objects effectively
    	public Point(Point another) {
    		this.lat = another.lat;
    		this.lng = another.lng;
    	}
    	
    	public Point(Double lat, Double lng) {
    		this.lat = lat;
    		this.lng = lng;
    	}
    	
    	//Constructor with no arguments for default properties
    	public Point() {
    	}
    }
    
    //OBJECT: Custom Sensor object
    public static class Sensor {
    	String location;
    	Double battery;
    	Double reading;
    	Point point;
    	
    	//Constructor created to clone custom objects effectively
    	public Sensor(Sensor another) {
    		this.location = another.location;
    		this.battery = another.battery;
    		this.reading = another.reading;
    		this.point = another.point;
    	}
    	
    	//Constructor with no arguments for default properties
		public Sensor() {
		}
    }
	
    //OBJECT: Custom building(no-fly-zone) object
    public static class Building {
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
    
    //OBJECT: lineGraph custom object
    public static class LineGraph {
    	Double gradient;
    	Double yint;
    	Point p1;
    	Point p2;
    	
    	//Constructor with input
    	public LineGraph(Point p1, Point p2) {
    		this.gradient = (p1.lat - p2.lat)/(p1.lng - p2.lng);
    		this.yint = -gradient*p1.lng + p1.lat;
    		this.p1 = p1;
    		this.p2 = p2;
    	}
    	
    	//Constructor with angle input
    	public LineGraph(Double angle, Point origin) {
    		this.gradient = Math.tan(angle);
    		this.yint = -gradient*origin.lng + origin.lat;
    		this.p1 = origin;
    	}
    	
    	//Default constructor
    	public LineGraph() {
    	}
    }
    
    public static class Move {
    	Point origin;
    	Point dest;
    	Double angle;
    	
    	public Move() {
    	}
    }
}