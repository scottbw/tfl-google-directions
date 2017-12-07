   Copyright 2017 Cetis LLP

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

package uk.org.cetis.google;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

public class GatherGoogleData {
	
	/*
	 * Limits
	 * 2,500 free elements per day, calculated as the sum of client-side and server-side queries.
	 * Maximum of 25 origins or 25 destinations per request.
	 * 100 elements per request.
	 * 100 elements per second, calculated as the sum of client-side and server-side queries
	 */

	
	/**
	 * User story: from a give set of origins (UK HEI campus locations in London) calculate distance
	 * to a given OUTCODE.
	 * Max 100 campuses
	 * Max 3000 outcodes
	 * Limit outcodes to a region?
	 * 
	 * Max 300,000 elements
	 * 4 x 25 = 100 per request
	 * 2,500 per day
	 * @param args
	 * @throws FileNotFoundException 
	 * @throws IllegalStateException 
	 */
	
	public static String APIKEY;
	public static GeoApiContext context;
	
	public static void main(String[] args) throws IllegalStateException, IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException, ConfigurationException {

		Configuration config = new PropertiesConfiguration("/etc/tfl-google/config.properties");
		APIKEY = config.getString("GOOGLE_APIKEY");
		
		context = new GeoApiContext.Builder()
		.apiKey(APIKEY)
		.build();

		//
		// 1. Set up our data array to hold the results
		//
		List<CampusToPostcode> results = new ArrayList<CampusToPostcode>();

		//
		// 2. Load in our campus data
		//
		List<Campus> campuses = new CsvToBeanBuilder(new FileReader("src/main/resources/campus.csv"))
		.withType(Campus.class).build().parse();

		//
		// 3. Load in our list of London and nearby postcode districts
		//
		List<PostCodeElement> postcodes = new CsvToBeanBuilder(new FileReader("src/main/resources/nearby-outcodes.csv"))
		.withType(PostCodeElement.class).build().parse();
		
		String[] destinationPostcodes = new String[postcodes.size()];
		for (int i = 0; i < postcodes.size(); i++){
			destinationPostcodes[i] = postcodes.get(i).getPostcode();
		}
		
		//
		// We have to turn this into blocks of 1x25 as thats the limit
		// for "free" API access
		//
		for (int o = 0; o < campuses.size(); o++){
			for (int d = 0; d < destinationPostcodes.length; d = d + 25){
				int end = d + 25;
				if (end > destinationPostcodes.length) end = destinationPostcodes.length;
				
				// Co-ords
				LatLng[] coords = new LatLng[end-d];
				String[] pcodes = new String[end-d];

				for (int i = 0; i < end-d; i++){
					coords[i] = new LatLng(postcodes.get(d+i).getLatitude(), (postcodes.get(d+i).getLongitude()));
					pcodes[i] = postcodes.get(d+i).getPostcode();
				}				
				
				System.out.print(".");
				
				results.addAll(getMatrix(campuses.get(o), coords, pcodes));
			}
		}
		
		System.out.println("Done!");

		//
		// 4. Save the data
		//
		Writer writer = new FileWriter("src/main/resources/wider-journeys-areas.csv");
		StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer).build();
		beanToCsv.write(results);
		writer.close();


	}
	
	public static List<CampusToPostcode> getMatrix(Campus campus, LatLng[] coords, String[] postcodes){
		
		List<CampusToPostcode> results = new ArrayList<CampusToPostcode>();
		
		DistanceMatrix matrix = DistanceMatrixApi.newRequest(context).mode(TravelMode.TRANSIT).origins(campus.postcode).destinations(coords).awaitIgnoreError();

		for (int i = 0; i < matrix.rows.length; i++){
			DistanceMatrixRow row = matrix.rows[i];
			System.out.println("FROM:"+matrix.originAddresses[i]);
			for (int d = 0; d < row.elements.length; d ++){
				try {
				DistanceMatrixElement element = row.elements[d];
				CampusToPostcode result = new CampusToPostcode(campus);
				result.duration = element.duration.inSeconds/60; //minutes
				result.distance = element.distance.inMeters/1000; //km
				result.originPostcode = postcodes[d];
				results.add(result);
				} catch (Exception ex){
					System.out.println("Error reading data for "+postcodes[d]);
				}
			}
		}
		
		return results;
	}
}

