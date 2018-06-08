package net.xqhs.graphs.nlp;

import java.util.ArrayList;

import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.text.DefaultComponentReader;
import net.xqhs.graphs.representation.text.GraphComponentReader;
import net.xqhs.graphs.representation.text.SettableEdge;

/**
 * {@link GraphComponentReader} instance that can read {@link NLNode} instances.
 *
 * @author andreiolaru
 */
public class NLComponentReader extends DefaultComponentReader
{
	@Override
	public SimpleNode readNode(String rawInput)
	{
		if(rawInput.startsWith(NodeP.NODEP_LABEL)
				&& rawInput.substring(NodeP.NODEP_LABEL.length()).startsWith(NodeP.NODEP_INDEX_MARK))
		{ // node is a generic node (?#number)
			int index = Integer
					.parseInt(rawInput.substring(NodeP.NODEP_LABEL.length() + NodeP.NODEP_INDEX_MARK.length()));
			// log.li("create new pattern node #[]", new Integer(index));
			return new NLNodeP(index);
		}
		String[] parts = rawInput.split("\\{");
		String[] detail = parts[1].substring(0, parts[1].length() - 1).split("\\|");
		ArrayList<FunctionWord> attrs = new ArrayList<>();
		for(int i = 3; i < detail.length; i++)
		{
			String[] fw = detail[i].split("/");
			attrs.add(new FunctionWord(fw[0], fw[1], Integer.parseInt(fw[2])));
		}
		return new NLNodeP(parts[0], Integer.parseInt(detail[0]), detail[1], detail[2], attrs);
	}
	
	@Override
	public SimpleEdge compileEdge(SettableEdge settableEdge)
	{
		return new NLEdgeP((NLNodeP) settableEdge.getFrom(), (NLNodeP) settableEdge.getTo(), settableEdge.getLabel());
	}
}