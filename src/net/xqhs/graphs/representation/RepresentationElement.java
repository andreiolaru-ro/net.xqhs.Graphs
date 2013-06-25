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
package net.xqhs.graphs.representation;

import net.xqhs.util.config.Config;

public abstract class RepresentationElement extends Config
{
	GraphRepresentation			rootRepresentation;
	VisualizableGraphComponent	representedComponent	= null;
	
	public RepresentationElement(GraphRepresentation root, VisualizableGraphComponent component)
	{
		this.rootRepresentation = root;
		this.representedComponent = component;
	}
	
	public GraphRepresentation getRootRepresentation()
	{
		return rootRepresentation;
	}
	
	public VisualizableGraphComponent getRepresentedComponent()
	{
		return representedComponent;
	}
}
