package com.dexels.replication.impl.protobuf;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.dexels.replication.api.ReplicationMessage;
import com.dexels.replication.api.ReplicationMessageParser;
import com.dexels.replication.factory.ReplicationFactory;
import com.dexels.replication.impl.json.JSONReplicationMessageParserImpl;

public class TestSer {
// /TODO protobuf binaries

	@Before
	public void setup() {
		
	}
	
	@Test
	public void testBinaryJSON() {
		final ReplicationMessageParser parser = new JSONReplicationMessageParserImpl();
		byte[] payload = "123".getBytes();
		final byte[] deserialized = testSerialization(parser, payload);
		Assert.assertArrayEquals(payload, deserialized);
	}
	
	@Test
	public void testBinaryProtobuf() {
		final ReplicationMessageParser parser = new FallbackReplicationMessageParser(true);
		byte[] payload = "123".getBytes();
		final byte[] deserialized = testSerialization(parser, payload);
		Assert.assertArrayEquals(payload, deserialized);
	}

	private byte[] testSerialization(final ReplicationMessageParser parser, byte[] payload) {
		ReplicationMessage r = ReplicationFactory.empty().with("binary", payload, "binary");
		byte[] encoded = r.toBytes(parser);
		ReplicationMessage s = parser.parseBytes(Optional.of("binary"),encoded);
		final byte[] deserialized = (byte[]) s.columnValue("binary");
//		System.err.println("payload: "+new String(deserialized));
		return deserialized;
	}
	
	
}
