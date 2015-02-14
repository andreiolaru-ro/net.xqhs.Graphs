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
 * Debug items for
 *
 * @author Andrei Olaru
 */
public class Debug extends net.xqhs.util.logging.Debug
{
	/**
	 * Debug items for graphs and graph matching.
	 *
	 * @author Andrei Olaru
	 */
	public static enum D_G implements DebugItem {

		/**
		 * Debug for the initial matching process.
		 */
		D_MATCHING_INITIAL(false),

		/**
		 * Debug for the progress of incremental matching.
		 */
		D_MATCHING_PROGRESS(false),

		/**
		 * Debug item for the current issue at hand.
		 */
		D_CURRENT(true),

		D_TEST_GRAPH_CONSTRUCTION(false),

		D_TEST_MATCH_NOTIFICATION(false),
		
		D_NO_SAVED_DATA(false),

		;

		/**
		 * Activation state.
		 */
		boolean	isset;

		/**
		 * Constructor.
		 *
		 * @param set
		 *            - activation state.
		 */
		private D_G(boolean set)
		{
			isset = set;
		}

		@Override
		public boolean toBool()
		{
			return isset;
		}

	}
}
