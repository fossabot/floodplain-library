package com.dexels.navajo.reactive.topology;

import java.util.Stack;

import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.kafka.streams.api.TopologyContext;
import com.dexels.kafka.streams.remotejoin.ReplicationTopologyParser;
import com.dexels.kafka.streams.remotejoin.TopologyConstructor;
import com.dexels.navajo.reactive.api.CompiledReactiveScript;
import com.dexels.navajo.reactive.api.ReactivePipe;
import com.dexels.navajo.reactive.source.topology.api.TopologyPipeComponent;

public class ReactivePipeParser {
	
	
	private final static Logger logger = LoggerFactory.getLogger(ReactivePipeParser.class);

	public static Topology parseReactiveStreamDefinition(CompiledReactiveScript crs, TopologyContext topologyContext, TopologyConstructor topologyConstructor) {
		Topology topology = new Topology();
		int pipeNr = 0;
		for (ReactivePipe pipe : crs.pipes) {
			ReactivePipeParser.processPipe(topologyContext, topologyConstructor, topology, pipeNr,new Stack<String>(), pipe);
		}
		ReplicationTopologyParser.materializeStateStores(topologyConstructor, topology);
		return topology;
	}
	
	public static int processPipe(TopologyContext topologyContext, TopologyConstructor topologyConstructor, Topology topology,
			int pipeNr, Stack<String> pipeStack, ReactivePipe pipe) {
		int size = pipe.transformers.size();
		for (int i = size; i >= 0; i--) {
			System.err.println(">>> "+i);
			TopologyPipeComponent source = (TopologyPipeComponent)pipe.source;
			if(i==0) {
				System.err.println("processing source");
			} else {
				Object type = pipe.transformers.get(i-1);
				if(type instanceof TopologyPipeComponent) {
					TopologyPipeComponent tpc = (TopologyPipeComponent)type;
					TopologyPipeComponent parent = i-2 < 0 ? source : (TopologyPipeComponent) pipe.transformers.get(i-2);
					System.err.println("processing transformer: "+(i-1));
					if(tpc.materializeParent()) {
						System.err.println("Materializing parent");
						parent.setMaterialize();
					}
					
//					pipeNr = tpc.addToTopology(pipeStack, pipeNr, topology, topologyContext, topologyConstructor);
				} else {
					System.err.println("Weird type found: "+type);
				}
				
			}
			
		}

		TopologyPipeComponent sourceTopologyComponent = (TopologyPipeComponent)pipe.source;
		pipeNr = sourceTopologyComponent.addToTopology(pipeStack, pipeNr, topology, topologyContext, topologyConstructor);
		for (Object e : pipe.transformers) {
			System.err.println("Transformer: "+e+" pipestack: "+pipeStack);
			if(e instanceof TopologyPipeComponent) {
				TopologyPipeComponent tpc = (TopologyPipeComponent)e;
				logger.info("Adding pipe component: "+tpc.getClass()+" to stack: "+pipeStack);
				pipeNr = tpc.addToTopology(pipeStack, pipeNr, topology, topologyContext, topologyConstructor);
			}
		}

		return pipeNr;
	}

}
