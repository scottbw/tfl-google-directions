package uk.org.cetis.google;

public class CampusToPostcode extends Campus {
	
	@Override
	public String toString() {
		return "To: " + this.ukprn + " " + this.campusId + " " + this.postcode + " From: "+ this.originPostcode + " Duration: "+this.duration + " Legs: "+this.legs ;
	}

	public String getOriginPostcode() {
		return originPostcode;
	}

	public void setOriginPostcode(String originPostcode) {
		this.originPostcode = originPostcode;
	}

	public double getDuration() {
		return duration;
	}

	public void setDuration(double duration) {
		this.duration = duration;
	}

	public int getLegs() {
		return legs;
	}

	public void setLegs(int legs) {
		this.legs = legs;
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	String originPostcode;
	double duration;
	int legs;
	double distance;
	
	public CampusToPostcode(Campus campus){
		this.ukprn = campus.ukprn;
		this.campusId = campus.campusId;
		this.postcode = campus.postcode;
	}
	
	
	
	

}
