package graph.pattern;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import system.Config;

/**
 * PatternGraph class
 * 
 * @author ksemer
 */
public class PatternGraph implements Serializable {

	// =================================================================

	private static final long serialVersionUID = 1L;

	// pattern graph id
	private int id;

	// pattern nodes
	private List<PatternNode> nodes;

	// TiPLa index
	private Map<Integer, List<String>> TiPLa;

	private Map<String, Set<PatternNode>> pathIndexWT;

	// help variables
	private Set<PatternNode> set;

	// =================================================================

	/**
	 * Constructor
	 * 
	 * @param id
	 */
	public PatternGraph(int id) {
		this.id = id;
		nodes = new ArrayList<PatternNode>();
	}

	/**
	 * Add a new pattern node
	 * 
	 * @param id
	 * @param label
	 */
	public void addNode(int id, int label) {
		nodes.add(new PatternNode(id, label));
	}

	/**
	 * Add a new pattern edge
	 * 
	 * @param src
	 * @param trg
	 */
	public void addEdge(int src, int trg) {
		nodes.get(src).addEdge(nodes.get(trg));
	}

	/**
	 * Return PNode with id node
	 * 
	 * @param node
	 * @return
	 */
	public PatternNode getNode(int node) {
		return nodes.get(node);
	}

	/**
	 * Return all pattern nodes
	 * 
	 * @return
	 */
	public List<PatternNode> getNodes() {
		return this.nodes;
	}

	/**
	 * Returns graph size
	 * 
	 * @return
	 */
	public int size() {
		return nodes.size();
	}

	/**
	 * Return pattern's graph id
	 * 
	 * @return
	 */
	public int getID() {
		return id;
	}

	/**
	 * Create TiNLa and CTiNLa indexes
	 */
	public void createTimeNeighborIndex() {

		int R = -1, label;
		Integer in;

		if (Config.TINLA_ENABLED)
			R = Config.TINLA_R;
		else if (Config.CTINLA_ENABLED)
			R = Config.CTINLA_R;

		// create TiNLa/CTiNLa structures
		for (PatternNode pn : nodes) {
			pn.initializeNeighborIndexes();
		}

		// for each r
		for (int r = 0; r < R; r++) {

			// for each pattern node
			for (PatternNode pn : nodes) {

				// for each node in adjacency or radius r
				for (PatternNode pt : pn.getAdjacency()) {

					if (Config.TINLA_ENABLED) {

						// update TiNLa in radius = 1
						if (r == 0)
							pn.getTiNLa(r).add(pt.getLabel());
						else // update TiNLa in radius > 1
							pn.getTiNLa(r).addAll(pt.getTiNLa(r - 1));

					} else if (Config.CTINLA_ENABLED) {

						// update CTiNLa for radius = 1
						if (r == 0) {
							label = pt.getLabel();

							if ((in = pn.getCTiNLa(r).get(label)) == null)
								pn.getCTiNLa(r).put(label, 1);
							else
								pn.getCTiNLa(r).put(label, in.intValue() + 1);

						} else {

							// update CTiNLa for radius > 1
							for (int label_ : pt.getCTiNLa(r - 1).keySet()) {

								if ((in = pn.getCTiNLa(r).get(label_)) == null)
									pn.getCTiNLa(r).put(label_, pt.getCTiNLa(r - 1).get(label_));
								else
									pn.getCTiNLa(r).put(label_, in.intValue() + pt.getCTiNLa(r - 1).get(label_));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Create path index Return for each pattern node the set of paths started from
	 * it
	 * 
	 * @return
	 */
	public void createPathIndex() {

		pathIndexWT = new HashMap<>();

		for (PatternNode n : nodes)
			traversePath(n);

		// key -> pattern node id,
		// value -> set of string which includes all the paths from pattern node
		Map<Integer, Set<String>> in = new HashMap<>();

		// initialize
		for (PatternNode p : nodes)
			in.put(p.getID(), new HashSet<>());

		// iterate path index
		for (Entry<String, Set<PatternNode>> entry : pathIndexWT.entrySet()) {

			String path = entry.getKey();

			// update in for each pattern node add the paths
			for (PatternNode p : entry.getValue())
				in.get(p.getID()).add(path);
		}

		TiPLa = new HashMap<>();
		List<String> paths;

		// sort paths by length, longer paths should be first since shorter paths are
		// subsets of the longest
		for (PatternNode p : nodes) {
			paths = new ArrayList<>(in.get(p.getID()));
			paths.sort((first, second) -> Integer.compare(second.length(), first.length()));
			TiPLa.put(p.getID(), paths);
		}

		if (Config.DEBUG) {

			System.out.println("Pattern Graph: " + id);

			// print for each pattern node its paths
			for (PatternNode p : nodes)
				System.out.println("PNodeID: " + p.getID() + "->" + TiPLa.get(p.getID()));
		}
	}

	/**
	 * TraversePath
	 * 
	 * @param n
	 */
	private void traversePath(PatternNode n) {

		Deque<n_info> toBeVisited = new ArrayDeque<>();

		n_info info = new n_info(n, null, 0);
		toBeVisited.add(info);

		while (!toBeVisited.isEmpty()) {
			info = toBeVisited.poll();

			// if we are in the last node defined by the depth
			if (info.depth == Config.TIPLA_MAX_DEPTH) {

				storePath(info);
				continue;
			}

			boolean addNew = false;

			for (PatternNode trg : info.n.getAdjacency()) {

				if (info.father == null) {
					addNew = true;
					toBeVisited.add(new n_info(trg, info, info.depth + 1));
				} else if (!info.father.n.equals(trg)) {
					addNew = true;
					toBeVisited.add(new n_info(trg, info, info.depth + 1));
				}
			}

			// if the path ends before maxDepth
			if (!addNew && (info.depth + 1) < Config.TIPLA_MAX_DEPTH)
				storePath(info);
		}
	}

	/**
	 * Corrects the order of the path and create the label paths
	 * 
	 * @param info
	 */
	private void storePath(n_info info) {
		List<PatternNode> path = new ArrayList<>();

		while (true) {

			if (info.father == null) {
				path.add(info.n);
				break;
			}

			// add the node to the path and update father
			path.add(info.n);
			info = info.father;
		}

		Collections.reverse(path);

		String labelPath = "";

		for (int d = 0; d < path.size(); d++) {

			labelPath += path.get(d).getLabel();

			if (Config.ISDIRECTED || d > 0) {

				if ((set = pathIndexWT.get(labelPath)) == null) {
					set = new HashSet<>();
					pathIndexWT.put(labelPath, set);
				}

				// add node that has the computed labelPath
				set.add(path.get(0));
			}

			labelPath += " ";
		}
	}

	/***
	 * Return TiPLa
	 * 
	 * @return
	 */
	public List<String> getTiPLa(int id) {
		return TiPLa.get(id);
	}

	class n_info {
		PatternNode n;
		n_info father;
		int depth;

		public n_info(PatternNode n, n_info father, int depth) {
			this.n = n;
			this.father = father;
			this.depth = depth;
		}
	}
}