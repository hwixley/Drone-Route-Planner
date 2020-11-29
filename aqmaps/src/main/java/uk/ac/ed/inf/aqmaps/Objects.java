package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

public class Objects {
	
    //OBJECT: custom Point object
    public static class Point {
    	Double lat = -1.0;
    	Double lng = -1.0;
    	
    	//Constructor created to clone custom objects effectively
    	public Point(Point another) {
    		this.lat = another.lat;
    		this.lng = another.lng;
    	}
    	
    	//Constructor with all variable arguments
    	public Point(Double lat, Double lng) {
    		this.lat = lat;
    		this.lng = lng;
    	}
    	
    	//Constructor with no arguments for default properties
    	public Point() {
    	}
    	
    	public static Boolean isEqual(Point pointA, Point pointB) {
    		if (pointA.lat == pointB.lat && pointA.lng == pointB.lng) {
    			return true;
    		} else {
    			return false;
    		}
    	}
    }
    
    //OBJECT: custom Sensor object (inherits Point object features)
    public static class Sensor extends Point {
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
	
    //OBJECT: custom Building(no-fly-zone) object
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
    
    //OBJECT: custom LineGraph object
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
    
    //OBJECT: custom Move object
    public static class Move {
    	Point origin;
    	Point dest;
    	Double angle;
    	
    	//Default constructor
    	public Move() {
    	}
    	
    	//Constructor with move variable
    	public Move(Move move) {
    		this.origin = move.origin;
    		this.dest = move.dest;
    		this.angle = move.angle;
    	}
    	
    	public Move(Double angle, Point dest) {
    		this.angle = angle;
    		this.dest = dest;
    	}
    	
    	//Constructor with all variable arguments
    	public Move(Point origin, Point dest, Double angle) {
    		this.origin = origin;
    		this.dest = dest;
    		this.angle = angle;
    	}
    	
    	//Method that checks whether a given Move object instance is null
    	public static Boolean isNull(Move move) {
    		if ((move.origin == null) && (move.dest == null) && (move.angle == null)) {
    			return true;
    		} else {
    			return false;
    		}
    	}
    }
    
    public static class Fragment {
    	Sensor sensor;
    	Double avgDist;
    	Sensor bestDestSensor;
    	
    	public Fragment() {
    	}
    	
    	public Fragment(Sensor sensor, Double avgDist) {
    		this.sensor = sensor;
    		this.avgDist = avgDist;
    	}
    }
}