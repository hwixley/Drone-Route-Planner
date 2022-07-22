package uk.ac.ed.inf.aqmaps;

/**
 * Custom class for the temperate() algorithm
 */

public class Fragment {
		
	private Sensor sensor;
	private Double avgDist;
	private Sensor bestDestSensor;
	
	//CONSTRUCTORS
	public Fragment() {
	}
	
	public Fragment(Sensor sensor, Double avgDist) {
		this.sensor = sensor;
		this.avgDist = avgDist;
	}
	
	
	//GETTERS
	public Sensor getSensor() {
		return sensor;
	}
	
	public Double getAvgDist() {
		return avgDist;
	}
	
	public Sensor getBestDestSensor() {
		return bestDestSensor;
	}
	
	//SETTERS
	public void setBestDestSensor(Sensor bestDestSensor) {
		this.bestDestSensor = bestDestSensor;
	}
}