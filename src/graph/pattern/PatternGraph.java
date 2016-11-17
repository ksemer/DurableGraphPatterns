package graph.pattern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import system.Config;

/**
 * PatternGraph class
 * 
 * @author ksemer
 */
public class PatternGraph {
	// =================================================================
	private List<PatternNode> nodes;
	// =================================================================

	/**
	 * Constructor
	 */
	public PatternGraph() {
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
	 * Create label adjacency for TiNLa and CTiNLa indexes
	 */
	public void createLabelAdjacency() {
		if (!Config.TINLA_ENABLED && !Config.CTINLA_ENABLED)
			return;

		int R = -1;
		Integer in;

		if (Config.TINLA_ENABLED)
			R = Config.TINLA_R;
		else if (Config.CTINLA_ENABLED)
			R = Config.CTINLA_R;

		// for each pattern node
		for (PatternNode pn : nodes) {

			Set<PatternNode> visited = new HashSet<>();
			Set<PatternNode> temp_nodes = new HashSet<>();
			Set<PatternNode> adjacency = new HashSet<>(pn.getAdjacency());

			// for each r
			for (int r = 0; r < R; r++) {

				// for each node in adjacency or radius r
				for (PatternNode trg : adjacency) {

					// for avoiding cycles
					if (R > 2 && visited.contains(trg))
						continue;

					if (Config.TINLA_ENABLED) {

						// update pn label adjacency
						pn.getLabelAdjacency(r).add(trg.getLabel());
					} else if (Config.CTINLA_ENABLED) {

						// update pn's label CTiNLa index
						if ((in = pn.getLabelAdjacency_C(r).get(trg.getLabel())) == null)
							pn.getLabelAdjacency_C(r).put(trg.getLabel(), 1);
						else
							pn.getLabelAdjacency_C(r).put(trg.getLabel(), in.intValue() + 1);
					}

					// store in temp set the nodes of the next hop
					temp_nodes.addAll(trg.getAdjacency());

					if (R > 2) {
						// add node to visited set
						visited.add(trg);
					}
				}

				// clear and set adjacency to show the next hop
				adjacency.clear();
				adjacency.addAll(temp_nodes);
				temp_nodes.clear();
			}
		}
	}
}