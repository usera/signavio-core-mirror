package pl.net.bluesoft.rnd.processtool.editor.jpdl.object;

import pl.net.bluesoft.rnd.processtool.editor.AperteWorkflowDefinitionGenerator;
import pl.net.bluesoft.rnd.processtool.editor.IndentedStringBuilder;

public class JPDLStartEvent extends JPDLComponent {
    public JPDLStartEvent(AperteWorkflowDefinitionGenerator generator) {
        super(generator);
    }

    @Override
	public void toXML(IndentedStringBuilder sb) {
		sb.append(String.format("<start name=\"%s\" g=\"%d,%d,%d,%d\">\n", name,boundsX, boundsY, width, height));
		sb.begin();
		getTransitionsXML(sb);
		sb.end();
		sb.append("</start>\n");
    }
	
	@Override
	public String getObjectName() {
		return "Start Event";
	}
}
