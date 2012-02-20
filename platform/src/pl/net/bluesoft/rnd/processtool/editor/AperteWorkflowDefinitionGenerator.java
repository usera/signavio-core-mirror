package pl.net.bluesoft.rnd.processtool.editor;

import com.signavio.platform.core.Platform;
import com.signavio.platform.core.PlatformProperties;
import com.signavio.platform.exceptions.RequestException;
import de.hpi.bpmn2_0.exceptions.BpmnConverterException;
import de.hpi.bpmn2_0.transformation.Diagram2XmlConverter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.aperteworkflow.editor.domain.Permission;
import org.aperteworkflow.editor.domain.ProcessConfig;
import org.aperteworkflow.editor.domain.Queue;
import org.aperteworkflow.editor.domain.QueueRolePermission;
import org.aperteworkflow.editor.json.ProcessConfigJSONHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oryxeditor.server.diagram.basic.BasicDiagram;
import org.oryxeditor.server.diagram.basic.BasicDiagramBuilder;
import org.xml.sax.SAXException;
import pl.net.bluesoft.rnd.processtool.editor.jpdl.exception.UnsupportedJPDLObjectException;
import pl.net.bluesoft.rnd.processtool.editor.jpdl.object.*;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.StringWriter;
import java.util.*;

public class AperteWorkflowDefinitionGenerator {

    //key = resourceId
    private Map<String, JPDLComponent> componentMap = new HashMap<String, JPDLComponent>();
    private Map<String, JPDLTransition> transitionMap = new HashMap<String, JPDLTransition>();

    private String json;

    private ProcessConfig processConfig;

    private final Logger logger = Logger.getLogger(AperteWorkflowDefinitionGenerator.class);

    private String processName;
    private String processFileName;
    private String bundleDesc;
    private String bundleName;
    private String processToolDeployment;
    private int offsetY;
    private int offsetX;
    private String processDefinitionLanguage;

    public AperteWorkflowDefinitionGenerator(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }


    public void init(String json) {
        try {
            this.json = json;
            JSONObject jsonObj = new JSONObject(json);

            processName = jsonObj.getJSONObject("properties").getString("name");
            processFileName = jsonObj.getJSONObject("properties").optString("aperte-process-filename");
            bundleDesc = jsonObj.getJSONObject("properties").optString("mf-bundle-description");
            bundleName = jsonObj.getJSONObject("properties").optString("mf-bundle-name");
            processToolDeployment = jsonObj.getJSONObject("properties").optString("mf-processtool-deployment");
            processDefinitionLanguage = jsonObj.getJSONObject("properties").optString("aperte-language");
            if (StringUtils.isEmpty(processName)) {
                throw new RequestException("Process name is empty.");
            }
            if (StringUtils.isEmpty(processFileName)) {
                throw new RequestException("Aperte process filename is empty.");
            }
            if (StringUtils.isEmpty(bundleName)) {
                throw new RequestException("Manifest Bundle-Name is empty.");
            }
            if (StringUtils.isEmpty(bundleDesc)) {
                throw new RequestException("Manifest Bundle-Description is empty.");
            }
            if (StringUtils.isEmpty(processToolDeployment)) {
                throw new RequestException("Manifest: ProcessTool-Process-Deployment is empty.");
            }


            String processConfJson = jsonObj.getJSONObject("properties").optString("process-conf");
            if (processConfJson != null && !processConfJson.trim().isEmpty()) {
                processConfig = ProcessConfigJSONHandler.getInstance().toObject(processConfJson);
            }

            JSONArray childShapes = jsonObj.getJSONArray("childShapes");
            for (int i = 0; i < childShapes.length(); i++) {
                JSONObject obj = childShapes.getJSONObject(i);
                JPDLObject jpdlObject = JPDLObject.getJPDLObject(obj);
                jpdlObject.fillBasicProperties(obj);
                if (jpdlObject instanceof JPDLComponent) {
                    ((JPDLComponent) jpdlObject).applyOffset(offsetX, offsetY);
                    componentMap.put(jpdlObject.getResourceId(), (JPDLComponent) jpdlObject);
                } else if (jpdlObject instanceof JPDLTransition) {
                    transitionMap.put(jpdlObject.getResourceId(), (JPDLTransition) jpdlObject);
                }
            }
        } catch (JSONException e) {
            logger.error("Error while generating JPDL file.", e);
            throw new RequestException("Error while generating JPDL file.", e);
        } catch (UnsupportedJPDLObjectException e) {
            logger.error("Error while generating JPDL file.", e);
            throw new RequestException(e.getMessage());
        }

        //second pass, complete the transition map
        for (String key : componentMap.keySet()) {
            JPDLComponent cmp = componentMap.get(key);
            for (String resourceId : cmp.getOutgoing().keySet()) {
                JPDLTransition transition = transitionMap.get(resourceId);
                transition.setTargetName(componentMap.get(transition.getTarget()).getName());
                cmp.putTransition(resourceId, transition);
            }
        }

    }

    public String generateDefinition() {
        if ("jpdl".equals(processDefinitionLanguage)) {
            return generateJpdl();
        } else {
            return generateBpmn20();
        }

    }


    public String generateJpdl() {
        StringBuffer jpdl = new StringBuffer();
        jpdl.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        jpdl.append(String.format("<process name=\"%s\" xmlns=\"http://jbpm.org/4.4/jpdl\">\n", processName));

        Set<String> swimlanes = new HashSet<String>();

        for (String key : componentMap.keySet()) {
            JPDLComponent comp = componentMap.get(key);
            if (comp instanceof JPDLUserTask) {
                JPDLUserTask userTask = (JPDLUserTask) comp;
                if (userTask.getSwimlane() != null && userTask.getSwimlane().trim().length() > 0) {
                    swimlanes.add(userTask.getSwimlane());
                }
            }
        }

        Iterator<String> i = swimlanes.iterator();
        while (i.hasNext()) {
            String sl = i.next();
            jpdl.append(String.format("<swimlane candidate-groups=\"%s\" name=\"%s\"/>\n", sl, sl));
        }


        for (String key : componentMap.keySet()) {
            jpdl.append(componentMap.get(key).toXML());
        }

        jpdl.append("</process>");

        return jpdl.toString();
    }

    public String generateBpmn20() {

        try {
//            PlatformProperties props = Platform.getInstance().getPlatformProperties();
            JSONObject jsonObj = new JSONObject(json);
            jsonObj.put("resource-id", jsonObj.getJSONObject(""))
            JSONArray childShapes = jsonObj.getJSONArray("childShapes");
            for (int i = 0; i < childShapes.length(); i++) {
                JSONObject obj = childShapes.getJSONObject(i);
                //update user step with assignment data
                String stencilId = obj.getJSONObject("stencil").getString("id");

                if ("Task".equals(stencilId)) {
                    String taskType = obj.getJSONObject("properties").getString("tasktype");
                    if ("User".equals(taskType)) {
                        enrichBpmn20AssignmentConfig(obj);
                    } else {
                        enrichBpmn20JavaTask(obj);
                    }
                } else if ("SequenceFlow".equals(stencilId)) {

                } else if ("Exclusive_Databased_Gateway".equals(stencilId)) {

                }
            }
            String jsonForBpmn20 = jsonObj.toString();
            BasicDiagram diagram = BasicDiagramBuilder.parseJson(jsonForBpmn20);
            
            Diagram2XmlConverter converter = new Diagram2XmlConverter(diagram,
                    Platform.getInstance().getFile("/WEB-INF/xsd/BPMN20.xsd").getAbsolutePath());

            return converter.getXml().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void enrichBpmn20JavaTask(JSONObject obj) {

    }


    private void enrichBpmn20AssignmentConfig(JSONObject obj) throws JSONException {
        JSONObject aperteCfg = new JSONObject(obj.getJSONObject("properties").getString("aperte-conf"));
        String assignee = aperteCfg.optString("assignee");
        //It looks like swimlanes are unsupported in Activiti :(
//        String swimlane = obj.optString("swimlane");
        String candidateGroups = aperteCfg.optString("candidate_groups");
        /*
        resources: {
            items: [{
                resource_type: "humanperformer",
                resourceassignmentexpr: "#{creator}"
            }, {
                resource_type: "potentialowner",
                resourceassignmentexpr: "TESTQ"
            },]
        }
         */
        //resources/items[0]/resource_type
        JSONObject resources = new JSONObject();
        JSONArray items = new JSONArray();
        if (assignee != null && !assignee.trim().isEmpty()) {
            JSONObject o = new JSONObject();
            o.put("resource_type", "humanperformer");
            o.put("resourceassignmentexpr", assignee);
            items.put(o);
        }
        if (candidateGroups != null && !candidateGroups.trim().isEmpty()) {
            JSONObject o = new JSONObject();
            o.put("resource_type", "potentialowner");
            o.put("resourceassignmentexpr", candidateGroups);
            items.put(o);
        }
        resources.put("items", items);
        resources.put("totalCount", items.length());
        if (items.length() != 0) {
            if (obj.getJSONObject("properties").opt("resources") != null)
                obj.getJSONObject("properties").remove("resources");
            obj.getJSONObject("properties").put("resources", resources);//overwrite
        }
    }

    public String generateProcessToolConfig() {
        StringBuffer ptc = new StringBuffer();
        ptc.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        ptc.append(String.format("<config.ProcessDefinitionConfig bpmDefinitionKey=\"%s\" description=\"%s\" processName=\"%s\">\n", processName, processName, processName));

        if (processConfig != null) {

            if (processConfig.getComment() != null && !processConfig.getComment().isEmpty()) {
                ptc.append(String.format("<comment>%s</comment>", XmlUtil.wrapCDATA(processConfig.getComment())));
            }

            if (processConfig.getProcessPermissions() != null && !processConfig.getProcessPermissions().isEmpty()) {
                ptc.append("<permissions>\n");

                for (Permission permission : processConfig.getProcessPermissions()) {
                    ptc.append(String.format("<config.ProcessDefinitionPermission privilegeName=\"%s\" roleName=\"%s\"/>", permission.getPrivilegeName(), permission.getRoleName()));
                }

                ptc.append("</permissions>\n");
            }

        }

        ptc.append("<states>\n");

        //processtool-config.xml generation
        for (String key : componentMap.keySet()) {
            JPDLComponent cmp = componentMap.get(key);
            if (cmp instanceof JPDLUserTask) {
                JPDLUserTask task = (JPDLUserTask) cmp;
                if (task.getWidget() != null) {
                    ptc.append(task.generateWidgetXML());
                }
            }
        }

        ptc.append("</states>\n");
        ptc.append("</config.ProcessDefinitionConfig>\n");
        return ptc.toString();
    }

    public String generateQueuesConfig() {

        StringBuffer q = new StringBuffer();
        q.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        q.append("<list>\n");

        if (processConfig != null) {
            if (processConfig.getQueues() != null && !processConfig.getQueues().isEmpty()) {
                for (Queue queue : processConfig.getQueues()) {
                    String description = queue.getDescription();
                    if (description == null) {
                        description = queue.getName();
                    }

                    q.append(String.format(
                            "<config.ProcessQueueConfig name=\"%s\" description=\"%s\">\n",
                            queue.getName(),
                            description
                    ));
                    q.append("<rights>\n");

                    if (queue.getRolePermissions() != null && !queue.getRolePermissions().isEmpty()) {
                        for (QueueRolePermission rolePermission : queue.getRolePermissions()) {
                            q.append(String.format(
                                    "<config.ProcessQueueRight roleName=\"%s\" browseAllowed=\"%b\"/>\n",
                                    rolePermission.getRoleName(),
                                    rolePermission.isBrowsingAllowed()
                            ));
                        }
                    }

                    q.append("</rights>\n");
                    q.append("</config.ProcessQueueConfig>\n");
                }
            }
        }

        q.append("</list>\n");
        return q.toString();
    }

    public String getProcessDefinitionLanguage() {
        return processDefinitionLanguage;
    }

    public String getProcessName() {
        return processName;
    }

    public String getProcessFileName() {
        return processFileName;
    }

    public String getBundleDesc() {
        return bundleDesc;
    }

    public String getBundleName() {
        return bundleName;
    }

    public String getProcessToolDeployment() {
        return processToolDeployment;
    }

    public Map<String, String> getMessages() {
        if (processConfig == null) {
            return null;
        }
        return processConfig.getMessages();
    }

    public byte[] getProcessIcon() {
        if (processConfig == null) {
            return null;
        }
        return processConfig.getProcessIcon();
    }


}
