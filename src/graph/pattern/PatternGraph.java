package graph.pattern;

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
public class PatternGraph {
	// =================================================================

	// pattern graph id
	private int id;

	// pattern nodes
	private List<PatternNode> nodes;

	// TiPLa index
	private Map<Integer, Set<String>> TiPLa;

	private Map<String, Set<PatternNode>> pathIndexWT;

	// temp variable
	private Map<Integer, boolean[]> hasBeenVisited;

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
	 * Add pattern node
	 * 
	 * @param id
	 * @param label
	 */
	public void addNode(int id, int label) {
		nodes.add(new PatternNode(id, label));
	}

	/**
	 * Add patter edge
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

		int R = -1;
		Integer in;

		if (Config.TINLA_ENABLED)
			R = Config.TINLA_R;
		else if (Config.CTINLA_ENABLED)
			R = Config.CTINLA_R;

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
							if ((in = pn.getCTiNLa(r).get(pt.getLabel())) == null)
								pn.getCTiNLa(r).put(pt.getLabel(), 1);
							else
								pn.getCTiNLa(r).put(pt.getLabel(), in.intValue() + 1);

						} else {

							// update CTiNLa for radius > 1
							for (int label : pt.getCTiNLa(r - 1).keySet()) {

								if ((in = pn.getCTiNLa(r).get(label)) == null)
									pn.getCTiNLa(r).put(label, pt.getCTiNLa(r - 1).get(label));
								else
									pn.getCTiNLa(r).put(label, in.intValue() + pt.getCTiNLa(r - 1).get(label));
							}
						}
					}
				}
			}
		}
	}

	/***
	 * Return TiPLa
	 * 
	 * @return
	 */
	public Set<String> getTiPLa(int id) {
		return TiPLa.get(id);
	}

	/**
	 * Create path index Return for each pattern node the set of paths started
	 * from it
	 * 
	 * @return
	 */
	public void createPathIndex() {
		System.out.println("Pattern Path Index is running");
		hasBeenVisited = new HashMap<>();
		pathIndexWT = new HashMap<>();

		for (int depth = Config.TIPLA_MAX_DEPTH; depth >= 1; depth--) {
			for (PatternNode n : nodes) {

				if (!hasBeenVisited.containsKey(n.getID()))
					hasBeenVisited.put(n.getID(), new boolean[nodes.size()]);

				traversePath(n, depth);
			}
		}

		// key -> pattern node id,
		// value -> set of string which includes all the paths from pattern node
		Map<Integer, Set<String>> in = new HashMap<>();

		// initialize
		for (PatternNode p : nodes) {
			in.put(p.getID(), new HashSet<>());

			if (Config.ISDIRECTED)
				// path size of 0 (contains only it self)
				if (p.getAdjacency().isEmpty())
				in.get(p.getID()).add("" + p.getLabel());
		}

		// iterate path index
		for (Entry<String, Set<PatternNode>> entry : pathIndexWT.entrySet()) {
			String path = entry.getKey();

			// update in for each pattern node add the paths
			for (PatternNode p : entry.getValue())
				in.get(p.getID()).add(path);
		}

		// print for each pattern node its paths
		for (PatternNode p : nodes)
			System.out.println("PNodeID: " + p.getID() + "->" + in.get(p.getID()));

		TiPLa = in;
	}

	/**
	 * TraversePath
	 * 
	 * @param n
	 * @param max_depth
	 */
	private void traversePath(PatternNode n, int max_depth) {

		Deque<n_info> toBeVisited = new ArrayDeque<>();
		List<PatternNode> path;

		n_info info = new n_info(n, null, 0);
		toBeVisited.add(info);

		while (!toBeVisited.isEmpty()) {
			info = toBeVisited.poll();

			if (info.depth == max_depth) {
				path = new ArrayList<>();

				while (true) {
					if (info.father == null) {
						if (max_depth == Config.TIPLA_MAX_DEPTH)
							hasBeenVisited.get(n.getID())[path.get(1).getID()] = true;

						path.add(info.n);
						break;
					}

					path.add(info.n);
					info = info.father;
				}

				Collections.reverse(path);

				// call recursive
				rec_labelComp(path, path.get(0), "", 0);

				continue;
			}

			for (PatternNode trg : info.n.getAdjacency()) {
				if (hasBeenVisited.get(n.getID())[trg.getID()])
					continue;

				if (info.father == null) {
					toBeVisited.add(new n_info(trg, info, info.depth + 1));
				} else if (!info.father.n.equals(trg)) {
					toBeVisited.add(new n_info(trg, info, info.depth + 1));
				}
			}
		}
	}

	/**
	 * Recursive function
	 * 
	 * @param path
	 * @param src
	 * @param label
	 * @param depth
	 */
	private void rec_labelComp(List<PatternNode> path, PatternNode src, String label, int depth) {
		PatternNode n = path.get(depth);

		if (depth + 1 < path.size())
			rec_labelComp(path, src, label + "|" + n.getLabel(), depth + 1);
		else {
			// i is the next label in path
			// we use integers to denote labels
			String Path = label + "|" + n.getLabel();

			if ((set = pathIndexWT.get(Path)) == null) {
				set = new HashSet<>();
				pathIndexWT.put(Path, set);
			}

			set.add(src);
		}
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