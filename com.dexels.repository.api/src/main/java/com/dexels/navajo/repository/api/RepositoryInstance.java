package com.dexels.navajo.repository.api;


import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RepositoryInstance extends Comparable<RepositoryInstance> {
	
	public File getRepositoryFolder();
	
	public File getTempFolder();

	public File getOutputFolder();

	public String getRepositoryName();

	public Map<String, Object> getSettings();

	public void addOperation(AppStoreOperation op, Map<String, Object> settings);

	public void removeOperation(AppStoreOperation op, Map<String, Object> settings);

	public List<String> getOperations();

	public void refreshApplication() throws IOException;
	
    public void refreshApplicationLocking() throws IOException;
	
	public String repositoryType();
	
	public String applicationType();

	public String getDeployment();
	
	public Set<String> getAllowedProfiles();
	
	public Map<String,Object> getDeploymentSettings(Map<String,Object> source);

	public boolean requiredForServerStatus();
	
	
}