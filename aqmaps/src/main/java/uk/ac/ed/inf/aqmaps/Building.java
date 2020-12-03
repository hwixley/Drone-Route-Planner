package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

public class Building {
	
	private ArrayList<Point> points = new ArrayList<Point>();
	
	//Constructor created to clone custom objects effectively
	public Building(Building another) {
		this.points = another.points;
	}
	
	//Constructor with no arguments for default properties
	public Building() {
	}
	
	//GETTERS
	
	public ArrayList<Point> getPoints() {
		return points;
	}
	
	//SETTERS
	
	public void setPoints(ArrayList<Point> points) {
		this.points = points;
	}
}