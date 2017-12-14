package graph.version.index;

import java.util.BitSet;

import graph.version.Node;

/**
 * NInfo class
 * @author ksemer
 */
public class NInfo {

	// ====================================================================
	Node n;
	NInfo father;
	BitSet lifespan;
	int depth;
	// ====================================================================

	/**
	 * Constructor
	 * 
	 * @param n
	 * @param father
	 * @param lifespan
	 * @param depth
	 */
	public NInfo(Node n, NInfo father, BitSet lifespan, int depth) {
		this.n = n;
		this.father = father;
		this.lifespan = lifespan;
		this.depth = depth;
	}
}