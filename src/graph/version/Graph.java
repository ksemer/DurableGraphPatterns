package graph.version;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import algorithm.indexes.TimePathIndex;
import system.Config;

/**
 * Labeled Version Graph class
 * 
 * @author ksemer
 */
public class Graph implements Serializable {

	private static final long serialVersionUID = 1L;

	// =================================================================
	private Map<Integer, Node> nodes;
	private List<Map<Integer, Set<Node>>> TiLa;
	private Map<Integer, Map<String, Set<Node>>> TiPLa;
	// =================================================================

	/**
	 * Constructor
	 * 
	 * @throws IOException
	 */
	public Graph() {
		nodes = new HashMap<Integer, Node>();
		TiLa = new ArrayList<>(Config.MAXIMUM_INTERVAL);

		Map<Integer, Set<Node>> map;

		for (int i = 0; i < Config.MAXIMUM_INTERVAL; i++) {
			map = new HashMap<>();
			TiLa.add(map);
		}
	}

	/**
	 * Add node in LVG
	 * 
	 * @param node
	 */
	public void addNode(int node) {
		if (nodes.get(node) == null)
			nodes.put(node, new Node(node));
	}

	/**
	 * Get node object with id = nodeID
	 * 
	 * @param nodeID
	 * @return
	 */
	public Node getNode(int nodeID) {
		return nodes.get(nodeID);
	}

	/**
	 * Add edge in LVG
	 * 
	 * @param src
	 * @param trg
	 * @param time
	 */
	public void addEdge(int src, int trg, int time) {
		nodes.get(src).addEdge(nodes.get(trg), time);
	}

	/**
	 * Return the version graph nodes
	 * 
	 * @return
	 */
	public Collection<Node> getNodes() {
		return nodes.values();
	}

	/**
	 * Graph Size
	 * 
	 * @return
	 */
	public int size() {
		return nodes.size();
	}

	/**
	 * Return nodes labeled with label at time instant t
	 * 
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
	 * 
	 * @return
	 */
	public List<Map<Integer, Set<Node>>> getTiLa() {
		return TiLa;
	}

	/**
	 * Return TiPLa index
	 * 
	 * @return
	 */
	public Map<Integer, Map<String, Set<Node>>> getTiPLa() {
		return TiPLa;
	}

	/**
	 * Create TiPLa index
	 * 
	 * @throws IOException
	 */
	public void createTiPLa() throws IOException {
		this.TiPLa = new TimePathIndex(Config.TIPLA_MAX_DEPTH).createPathIndex(this);
	}

	/**
	 * Create TiNLa && CTiNLa index
	 */
	public void createTimeNeighborIndex() {
		int R = -1, label;
		Node trg;
		BitSet lifespan;

		if (Config.TINLA_ENABLED) {
			System.out.println("TiNLa(" + Config.TINLA_R + ") construction is starting...");
			R = Config.TINLA_R;
		} else if (Config.CTINLA_ENABLED) {
			System.out.println("CTiNLa(" + Config.CTINLA_R + ") construction is starting...");
			R = Config.CTINLA_R;
		}

		long time = System.currentTimeMillis();

		// for all nodes
		for (Node n : nodes.values()) {

			Set<Node> visited = new HashSet<>();
			Set<Edge> temp_edges = new HashSet<>();
			Set<Edge> adjacency = new HashSet<>(n.getAdjacency());

			// for each r
			for (int r = 0; r < R; r++) {

				// for each adjacent node of radius r
				for (Edge e : adjacency) {
					trg = e.getTarget();

					// for avoiding cycles
					if (R > 2) {
						if (visited.contains(trg))
							continue;
						else {
							// add node to visited set
							visited.add(trg);
						}
					}

					// for each label of trg node
					for (Entry<Integer, BitSet> entry : trg.getLabels().entrySet()) {
						// label
						label = entry.getKey();

						// lifespan of the label
						lifespan = entry.getValue();

						// for each active time instant update TiNLa index
						for (Iterator<Integer> it = lifespan.stream().iterator(); it.hasNext();) {
							int t = it.next();

							if (Config.TINLA_ENABLED)
								n.updateTiNLa(r, label, t);
							else if (Config.CTINLA_ENABLED)
								n.updateCTiNLa(r, label, t);
						}
					}
					
					//TODO skepsou kalutero tropo gia r > 1
					// store in temp set the edges of the next hop
					temp_edges.addAll(trg.getAdjacency());
				}

				// clear and set adjacency to show the next hop
				adjacency.clear();
				adjacency.addAll(temp_edges);
				temp_edges.clear();
			}
		}

		if (Config.TINLA_ENABLED)
			System.out.println(
					"TiNLa(" + Config.TINLA_R + ") time: " + (System.currentTimeMillis() - time) / 1000 + " (sec)");
		else if (Config.CTINLA_ENABLED)
			System.out.println(
					"CTiNLa(" + Config.CTINLA_R + ") time: " + (System.currentTimeMillis() - time) / 1000 + " (sec)");
	}
}