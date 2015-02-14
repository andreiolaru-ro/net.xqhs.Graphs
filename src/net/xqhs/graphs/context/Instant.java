package net.xqhs.graphs.context;

public class Instant
{
	long	time;

	public interface TickReceiver
	{
		public void tick(TimeKeeper ticker, Instant now);
	}

	public interface TimeKeeper
	{
		public Instant now();

		public void registerTickReceiver(TickReceiver receiver, Offset tickLength);
	}

	public static class Offset
	{
		long	length;

		public Offset(long offsetLength)
		{
			if(offsetLength <= 0)
				throw new IllegalArgumentException("Offsets are strictily positive");
			length = offsetLength;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			return (obj instanceof Offset) && (length == ((Offset) obj).length);
		}
		
		@Override
		public int hashCode()
		{
			return new Long(length).hashCode();
		}

		public long toLong()
		{
			return length;
		}

		@Override
		public String toString()
		{
			return new Long(length).toString();
		}
		
		/**
		 * Pretty prints the interval as hours and minutes (e.g. 5h31), considering the time as being in seconds.
		 *
		 * @return the pretty string.
		 */
		public String toStringPrint()
		{
			return printTime(length);
		}
	}

	public Instant(long instantTime)
	{
		time = instantTime;
	}

	public Instant offsetInstant(Offset offset)
	{
		return new Instant(time + offset.length);
	}

	public Offset getOffsetTo(Instant otherTime)
	{
		return new Offset(otherTime.time - this.time);
	}

	public boolean before(Instant instant)
	{
		return time < instant.time;
	}

	public boolean after(Instant instant)
	{
		return time > instant.time;
	}

	@Override
	public boolean equals(Object obj)
	{
		return (obj instanceof Instant) && (time == ((Instant) obj).time);
	}
	
	@Override
	public int hashCode()
	{
		return new Long(time).hashCode();
	}

	public long toLong()
	{
		return time;
	}

	@Override
	public String toString()
	{
		return new Long(time).toString();
	}
	
	/**
	 * Pretty prints the time as hours and minutes (e.g. 5h31), considering the time as being in seconds.
	 *
	 * @return the pretty string.
	 */
	public String toStringPrint()
	{
		return printTime(time);
	}
	
	/**
	 * It is assumed that time is in seconds.
	 * <p>
	 * If it is desired to pretty print an {@link Instant} or {@link Offset} instance, call {@link #toStringPrint()}.
	 *
	 * @param time
	 *            - in seconds
	 * @return pretty print string
	 */
	public static String printTime(long time)
	{
		int h = (int) (time / 3600);
		int m = (int) ((time % 3600) / 60);
		String ret = h + "h";
		if(m > 0)
			ret += m;
		return ret;
	}
}
