package graph.version;

import java.io.Serializable;
import java.util.BitSet;

/**
 * This class represents the Edges
 * 
 * @author ksemer
 */
public class Edge implements Serializable {

	private static final long serialVersionUID = 1L;

	// =================================================================
	// keeps the target node object
	private Node trg_node;

	// keeps the edge lifespan
	private BitSet lifespan;
	// =================================================================

	/**
	 * Constructor
	 * 
	 * @param trg_node
	 * @param lifespan
	 */
	public Edge(Node trg_node, BitSet lifespan) {
		this.trg_node = trg_node;
		this.lifespan = lifespan;
	}

	/**
	 * Returns target
	 * 
	 * @return
	 */
	public Node getTarget() {
		return trg_node;
	}

	/**
	 * Returns lifespan
	 * 
	 * @return
	 */
	public BitSet getLifetime() {
		return lifespan;
	}

	/**
	 * Update lifespan set t position to true
	 * 
	 * @param t
	 */
	public void updateLifetime(int t) {
		lifespan.set(t);
	}
}