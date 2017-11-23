package uk.org.cetis.google;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostCodeElement {

		public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getPostcode() {
		return postcode;
	}
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	
	public String getArea(){
		String pattern = "(\\D{1,2})";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(postcode);
		m.find();
	    return m.group(1);
		//return this.getPostcode().replaceAll("\\d","");
	}
		private int id;
	     private String postcode;
	     private double latitude;
	     private double longitude;

	     
}
