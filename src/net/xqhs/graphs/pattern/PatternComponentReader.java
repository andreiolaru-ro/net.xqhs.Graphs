package net.xqhs.graphs.pattern;

import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.representation.text.DefaultComponentReader;
import net.xqhs.graphs.representation.text.GraphComponentReader;

/**
 * {@link GraphComponentReader} implementation that can read pattern components.
 *
 * @author andreiolaru
 */
public class PatternComponentReader extends DefaultComponentReader
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
			return new NodeP(index);
		}
		return super.readNode(rawInput);
	}
}
