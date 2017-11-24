package uk.org.cetis.google;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

public class TfLDrone extends Thread{
	
	List<PostCodeElement> start;
	Campus end;
	List<CampusToPostcode> results;
	String APPID;
	String APIKEY;
	public boolean completed = false;
	private CyclicBarrier barrier;
	
	public TfLDrone(List<PostCodeElement> start, Campus end, String appId, String apiKey, CyclicBarrier barrier){
		super();
		this.start = start;
		this.end = end;
		this.APPID = appId;
		this.APIKEY = apiKey;
		this.barrier = barrier;
		results = new ArrayList<CampusToPostcode>();

	}

	@Override
	public void run() {
		System.out.println("Thread start");
		try {
			barrier.await();
			for (PostCodeElement postcode: start){
				try {
					CampusToPostcode result = callJourneyApiWithRetry(end, postcode);
					if (result != null){
						results.add(result);
						System.out.print('.');
					} else {
						System.out.println("Error: to " + end.campusId + " from "+postcode.getPostcode());
					}

				} catch (Exception e) {
					System.out.print('X');
				}
			}
			this.completed = true;
			barrier.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public CampusToPostcode callJourneyApi(Campus campus, PostCodeElement postcode) throws ClientProtocolException, IOException, URISyntaxException{
		CampusToPostcode data = new CampusToPostcode(campus);
		data.originPostcode = postcode.getPostcode();

		String path = String.format("/journey/journeyresults/%f,%f/to/%s?date=20171120&time=1000&timeis=arriving", postcode.getLatitude(), postcode.getLongitude(), campus.postcode.trim().replace(" ", ""));

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
	
	public CampusToPostcode callJourneyApiWithRetry(Campus campus, PostCodeElement postcode) throws Exception{

		CampusToPostcode result = null;
		int retryCounter = 0;
		
		while (result == null && retryCounter < 5){
			try {
				Thread.sleep(1000);
				result = this.callJourneyApi(campus, postcode);
			} catch (Exception e) {
				System.out.print("error for thread "+campus.campusId);
				retryCounter++;
			}
		}
		
		return result;
	}
	
	
	
}