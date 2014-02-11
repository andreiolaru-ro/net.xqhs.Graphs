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
package net.xqhs.graphs.representation.text;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.xqhs.graphs.graph.Edge;
import net.xqhs.graphs.graph.Node;
import net.xqhs.graphs.graph.SimpleNode;
import net.xqhs.graphs.pattern.NodeP;
import net.xqhs.graphs.representation.GraphRepresentation;
import net.xqhs.graphs.representation.RepresentationElement;
import net.xqhs.graphs.representation.VisualizableGraphComponent;
import net.xqhs.graphs.util.ContentHolder;
import net.xqhs.util.logging.UnitComponent;

/**
 * The class contains the information that is necessary to represent one component of the graph represented by the
 * parent {@link TextGraphRepresentation}, such as a node, an edge, a subgraph, a graph.
 * <p>
 * The class contains method for displaying a textual representation of the represented element (and its children) and
 * for reading a textual representation of such an element and its children.
 * 
 * @author Andrei Olaru
 */
public class TextRepresentationElement extends RepresentationElement
{
	/**
	 * Enumeration containing the types of elements that may appear in a textual graph representation.
	 * 
	 * @author Andrei Olaru
	 */
	public enum Type {
		/**
		 * A node in the graph.
		 */
		NODE,
		
		/**
		 * An edge to a node that has not previously appeared in the representation.
		 */
		BRANCH,
		
		/**
		 * An edge to a node that has already appeared in the representation (from left to right).
		 */
		INTERNAL_LINK,
		
		/**
		 * An edge to a node that is not part of the current subgraph.
		 */
		EXTERNAL_LINK,
		
		/**
		 * A subgraph.
		 */
		SUBGRAPH,
		
		/**
		 * The container representation for a graph, that may be part of a multilevel representation.
		 */
		ELEMENT_CONTAINER,
	}
	
	/**
	 * An enumeration of the characters that may appear in a textual representation. None of them is allowed to appear
	 * in the label of nodes or edges, except for {@link #EDGE_LIMIT}, that may appear in edge labels.
	 * <p>
	 * These constants are used both at reading and at producing a representation.
	 * 
	 * @author Andrei Olaru
	 */
	public enum Symbol {
		/**
		 * Separator between the representations of two subgraphs.
		 */
		SUBGRAPH_SEPARATOR(";"),
		
		/**
		 * Prefix to the representation of a node that has appeared previously (from left to right) in the
		 * representation.
		 */
		INTERNAL_LINK_PREFIX("*"),
		
		/**
		 * Prefix to the representation of a node that is not part of the current subgraph.
		 */
		EXTERNAL_LINK_PREFIX("^"),
		
		/**
		 * Symbol that marks the beginning of a subtree with the last node as root, that is not the last subtree of the
		 * root.
		 */
		BRANCH_IN("("),
		
		/**
		 * Symbol that marks the beginning of a subtree with the last node as root, that is not the last subtree of the
		 * root.
		 */
		BRANCH_OUT(")"),
		
		/**
		 * Symbol that marks the beginning of an edge, and may also be found immediately before the destination marker.
		 * It may be contained inside the label of an edge.
		 */
		EDGE_LIMIT("-"),
		
		/**
		 * Symbol that indicates the destination end of an edge. It is followed by the representation of the destination
		 * node.
		 */
		EDGE_ENDING_FORWARD(">"),
		
		/**
		 * Symbol that indicates the destination end of an edge in a 'backwards' representation. It is preceded by the
		 * representation of the destination node.
		 */
		EDGE_ENDING_BACKWARD("<"),
		
		;
		
		/**
		 * The actual textual representation of the symbol.
		 */
		private String	symbol;
		
		/**
		 * Default constructor.
		 * 
		 * @param symb
		 *            - the representation of the symbol.
		 */
		private Symbol(String symb)
		{
			symbol = symb;
		}
		
		@Override
		public String toString()
		{
			return symbol;
		}
		
		/**
		 * Returns a value that can be included in a regular expressions pattern (as for {@link String#split(String)})
		 * without worries that the characters in the symbol will be taken as special values.
		 * 
		 * @return the 'quoted' symbol value.
		 */
		public String toRegexp()
		{
			return Pattern.quote(symbol);
		}
	}
	
	/**
	 * The indent for the default representation used in {@link #toString()}. See
	 * {@link #toString(String, String, int, boolean)}
	 */
	public static final String		DEFAULT_INDENT				= "";
	/**
	 * The indent increment for the default representation used in {@link #toString()}. See
	 * {@link #toString(String, String, int, boolean)}.
	 */
	public static final String		DEFAULT_INDENT_INCREMENT	= " ";
	/**
	 * The limit to indent incrementing for the default representation used in {@link #toString()}. See
	 * {@link #toString(String, String, int, boolean)}.
	 */
	public static final int			DEFAULT_INDENT_LIMIT		= 3;
	
	/**
	 * Not currently used. TODO
	 */
	int								nestingLevel				= 0;
	/**
	 * <code>true</code> if it is the last child of the parent.
	 */
	boolean							isLastChild					= false;
	/**
	 * <code>true</code> if it is the only child of the parent.
	 */
	boolean							isOnlyChild					= false;
	/**
	 * <code>true</code> if it is the first subgraph in the container.
	 */
	boolean							isFirstSubgraph				= false;
	/**
	 * The type of the element (as one of {@link Type}).
	 */
	Type							linkType;
	/**
	 * The nested elements.
	 */
	List<TextRepresentationElement>	content						= null;
	
	/**
	 * Creates a text representation for an element container (a level in a multi-level representation).
	 * 
	 * @param root
	 *            - the root representation.
	 * @param type
	 *            - must be {@link Type#ELEMENT_CONTAINER} (otherwise an exception is thrown).
	 */
	public TextRepresentationElement(GraphRepresentation root, Type type)
	{
		this(root, null, type, 0, false, false, false);
		
		if(type != Type.ELEMENT_CONTAINER)
			throw new IllegalArgumentException();
	}
	
	/**
	 * Creates a text representation of a subgraph (connected part of a graph).
	 * 
	 * @param root
	 *            - the root representation.
	 * @param type
	 *            - must be {@link Type#SUBGRAPH} (otherwise an exception is thrown).
	 * @param first
	 *            - <code>true</code> if it is the first subgraph in a series (otherwise, a separator will be included
	 *            in the representation).
	 */
	public TextRepresentationElement(GraphRepresentation root, Type type, boolean first)
	{
		this(root, null, type, 0, false, false, first);
		
		if(type != Type.SUBGRAPH)
			throw new IllegalArgumentException();
	}
	
	/**
	 * Creates a text representation of a graph edge.
	 * 
	 * @param root
	 *            - the root representation.
	 * @param representedEdge
	 *            - the {@link Edge} instance (must also implement {@link VisualizableGraphComponent}.
	 * @param type
	 *            - must be one of {@link Type#EXTERNAL_LINK}, {@link Type#INTERNAL_LINK} or {@link Type#BRANCH}
	 *            (otherwise an exception is thrown).
	 * @param level
	 *            - currently not used // FIXME
	 * @param lastChild
	 *            - <code>true</code> it it is the last edge going out of its source node.
	 * @param alone
	 *            - <code>true</code> if it is the only outgoing edge of its source node.
	 */
	public TextRepresentationElement(GraphRepresentation root, VisualizableGraphComponent representedEdge, Type type,
			int level, boolean lastChild, boolean alone)
	{
		this(root, representedEdge, type, level, lastChild, alone, false);
		
		if(!isEdgeType(type))
			throw new IllegalArgumentException();
	}
	
	/**
	 * Creates a text representation of a graph node.
	 * 
	 * @param root
	 *            - the root representation.
	 * @param representedNode
	 *            - the {@link Node} instance (must also implement {@link VisualizableGraphComponent}.
	 * @param type
	 *            - must be {@link Type#NODE} (otherwise an exception is thrown).
	 */
	public TextRepresentationElement(GraphRepresentation root, VisualizableGraphComponent representedNode, Type type)
	{
		this(root, representedNode, type, 0, false, false, false);
		
		if(type != Type.NODE)
			throw new IllegalArgumentException();
	}
	
	/**
	 * Constructor aggregating all the other constructors.
	 * 
	 * @param root
	 *            - the root representation.
	 * @param representedElement
	 *            - the {@link VisualizableGraphComponent} that is beign represented.
	 * @param type
	 *            - the type of the element (as one of {@link Type}).
	 * @param level
	 *            - TODO
	 * @param lastChild
	 *            - <code>true</code> if it is the last child of the parent.
	 * @param alone
	 *            - <code>true</code> if it is the only child of the parent.
	 * @param firstSubgraph
	 *            - <code>true</code> if it is the first subgraph in the container.
	 */
	protected TextRepresentationElement(GraphRepresentation root, VisualizableGraphComponent representedElement,
			Type type, int level, boolean lastChild, boolean alone, boolean firstSubgraph)
	{
		super(root, representedElement);
		
		content = new LinkedList<TextRepresentationElement>();
		
		linkType = type;
		nestingLevel = level;
		isLastChild = lastChild;
		isOnlyChild = alone;
		isFirstSubgraph = firstSubgraph;
		
		if(getRepresentedComponent() != null)
			getRepresentedComponent().addRepresentation(this);
	}
	
	/**
	 * Adds a subordinate element to this representation element.
	 * 
	 * @param sub
	 *            - the subordinate element to add.
	 * @return the instance itself.
	 */
	protected TextRepresentationElement addSub(TextRepresentationElement sub)
	{
		content.add(sub);
		return this;
	}
	
	/**
	 * Adds a list of subordinate elements to this representation element.
	 * 
	 * @param subs
	 *            - the subordinate elements to add, as a {@link List}.
	 * @return the instance itself.
	 */
	protected TextRepresentationElement addSub(List<TextRepresentationElement> subs)
	{
		content.addAll(subs);
		return this;
	}
	
	/**
	 * Creates a textual rendering of the current representation element, using default settings (
	 * {@link #DEFAULT_INDENT}, {@link #DEFAULT_INDENT_INCREMENT}, {@link #DEFAULT_INDENT_LIMIT}).
	 */
	@Override
	public String toString()
	{
		return toString(DEFAULT_INDENT, DEFAULT_INDENT_INCREMENT, DEFAULT_INDENT_LIMIT, false);
	}
	
	/**
	 * The method creates a textual rendering of the current representation element, as well as of its children.
	 * <p>
	 * See {@link TextGraphRepresentation}.
	 * 
	 * @param indent
	 *            - this will precede each new element, before <code>indentLimit</code> is reached. 'Only' children and
	 *            nodes will not be prexeded by indent.
	 * @param indentIncrement
	 *            - will be added to the current increment every time the rendering advances one level, but no more than
	 *            <code>indentLimit</code> times. It is not added to the indent when the next level contains only one
	 *            child.
	 * @param indentLimit
	 *            - maximum times <code>indentIncrement</code> is added to <code>indent</code>. After this limit is
	 *            reached, no more indent will be displayed.
	 * @param isBackwards
	 *            - <code>true</code> if the representation should be 'backwards' (edges point to the left instead of
	 *            right).
	 * @return the {@link String} representation of the element.
	 */
	public String toString(String indent, String indentIncrement, int indentLimit, boolean isBackwards)
	{
		String displayedIndent = indent;
		String displayedIndentIncrement = indentIncrement;
		int nextIndentLimit = indentLimit;
		
		String ret = "";
		
		if((linkType == Type.SUBGRAPH) && !isFirstSubgraph)
			ret += Symbol.SUBGRAPH_SEPARATOR;
		
		if(indentLimit == 0) // cannot use <= 0 because -1 means no limit.
		{
			displayedIndent = "";
			displayedIndentIncrement = "";
		}
		if(!isOnlyChild && linkType != Type.NODE)
			ret += displayedIndent;
		if((linkType != Type.ELEMENT_CONTAINER) && (content.size() > 1))
			displayedIndent += displayedIndentIncrement;
		if(isEdgeType(linkType))
			nextIndentLimit = indentLimit - 1;
		
		if(isEdgeType(linkType) && !isOnlyChild && !isLastChild)
			ret += Symbol.BRANCH_IN;
		
		if(isEdgeType(linkType))
		{
			if(isBackwards)
				ret += Symbol.EDGE_ENDING_BACKWARD;
			ret += Symbol.EDGE_LIMIT;
			if(((Edge) getRepresentedComponent()).getLabel() != null)
				ret += ((Edge) getRepresentedComponent()).getLabel() + Symbol.EDGE_LIMIT;
			if(!isBackwards)
				ret += Symbol.EDGE_ENDING_FORWARD;
		}
		if(linkType == Type.EXTERNAL_LINK)
			ret += Symbol.EXTERNAL_LINK_PREFIX;
		if(linkType == Type.INTERNAL_LINK)
			ret += Symbol.INTERNAL_LINK_PREFIX;
		
		if(linkType == Type.NODE)
			// appropriate use of toString() as nodes may have various representations
			ret += getRepresentedComponent().toString();
		
		if(content != null)
			for(TextRepresentationElement el : content)
				ret += el.toString(displayedIndent, displayedIndentIncrement, nextIndentLimit, isBackwards);
		
		if(isEdgeType(linkType) && !isOnlyChild && !isLastChild)
			ret += Symbol.BRANCH_OUT;
		
		return ret;
	}
	
	/**
	 * True if the specified {@link Type} is a type of edge. True for {@link Type#INTERNAL_LINK},
	 * {@link Type#EXTERNAL_LINK} and {@link Type#BRANCH}.
	 * 
	 * @param elementType
	 *            - the type of element.
	 * @return <code>true</code> if the type is a type of edge.
	 */
	static protected boolean isEdgeType(Type elementType)
	{
		return (elementType == Type.INTERNAL_LINK) || (elementType == Type.EXTERNAL_LINK)
				|| (elementType == Type.BRANCH);
	}
	
	/**
	 * The method reads the representation of a whole graph from the given text input. While the edges are placed
	 * correctly, they have no adjacent nodes. They will be filed in later, in
	 * {@link TextGraphRepresentation#readRepresentation(String)}. Basically, elements are currently correct with
	 * respect to their labels and their placement in a textual layout ({@link #toString()} returns an apparently
	 * correct representation). The actual, correct graph is build later.
	 * 
	 * @param input
	 *            - the input, as a {@link String}.
	 * @param root
	 *            - the root representation that the read element belongs to.
	 * @param log
	 *            - a {@link UnitComponent} to use for logging messages.
	 * @return the root element (of type {@link Type#ELEMENT_CONTAINER}) of the representation.
	 */
	static protected TextRepresentationElement readRepresentation(String input, TextGraphRepresentation root,
			UnitComponent log)
	{
		return readRepresentation(new ContentHolder<String>(input), Type.ELEMENT_CONTAINER, true, root, log);
	}
	
	/**
	 * The method uses the input string to read the representation of an element, removing the representation from the
	 * input and leaving other calls of the method to read the rest (if any).
	 * <p>
	 * The input is modified by using {@link ContentHolder#set(Object)}.
	 * 
	 * @param input
	 *            - the input, held be a {@link ContentHolder} instance.
	 * @param type
	 *            - the type of element to read from the input, as one of {@link Type}.
	 * @param firstSibling
	 *            - <code>true</code> if this is the first instance in a list of sibling instances (first child of a
	 *            parent).
	 * @param root
	 *            - the root representation that the read element belongs to.
	 * @param log
	 *            - a {@link UnitComponent} to use for logging messages.
	 * @return the element read from the input, as a {@link TextRepresentationElement}.
	 */
	static protected TextRepresentationElement readRepresentation(ContentHolder<String> input, Type type,
			boolean firstSibling, TextGraphRepresentation root, UnitComponent log)
	{
		log.lf("reading [] from []", type, input);
		TextRepresentationElement ret = null;
		int nChildren = 0; // used to detect the first child
		switch(type)
		{
		case ELEMENT_CONTAINER:
			ret = new TextRepresentationElement(root, type);
			while(input.get().length() > 0)
				// input will be modified in the call below; each call reads a subgraph
				ret.addSub(readRepresentation(input, Type.SUBGRAPH, (nChildren++ == 0), root, log));
			break;
		case SUBGRAPH:
		{
			String rez[] = input.get().split(Symbol.SUBGRAPH_SEPARATOR.toRegexp(), 2);
			// should have current subgraph in rez[0], rest of the graph in rez[1], if any
			if(rez.length > 0 && rez[0].length() > 0)
			{
				input.set(rez[0]);
				log.li("create new subgraph");
				ret = new TextRepresentationElement(root, Type.SUBGRAPH, firstSibling);
				// a representation begins with a node
				ret.addSub(readRepresentation(input, Type.NODE, true, root, log));
			}
			if(rez.length > 1) // more subgraphs left
				input.set(rez[1]);
			else
				input.set("");
			break;
		}
		case EXTERNAL_LINK:
		case INTERNAL_LINK:
			// these two above are not really used, as we can't know beforehand if an edge is external, internal, or
			// normal
		case BRANCH:
		{
			String branchString; // the representation of the current subtree / branch
			String rest; // the rest of the input
			boolean isLastChild;
			// isolate branch
			input.set(input.get().trim()); // be sure that first char is non-whitespace
			if(input.get().startsWith(Symbol.BRANCH_IN.toString()))
			{ // there will be other branches / subtrees after this subtree.
				// locate corresponding closing symbol
				String open = Symbol.BRANCH_IN.toString();
				String close = Symbol.BRANCH_OUT.toString();
				int openlen = open.length();
				int closelen = close.length();
				int lastIndex = 0; // the index of the corresponding closing symbol
				int openBranches = 0; // current number of open branches / opening symbols found
				int pastLength = openlen; // length already explored
				String str = input.get().substring(openlen); // go beyond opening brace
				while(lastIndex == 0)
				{
					int openIndex = str.indexOf(open), closeIndex = str.indexOf(close);
					if(openIndex >= 0 && openIndex < closeIndex)
					{ // next a branch opens
						openBranches++;
						pastLength += openIndex + openlen;
						str = str.substring(openIndex + openlen);
					}
					else
					{ // next a branch closes
						if(openBranches == 0)
							// it's our branch
							lastIndex = pastLength + closeIndex;
						else
						{
							openBranches--;
							pastLength += closeIndex + closelen;
							str = str.substring(closeIndex + closelen);
						}
					}
				}
				
				branchString = input.get().substring(openlen, lastIndex).trim();
				rest = input.get().substring(lastIndex + closelen);
				isLastChild = false;
			}
			else
			{
				branchString = input.get().trim();
				rest = "";
				isLastChild = true;
			}
			log.lf("identified [] edge []", (isLastChild ? "last" : ""), branchString);
			
			// read current branch
			// get the label
			int firstIndex, lastIndex; // first and last index of the label of the edge, in branchstring
			String nextNode; // the rest of the branch.
			
			if(!root.isBackwards())
			{ // - forward-edge [-]> next node
				String rez[] = branchString.split(Symbol.EDGE_ENDING_FORWARD.toRegexp(), 2);
				// rez now contains: edge representation | rest of the branch
				firstIndex = rez[0].indexOf(Symbol.EDGE_LIMIT.toString()) + Symbol.EDGE_LIMIT.toString().length();
				if(rez[0].length() == 0)
					lastIndex = -1; // no label; edge is just the EDGE_ENDING_FORWARD symbol.
				else if(rez.length < 2)
					throw new IllegalArgumentException("No ending for edge.");
				else if(rez[0].substring(rez[0].length() - 1).equals(Symbol.EDGE_LIMIT.toString()))
					// the last character before EDGE_ENDING_FORWARD is again EDGE_LIMIT; ignore it.
					lastIndex = rez[0].length() - 1;
				else
					lastIndex = rez[0].length();
				nextNode = rez[1].trim();
			}
			else
			{ // <[-] backward-edge - next node
				// assume first character is (EDGE_ENDING_BACKWARD) (edgeString is trimmed)
				// identify edge name ending by last EDGE_LIMIT before the next EDGE_ENDING_BACKWARD
				branchString = branchString.substring(Symbol.EDGE_ENDING_BACKWARD.toString().length());
				String rez[] = branchString.split(Symbol.EDGE_ENDING_BACKWARD.toRegexp(), 2);
				// rez now contains: representation until before the next edge | next edge and rest
				// second part of rez is not used, and the string is anyway checked in the 'NODE' case
				lastIndex = rez[0].lastIndexOf(Symbol.EDGE_LIMIT.toString());
				if(rez[0].startsWith(Symbol.EDGE_LIMIT.toString()))
					// the string begins with the EDGE_LIMIT symbol next to the EDGE_ENDING_BACKWARD symbol; ignore it.
					firstIndex = Symbol.EDGE_LIMIT.toString().length();
				else
					firstIndex = 0;
				nextNode = branchString.substring(lastIndex + Symbol.EDGE_LIMIT.toString().length()).trim();
			}
			String edgeLabel;
			if(firstIndex <= lastIndex)
			{
				// log.lf("===== [] === [] ", firstIndex, lastIndex);
				edgeLabel = branchString.substring(firstIndex, lastIndex).trim();
				if(edgeLabel.length() == 0)
					edgeLabel = null;
			}
			else
				// unnamed edge
				edgeLabel = null;
			
			// get the type (internal, external or normal branch)
			// nextNode has been trimmed
			Type edgeType;
			String edgeTypeChar = "";
			if(nextNode.startsWith(Symbol.EXTERNAL_LINK_PREFIX.toString()))
			{
				edgeType = Type.EXTERNAL_LINK;
				nextNode = nextNode.substring(Symbol.EXTERNAL_LINK_PREFIX.toString().length());
				edgeTypeChar = Symbol.EXTERNAL_LINK_PREFIX.toString();
			}
			else if(nextNode.startsWith(Symbol.INTERNAL_LINK_PREFIX.toString()))
			{
				edgeType = Type.INTERNAL_LINK;
				nextNode = nextNode.substring(Symbol.INTERNAL_LINK_PREFIX.toString().length());
				edgeTypeChar = Symbol.INTERNAL_LINK_PREFIX.toString();
			}
			else
				edgeType = Type.BRANCH;
			
			// create
			log.li("create new [][] edge: [].", edgeTypeChar, edgeType, edgeLabel);
			SettableEdge edge = new SettableEdge(edgeLabel); // node names will be filled in later
			// level is unused
			ret = new TextRepresentationElement(root, edge, edgeType, -1, isLastChild, isLastChild && firstSibling);
			ret.addSub(readRepresentation(input.set(nextNode), Type.NODE, true, root, log));
			input.set(rest);
			break;
		}
		case NODE:
		{
			String regex = Symbol.EDGE_LIMIT.toRegexp() + "|" + Symbol.BRANCH_IN.toRegexp() + "|"
					+ Symbol.EDGE_ENDING_FORWARD.toRegexp() + "|" + Symbol.EDGE_ENDING_BACKWARD.toRegexp();
			String parts[] = input.get().split(regex, 2);
			// parts now contains: the node label | the rest, without the separator
			String nodeName = parts[0].trim();
			if(nodeName.length() == 0)
				throw new IllegalArgumentException("Node name is empty.");
			SimpleNode node = null;
			if(nodeName.startsWith(NodeP.NODEP_LABEL)
					&& nodeName.substring(NodeP.NODEP_LABEL.length()).startsWith(NodeP.NODEP_INDEX_MARK))
			{ // node is a generic node (?#number)
				int index = Integer.parseInt(nodeName.substring(NodeP.NODEP_LABEL.length()
						+ NodeP.NODEP_INDEX_MARK.length()));
				log.li("create new pattern node #[]", new Integer(index));
				// for internal links, this should not be a new node; it will be replaced later
				node = new NodeP(index);
			}
			else
			{ // normal, labeled node
				log.li("create new node: []", nodeName);
				// for internal links, this should not be a new node; it will be replaced later
				node = new SimpleNode(nodeName);
			}
			ret = new TextRepresentationElement(root, node, Type.NODE);
			if(parts.length > 1)
			{
				// remember what was after the node name
				input.set(input.get().substring(parts[0].length()));
				while(input.get().length() > 0)
					ret.addSub(readRepresentation(input, Type.BRANCH, (nChildren++ == 0), root, log));
			}
			break;
		}
		}
		return ret;
	}
}
