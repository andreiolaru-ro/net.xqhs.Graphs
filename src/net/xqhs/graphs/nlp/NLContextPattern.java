package net.xqhs.graphs.nlp;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.graphs.context.ContextPattern;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.pattern.NodeP;

public class NLContextPattern extends ContextPattern {

	Map<Integer, NodeP> nodesByIndex;

	public NLContextPattern(ContextPattern cxt) throws Exception {
		nodesByIndex = new HashMap<Integer, NodeP>();
		for (Node node : cxt.getNodes()) {
			if (node instanceof NLNodeP) {
				NLNodeP nlNode = (NLNodeP) node;
				nodesByIndex.put(nlNode.getWordIndex(), nlNode);
			}

			else
				throw new Exception(
						"Cannot construct a NLContextPattern from non-NL nodes");
		}
	}

}
