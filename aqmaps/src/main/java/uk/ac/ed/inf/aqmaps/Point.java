package uk.ac.ed.inf.aqmaps;

public class Point {
	
	private Double lng;
	private Double lat;
	
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
	
	//Method that checks whether 2 points are equivalent (returns true if they are)
	public Boolean isEqual(Point pointA) {
		
		if (pointA.getLat() - lat == 0 && pointA.getLng() - lng == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public Double getLat() {
		return lat;
	}
	
	public Double getLng() {
		return lng;
	}
	
	public void setLat(Double lat) {
		this.lat = lat;
	}
	
	public void setLng(Double lng) {
		this.lng = lng;
	}
}