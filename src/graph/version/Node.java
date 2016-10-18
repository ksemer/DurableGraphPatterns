package graph.version;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import system.Config;

/**
 * This class represents the Node objects
 * @author ksemer
 */
public class Node{
	//=================================================================
	private int id;
	private Map<Integer, BitSet> labels;
	private Map<Node, Edge> adjacencies;
	private List<Map<Integer, BitSet>> TiNLa;
	//=================================================================

	/**
	 * Constructor
	 * @param id
	 */
	public Node(int id) {
		this.id = id;
		this.adjacencies = new HashMap<>();
		this.labels = new HashMap<>();
		
		if (Config.TINLA_ENABLED) {
			TiNLa = new ArrayList<>(Config.TINLA_R);
			
			for (int i = 0; i < Config.TINLA_R; i++)
				TiNLa.add(i, new HashMap<Integer, BitSet>());
		}
	}

	/**
	 * Add new edge or update existed
	 * @param node
	 * @param time
	 */
	public void addEdge(Node node, int time) {
		Edge e = adjacencies.get(node);
		
		if (e == null) {
			e = new Edge(node, new BitSet(Config.MAXIMUM_INTERVAL));
			adjacencies.put(node, e);
		}
		
		e.updateLifetime(time);
	}
	
	/**
	 * Return label's lifetime
	 * @param label
	 * @return
	 */
	public BitSet getLabel(int label) {
		return labels.get(label);
	}
	
	/**
	 * Update label lifetime
	 * @param label
	 * @param t
	 */
	public void updateLabelLifetime(int label, int t) {
		BitSet lifetime;

		if ((lifetime = labels.get(label)) == null) {
			lifetime = new BitSet(Config.MAXIMUM_INTERVAL);
			labels.put(label, lifetime);
		}
		
		lifetime.set(t);
	}
	
	/**
	 * Return nodes neighbor label index for label
	 * @param r
	 * @param label
	 * @return
	 */
	public BitSet getTiNLa(int r, int label) {
		return TiNLa.get(r).get(label);
	}
	
	/**
	 * Update TiNLa(r) set for label the time instant t
	 * @param r
	 * @param label
	 * @param t
	 */
	public void updateTiNLa(int r, int label, int t) {
		BitSet lifetime;
		
		if ((lifetime = TiNLa.get(r).get(label)) == null) {
			lifetime = new BitSet(Config.MAXIMUM_INTERVAL);
			TiNLa.get(r).put(label, lifetime);
		}
		
		lifetime.set(t);
	}
	
	/**
	 * Return nodes's adjacency
	 * @return
	 */
	public Collection<Edge> getAdjacency() {
		return adjacencies.values();
	}
	
	/**
	 * Return edge object for neighbor node n
	 * @param n
	 * @return
	 */
	public Edge getEdge(Node n) {
		return adjacencies.get(n);
	}

	/**
	 * Returns node's id
	 * @return
	 */
	public int getID() {
		return id;
	}
}