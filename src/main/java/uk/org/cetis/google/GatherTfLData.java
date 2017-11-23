package uk.org.cetis.google;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;

/**
 * User story:
 * 	Get all postcode sectors OR districts
 *  for ~100 london campus locations
 *  
 *  Campus metadata:
 *  	UKPRN
 *      Campus ID
 *      Postcode
 *      
 *  Origin data:
 *  	PostCode 
 *  	Distance
 *  	Travel Duration
 *  	Number of journey steps
 *      
 *  How many calls?
 *  
 * @author scottw
 *
 */

public class GatherTfLData {

	static String APPID;
	static String APIKEY;

	public static void main(String[] args) throws Exception{

		// An example of the sort of thing we're calling:
		//
		// https://api.tfl.gov.uk/journey/journeyresults/51.501,-0.123/to/n225nb
		//
		Configuration config = new PropertiesConfiguration("/etc/tfl-google/config.properties");
		APPID = config.getString("TFL_APPID");
		APIKEY = config.getString("TFL_APIKEY");
		
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
		// 3. Load in our list of London postcode sectors
		//
		List<PostCodeElement> postcodes = new CsvToBeanBuilder(new FileReader("src/main/resources/london-postcodesectors.csv"))
		.withType(PostCodeElement.class).build().parse();

		//
		// 4. Call the API for each pairing of source and destination
		//
		for (Campus campus: campuses){
			for (PostCodeElement postcode: postcodes){

				try {
					CampusToPostcode result = callJourneyApi(campus, postcode);
					results.add(result);

					try {
						Thread.sleep(500);                 //1000 milliseconds is one second.
					} catch(InterruptedException ex) {
						Thread.currentThread().interrupt();
					}

					System.out.print('.');
				} catch (Exception e) {
					System.out.print(postcode.getPostcode());
				}

			}

		}

		System.out.println("Done!");

		//
		// 4. Save the data
		//
		Writer writer = new FileWriter("src/main/resources/london-journeys-sectors.csv");
		StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer).build();
		beanToCsv.write(results);
		writer.close();
	}

	public static CampusToPostcode callJourneyApi(Campus campus, PostCodeElement postcode) throws ClientProtocolException, IOException, URISyntaxException{
		CampusToPostcode data = new CampusToPostcode(campus);
		data.originPostcode = postcode.getPostcode();

		String path = String.format("/journey/journeyresults/%f,%f/to/%s", postcode.getLatitude(), postcode.getLongitude(), campus.postcode.trim().replace(" ", ""));

		HttpClient client = HttpClientBuilder.create().build();

		URIBuilder builder = new URIBuilder();
		builder.setScheme("https").setHost("api.tfl.gov.uk").setPath(path)
		.setParameter("app_id", APPID)
		.setParameter("app_key", APIKEY);

		HttpGet request = new HttpGet(builder.build());

		request.addHeader("accept", "application/json");

		HttpResponse response = client.execute(request); 

		String json = IOUtils.toString(response.getEntity().getContent());
		JSONObject obj = new JSONObject(json);
		JSONArray journeys = obj.getJSONArray("journeys");
		JSONObject journey = journeys.getJSONObject(0);
		data.duration = journey.getInt("duration");
		data.legs = journey.getJSONArray("legs").length();
		
		return data;
	}



}
