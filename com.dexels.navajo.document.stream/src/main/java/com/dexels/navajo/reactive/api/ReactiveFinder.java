package com.dexels.navajo.reactive.api;

import java.util.Set;
import java.util.function.Function;

import com.dexels.navajo.expression.api.FunctionClassification;

public interface ReactiveFinder {

	public Set<String> sourceFactories();

	public Set<String> transformerFactories();

	public Set<String> reactiveMappers();

	public ReactiveSourceFactory getSourceFactory(String name);

	public ReactiveTransformerFactory getTransformerFactory(String name);

	public ReactiveMerger getMergerFactory(String name);

//	public void addReactiveSourceFactory(ReactiveSourceFactory factory);
//	
//	public void addReactiveTransformerFactory(ReactiveTransformerFactory transformer);

	public void addReactiveSourceFactory(ReactiveSourceFactory factory, String name);

	public void addReactiveTransformerFactory(ReactiveTransformerFactory factory, String name);
	
	public Function<String,FunctionClassification> functionClassifier();
}