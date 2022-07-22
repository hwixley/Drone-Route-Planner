package uk.ac.ed.inf.aqmaps;

/**
 * Class used to store all geometrical calculation methods
 */

public class GeometricalCalcs {
	
	
	//METHODS
	
    //Calculates Euclidean distance between 2 points
    public static Double calcDistance(Point p1, Point p2) { 
    	Double lats = Math.pow(p1.getLat() - p2.getLat(),2);
    	Double lngs = Math.pow(p1.getLng() - p2.getLng(), 2);
    	
    	return Math.sqrt(lats + lngs);
    }
    
    //Calculates angle between 2 points
    public static Double calcAngle(Point origin, Point dest) {
    	Double grad = (dest.getLat() - origin.getLat())/(dest.getLng() - origin.getLng());
    	Double angle = Math.toDegrees(Math.atan(grad));
    	
    	if ((dest.getLng() > origin.getLng()) && (dest.getLat() < origin.getLat())) {
    		angle += 360;
    		
    	} else if ((dest.getLng() < origin.getLng()) && (dest.getLat() > origin.getLat())) {
    		angle += 180;

    	} else if ((dest.getLng() < origin.getLng()) && (dest.getLat() < origin.getLat())) {
    		angle += 180;
    	}
    	
    	return angle;
    }
    
    //Transform point (returns the transformed point by using the angle of the drone's movement)
    public static Point transformPoint(Point origin, Double angle) {
    	Point out = new Point(origin);
    	angle = Math.toRadians(angle);
    	
    	//Uses planar trigonometry to transform the current point given the angle of movement
    	out.setLat(out.getLat() + App.pathLength*Math.sin(angle));
    	out.setLng(out.getLng() + App.pathLength*Math.cos(angle));
    	
    	return out;
    }
}