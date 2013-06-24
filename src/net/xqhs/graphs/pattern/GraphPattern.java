/*******************************************************************************
 * Copyright (C) 2013 Andrei Olaru.
 * 
 * This file is part of net.xqhs.Graphs.
 * 
 * net.xqhs.Graphs is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * net.xqhs.Graphs is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with net.xqhs.Graphs.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.graphs.pattern;

import java.util.ArrayList;
import java.util.Collection;

import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleGraph;
import net.xqhs.util.logging.Unit;
import net.xqhs.util.logging.UnitConfigData;

/**
 * Graph patterns are graphs that allow nodes with unspecified labels (marked with question marks) and edges labeled
 * with regular expressions.
 * <p>
 * The class inherits from {@link SimpleGraph}.
 * <p>
 * It is expected that a {@link GraphPattern} only contains elements that are instances of {@link NodeP} and
 * {@link EdgeP}.
 * 
 * @author Andrei Olaru
 * 
 */
public class GraphPattern extends SimpleGraph
{
	/**
	 * Creates an empty graph pattern.
	 */
	public GraphPattern()
	{
		this(null);
	}
	
	/**
	 * Creates an empty graph pattern and configures the underlying {@link Unit} for logging.
	 * 
	 * @param unitConfig
	 *            - configuration for the underlying {@link Unit}.
	 */
	public GraphPattern(UnitConfigData unitConfig)
	{
		super(unitConfig);
	}
	
	/**
	 * Only {@link NodeP} instances can be added to a {@link GraphPattern}. This makes it easier to work with the nodes
	 * in the pattern, because conversion won't fail.
	 * <p>
	 * This does not stop {@link NodeP} instances to represent normal, labeled nodes.
	 * <p>
	 * Generic nodes will be (re)indexed.
	 */
	// the parameter is not of NodeP type because this would allow someone to easily work around this method and call
	// the method in the super class.
	@Override
	public GraphPattern addNode(Node node)
	{
		if(!(node instanceof NodeP))
			throw new IllegalArgumentException("cannot add Node instances to a pattern");
		return this.addNode((NodeP) node, true);
	}
	
	/**
	 * Adds a node to the graph, also indexing it if necessary (for generic {@link NodeP} instances).
	 * <p>
	 * <b>Warning:</b> while the method allows not indexing the added generic nodes (by setting <code>doindex</code> to
	 * <code>false</code>), this is strongly discouraged and should be used with caution.
	 * 
	 * @param node
	 *            - the node to be added
	 * @param doindex
	 *            - if set to <code>true</code>, the node will be (re)indexed according to the pre-existing nodes in the
	 *            graph
	 * @return the graph itself
	 */
	public GraphPattern addNode(NodeP node, boolean doindex)
	{
		if(doindex)
		{
			int maxIdx = 0;
			NodeP lastEquiv = null;
			for(Node n : this.nodes)
				if((n.getLabel().equals(node.getLabel())) && (maxIdx <= ((NodeP) n).labelIndex))
				{
					maxIdx = ((NodeP) n).labelIndex;
					lastEquiv = (NodeP) n;
				}
			if((lastEquiv != null) && (maxIdx == 0))
			{
				lastEquiv.labelIndex++;
				maxIdx = lastEquiv.labelIndex;
			}
			if(maxIdx > 0)
				node.labelIndex = maxIdx + 1;
		}
		super.addNode(node);
		return this;
	}
	
	public Collection<NodeP> getNodesP()
	{
		Collection<NodeP> ret = new ArrayList<>();
		for(Node n : getNodes())
			ret.add((NodeP) n);
		return ret;
	}
}
