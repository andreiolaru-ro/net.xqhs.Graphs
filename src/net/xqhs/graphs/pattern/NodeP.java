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

public class NodeP extends SimpleNode
{
	public static final String	NODEP_LABEL			= "?";
	public static final String	NODEP_INDEX_MARK	= "#";
	
	boolean						generic				= false;
	int							labelIndex			= 0;		// must be greater than 0 for generic nodes;
																
	public NodeP()
	{
		super(NODEP_LABEL);
		generic = true;
	}
	
	/**
	 * WARNING: use this with grate caution;
	 * 
	 * @param genericIndex
	 *            : be absolutely certain this is not the same index with other nodes in the graph
	 */
	public NodeP(int genericIndex)
	{
		super(NODEP_LABEL);
		generic = true;
		labelIndex = genericIndex;
	}
	
	public NodeP(String label)
	{
		super(label);
	}
	
	public boolean isGeneric()
	{
		return generic;
	}
	
	public int genericIndex()
	{
		return labelIndex;
	}
	
	@Override
	public String toString()
	{
		return super.toString() + (labelIndex > 0 ? NODEP_INDEX_MARK + labelIndex : "");
	}
}
