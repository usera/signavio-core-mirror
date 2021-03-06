package pl.net.bluesoft.rnd.processtool.editor.jpdl.object;

import com.signavio.platform.exceptions.RequestException;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import pl.net.bluesoft.rnd.processtool.editor.AperteWorkflowDefinitionGenerator;
import pl.net.bluesoft.rnd.processtool.editor.IndentedStringBuilder;
import pl.net.bluesoft.rnd.processtool.editor.XmlUtil;

import java.util.*;


public class JPDLTransition extends JPDLObject {

    public JPDLTransition(AperteWorkflowDefinitionGenerator generator) {
        super(generator);
    }

    private static final String DEFAULT_BUTTON_NAME = "Default";
	
	private String target;
    private String targetName;
    private List<Docker> dockers;
    
    //action properties
    private String buttonName;
    private List<String> actionPermissions = new ArrayList<String>();
    private Map<String,Object> actionAttributes = new TreeMap<String,Object>();
    private Map<String,Object> actionAutowiredProperties = new TreeMap<String,Object>();
    
    //for 'decision'
    private String condition;
	
	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}
	
	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
	
	public String getButtonName() {
		return buttonName;
	}

	public void setButtonName(String buttonName) {
		this.buttonName = buttonName;
	}

	private void generateActionPermissionsXML(IndentedStringBuilder sb) {
		if (!actionPermissions.isEmpty()) {
			sb.append("<permissions>\n");
			sb.begin();
		}
		for (String perm : actionPermissions) {
			sb.append(String.format("<config.ProcessStateActionPermission roleName=\"%s\" />\n", perm));
		}
		if (!actionPermissions.isEmpty()) {
			sb.end();
			sb.append("</permissions>\n");
		}
	}
	
	private void generateActionAttributesXML(IndentedStringBuilder sb) {
		if (!actionAttributes.isEmpty()) {
			sb.append("<attributes>\n");
			sb.begin();

			for (String name : actionAttributes.keySet()) {
				Object value = actionAttributes.get(name);
				if (!isBlank(value)) {
					sb.append(String.format("<config.ProcessStateActionAttribute name=\"%s\" value=\"%s\"/>\n", name, value));
				}
			}

			sb.end();
			sb.append("</attributes>\n");
		}
	}

	private static boolean isBlank(Object value) {
		if (value instanceof String) {
			return ((String)value).trim().isEmpty();
		}
		return value == null;
	}

	public void generateStateActionXML(IndentedStringBuilder sb) {
		sb.append(String.format("<config.ProcessStateAction bpmName=\"%s\" buttonName=\"%s\" ", name, buttonName));
		for (String name : actionAutowiredProperties.keySet()) {
			Object value = actionAutowiredProperties.get(name);
			if (!isBlank(value)) {
				sb.append(String.format("%s=\"%s\" ", name, value));
			}
		}
	    sb.append(" >\n");
		sb.begin();
	    generateActionPermissionsXML(sb);
	    generateActionAttributesXML(sb);
		sb.end();
	    sb.append("</config.ProcessStateAction>\n");
	}
 
	public void fillBasicProperties(JSONObject json) throws JSONException {
		super.fillBasicProperties(json);
		
		JSONArray dockerArray = json.getJSONArray("dockers");
		
		dockers = new ArrayList<Docker>();
        //TODO support when somebody is not using default dock point on a node
//		for (int i = 0; i < dockerArray.length(); i++) { //go through all the dockers, since you can dock transition line to many places on a node
		for (int i = 1; i < dockerArray.length()-1; i++) {
			JSONObject docker = dockerArray.getJSONObject(i);
			int x = round(docker.getString("x"));
			int y = round(docker.getString("y"));
			dockers.add(new Docker(x, y));
		}
		
		
		
		if (json.optJSONObject("target") != null) {
		  target = json.getJSONObject("target").optString("resourceId");
		} else {
		  throw new RequestException("Transition '" + name + "' has no target.");
		}
        JSONObject properties = json.getJSONObject("properties");
        buttonName = properties.optString("button-type");
		condition = properties.optString("conditionexpression");
//		if (!StringUtils.isEmpty(condition) && !condition.startsWith("#{")) {
//			condition = "#{" + condition + "}";
//		}
		
		if (StringUtils.isEmpty(buttonName)) buttonName = DEFAULT_BUTTON_NAME;
		
		JSONObject permissions = properties.optJSONObject("action-permissions");
		if (permissions != null) {
			 JSONArray permissionsItems = permissions.optJSONArray("items");
			 for (int i = 0; i < permissionsItems.length(); i++) {
				 JSONObject obj = permissionsItems.getJSONObject(i);
				 actionPermissions.add(obj.optString("rolename"));
			 }
		}

		loadAttributeMap(properties, "action-properties", actionAutowiredProperties);
		loadAttributeMap(properties, "action-attributes", actionAttributes);
	}

	private void loadAttributeMap(JSONObject properties, String propertyName, Map<String, Object> targetMap) throws JSONException {
		String autowiredProps = properties.optString(propertyName);
		if (!StringUtils.isEmpty(autowiredProps)) {
			JSONObject jsonObj = new JSONObject(XmlUtil.decodeXmlEscapeCharacters(autowiredProps));
			Iterator i = jsonObj.keys();
			while (i.hasNext()) {
				String key = (String)i.next();
				targetMap.put(key, jsonObj.get(key));
			}
		}
	}
	
	@Override
	public String getObjectName() {
		return "Transition";
	}
	
	public String getDockers(int offsetX, int offsetY) {
		StringBuilder dockerString = new StringBuilder();
		for(Docker d : dockers) {
			dockerString.append(d.getX()+offsetX).append(',').append(d.getY()+offsetY);
			if(dockers.indexOf(d) == dockers.size() - 1)
				dockerString.append(":0,0");
			else
				dockerString.append(';');
		}
		return "g=\"" + dockerString + '"';
	}
	
	private class Docker {
		private int x;
		private int y;
		
		public int getX() {
			return x;
		}
		public void setX(int x) {
			this.x = x;
		}
		public int getY() {
			return y;
		}
		public void setY(int y) {
			this.y = y;
		}
		
		public Docker(int x, int y) {
			super();
			this.x = x;
			this.y = y;
		}
	}
}
