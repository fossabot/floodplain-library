package com.dexels.kafka.streams.transformer.custom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.dexels.immutable.api.ImmutableMessage;
import com.dexels.immutable.factory.ImmutableFactory;
import com.dexels.replication.api.ReplicationMessage;
import com.dexels.replication.transformer.api.MessageTransformer;

public class CommunicationTransformer implements MessageTransformer {

	@Override
	public ReplicationMessage apply(Map<String, String> params, ReplicationMessage msg) {
		Optional<List<ImmutableMessage>> o = msg.subMessages("Communication");
		List<ImmutableMessage> comm = o.isPresent()?o.get():Collections.emptyList();
		Map<String,Object> commdata = new HashMap<>();
		Map<String,String> commtypes = new HashMap<>();
		List<ImmutableMessage> twitterList = new ArrayList<>();
		for (ImmutableMessage e : comm) {
			String type = (String) e.value("typeofcommunication").orElse("");
			if("TWITTER".equals(type)) {
				twitterList.add(e.without("organizationid")
						.without("organizationtype")
						.without("preferred")
					);
			} else {
				Integer preferred = (Integer) e.columnValue("preferred");
				if(preferred!=null) {
					switch(preferred) {
						case 3:
							commdata.put("Email",e.columnValue("communicationdata"));
							commtypes.put("Email", "string");
							break;
						case 1:
							commdata.put("Telephone",e.columnValue("communicationdata"));
							commtypes.put("Telephone", "string");
							break;
						case 5:
							commdata.put("MobilePhone",e.columnValue("communicationdata"));
							commtypes.put("MobilePhone", "string");
							break;
						case 4:
							commdata.put("URL",e.columnValue("communicationdata"));
							commtypes.put("URL", "string");
							break;
						default:
							commdata.put("Other",e.columnValue("communicationdata"));
							commtypes.put("Other", "string");
							break;
					}
				}
				
			}
		}
		
		ImmutableMessage result = ImmutableFactory.create(commdata,commtypes);
		return msg.withoutSubMessages("Communication").withSubMessage("Communication", result).withSubMessages("Twitter", twitterList);
	}

}
