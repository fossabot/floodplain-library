package com.dexels.navajo.reactive.stored;

import com.dexels.immutable.api.ImmutableMessage;
import com.dexels.navajo.document.Property;
import com.dexels.navajo.document.stream.DataItem.Type;
import com.dexels.navajo.reactive.api.ReactiveParameters;
import com.dexels.navajo.reactive.api.ReactiveSource;
import com.dexels.navajo.reactive.api.ReactiveSourceFactory;
import com.dexels.navajo.reactive.api.SourceMetadata;

import java.util.*;

public class InputStreamSourceFactory implements ReactiveSourceFactory, SourceMetadata {

	public InputStreamSourceFactory() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public ReactiveSource build(ReactiveParameters parameters) {
		return new InputStreamSource(this,parameters);
	}

	@Override
	public Type sourceType() {
		return Type.MESSAGE;
	}

	@Override
	public Optional<List<String>> allowedParameters() {
		return Optional.of(Arrays.asList(new String[]{"path","classpath"}));
	}

	@Override
	public Optional<List<String>> requiredParameters() {
		return Optional.of(Arrays.asList(new String[]{}));
	}

	@Override
	public Optional<Map<String, ImmutableMessage.ValueType>> parameterTypes() {
		return Optional.of(Map.of("path",ImmutableMessage.ValueType.STRING,"classpath", ImmutableMessage.ValueType.STRING));
	}

	@Override
	public String name() {
		return "input";
	}



}
