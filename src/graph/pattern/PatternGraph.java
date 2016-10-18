package graph.pattern;

import java.util.ArrayList;
import java.util.List;

/**
 * PatternGraph class
 * @author ksemer
 */
public class PatternGraph {
	//=================================================================
	private List<PatternNode> nodes;
	//=================================================================
	
	/**
	 * Constructor
	 */
	public PatternGraph() {
		nodes = new ArrayList<PatternNode>();
	}
	
	/**
	 * Add pattern node
	 * @param id
	 * @param label
	 */
	public void addNode(int id, int label) {
		nodes.add(new PatternNode(id, label));
	}
	
	/**
	 * Add patter edge
	 * @param src
	 * @param trg
	 */
	public void addEdge(int src, int trg) {
		nodes.get(src).addEdge(nodes.get(trg));
	}
	
	/**
	 * Return PNode with id node
	 * @param node
	 * @return
	 */
	public PatternNode getNode(int node) {
		return nodes.get(node);
	}
	
	/**
	 * Return all pattern nodes
	 * @return
	 */
	public List<PatternNode> getNodes() {
		return this.nodes;
	}

	/**
	 * Returns graph size
	 * @return
	 */
	public int size() {
		return nodes.size();
	}
}