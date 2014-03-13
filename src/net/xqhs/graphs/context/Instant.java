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
		
	}
	
	public Instant(long instantTime)
	{
		time = instantTime;
	}
	
	public Instant offset(Offset offset)
	{
		return new Instant(time + offset.length);
	}
	
	public boolean before(Instant instant)
	{
		return time < instant.time;
	}
	
	public boolean after(Instant instant)
	{
		return time > instant.time;
	}
	
}
