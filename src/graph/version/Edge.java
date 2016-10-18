package graph.version;

import java.util.BitSet;

/**
 * This class represents the Edges
 * @author ksemer
 */
public class Edge {
	//=================================================================	
	// keeps the target node object
	private Node trg_node;
	
	// keeps the edge lifetime
	private BitSet lifetime;	
	//=================================================================

	/**
	 * Constructor
	 * @param trg_node
	 * @param lifetime
	 */
	public Edge(Node trg_node, BitSet lifetime) {
		this.trg_node = trg_node;
		this.lifetime = lifetime;
	}
	
	/**
	 * Returns target
	 * @return
	 */
	public Node getTarget() {
		return trg_node;
	}
	
	/**
	 * Returns lifetime
	 * @return
	 */
	public BitSet getLifetime() {
		return lifetime;
	}
	
	/**
	 * Update lifetime set t position to true
	 * @param t
	 */
	public void updateLifetime(int t) {
		lifetime.set(t);
	}
}