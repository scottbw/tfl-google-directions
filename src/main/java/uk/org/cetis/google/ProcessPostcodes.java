package uk.org.cetis.google;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.opencsv.CSVReader;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

public class ProcessPostcodes {
	
	static String[] london;

	public static void main(String[] args) throws IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException {

		 // Load master postcode file
		 List<PostCodeElement> beans = new CsvToBeanBuilder(new FileReader("src/main/resources/postcode-outcodes.csv"))
	       .withType(PostCodeElement.class).build().parse();
	     
		 // Load london areas file
		 CSVReader reader = new CSVReader(new FileReader("src/main/resources/london-postcode-areas"));
	     london = reader.readAll().get(0);
		
		 // Filter output
	     List<PostCodeElement> londonOutCodes = beans.stream().filter(s -> isInLondon(s.getArea()) && s.getLatitude() != 0).collect(Collectors.toList());
		
	     // Write out just the London ones
	     Writer writer = new FileWriter("src/main/resources/london-outcodes.csv");
	     StatefulBeanToCsv beanToCsv = new StatefulBeanToCsvBuilder(writer).build();
	     beanToCsv.write(londonOutCodes);
	     writer.close();
	}
	
	public static boolean isInLondon(String postcode){
		return Arrays.stream(london).anyMatch(s -> postcode.startsWith(s));
	}

}
