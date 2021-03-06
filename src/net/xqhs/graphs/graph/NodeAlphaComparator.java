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
package net.xqhs.graphs.graph;

import java.util.Comparator;

import net.xqhs.graphs.pattern.NodeP;

/**
 * A simple {@link Comparator} class for {@link Node} instances, that also supports comparison of {@link NodeP}
 * instances by generic index.
 * 
 * Sorts labeled nodes lexicographically and generic nodes (if both arguments are {@link NodeP} instances) by increasing
 * generic index.
 * 
 * 
 * @author Andrei Olaru
 * 
 */
public class NodeAlphaComparator implements Comparator<Node>
{
	@Override
	public int compare(Node n0, Node n1)
	{
		if(n0 == n1)
			return 0;
		if(n0 == null)
			return -1;
		if(n1 == null)
			return 1;
		if((n0 instanceof NodeP) && ((NodeP) n0).isGeneric() && (n0 instanceof NodeP) && ((NodeP) n0).isGeneric()
				&& n0.getLabel().equals(n1.getLabel()))
			return ((NodeP) n0).genericIndex() - ((NodeP) n1).genericIndex();
		if((n0.getLabel() != null) && (n1.getLabel() != null))
			return n0.getLabel().compareTo(n1.getLabel());
		if((n0.toString() != null) && (n1.toString() != null))
			return n0.toString().compareTo(n1.toString());
		return n0.hashCode() - n1.hashCode();
	}
}
