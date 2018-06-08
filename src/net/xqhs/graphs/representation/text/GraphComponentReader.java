package net.xqhs.graphs.representation.text;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleEdge;
import net.xqhs.graphs.graph.SimpleNode;

/**
 * Classes implementing {@link GraphComponentReader} offer functionality to read nodes and edges from strings, at the
 * time when these should be read from a text representation.
 * <p>
 * In the current implementation of {@link TextRepresentationElement}, the component is first isolated and then the
 * content is read, so the reader will only receive the part of the input which contains the information fo insterest.
 *
 * @author andreiolaru
 */
public interface GraphComponentReader
{
	// FUTURE / ALTERNATIVE: if the parsing is done with a regular grammar, the reader must consume some of the output
	// and leave the rest for further processing.
	// /**
	// * Is called to read from the input when a node should be next. It will replace the String in the
	// * {@link ContentHolder} instance with a new String, from which the representation of the {@link GraphComponent}
	// has
	// * been eliminated.
	// *
	// * @param rawInput
	// * - the input which is left to process, held within a {@link ContentHolder} instance.
	// * @return the {@link GraphComponent} whose representation is the prefix of the input.
	// */
	// public GraphComponent readNode(ContentHolder<String> rawInput);
	//
	// /**
	// * Is called to read from the input when the represenation of an edge's data should be next (e.g. just the label
	// of
	// * the edge, without the representation of the arrow.
	// * <p>
	// * It will replace the String in the {@link ContentHolder} instance with a new String, from which the
	// representation
	// * of the {@link GraphComponent} has been eliminated.
	// *
	// * @param rawInput
	// * - the input which is left to process, held within a {@link ContentHolder} instance.
	// * @return the {@link GraphComponent} whose representation is the prefix of the input.
	// */
	// public GraphComponent readEdge(ContentHolder<String> rawInput);
	
	// ACTUAL: the reader only receives the part of interest.
	/**
	 * Is called to read from the representation of a node.
	 *
	 * @param rawInput
	 *            - the input to process.
	 * @return the {@link Node} whose representation is in the input.
	 */
	public SimpleNode readNode(String rawInput);
	
	/**
	 * Is called to read from the representation of an edge. The representation will not yet contain node data (see
	 * {@link TextRepresentationElement}, so the result is a {@link SettableEdge} that will be completed with adjacent
	 * node information later, and {@link #compileEdge(SettableEdge)} will be called to create a non-settable edge from
	 * it once the node information is complete.
	 *
	 * @param rawInput
	 *            - the input to process.
	 * @return the edge whose representation is in the input.
	 */
	public SettableEdge readEdge(String rawInput);
	
	/**
	 * Is called when the information on an edge (represented as a {@link SettableEdge} instance) is complete and an
	 * {@link Edge} instance can be created.
	 *
	 * @param settableEdge
	 *            - the {@link SettableEdge} edge, now with complete information
	 * @return an immutable version of the {@link SettableEdge}.
	 */
	public SimpleEdge compileEdge(SettableEdge settableEdge);
}
