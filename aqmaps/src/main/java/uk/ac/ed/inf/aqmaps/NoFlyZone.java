package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

/**
 * Custom class used to represent a no-fly zone
 */

public class NoFlyZone {
	
	//Variable to store the vertices of the given no-fly zone
	private ArrayList<Point> points = new ArrayList<Point>();
	
	
	//CONSTRUCTORS
	
	//Constructor created to clone custom objects effectively
	public NoFlyZone(NoFlyZone another) {
		this.points = another.points;
	}
	
	//Constructor with no arguments for default properties
	public NoFlyZone() {
	}
	
	
	//GETTER
	public ArrayList<Point> getPoints() {
		return points;
	}
	
	
	//SETTER
	public void setPoints(ArrayList<Point> points) {
		this.points = points;
	}
}