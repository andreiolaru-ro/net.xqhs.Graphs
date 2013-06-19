package net.xqhs.graphs.util;

import net.xqhs.util.logging.Debug.DebugItem;

public class Debug
{
	public static enum D_G implements DebugItem {
		
		D_MATCHING_INITIAL(false),
		
		D_CURRENT(true),
		
		;
		
		boolean	isset;
		
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
