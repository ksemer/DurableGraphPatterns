package graph.version;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import algorithm.TimePathIndex;
import system.Config;
import utils.Storage;

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
	 * Create TiPLa index
	 * 
	 * @throws IOException
	 */
	public void createTiPLa() throws IOException {
		this.TiPLa = new TimePathIndex().createPathIndex(this);
	}

	/**
	 * Create TiNLa && CTiNLa index
	 */
	public void createTimeNeighborIndex() {
		int R = -1;
		Node trg;

		if (Config.TINLA_ENABLED) {
			System.out.println("TiNLa(" + Config.TINLA_R + ") construction is starting...");
			R = Config.TINLA_R;
		} else if (Config.CTINLA_ENABLED) {
			System.out.println("CTiNLa(" + Config.CTINLA_R + ") construction is starting...");
			R = Config.CTINLA_R;
		}

		long time = System.currentTimeMillis();

		// for each r
		for (int r = 0; r < R; r++) {

			// for all nodes
			for (Node n : nodes.values()) {

				// for each adjacent node
				for (Edge e : n.getAdjacency()) {
					trg = e.getTarget();

					// update TiNLa and CTiNLa in radius = 1
					if (r == 0) {
						if (Config.TINLA_ENABLED)
							n.updateTiNLa(r, trg.getLabels());
						else if (Config.CTINLA_ENABLED)
							n.updateCTiNLa(r, trg.getLabels());
					} else {
						// update TiNLa and CTiNLa in radius > 1
						if (Config.TINLA_ENABLED)
							n.updateTiNLa(r, trg.getTiNLa().get(r - 1));
						else if (Config.CTINLA_ENABLED)
							n.updateCTiNLaR(r, trg.getCTiNLa().get(r - 1));
					}
				}
			}

			if (Config.SHOW_MEMORY) {
				Runtime runtime = Runtime.getRuntime();

				// Run the garbage collector
				runtime.gc();

				// Calculate the used memory
				long memory = runtime.totalMemory() - runtime.freeMemory();

				if (Config.TINLA_ENABLED)
					System.out.println("Used memory with TiNLa(" + (r + 1) + "): " + Storage.bytesToMegabytes(memory));
				else if (Config.CTINLA_ENABLED)
					System.out.println("Used memory with CTiNLa(" + (r + 1) + "): " + Storage.bytesToMegabytes(memory));
			}

			if (Config.TINLA_ENABLED)
				System.out.println("TiNLa(" + (r + 1) + ") time: " + (System.currentTimeMillis() - time) + " (ms)");
			else if (Config.CTINLA_ENABLED)
				System.out.println("CTiNLa(" + (r + 1) + ") time: " + (System.currentTimeMillis() - time) + " (ms)");
		}
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

}