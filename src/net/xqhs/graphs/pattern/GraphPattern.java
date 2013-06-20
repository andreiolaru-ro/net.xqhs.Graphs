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
import net.xqhs.util.logging.UnitConfigData;

public class GraphPattern extends SimpleGraph
{
	public GraphPattern()
	{
		this(null);
	}
	
	public GraphPattern(UnitConfigData unitConfig)
	{
		super(unitConfig);
	}
	
	/**
	 * Only {@link NodeP} instances can be added to a {@link GraphPattern}. This makes it easier to work with the nodes
	 * in the pattern, because conversion won't fail.
	 */
	@Override
	public GraphPattern addNode(Node node)
	{
		if(!(node instanceof NodeP))
			throw new IllegalArgumentException("cannot add Node instances to a pattern");
		return this.addNode((NodeP) node, true);
	}
	
	public GraphPattern addNode(NodeP node, boolean doindex)
	{
		// Map<String, Integer> labelNs = new HashMap<String, Integer>();
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
