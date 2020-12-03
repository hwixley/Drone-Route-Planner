package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

/**
 * Custom class used to store all single move calculation methods.
 */

public class MoveCalcs {
	
    //findNextMove method temporary variables
    private static Move lastMove = new Move();
    private static Point lastSensorPoint = new Point();
	
    
	//FIND NEXT POINT METHOD 
    
    //Find next valid point to move to given the current and destination sensors
    public static Move findNextMove(Point currPoint, Point nextPoint) {
		Double angle = GeometricalCalcs.calcAngle(currPoint, nextPoint);
		Move move = new Move();
		move.setOrigin(currPoint);
		
		//Try floor and ceiling angles
		Double floorAngle = angle - (angle % 10);
		Double ceilAngle = floorAngle + 10;
		
		if (floorAngle == 350.0) {
			ceilAngle = 0.0;
		}
		
		Point floorPoint = new Point(GeometricalCalcs.transformPoint(currPoint,floorAngle));
		Point ceilPoint = new Point(GeometricalCalcs.transformPoint(currPoint,ceilAngle));
		
		//Iterate until valid floored angle point is found
		while (!isPathValid(currPoint, floorPoint)) {
			if (floorAngle == 0.0) {
				floorAngle = 350.0;
			} else {
				floorAngle -= 10.0;
			}

			floorPoint = new Point(GeometricalCalcs.transformPoint(currPoint, floorAngle));
		}
		
		//Iterate until valid ceilinged angle point is found
		while (!isPathValid(currPoint, ceilPoint)) {
			if (ceilAngle == 350.0) {
				ceilAngle = 0.0;
			} else {
				ceilAngle += 10.0;
			}

			ceilPoint = new Point(GeometricalCalcs.transformPoint(currPoint,ceilAngle));
		}

		//Calculate distances from the next sensor
		Double floorDist = GeometricalCalcs.calcDistance(nextPoint, floorPoint);
		Double ceilDist = GeometricalCalcs.calcDistance(nextPoint, ceilPoint);
		
		//Move variables for each angle option
		Move floorMove = new Move(floorAngle, floorPoint);
		Move ceilMove = new Move(ceilAngle, ceilPoint);
		
		//Check if the floored angle point is best and valid
		if ((floorDist < ceilDist) && (!isMoveRedundant(floorMove,nextPoint))) {
			move.setAngle(floorAngle);
			move.setDest(floorPoint);
		
		//Otherwise check if the ceilinged angle point is valid
		} else if (!isMoveRedundant(ceilMove,nextPoint)) {
			move.setAngle(ceilAngle);
			move.setDest(ceilPoint);
			
		//Otherwise use the floored angle point
		} else {
			move.setAngle(floorAngle);
			move.setDest(floorPoint);
		}
		
		lastMove = move;
		lastSensorPoint = new Point(nextPoint);
		return move;
    }
    
    
    //METHOD THAT CHECKS FOR REDUNDANT MOVES (indicates algorithm is stuck)
    
    //Checks if algorithm is stuck (checks if last and current moves are opposite) returns true if so
    private static Boolean isMoveRedundant(Move current, Point currSensorPoint) {
    	
    	//Returns false if no move has been made yet
    	if (lastMove.isNull()) {
    		return false;
    	} else {
    		
    		//Returns true if the last and current moves are opposite (angle difference of 180 degrees)
	    	if (((int)Math.abs(lastMove.getAngle() - current.getAngle()) == 180) && currSensorPoint.isEqual(lastSensorPoint)) {
	    		return true;
	    	} else {
	    		return false;
	    	}
    	}
    }
    
    
    //METHOD THAT CHECKS IF A POINT IS WITHIN RANGE OF THE DESTINATION SENSOR
    
    //Checks if valid point (within the range of a sensor)
    public static Boolean isPointInRange(Point destination, Point actual) {
    	
    	if (GeometricalCalcs.calcDistance(destination, actual) < App.errorMargin) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    
    //METHODS FOR CALCULATING ROUTE AND MOVE COST (used in route optimisation methods)
    
    //Calculate distance of route
	public static Double calcRouteCost(ArrayList<Sensor> sens) {
    	Double cost = 0.0;

    	ArrayList<Sensor> unreadSens = new ArrayList<Sensor>(sens);
    	ArrayList<Sensor> route = new ArrayList<Sensor>();
    	route.add(sens.get(0));
    	unreadSens.remove(0);
    	
    	//Iterates through the points in the route 
    	while (unreadSens.size() > 0) {
			cost += calcEdgeCost(route.get(route.size()-1).getPoint(),unreadSens.get(0).getPoint());
			
			route.add(unreadSens.get(0));
			unreadSens.remove(0);
    	}
    	//Adds last edge from the first and final sensor
    	cost += calcEdgeCost(route.get(route.size()-1).getPoint(),route.get(0).getPoint());
    	
    	return cost;
    }
	
	//Returns the estimated distance between two points
	public static Double calcEdgeCost(Point origin, Point dest) {
		Double dist = GeometricalCalcs.calcDistance(origin,dest);
		
		//If the path between adjacent points is not valid (intersects a building) we increase the added cost 
		if (!isPathValid(origin,dest)) {
			dist = calcActualDist(origin,dest);
		}
		
		return dist;
	}
	
	//Returns the sum of Euclidean distances for paths which pass through no fly zones (distance to go around the no fly zones)
    public static Double calcActualDist(Point origin, Point destination) {
    	Point currPoint = new Point(origin);
    	Double totDist = 0.0;
    	
    	//Iterates through moves until drone arrives at the sensor in order to calculate real distance
    	while (true) {
    		Double dist = GeometricalCalcs.calcDistance(currPoint, destination);
    		totDist += App.pathLength;
    		
    		//Checks if drone is in proximity of the specified sensor
    		if (dist < App.pathLength+App.errorMargin) {
    			break;
    		}
    		
    		//Find the next move
    		currPoint = new Point(findNextMove(currPoint, destination).getDest());
    	}
    	return totDist;
    }
	
	
	//METHODS FOR CHECKING FOR VALID MOVES (within confinement area and outside of no-fly-zone buildings)
	
	//Returns true if path is valid (within confinement and outside no-fly-zones)
	private static Boolean isPathValid(Point origin, Point dest) {
		
		if (dest.checkConfinement() && checkNoFlyZones(origin, dest)) {
			return true;
		} else {
			return false;
		}
	}
	
	//Returns true if path between p1 and p2 does not pass through any buildings
	private static Boolean checkNoFlyZones(Point p1, Point p2) {
		
		//Define the function for our given path
		LineGraph path = new LineGraph(p1,p2);
		
		//Iterates through the no-fly-zone buildings
		for (int i = 0; i < App.noFlyZones.size(); i++) {
			NoFlyZone zone = new NoFlyZone(App.noFlyZones.get(i));
			
			//Iterates through the bounds of a given building
			for (int j=0; j < zone.getPoints().size(); j++) {
				Point next = new Point();
				
				//Initialises value of next point
				if (j == zone.getPoints().size()-1) {
					next = zone.getPoints().get(0);
				} else {
					next = zone.getPoints().get(j+1);
				}
				//Define the function for the given bound of the building
				LineGraph bound = new LineGraph(zone.getPoints().get(j), next);
				
				//Checks if the path intersects the given bound (if so then returns false)
				if (!checkBound(path,bound)) {
					return false;
				}
			}
				
		}
		return true;
	}
	
	//Returns True if these lines do not intersect
	private static Boolean checkBound(LineGraph path, LineGraph bound) {
		
		//Variables to determine point of intersection between the functions
		Double netGrad = path.getGradient() - bound.getGradient();
		Double netYint = bound.getYint() - path.getYint();
		
		//Variables to define the bounds of latitude and longitude values for the given building boundary
		Double maxBoundLat = bound.getPoint1().getLat();
		Double minBoundLat = bound.getPoint2().getLat();
		Double maxBoundLng = bound.getPoint1().getLng();
		Double minBoundLng = bound.getPoint2().getLng();
		//Initialise bound boundary variables appropriately
		if (bound.getPoint2().getLat() > bound.getPoint1().getLat()) {
			maxBoundLat = bound.getPoint2().getLat();
			minBoundLat = bound.getPoint1().getLat();
		}
		if (bound.getPoint2().getLng() > bound.getPoint1().getLng()) {
			maxBoundLng = bound.getPoint2().getLng();
			minBoundLng = bound.getPoint1().getLng();
		}
		
		//Variables to define the bounds of latitude and longitude values for the given path
		Double maxPathLat = path.getPoint1().getLat();
		Double minPathLat = path.getPoint2().getLat();
		Double maxPathLng = path.getPoint1().getLng();
		Double minPathLng = path.getPoint2().getLng();
		//Initialise path boundary variables appropriately
		if (path.getPoint2().getLat() > path.getPoint1().getLat()) {
			maxPathLat = path.getPoint2().getLat();
			minPathLat = path.getPoint1().getLat();
		}
		if (path.getPoint2().getLng() > path.getPoint1().getLng()) {
			maxPathLng = path.getPoint2().getLng();
			minPathLng = path.getPoint1().getLng();
		}
		
		
		//Checks if the path is a vertical line (given when angle = 90/180)
		if ((path.getGradient() == Double.NEGATIVE_INFINITY) || (path.getGradient() == Double.POSITIVE_INFINITY)) {
			
			//Checks if the coordinates of the path is within the bounds of the given building boundary (meaning an intersection)
			if ((path.getPoint1().getLng() <= maxBoundLng) && (path.getPoint1().getLng() >= minBoundLng) && (minBoundLat <= maxPathLat) && (maxBoundLat >= minPathLat)) {
				return false;
			}
			
		//Checks if the bound is a vertical line
		} else if ((bound.getGradient() == Double.NEGATIVE_INFINITY) || (bound.getGradient() == Double.POSITIVE_INFINITY)) {
			
			//Checks if the coordinates of the bound is within the bounds of the given path (meaning an intersection)
			if ((bound.getPoint1().getLng() <= maxPathLng) && (bound.getPoint1().getLng() >= minPathLng) && (minPathLat <= maxBoundLat) && (maxPathLat >= minBoundLat)) {
				return false;
			}
			
		//Checks that the net gradient is not zero (meaning these lines are not parallel, thus an intersection at some point)
		} else if (netGrad != 0) {
			Double icLng = netYint/netGrad;
			Double icLat = path.getGradient()*icLng + path.getYint();
			
			//Checks whether the point of intersection is within the bounds of the given building boundary (meaning an intersection)
			if (((icLng <= maxBoundLng) && (icLng >= minBoundLng) && (icLng <= maxPathLng) && (icLng >= minPathLng)) || ((icLat <= maxBoundLat) && (icLat >= minBoundLat) && (icLat <= maxPathLat) && (icLat >= minPathLat))) {
				return false;
			}
		
		//Checks if these lines are the same and share points in the same bounds (meaning an intersection)
		} else if ((netYint == 0.0) && (minBoundLat <= maxPathLat) && (maxBoundLat >= minPathLat)) {
			return false;
		}
		
		return true;
	}
}