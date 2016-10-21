package uk.gov.justice.payment.api.integration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

public class TestBase {

	public static Properties CONFIG = null;
	
	public void initialize() throws IOException{
	
		CONFIG = new Properties();
		FileInputStream fn = new FileInputStream(System.getProperty("user.dir")+"//src//test//resources//Config//config.properties");
		CONFIG.load(fn);
		
	}
	
	// This function used for converting JSON to String
	
	protected String loadFile(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(PaymentsHappyPath.class.getClassLoader().getResourceAsStream(filename)));
		StringBuilder string = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			string.append(line);
		}
		return string.toString();

	}
	
	// This function used for passing parameters using velocity template
	protected static String getProcessedTemplateValue(final String templateString,
				final Map<String, String> templateValueMap) {
			String retValue = null;
			if (templateString == null) {
				return retValue;
			}
			VelocityContext context = new VelocityContext();
			if (templateValueMap != null && !templateValueMap.isEmpty()) {
				Iterator<String> templateValueMapIter = templateValueMap.keySet().iterator();
				while (templateValueMapIter.hasNext()) {
					String key = templateValueMapIter.next();
					Object value = templateValueMap.get(key);
					context.put(key, value);
				}
			}
			StringWriter swOut = new StringWriter();

			Velocity.evaluate(context, swOut, "Log template", templateString);
			retValue = swOut.toString();

			return retValue;
		}


}
