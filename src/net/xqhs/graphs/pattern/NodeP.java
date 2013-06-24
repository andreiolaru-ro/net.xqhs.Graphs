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

import net.xqhs.graphs.graph.SimpleNode;

/**
 * The {@link NodeP} is a node that is part of a {@link GraphPattern} and may be generic (used in graph matching to
 * match any node in the matched graph).
 * <p>
 * A generic node is characterized by a label equal to a specific symbol (usually a question mark) and an index, to
 * identify a specific generic node.
 * <p>
 * It is expected that all nodes in a {@link GraphPattern} are instances of {@link NodeP}.
 * 
 * @author Andrei Olaru
 * 
 */
public class NodeP extends SimpleNode
{
	/**
	 * The label of all generic nodes.
	 */
	public static final String	NODEP_LABEL			= "?";
	/**
	 * The symbol used in the string representation between the label and the generic index.
	 */
	public static final String	NODEP_INDEX_MARK	= "#";
	
	/**
	 * Indicates that the node is generic.
	 */
	boolean						generic				= false;
	/**
	 * Indicates the identifier of the generic node. It will be strictly positive for generic nodes.
	 * <p>
	 * <b>Note:</b> this can only be used in one graph at a time.
	 */
	int							labelIndex			= 0;
	
	/**
	 * Creates a new generic {@link NodeP}, with an uninitialized index.
	 * <p>
	 * <b>Note:</b> the node needs to be indexed before being added to a graph. The <code>add(node)</code> method in
	 * {@link GraphPattern} does that.
	 */
	public NodeP()
	{
		super(NODEP_LABEL);
		generic = true;
	}
	
	/**
	 * Creates a new generic {@link NodeP}, with a specified index.
	 * <p>
	 * <b>WARNING:</b> use this with great caution.
	 * 
	 * @param genericIndex
	 *            - be absolutely certain this is not the same index with other nodes in the graph
	 */
	public NodeP(int genericIndex)
	{
		this();
		labelIndex = genericIndex;
	}
	
	/**
	 * Creates a new, non-generic {@link NodeP}, with a specified label. It calls the constructor in {@link SimpleNode}.
	 * <p>
	 * It is assumed that the label is not equal to <code>NODEP_LABEL</code>.
	 * 
	 * @param label
	 *            - the label for the new node
	 */
	public NodeP(String label)
	{
		super(label);
	}
	
	/**
	 * @return <code>true</code> is the node is generic.
	 */
	public boolean isGeneric()
	{
		return generic;
	}
	
	/**
	 * @return the index of the generic node, or 0 if the node is not generic.
	 */
	public int genericIndex()
	{
		return labelIndex;
	}
	
	@Override
	public String toString()
	{
		return super.toString() + (generic ? NODEP_INDEX_MARK + labelIndex : "");
	}
}
