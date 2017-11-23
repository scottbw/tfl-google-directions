package uk.org.cetis.google;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

public class ProcessPostcodes {
	
	static String[] london;
	static String[] nearby;

	public static void main(String[] args) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

		 // Load master postcode file
		 List<PostCodeElement> beans = new CsvToBeanBuilder(new FileReader("src/main/resources/postcode-outcodes.csv"))
	       .withType(PostCodeElement.class).build().parse();
	     
		 // Load london areas file
		 CSVReader reader = new CSVReader(new FileReader("src/main/resources/london-postcode-areas"));
	     london = reader.readAll().get(0);
	     
		 // Load nearby areas file
		 reader = new CSVReader(new FileReader("src/main/resources/near-london-postcode-areas"));
	     nearby = reader.readAll().get(0);
		
		 // Filter output
	     List<PostCodeElement> londonOutCodes = beans.stream().filter(s -> isIn(s.getArea(), london) && s.getLatitude() != 0).collect(Collectors.toList());
		
	     // Write out just the London ones
	     Writer writer = new FileWriter("src/main/resources/london-outcodes.csv");
	     StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer).build();
	     beanToCsv.write(londonOutCodes);
	     writer.close();
	     
		 // Filter output
	     List<PostCodeElement> nearbyOutCodes = beans.stream().filter(s -> isIn(s.getArea(), nearby) && s.getLatitude() != 0).collect(Collectors.toList());

	     // Write out just the nearby ones
	     writer = new FileWriter("src/main/resources/nearby-outcodes.csv");
	     beanToCsv = new StatefulBeanToCsvBuilder(writer).build();
	     beanToCsv.write(nearbyOutCodes);
	     writer.close();
	}
	
	public static boolean isIn(String postcode, String[] matches){
		return Arrays.stream(matches).anyMatch(s -> postcode.equals(s));
	}

}
