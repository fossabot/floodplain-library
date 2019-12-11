package com.dexels.navajo.tipi.dev.server.websocket.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;


@Component(name="tipi.dev.websocket",immediate=true,service={Servlet.class},property={"alias=/websocket","servlet-name=websocket"})
public class CallbackServlet extends WebSocketServlet implements Runnable {

	private static final long serialVersionUID = 8386266364760399706L;
	
	private final Set<SCSocket> members = new CopyOnWriteArraySet<SCSocket>();
	private Thread heartbeatThread = null;

	private BundleContext bundleContext;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().write("get?");
	}
	
	

	@Override
	public void init() throws ServletException {
		super.init();
		heartbeatThread =  new Thread(this);
		heartbeatThread.start();
	}

	@Activate
	public void activate(Map<String,Object> settings, BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	@Deactivate
	public void deactivate() {
		this.bundleContext = null;
		if(heartbeatThread!=null) {
			heartbeatThread.interrupt();
			heartbeatThread = null;
		}
	}
	public BundleContext getBundleContext() {
		return bundleContext;
	}



	/*
	 * @see org.eclipse.jetty.websocket.WebSocketServlet#doWebSocketConnect(javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
//	@Override
//	public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
//		return new SCSocket(this);
//	}
//	
	public void addSocket(SCSocket s) {
		members.add(s);
	}

	public void removeSocket(SCSocket s) {
		members.remove(s);
	}

	
	private void notifyMembers(String message) {

		for(SCSocket member: members){
			if(member.isOpen()){
				try{
					member.sendMessage(message);
				} catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void run() {
		int count = 0;
		while(heartbeatThread!=null) {
			notifyMembers("Heartbeat #"+count);
			count++;
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
			}
		}
	}



	@Override
	public void configure(WebSocketServletFactory factory) {
        factory.getPolicy().setIdleTimeout(300000);
        factory.setCreator(new CallbackSocketCreator(this));
        factory.register(SCSocket.class);
	}
 
}
