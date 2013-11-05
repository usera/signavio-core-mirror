package pl.net.bluesoft.rnd.processtool.editor.jpdl.object;

import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pl.net.bluesoft.rnd.processtool.editor.AperteWorkflowDefinitionGenerator;
import pl.net.bluesoft.rnd.processtool.editor.IndentedStringBuilder;

public abstract class JPDLComponent extends JPDLObject {

	protected Map<String, JPDLTransition> outgoing = new TreeMap<String, JPDLTransition>();

    protected int boundsX, boundsY, width, height;
    protected int offsetX, offsetY;

    protected JPDLComponent(AperteWorkflowDefinitionGenerator generator) {
        super(generator);
    }

    public void applyOffset(int offsetX, int offsetY) {
        boundsX = boundsX + offsetX;
        boundsY = boundsY + offsetY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }
    public Map<String, JPDLTransition> getOutgoing() {
		return outgoing;
	}

	public void setOutgoing(Map<String, JPDLTransition> outgoing) {
		this.outgoing = outgoing != null ? new TreeMap<String, JPDLTransition>(outgoing) : null;
	}
	
	public JPDLTransition getTransition(String resourceId) {
		return outgoing.get(resourceId);
	}
	
	public void putTransition(String resourceId, JPDLTransition transition) {
		outgoing.put(resourceId, transition);
	}
	
	public abstract void toXML(IndentedStringBuilder sb);
	
	@Override
	public void fillBasicProperties(JSONObject json) throws JSONException {
		super.fillBasicProperties(json);
		
		JSONArray array = json.getJSONArray("outgoing");
		
		for (int i = 0; i < array.length(); i++) {
			JSONObject arrObj = array.getJSONObject(i);
			outgoing.put(arrObj.getString("resourceId"), null);
		}

        JSONObject bounds = json.getJSONObject("bounds");
        if (bounds != null) {
            JSONObject upperLeft = bounds.getJSONObject("upperLeft");
            JSONObject lowerRight = bounds.getJSONObject("lowerRight");
            boundsX = upperLeft.getInt("x");
            boundsY = upperLeft.getInt("y");
            this.width = lowerRight.getInt("x") - boundsX;
            this.height = lowerRight.getInt("y") - boundsY;
        }
	}
	
	protected void getTransitionsXML(IndentedStringBuilder sb) {
		for (JPDLTransition transition : getOrderedTransitions()) {
			sb.append(String.format("<transition %s name=\"%s\" to=\"%s\">\n", transition.getDockers(offsetX, offsetY), transition.getName(), transition.getTargetName()));
			sb.begin();
			if (transition.getCondition() != null && !transition.getCondition().trim().isEmpty()) {
				sb.append(String.format("<condition expr=\"%s\"/>\n", transition.getCondition()));
			}
			sb.end();
			sb.append("</transition>\n");
		}
	}

	public List<JPDLTransition> getOrderedTransitions() {
		List<JPDLTransition> result = new ArrayList<JPDLTransition>(outgoing.values());
		Collections.sort(result, BY_NAME);
		return result;
	}

	private static final Comparator<JPDLTransition> BY_NAME = new Comparator<JPDLTransition>() {
		@Override
		public int compare(JPDLTransition t1, JPDLTransition t2) {
			String n1 = t1.getName();
			String n2 = t2.getName();

			if (n1 == n2) {
				return 0;
			}
			if (n1 == null) {
				return 1;
			}
			if (n2 == null) {
				return -1;
			}
			return n1.compareTo(n2);
		}
	};
}
