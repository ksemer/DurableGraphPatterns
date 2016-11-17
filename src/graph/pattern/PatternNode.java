package graph.pattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import system.Config;

/**
 * Class of pattern node
 * 
 * @author ksemer
 */
public class PatternNode {

	// =================================================================
	private int id;
	private int label;
	private List<PatternNode> adjacency;
	private List<Set<Integer>> labelAdjacency;
	private List<Map<Integer, Integer>> labelAdjacency_C;
	// =================================================================

	/**
	 * Constructor
	 * 
	 * @param id
	 * @param label
	 */
	public PatternNode(int id, int label) {
		this.id = id;
		this.label = label;
		this.adjacency = new ArrayList<PatternNode>();

		if (Config.TINLA_ENABLED) {
			this.labelAdjacency = new ArrayList<>();

			for (int i = 0; i < Config.TINLA_R; i++)
				this.labelAdjacency.add(i, new HashSet<Integer>());
		} else if (Config.CTINLA_ENABLED) {
			this.labelAdjacency_C = new ArrayList<>();

			for (int i = 0; i < Config.CTINLA_R; i++)
				this.labelAdjacency_C.add(i, new HashMap<>());
		}
	}

	/**
	 * Add edge this->trg
	 * 
	 * @param trg
	 */
	public void addEdge(PatternNode trg) {
		adjacency.add(trg);
	}

	/**
	 * Return id
	 * 
	 * @return
	 */
	public int getID() {
		return id;
	}

	/**
	 * Return node's label
	 * 
	 * @return
	 */
	public int getLabel() {
		return label;
	}

	/**
	 * Get adjacency
	 * 
	 * @return
	 */
	public List<PatternNode> getAdjacency() {
		return adjacency;
	}

	/**
	 * Get label adjacency
	 * 
	 * @return
	 */
	public Set<Integer> getLabelAdjacency(int r) {
		return labelAdjacency.get(r);
	}

	public Map<Integer, Integer> getLabelAdjacency_C(int r) {
		return labelAdjacency_C.get(r);
	}
}