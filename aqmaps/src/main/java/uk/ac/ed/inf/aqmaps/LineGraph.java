package uk.ac.ed.inf.aqmaps;

/**
 * Custom class used to represent a straight line function
 * 
 * By using coordinate geometry we can use this in order to 
 * see if a given drone path crosses a no-fly zone boundary.
 */

public class LineGraph {
	
	private Double gradient;
	private Double yint;
	private Point p1;
	private Point p2;
	
	//Constructor with point inputs
	public LineGraph(Point p1, Point p2) {
		this.gradient = (p1.getLat() - p2.getLat())/(p1.getLng() - p2.getLng());
		this.yint = -gradient*p1.getLng() + p1.getLat();
		this.p1 = p1;
		this.p2 = p2;
	}
	
	//Constructor with angle and point input
	public LineGraph(Double angle, Point origin) {
		this.gradient = Math.tan(angle);
		this.yint = -gradient*origin.getLng() + origin.getLat();
		this.p1 = origin;
	}
	
	//Default constructor
	public LineGraph() {
	}
	
	//GETTERS
	
	public Double getGradient() {
		return gradient;
	}
	
	public Double getYint() {
		return yint;
	}
	
	public Point getPoint1() {
		return p1;
	}
	
	public Point getPoint2() {
		return p2;
	}
}