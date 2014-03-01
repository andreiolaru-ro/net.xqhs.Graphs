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
package net.xqhs.graphs.util;

/**
 * The class serves as a mutable instance of type T. The user is able to get and set the content of the holder.
 * 
 * @author Andrei Olaru
 * 
 * @param <T>
 *            the type of the content.
 */
public class ContentHolder<T>
{
	/**
	 * The content.
	 */
	protected T	theContent;
	
	/**
	 * Creates a new content holder for type T, containing the <code>content</code>.
	 * 
	 * @param content
	 *            - the content to be held by the instance.
	 */
	public ContentHolder(T content)
	{
		theContent = content;
	}
	
	/**
	 * Returns the current content of the instance.
	 * 
	 * @return the content
	 */
	public T get()
	{
		return theContent;
	}
	
	/**
	 * Sets the current content of the instance.
	 * 
	 * @param content
	 *            - the content.
	 * @return the instance itself.
	 */
	public ContentHolder<T> set(T content)
	{
		theContent = content;
		return this;
	}
	
	/**
	 * Returns the result of the {@link #toString()} of the content.
	 */
	@Override
	public String toString()
	{
		return theContent.toString();
	}
}
