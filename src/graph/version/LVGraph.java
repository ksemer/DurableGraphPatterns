package graph.version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import system.Config;

/**
 * Labeled Version Graph class
 * @author ksemer
 */
public class LVGraph {
	//=================================================================
	private Map<Integer, Node> nodes;
	private List<Map<Integer, Set<Node>>> TiLa;
	//=================================================================
		
	/**
	 * Constructor
	 * @param size
	 * @throws IOException
	 */
	public LVGraph(int size) {
		nodes = new HashMap<Integer, Node>(size);
		TiLa = new ArrayList<>(Config.MAXIMUM_INTERVAL);	
	
		Map<Integer, Set<Node>> map;
		
		for (int i = 0; i < Config.MAXIMUM_INTERVAL; i++) {
			map = new HashMap<>();
			TiLa.add(map);
		}
	}
	
	/**
	 * Add node in LVG
	 * @param node
	 */
	public void addNode(int node) {
		if (nodes.get(node) == null)
			nodes.put(node, new Node(node));
	}
	
	/**
	 * Get node object with id = nodeID
	 * @param nodeID
	 * @return
	 */
	public Node getNode(int nodeID) {
		return nodes.get(nodeID);
	}

	/**
	 * Add edge in LVG
	 * @param src
	 * @param trg
	 * @param time
	 */
	public void addEdge(int src, int trg, int time) {
		nodes.get(src).addEdge(nodes.get(trg), time);
	}
	
	/**
	 * Return the version graph nodes
	 * @return
	 */
	public Collection<Node> getNodes() {
		return nodes.values();
	}
	
	/**
	 * Graph Size
	 * @return
	 */
	public int size() {
		return nodes.size();
	}
	
	/**
	 * Return nodes labeled with label at time instant t
	 * @param t
	 * @param label
	 * @return
	 */
	public Set<Node> getTiLaNodes(int t, int label) {
		Set<Node> set;

		if ((set = TiLa.get(t).get(label)) != null)
			return set;

		return Collections.emptySet();
	}
	
	/**
	 * Update TiLa add node in label set at time instant t
	 * 
	 * @param t
	 * @param label
	 * @param n
	 */
	public void udpateTiLa(int t, int label, Node n) {
		Set<Node> nodes;
		
		if ((nodes = TiLa.get(t).get(label)) == null) {
			nodes = new HashSet<>();
			TiLa.get(t).put(label, nodes);
		}
		
		nodes.add(n);
	}
	
	/**
	 * Return TiLa
	 * @return
	 */
	public List<Map<Integer, Set<Node>>> getTiLa() {
		return TiLa;
	}
}