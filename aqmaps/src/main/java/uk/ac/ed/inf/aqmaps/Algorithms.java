package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Algorithms {

	/**
	 * INITIAL ROUTE SETTING ALGORITHMS:
	 * 
	 * greedy() and temperate()
	 * @return: Returns a list of sensors that represents the order the
     * 			sensors for the given day will be visited by the drone.
	 */
	
    //Greedy route optimisation algorithm
    public static ArrayList<Sensor> greedy() {
    	ArrayList<Sensor> sensorRoute = new ArrayList<Sensor>();
		ArrayList<Sensor> unexploredSensors = new ArrayList<Sensor>(App.sensors);
		 
		//Iterates through all the sensors
		for (int s = 0; s < App.sensors.size()+1; s++) {
			Point currPoint;
			if (s == 0) {
				currPoint = new Point(App.startPoint);
			} else {
				currPoint = sensorRoute.get(s-1).getPoint();
			}
			
			Double minDist = 100.0;
			int minSensor = -1;
			
			//Finds the closest unexplored sensor to sensor s
			for (int u = 0; u < unexploredSensors.size(); u++) {
				Sensor nextSensor = unexploredSensors.get(u);
				 
				if (MoveCalculations.calcEdgeCost(nextSensor.getPoint(), currPoint) < minDist) {
					minDist = MoveCalculations.calcEdgeCost(nextSensor.getPoint(), currPoint);
					minSensor = u;
				}
				 
			}
			//Adds the closest sensor to the sensor route
			if (unexploredSensors.size() > 0) {
		    	sensorRoute.add(unexploredSensors.get(minSensor));
		    	unexploredSensors.remove(minSensor);
			}
		}
		return sensorRoute;
    }
    
    
  //Custom 'Temperate' route optimisation algorithm
    @SuppressWarnings("unused")
	public static ArrayList<Sensor> temperate() {
    	ArrayList<Sensor> sensorRoute = new ArrayList<Sensor>();
    	ArrayList<Double> avgDistances = new ArrayList<Double>();
    	ArrayList<Fragment> bestFrags = new ArrayList<Fragment>();
    	
    	//Calculate average distance for each sensor
    	for (int s = 0; s < App.sensors.size(); s++) {
    		Sensor sens = App.sensors.get(s);
    		Double avg = 0.0;
    				
    		for (int t = 0; t < App.sensors.size(); t++) {
    			if (t != s) {
    				avg += MoveCalculations.calcEdgeCost(sens.getPoint(),App.sensors.get(t).getPoint());
    			}
    		}
    		bestFrags.add(new Fragment(sens,avg));
    		avgDistances.add(avg);
    	}

    	//Order fragments by AvgDistance (descending)
    	for (int i = 0; i < App.sensors.size(); i++) {
    		
    		if ( i < App.sensors.size()-1) {
	    		Double maxDist = Collections.max(avgDistances.subList(i, App.sensors.size()-1));
	    		int maxIndex = avgDistances.indexOf(maxDist);
	    		
	    		Fragment oldHead = bestFrags.get(i);
	    		bestFrags.set(i, bestFrags.get(maxIndex));
	    		bestFrags.set(maxIndex, oldHead);
    		}
    	}
    	
    	Map<Sensor, Integer> usedSensors = new HashMap<Sensor, Integer>();
    	
    	//Calculate best transitions for each sensor
    	for (int r = 0; r < App.sensors.size(); r++) {
    		Fragment frag = bestFrags.get(r);
    		ArrayList<Sensor> closestSensors = getClosestSensors(frag.sensor);
    		
    		for (int k = 0; k < closestSensors.size(); k++) {
    			int keyVal = 0;
    			if (usedSensors.containsKey(closestSensors.get(k))) {
    				keyVal = usedSensors.get(closestSensors.get(k));
    			}
    			
    			if (keyVal < 2) {
					frag.bestDestSensor = closestSensors.get(k);
					bestFrags.set(r, frag);
					usedSensors.put(closestSensors.get(k), keyVal+1);
					break;
    			}
    		}
    	}
    	
    	sensorRoute.add(bestFrags.get(0).sensor);
    	sensorRoute.add(bestFrags.get(0).bestDestSensor);
    	bestFrags.remove(0);
    	
    	//Calculate route
    	while (sensorRoute.size() < App.sensors.size()) {
    		Sensor lastSens = sensorRoute.get(sensorRoute.size()-1);
    		ArrayList<Integer> redundancies = new ArrayList<Integer>();
    		
    		for (int b = 0; b < bestFrags.size(); b++) {
    			Fragment frag = bestFrags.get(b);
    			
    			//Loops through fragments until it finds one which contains the last point in our route
    			if (((frag.sensor.equals(lastSens)) && (sensorRoute.indexOf(frag.bestDestSensor) == -1)) || ((frag.bestDestSensor.equals(lastSens)) && (sensorRoute.indexOf(frag.sensor) == -1))) {
    				if (frag.sensor.equals(lastSens)) {
    					sensorRoute.add(frag.bestDestSensor);
    				} else {
    					sensorRoute.add(frag.sensor);
    				}
    				bestFrags.remove(b);
    				break;
    			
    			//Stores fragment redundancies so they can be deleted after the loop (prevents repeated & redundant computations)
    			} else if ((frag.sensor.equals(lastSens)) || (frag.bestDestSensor.equals(lastSens))) {
    				redundancies.add(b);
    			}
    		}
    		
    		//Remove redundant edges 
    		for (int r = 0; r < redundancies.size(); r++) {
    			bestFrags.remove(redundancies.get(r).intValue()-r);
    		}
    		redundancies.clear();
    		
    		if (lastSens.equals(sensorRoute.get(sensorRoute.size()-1))) {
    			sensorRoute.add(getClosestSensor(lastSens, sensorRoute));
    		}
    	}
    	return sensorRoute;
    }
    
    
    
    /**
     * NAIVE ROUTE REFINEMENT ALGORITHMS:
     * 
     * twoOpt(@param sensorRoute) and Swap(@param sensorRoute)
     * @param sensorRoute: 	This is a list of sensors that will be swapped
     * 						around by these refinement algorithms. This is
     * 						useful for refining initial routes made by
     * 						greedy() or temperate()
     * 						If empty, this will be populated with the global
     * 						variable App.sensors
     * @return: Returns a list of sensors that represents the order the
     * 			sensors for the given day will be visited by the drone.
     */
    
    //2-Opt heuristic route optimisation algorithm
    public static ArrayList<Sensor> twoOpt(ArrayList<Sensor> sensorRoute) {
		Boolean better = true;
		
		//Variable to prevent infinite loops
		int indexTwoOp = 0;
		
		//Initialises the sensorRoute variable if it is empty
		if (sensorRoute.isEmpty()) {
			sensorRoute = new ArrayList<Sensor>(App.sensors);
		}
		
		//Iterate as long as the route continues to be improved
		while (better) {
			better = false; 
			
			//Iterates through the sensors in the route
			for (int j = 0; j < sensorRoute.size()-1; j++) {
				//Iterates through the sensors in the route preceding sensor j
				for (int i = 0; i < j; i++) {
					
					//Cost before route is changed
					Double oldCost = MoveCalculations.calcRouteCost(sensorRoute);
					indexTwoOp += 1;
					
					//Initialisation of points to be swapped in the route
					Point iPoint = sensorRoute.get(i).getPoint();
					Point iPointP = new Point();
					if (i == 0) {
						iPointP = sensorRoute.get(sensorRoute.size()-1).getPoint();
					} else {
						iPointP = sensorRoute.get(i-1).getPoint();
					}
					Point jPoint = sensorRoute.get(j).getPoint();
					Point jPointP = sensorRoute.get(j+1).getPoint();
					
					//Cost after route is changed
					Double newCost = oldCost - MoveCalculations.calcEdgeCost(iPointP, iPoint) - MoveCalculations.calcEdgeCost(jPoint, jPointP) + MoveCalculations.calcEdgeCost(iPointP, jPoint) + MoveCalculations.calcEdgeCost(iPoint, jPointP);
					
					//Checks for infinite loops
					if (indexTwoOp <= 1000) {
						//Checks if new route is better than the old route
						if (newCost < oldCost) {
							//If so, then the order of sensors in the route from i to j are reversed
							
							ArrayList<Sensor> revSensors = new ArrayList<Sensor>();
							 
							//Stores the reversed ordering of sensors
							for (int v = 0; v < j-i+1; v++) {
								revSensors.add(sensorRoute.get(i+v));
							}
							//Updates the sensor route
							for (int z = 0; z < j-i+1; z++) {
								sensorRoute.set(i+z, revSensors.get(j-i-z));
							}
							 
							better = true;
						}
					}
				}
		 	}
		}
		return sensorRoute;
    }
    
    //Swap heuristic route optimisation algorithm
    @SuppressWarnings("unused")
	public static ArrayList<Sensor> swap(ArrayList<Sensor> sensorRoute) {
    	Boolean better = true;
    	
    	//Variable to prevent infinite loops
    	int indexSwap = 0;
    	
    	//Initialises the sensorRoute variable if it is empty
		if (sensorRoute.isEmpty()) {
			sensorRoute = new ArrayList<Sensor>(App.sensors);
		}
		
		//Iterate as long as the route continues to be improved
		while (better) {
			better = false;
			
			//Iterates through the sensors in the route
			for (int i = 0; i < sensorRoute.size(); i++) {
				indexSwap += 1;
				
				//Route cost before adjacent sensors in the route were swapped
				Double oldCost = MoveCalculations.calcRouteCost(sensorRoute);
				
				int indexI2 = i+1;
				if (i+1 == sensorRoute.size()) {
					indexI2 = 0;
				}
				
				Sensor newI = sensorRoute.get(indexI2);
				Sensor newI2 = sensorRoute.get(i);
				sensorRoute.set(indexI2, newI2);
				sensorRoute.set(i, newI);
				
				//Route cost after adjacent sensors in the route were swapped
				Double newCost = MoveCalculations.calcRouteCost(sensorRoute);
				
				//Checks if route was better after swapping sensors
				if (newCost < oldCost) {
					better = true;
				} else {
					sensorRoute.set(i, newI2);
					sensorRoute.set(indexI2, newI);
				}
				
				//If caught in an infinite loop we break the while loop
				if (indexSwap > 1000) {
					better = false;
					break;
				}
			}
		}
		return sensorRoute;
    }
    
    
    
    /**
     * TEMPERATE HELPER METHODS:
     * 
     * getClosestSensor( @param sens, @param sensorRoute)
     * Returns the closest sensor to 'sens' from 'sensorRoute'
     * 
     * getClosestSensors( @param sens )
     * Returns a ordered list of the closest sensors to 'sens' (ascending in distance)
     */
    
    //Returns closest sensor to 'sens'
    private static Sensor getClosestSensor(Sensor sens, ArrayList<Sensor> sensorRoute) {
    	Double minDist = 10000.0;
    	int minIndex = -1;
    	
    	//Iterates through all the sensors
    	for (int s = 0; s < App.sensors.size(); s++) {
    		Sensor next = App.sensors.get(s);
    		
    		//Ensures we are not using repeated sensors
    		if ((sensorRoute.indexOf(next) != -1) || (next.equals(sens))) {
    			continue;
    		} else {
    			Double dist = MoveCalculations.calcEdgeCost(sens.getPoint(),next.getPoint());
    			
    			if (dist < minDist) {
    				minDist = dist;
    				minIndex = s;
    			}
    		}
    	}
    	
    	return App.sensors.get(minIndex);
    }
    
    //Return ordered list of closest sensors (ascending in distance)
    private static ArrayList<Sensor> getClosestSensors(Sensor sens) {
    	ArrayList<Double> distances = new ArrayList<Double>();
    	ArrayList<Sensor> output = new ArrayList<Sensor>();
    	
    	//Iterates through all the sensors
    	for (int s = 0; s < App.sensors.size(); s++) {
    		Sensor next = App.sensors.get(s);
    		
    		//Ensures we are not comparing the input sensor with itself
    		if (!next.equals(sens)) {
    			Double dist = MoveCalculations.calcEdgeCost(sens.getPoint(), next.getPoint());
    			
    			int startIndex = 0;
    			int endIndex = distances.size();
    			
    			//Iterates until the list is sorted (ascending order of distances)
    			while(true) {
    				if (distances.isEmpty()) {
    					distances.add(dist);
    					output.add(next);
    					break;

    				} else if (startIndex == endIndex) {
    					break;
    					
    				} else if (dist < Collections.min(distances.subList(startIndex, endIndex))) {
	    				distances.add(startIndex, dist);
	    				output.add(startIndex, next);
	    				break;
	    				
	    			} else if (dist > Collections.max(distances.subList(startIndex, endIndex))) {
	    				distances.add(dist);
	    				output.add(next);
	    				break;
	    			}
	    			startIndex += 1;
    			}
    		}
    	}
    	return output;
    }
}