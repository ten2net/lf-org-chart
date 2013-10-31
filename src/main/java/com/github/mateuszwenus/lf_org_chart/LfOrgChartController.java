package com.github.mateuszwenus.lf_org_chart;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.portlet.PortletException;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceURL;
import javax.servlet.http.HttpServletRequest;

import com.github.mateuszwenus.lf_org_chart.loaders.ChildrenLoader;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;

public class LfOrgChartController extends MVCPortlet {

	private final Log log = LogFactoryUtil.getLog(getClass());

	private final Gson gson = new Gson();
	private final ChildrenLoader childrenLoader = new ChildrenLoader();

	public void serveResource(ResourceRequest resourceRequest, ResourceResponse resourceResponse) throws IOException, PortletException {
		resourceResponse.setContentType("application/json;charset=UTF-8");
		PrintWriter out = resourceResponse.getWriter();
		HttpServletRequest httpRequest = PortalUtil.getOriginalServletRequest(PortalUtil.getHttpServletRequest(resourceRequest));
		String resourceId = httpRequest.getParameter("p_p_resource_id");
		if ("loadChildren".equals(resourceId)) {
			loadChildren(resourceRequest, resourceResponse, httpRequest, out);
		}
		out.flush();
	}

	private void loadChildren(ResourceRequest request, ResourceResponse response, HttpServletRequest httpRequest, PrintWriter out) {
		String ppId = httpRequest.getParameter("p_p_id");
		String uid = httpRequest.getParameter("uid");
		Long nodeId = getNodeId(httpRequest);
		NodeType nodeType = getNodeType(httpRequest);
		if (validateParams(ppId, uid, nodeId, nodeType)) {
			List<Node> nodes = childrenLoader.loadChildren(nodeType, nodeId, request);
			String basicURL = createBasicURL(response, ppId);
			List<AuiNode> auiNodes = new ArrayList<AuiNode>(nodes.size());
			for (Node node : nodes) {
				auiNodes.add(new AuiNode(basicURL, uid, node));
			}
			sendResponse(auiNodes, out);
		} else {
			sendResponse(Collections.<AuiNode>emptyList(), out);
		}
	}

	private Long getNodeId(HttpServletRequest httpRequest) {
		String id = httpRequest.getParameter("id");
		Long result = null;
		if (id != null) {
			try {
				result = Long.parseLong(id);
			} catch (NumberFormatException ignored) {
			}
		}
		return result;
	}

	private NodeType getNodeType(HttpServletRequest httpRequest) {
		return NodeType.forString(httpRequest.getParameter("type"));
	}

	private boolean validateParams(String ppId, String uid, Long nodeId, NodeType nodeType) {
		return true; // TODO
	}
	
	private String createBasicURL(ResourceResponse response, String ppId) {
		ResourceURL resURL = response.createResourceURL();
		String basicURL = resURL.toString();
		basicURL = basicURL.substring(0, basicURL.indexOf('?'));
		basicURL += "?p_p_id=" + ppId + "&p_p_resource_id=loadChildren&p_p_lifecycle=2";
		return basicURL;
	}

	private void sendResponse(List<AuiNode> nodes, PrintWriter out) {
		Type typeOfSrc = new TypeToken<List<AuiNode>>() {
		}.getType();
		String json = gson.toJson(nodes, typeOfSrc);
		out.write(json);
	}
}
