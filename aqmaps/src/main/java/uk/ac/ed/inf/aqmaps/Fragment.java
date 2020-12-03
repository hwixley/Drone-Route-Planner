package uk.ac.ed.inf.aqmaps;

public class Fragment {
	
	Sensor sensor;
	Double avgDist;
	Sensor bestDestSensor;
	
	public Fragment() {
	}
	
	public Fragment(Sensor sensor, Double avgDist) {
		this.sensor = sensor;
		this.avgDist = avgDist;
	}
}