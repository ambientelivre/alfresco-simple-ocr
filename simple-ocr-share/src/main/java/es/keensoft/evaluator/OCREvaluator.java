package es.keensoft.evaluator;

import org.alfresco.web.evaluator.BaseEvaluator;
import org.json.simple.JSONObject;

public class OCREvaluator extends BaseEvaluator {

	@Override
	public boolean evaluate(JSONObject json) {
		
		JSONObject node = (JSONObject) json.get("node");
		JSONObject properties = (JSONObject) node.get("properties");
		
		if(!properties.containsKey("ocr:versionApplied")) {
			return false;
		}
		
		String currentVersion = properties.get("cm:versionLabel").toString();
		String appliedVersion = properties.get("ocr:versionApplied").toString();
		
		return currentVersion.equals(appliedVersion);
	}
	
}
