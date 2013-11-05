package pl.net.bluesoft.rnd.processtool.editor.jpdl.object;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import pl.net.bluesoft.rnd.processtool.editor.AperteWorkflowDefinitionGenerator;
import pl.net.bluesoft.rnd.processtool.editor.IndentedStringBuilder;
import pl.net.bluesoft.rnd.processtool.editor.XmlUtil;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class JPDLJavaTask extends JPDLTask {
	private Map<String,String> stepDataMap = new TreeMap<String,String>();

    public JPDLJavaTask(AperteWorkflowDefinitionGenerator generator) {
        super(generator);
    }

    @Override
	public void toXML(IndentedStringBuilder sb) {
		sb.append(String.format("<java auto-wire=\"true\" cache=\"false\" class=\"pl.net.bluesoft.rnd.pt.ext.jbpm.JbpmStepAction\" " +
                "g=\"%d,%d,%d,%d\" method=\"invoke\" name=\"%s\" var=\"RESULT\">\n", boundsX, boundsY, width, height,name));
		sb.begin();
		sb.append("<field name=\"stepName\">\n");
		sb.begin();
		sb.append(String.format("<string value=\"%s\"/>\n", taskType));
		sb.end();
		sb.append("</field>\n");
		sb.append("<field name=\"params\">\n");
		sb.begin();
		sb.append("<map>\n");
		sb.begin();
		if (!stepDataMap.isEmpty()) {
			for (String key : stepDataMap.keySet()) {
				sb.append("<entry>\n");
				sb.begin();
				sb.append("<key>\n");
				sb.begin();
				sb.append(String.format("<string value=\"%s\"/>\n", key));
				sb.end();
				sb.append("</key>\n");
				sb.append("<value>\n");
				sb.begin();
				sb.append(String.format("<string value=\"%s\"/>\n", getValue(key)));
				sb.end();
				sb.append("</value>\n");
				sb.end();
				sb.append("</entry>\n");
			}
		}
		sb.end();
		sb.append("</map>\n");
		sb.end();
		sb.append("</field>\n");
		getTransitionsXML(sb);
		sb.end();
		sb.append("</java>\n");
    }

	private String getValue(String key) {
		// check for the quote symbol, because we don't have specific XML library here
		String value = stepDataMap.get(key);
		if (value.contains("\"")) {
			value = value.replaceAll("\"", "'");
		}
		return value;
	}

	@Override
	public void fillBasicProperties(JSONObject json) throws JSONException {
		super.fillBasicProperties(json);
		String stepDataJson = json.getJSONObject("properties").getString("aperte-conf");
		if (stepDataJson != null && stepDataJson.trim().length() != 0) {
		  stepDataJson = XmlUtil.decodeXmlEscapeCharacters(stepDataJson);
		  JSONObject stepDataJsonObj = new JSONObject(stepDataJson);
		  Iterator i = stepDataJsonObj.keys();
		  while(i.hasNext()) {
			String key = (String)i.next();  
            Object value = stepDataJsonObj.get(key);  
            if (value instanceof String) {
                byte[] bytes = Base64.decodeBase64(((String) value).getBytes());
                value = new String(bytes);
            }
			stepDataMap.put(key, value.toString());
		  }
		}

	}
	
	@Override
	public String getObjectName() {
		return "Automatic step";
	}
}
