package com.dexels.navajo.tipi.dev.server.appmanager.operations.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.dexels.navajo.repository.api.AppStoreOperation;
import com.dexels.navajo.repository.api.RepositoryInstance;
import com.dexels.navajo.tipi.dev.server.appmanager.AppStoreManager;
import com.dexels.navajo.tipi.dev.server.appmanager.operations.core.BaseOperation;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Component(name="tipi.dev.operation.authorize",immediate=true,service={Servlet.class},property={"alias=/authorize","name=authorize","servlet-name=authorize","type=global"})
public class Authorize extends BaseOperation implements AppStoreOperation {

	private static final long serialVersionUID = 8640712571228602628L;
	
	@Activate
	public void activate(Map<String,Object> settings) {
		super.activate(settings);
	}
	
	@Deactivate
	public void deactivate() {
		super.deactivate();
	}
	
	
	@Reference(unbind="clearAppStoreManager",policy=ReferencePolicy.DYNAMIC)
	public void setAppStoreManager(AppStoreManager am) {
		super.setAppStoreManager(am);
	}

	public void clearAppStoreManager(AppStoreManager am) {
		super.clearAppStoreManager(am);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		HttpSession session = req.getSession();
		String code = req.getParameter("code");
		resp.setContentType("application/json");
		Map<String,String> params = new HashMap<String, String>();
		params.put("client_id", appStoreManager.getClientId());
		params.put("code", code);
		params.put("client_secret", appStoreManager.getClientSecret());

		post("https://github.com/login/oauth/access_token",params,session);
		final String access_token = (String) session.getAttribute("access_token");
		JsonNode user = callGithub("/user", access_token);
		final String login = user.get("login").asText();
		System.err.println("username: "+login);
		JsonNode username = user.get("name");
		
		boolean found = false;
		String organization = appStoreManager.getOrganization();
		ArrayNode members = (ArrayNode) callGithub("/orgs/"+organization+"/members", access_token);
		for (JsonNode member : members) {
			String currentLogin = member.get("login").asText();
			if(currentLogin.equals(login)) {
				found = true;
			}
		}
		if(found) {
			if (username!=null) {
				System.err.println("Name: "+username.asText());
				session.setAttribute("username", username.asText());
			} else {
				session.setAttribute("username", "<Unknown>");
			}
			session.setAttribute("authorized", true);
			final JsonNode avatarUrl = user.get("avatar_url");
			if (avatarUrl!=null) {
				session.setAttribute("image", avatarUrl.asText());
			} else {
//				session.setAttribute("image", avatarUrl.asText());
			}
			final JsonNode company = user.get("company");
			if (company!=null) {
				session.setAttribute("company", company.asText());
			} else {
				session.setAttribute("company", "Unknown company");
			}
			final JsonNode email = user.get("email");
			if (email!=null) {
				session.setAttribute("email", email.asText());
			} else {
				session.setAttribute("email", "unknown@email");
			}
		}
		resp.sendRedirect("ui/index.html");
	}
	
	
	private JsonNode callGithub(String path, String access_token) throws JsonParseException, IOException {
		String url = "https://api.github.com"+path+"?access_token="+access_token;
		URL u = new URL(url);
		ObjectMapper mapper = new ObjectMapper();
		JsonFactory factory = mapper.getFactory(); // since 2.1 use mapper.getFactory() instead
		JsonParser jp = factory.createParser(u.openStream());
		JsonNode node = mapper.readTree(jp);		
		mapper.writerWithDefaultPrettyPrinter().writeValue(System.err,node);
		return node;
		
		
	}
	private void post(String url, Map<String,String> params, HttpSession session) throws IOException {
		URL u = new URL(url);
		HttpURLConnection uc = (HttpURLConnection) u.openConnection();

		uc.setReadTimeout(10000);
		uc.setConnectTimeout(15000);
		uc.setRequestMethod("POST");
		uc.setRequestProperty("Accept", "application/json");
		uc.setDoInput(true);
		uc.setDoOutput(true);
		OutputStream os = uc.getOutputStream();
		BufferedWriter writer = new BufferedWriter(
		        new OutputStreamWriter(os, "UTF-8"));
		writer.write(getPostParamString(params));
		writer.flush();
		writer.close();
		os.close();

		uc.connect();	
		JsonFactory jfactory = new JsonFactory();
		 
		/*** read from file ***/
		JsonParser jp = jfactory.createParser(uc.getInputStream());

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.readTree(jp);		
		
		String token = node.get("access_token").asText();
		
		session.setAttribute("access_token", token);
		session.setAttribute("authorized", true);
		
	}
	
	private static String getPostParamString(Map<String, String> params) throws UnsupportedEncodingException {
	    if(params.size() == 0)
	        return "";

	    StringBuffer buf = new StringBuffer();
	    Set<String> keys = params.keySet();
	    for (String key : keys) {
	        buf.append(buf.length() == 0 ? "" : "&");
	        buf.append(URLEncoder.encode(key,"UTF-8")).append("=").append(URLEncoder.encode(params.get(key),"UTF-8"));
		}
	    return buf.toString();
	}

	@Override
	public void build(RepositoryInstance a) throws IOException {

	}
}
