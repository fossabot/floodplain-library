package com.dexels.navajo.reactive.transformer.eventstream;

import com.dexels.immutable.api.ImmutableMessage;
import com.dexels.navajo.document.Property;
import com.dexels.navajo.document.stream.DataItem.Type;
import com.dexels.navajo.document.stream.ReactiveParseProblem;
import com.dexels.navajo.reactive.api.ReactiveParameters;
import com.dexels.navajo.reactive.api.ReactiveTransformer;
import com.dexels.navajo.reactive.api.ReactiveTransformerFactory;
import com.dexels.navajo.reactive.api.TransformerMetadata;

import java.util.*;

import static com.dexels.immutable.api.ImmutableMessage.*;

public class EventStreamMessageTransformerFactory implements ReactiveTransformerFactory, TransformerMetadata {

	public EventStreamMessageTransformerFactory() {
	}

	@Override
	public ReactiveTransformer build(List<ReactiveParseProblem> problems,
			ReactiveParameters parameters) {
		return new EventStreamMessageTransformer(this,parameters);
	}

	
	@Override
	public Set<Type> inType() {
		return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(new Type[]{Type.SINGLEMESSAGE,Type.MESSAGE}))); // Type.SINGLEMESSAGE;
	}

	@Override
	public Type outType() {
		return Type.EVENTSTREAM;
	}

	@Override
	public Optional<List<String>> allowedParameters() {
		return Optional.of(Arrays.asList(new String[]{"messageName","isArray"}));
	}

	@Override
	public Optional<List<String>> requiredParameters() {
		return Optional.of(Arrays.asList(new String[]{"messageName"}));
	}

	@Override
	public Optional<Map<String, ValueType>> parameterTypes() {
		Map<String,ValueType> r = new HashMap<>();
		r.put("messageName", ValueType.STRING);
		r.put("isArray", ValueType.BOOLEAN);
		return Optional.of(r);
	}

	@Override
	public String name() {
		return "tmlstream";
	}


}
