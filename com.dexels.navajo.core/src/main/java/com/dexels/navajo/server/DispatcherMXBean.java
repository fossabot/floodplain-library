package com.dexels.navajo.server;

import com.dexels.navajo.server.resource.ResourceManager;

public interface DispatcherMXBean extends ResourceManager {

	/**
	 * Gets the instance_name as specified in the server.xml configuration file.
	 * @return
	 */
	public String getApplicationId();
	
	/**
	 * Gets the total number of currently active Navajo services.
	 * @return
	 */
	public int getAccessSetSize();
	
	/**
	 * Return the peak number of active Navajo services since startup.
	 * @return
	 */
	public int getPeakAccessSetSize();
	
	/**
	 * Resets the peak number of active Navajo services to zero.
	 */
	public void resetAccessSetPeakSize();
	
	/**
	 * Gets the (normalized) CPU load.
	 * @return
	 */
	public double getCPULoad();
	
	/**
	 * Return the current request rate (web services/second).
	 */
	public float getRequestRate();
	
	/**
	 * Return the total number of requests since startup.
	 * @return
	 */
	public long getRequestCount();

}
