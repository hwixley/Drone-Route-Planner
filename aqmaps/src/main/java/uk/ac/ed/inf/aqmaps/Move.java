package uk.ac.ed.inf.aqmaps;

public class Move {
	
	private Point origin;
	private Point dest;
	private Double angle;
	
	//Default constructor
	public Move() {
	}
	
	//Constructor with move variable
	public Move(Move move) {
		this.origin = move.origin;
		this.dest = move.dest;
		this.angle = move.angle;
	}
	
	//Constructor with angle and destination Point variables
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
	public Boolean isNull() {
		if (origin == null && dest == null && angle == null) {
			return true;
		} else {
			return false;
		}
	}
	
	//GETTERS
	
	public Point getOrigin() {
		return origin;
	}
	
	public Point getDest() {
		return dest;
	}
	
	public Double getAngle() {
		return angle;
	}
	
	//SETTERS
	
	public void setOrigin(Point origin) {
		this.origin = origin;
	}
	
	public void setDest(Point dest) {
		this.dest = dest;
	}
	
	public void setAngle(Double angle) {
		this.angle = angle;
	}
	
	
}