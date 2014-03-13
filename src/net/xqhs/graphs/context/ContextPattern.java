package net.xqhs.graphs.context;

import net.xqhs.graphs.context.Instant.Offset;
import net.xqhs.graphs.graph.GraphComponent;
import net.xqhs.graphs.pattern.GraphPattern;

public class ContextPattern extends GraphPattern
{
	Offset	persistence;
	
	public ContextPattern()
	{
		super();
	}
	
	public ContextPattern setPersistence(Offset patternPersistence)
	{
		try
		{
			locked();
		} catch(ConfigLockedException e)
		{
			throw new IllegalStateException(e);
		}
		persistence = patternPersistence;
		return this;
	}
	
	@Override
	public ContextPattern add(GraphComponent component)
	{
		try
		{
			locked();
		} catch(ConfigLockedException e)
		{
			throw new IllegalStateException(e);
		}
		return (ContextPattern) super.add(component);
	}
	
	@Override
	public ContextPattern remove(GraphComponent component)
	{
		try
		{
			locked();
		} catch(ConfigLockedException e)
		{
			throw new IllegalStateException(e);
		}
		return (ContextPattern) super.remove(component);
	}
}
