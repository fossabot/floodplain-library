package com.dexels.navajo.document.stream.json;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.davidmoten.rx2.Bytes;

import io.reactivex.Flowable;

public class JSONObservableTest {

	@Test
	public void testRxJSON() {
		long l = Bytes.from(JSONObservableTest.class.getClassLoader().getResourceAsStream("person.json"))
			.lift(JSON.parseFlowable(10))
			.flatMap(e->e)
			.doOnNext(e->System.err.println("Event: "+e))
					
			.count()
			.blockingGet();
		Assert.assertEquals(364, l);
		//			.blockingForEach(e->System.err.println("Element: "+e+" -> "));
	}
	
	@Test
	public void testSingleJSON() {
		Flowable<byte[]> dataSource = Bytes.from(JSONObservableTest.class.getClassLoader().getResourceAsStream("testlist.json"),10);
		JsonNode node = dataSource
				.compose(JSON.collectBytesToSingle())
				.firstOrError()
				.map(input->JSON.parseObjectNodes(input))
				.blockingGet();
		
			
		ArrayNode nde = (ArrayNode) node.get("Objects");
		int size = nde.size();
		Assert.assertEquals(5, size);
		//			.blockingForEach(e->System.err.println("Element: "+e+" -> "));
	}

}