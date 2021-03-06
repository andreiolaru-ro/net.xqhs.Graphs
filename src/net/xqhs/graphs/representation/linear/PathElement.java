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
package net.xqhs.graphs.representation.linear;

import java.util.LinkedList;
import java.util.List;

import net.xqhs.graphs.graph.Node;

/**
 * Class for the element of a path in a graph linearization (see {@link LinearGraphRepresentation}).
 * <p>
 * Each path element is associated with a node in the graph (seen as a {@link Node} implementation).
 * <p>
 * An instance contains information about the node, the distance from the start of the path, the previous node in the
 * path (its parent), and its children: "own children" are the children of the node, according to the tree represented
 * by the path. The "other children" are nodes connected with the current one (there exist edges from the node to the
 * children), but have been marked by the algorithm as "own children" of another path element.
 * 
 * @author Andrei Olaru
 */
public class PathElement
{
	/**
	 * The node to which this instance is associated.
	 */
	Node				node			= null;
	/**
	 * The distance from the root of the path. It is 0-based, with 0 for the root.
	 */
	int					depth			= 0;
	/**
	 * The distance, in the path, from the current element to the farthest leaf in the path.
	 */
	int					forwardLength	= -1;
	/**
	 * The parent of the element, in the current path.
	 */
	PathElement			parent			= null;
	// FIXME: what was this for - previously documented as 'order of the sub-tree containing this element'
	// int treeOrder = -1;
	/**
	 * The (ordered) list of children ("own children") of the current element in the path.
	 */
	List<PathElement>	children		= new LinkedList<PathElement>();
	/**
	 * The (ordered) list of nodes / elements that are connected to the node, but are not children of it in the current
	 * path ("other children").
	 */
	List<PathElement>	otherChildren	= new LinkedList<PathElement>();
	
	/**
	 * Creates a new instance, associated with a {@link Node} instance. The constructor requires the parent and the
	 * distance from root.
	 * 
	 * @param node
	 *            : the associated node.
	 * @param distance
	 *            : the distance from root (depth of the node). 0 is for root.
	 * @param parent
	 *            : the parent element, in the current path.
	 */
	public PathElement(Node node, int distance, PathElement parent)
	{
		this.node = node;
		this.depth = distance;
		this.parent = parent;
	}
	
	/**
	 * Checks if the path from root to the current node already contains a specified element.
	 * 
	 * @param el1
	 *            : the element to check for.
	 * @return <code>true</code> if the element is contained on the path, <code>false</code> otherwise.
	 */
	public boolean pathContains(PathElement el1)
	{
		PathElement el = this;
		while(el.parent != null)
		{
			if(el == el1)
				return true;
			el = el.parent;
		}
		return (el == el1);
	}
	
	/**
	 * Returns a compact string representation of the path element:
	 * <p>
	 * node label (depth : parent / n-children : n-otherChildren / forwardLength
	 */
	@Override
	public String toString()
	{
		if(node == null)
			return "<corrupt>";
		return node.toString() + "(" + depth + ":" + (parent != null ? parent.node.toString() : "-")
				+ (!children.isEmpty() ? "/" + children.size() : ".")
				+ (otherChildren.isEmpty() ? "" : "+" + otherChildren.size()) + "/" + forwardLength + ")";
	}
	
	/**
	 * @return the node associated with this element.
	 */
	public Node getNode()
	{
		return node;
	}
	
	/**
	 * @return the children of this element ("own children").
	 */
	public List<PathElement> getChildren()
	{
		return children;
	}
	
	/**
	 * @return the "other children" of this element.
	 */
	public List<PathElement> getOtherChildren()
	{
		return otherChildren;
	}
}
