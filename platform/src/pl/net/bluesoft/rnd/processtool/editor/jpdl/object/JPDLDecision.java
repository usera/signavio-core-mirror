package pl.net.bluesoft.rnd.processtool.editor.jpdl.object;

import org.json.JSONException;
import org.json.JSONObject;
import pl.net.bluesoft.rnd.processtool.editor.AperteWorkflowDefinitionGenerator;
import pl.net.bluesoft.rnd.processtool.editor.IndentedStringBuilder;

public class JPDLDecision extends JPDLComponent {
    public JPDLDecision(AperteWorkflowDefinitionGenerator generator) {
        super(generator);
    }

    @Override
	public void fillBasicProperties(JSONObject json) throws JSONException {
		super.fillBasicProperties(json);
	}
    
    @Override
	public void toXML(IndentedStringBuilder sb) {
    	sb.append(String.format("<decision name=\"%s\" g=\"%d,%d,%d,%d\" >\n", name,
                boundsX, boundsY, width, height
                ));
		sb.begin();
		getTransitionsXML(sb);
		sb.end();
		sb.append("</decision>\n");
    }

	@Override
	public String getObjectName() {
		return "Decision";
	}
}
