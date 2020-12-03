package uk.ac.ed.inf.aqmaps;

/**
 * Custom class used to represent geographical coordinates
 */

public class Point {
	
	private Double lng;
	private Double lat;
	
	
	//CONSTRUCTORS
	
	public Point(Point another) {
		this.lat = another.lat;
		this.lng = another.lng;
	}
	
	public Point(Double lat, Double lng) {
		this.lat = lat;
		this.lng = lng;
	}
	
	public Point() {
	}
	
	
	//METHODS
	
	//Method that checks whether 2 points are equivalent (returns true if they are)
	public Boolean isEqual(Point pointA) {
		
		if (pointA.getLat() - lat == 0 && pointA.getLng() - lng == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	//Returns true if point is in confinement area
	public Boolean checkConfinement() {
		if ((lat < App.maxLat) && (lat > App.minLat) && (lng < App.maxLng) && (lng > App.minLng)) {
			return true;
		} else {
			return false;
		}
	}
	
	
	//GETTERS
	
	public Double getLat() {
		return lat;
	}
	
	public Double getLng() {
		return lng;
	}
	
	
	//SETTERS
	
	public void setLat(Double lat) {
		this.lat = lat;
	}
	
	public void setLng(Double lng) {
		this.lng = lng;
	}
}