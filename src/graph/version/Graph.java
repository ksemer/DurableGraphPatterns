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

import graph.version.index.TimePathIndex;

import java.util.Set;
import java.util.Map.Entry;

import system.Config;
import utils.Storage;

/**
 * Labeled Version Graph class
 * 
 * @author ksemer
 */
public class Graph implements Serializable {

	// =================================================================

	private static final long serialVersionUID = 1L;
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
	 * Add edge in LVG with lifespan ranging from start to end - 1
	 * 
	 * @param src
	 * @param trg
	 * @param start
	 * @param end
	 */
	public void addEdge(int src, int trg, int start, int end) {
		nodes.get(src).addEdge(nodes.get(trg), start, end);
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

		if (Config.ENABLE_STAR_LABEL_PATTERNS) {

			if ((nodes = TiLa.get(t).get(Config.STAR_LABEL)) == null) {
				nodes = new HashSet<>();
				TiLa.get(t).put(Config.STAR_LABEL, nodes);
			}

			nodes.add(n);
		}
	}

	/**
	 * Update TiLa add node in label set for range start to end
	 * 
	 * @param start
	 * @param end
	 * @param label
	 * @param n
	 */
	public void udpateTiLa(int start, int end, int label, Node n) {
		Set<Node> nodes;

		for (int t = start; t < end; t++) {

			if ((nodes = TiLa.get(t).get(label)) == null) {
				nodes = new HashSet<>();
				TiLa.get(t).put(label, nodes);
			}

			nodes.add(n);

			if (Config.ENABLE_STAR_LABEL_PATTERNS) {

				if ((nodes = TiLa.get(t).get(Config.STAR_LABEL)) == null) {
					nodes = new HashSet<>();
					TiLa.get(t).put(Config.STAR_LABEL, nodes);
				}

				nodes.add(n);
			}
		}
	}

	/**
	 * Create TiPLa index
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void createTiPLa() throws IOException, InterruptedException {

		if (Config.BLOOM_ENABLED) {
			new TimePathIndex().createPathIndex(this);
		} else {
			TiPLa = new TimePathIndex().createPathIndex(this);

			if (Config.DEBUG) {
				for (Entry<Integer, Map<String, Set<Node>>> entry : TiPLa.entrySet()) {

					System.out.println("Time instance: " + entry.getKey());

					for (String p : entry.getValue().keySet())
						System.out.println(p);

					System.out.println("------------------------");
				}
			}
		}
	}

	/**
	 * Create TiNLa && CTiNLa index
	 */
	public void createTimeNeighborIndex() {

		int R = -1;

		if (Config.BLOOM_ENABLED) {
			createBloomTimeNeighborIndex();
			return;
		}

		String in = "";

		if (Config.TINLA_ENABLED) {
			in = "TiNLa";
			R = Config.TINLA_R;
		} else if (Config.CTINLA_ENABLED) {
			in = "CTiNLa";
			R = Config.CTINLA_R;
		}

		System.out.println(in + "(" + R + ") construction is starting...");

		long time = System.currentTimeMillis();

		// for each r
		for (int r = 0; r < R; r++) {

			final int r_ = r;

			// for all nodes
			nodes.values().parallelStream().forEach(n -> {

				if (Config.TINLA_ENABLED)
					n.initializeTiNLa(r_);
				else if (Config.CTINLA_ENABLED)
					n.initializeCTiNLa(r_);

				Node trg;

				// for each adjacent node
				for (Edge e : n.getAdjacency()) {
					trg = e.getTarget();

					// update TiNLa and CTiNLa in radius = 1
					if (r_ == 0) {

						if (Config.TINLA_ENABLED)
							n.updateTiNLa(r_, trg.getLabels());
						else if (Config.CTINLA_ENABLED)
							n.updateCTiNLa(r_, trg.getLabels());

					} else {

						// update TiNLa and CTiNLa in radius > 1
						if (Config.TINLA_ENABLED)
							n.updateTiNLa(r_, trg.getTiNLa().get(r_ - 1));
						else if (Config.CTINLA_ENABLED)
							n.updateCTiNLaR(r_, trg.getCTiNLa().get(r_ - 1));
					}
				}
			});

			if (Config.SHOW_MEMORY)
				System.out.println(in + "(" + (r + 1) + ") memory: " + Storage.bytesToMegabytes(getMemory()));

			System.out.println(in + "(" + (r + 1) + ") time: " + (System.currentTimeMillis() - time) + " (ms)");
		}
	}

	/**
	 * Create TiNLaBloom && CTiNLaBloom index
	 */
	private void createBloomTimeNeighborIndex() {

		int R = -1;

		String in = "", auxIn = "";

		if (Config.TINLA_ENABLED) {
			in = "TiNLaBloom";
			auxIn = "TiNLa";
			R = Config.TINLA_R;
		} else if (Config.CTINLA_ENABLED) {
			in = "CTiNLaBloom";
			auxIn = "CTiNLa";
			R = Config.CTINLA_R;
		}

		System.out.println(in + "(" + R + ") construction is starting...");

		long time = System.currentTimeMillis();

		// for each r
		for (int r = 0; r < R; r++) {

			final int r_ = r;

			// for all nodes
			nodes.values().parallelStream().forEach(n -> {

				if (Config.TINLA_ENABLED)
					n.initializeTiNLa(r_);
				else if (Config.CTINLA_ENABLED)
					n.initializeCTiNLa(r_);

				Node trg;

				// for each adjacent node
				for (Edge e : n.getAdjacency()) {
					trg = e.getTarget();

					// update TiNLa and CTiNLa in radius = 1
					if (r_ == 0) {
						if (Config.TINLA_ENABLED)
							n.updateTiNLa(r_, trg.getLabels());
						else if (Config.CTINLA_ENABLED)
							n.updateCTiNLa(r_, trg.getLabels());
					} else {
						// update TiNLa and CTiNLa in radius > 1
						if (Config.TINLA_ENABLED)
							n.updateTiNLa(r_, trg.getTiNLa().get(r_ - 1));
						else if (Config.CTINLA_ENABLED)
							n.updateCTiNLaR(r_, trg.getCTiNLa().get(r_ - 1));
					}
				}
			});

			if (Config.SHOW_MEMORY)
				System.out.println(auxIn + "(" + (r + 1) + ") memory: " + Storage.bytesToMegabytes(getMemory()));

			System.out.println(auxIn + "(" + (r + 1) + ") time: " + (System.currentTimeMillis() - time) + " (ms)");

			// create bloom
			if (Config.TINLA_ENABLED) {

				nodes.values().parallelStream().forEach(n -> {
					n.createTiNLaBloom(r_);
					
					if (r_ > 0)
						n.clearTiNLa(r_ - 1);
				});

			} else if (Config.CTINLA_ENABLED) {

				nodes.values().parallelStream().forEach(n -> {
					n.createCTiNLaBloom(r_);
					
					if (r_ > 0)
						n.clearCTiNLa(r_ - 1);
				});

			}

			System.out.println(in + "(" + (r + 1) + ") time: " + (System.currentTimeMillis() - time) + " (ms)");

			if (Config.SHOW_MEMORY)
				System.out.println(in + "(" + (r + 1) + ") memory: " + Storage.bytesToMegabytes(getMemory()));
		}

		if (Config.TINLA_ENABLED) {

			nodes.values().parallelStream().forEach(n -> {
				n.clearTiNLa();
			});

		} else if (Config.CTINLA_ENABLED) {

			nodes.values().parallelStream().forEach(n -> {
				n.clearCTiNLa();
			});
		}

		System.out
				.println(in + "(" + ") memory without auxiliary structures: " + Storage.bytesToMegabytes(getMemory()));
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

	/**
	 * Return available memory
	 * 
	 * @return
	 */
	private long getMemory() {
		Runtime runtime = Runtime.getRuntime();

		// Run the garbage collector
		runtime.gc();

		// Calculate the used memory
		return runtime.totalMemory() - runtime.freeMemory();
	}
}