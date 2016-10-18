package graph.pattern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import system.Config;

/**
 * Class of pattern node
 * @author ksemer
 */
public class PatternNode {
	//=================================================================
	private int id;
	private int label;
	private List<PatternNode> adjacency;
	private List<Set<Integer>> labelAdjacency;
	//=================================================================
	
	/**
	 * Constructor
	 * @param id
	 * @param label
	 */
	public PatternNode(int id, int label) {
		this.id = id;
		this.label = label;
		this.adjacency = new ArrayList<PatternNode>();
		this.labelAdjacency = new ArrayList<>();
		
		for (int i = 0; i < Config.TINLA_R; i++) 
			this.labelAdjacency.add(i, new HashSet<Integer>());
	}
	
	/**
	 * Add edge this->trg
	 * @param trg
	 */
	public void addEdge(PatternNode trg) {		
		adjacency.add(trg);
	}
	
	/**
	 * Return id
	 * @return
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * Return node's label
	 * @return
	 */
	public int getLabel() {
		return label;
	}
	
	/**
	 * Get adjacency
	 * @return
	 */
	public List<PatternNode> getAdjacency() {
		return adjacency;
	}
	
	/**
	 * Get label adjacency
	 * @return
	 */
	public Set<Integer> getLabelAdjacency(int r) {
		return labelAdjacency.get(r);
	}

	/**
	 * Create label adjacency for TiNLa index
	 */
	public void createLabelAdjacency() {
		Set<PatternNode> pNodes = new HashSet<>(adjacency), pNodes_toCheck = null;
		
		// more layers are enabled
		if (Config.TINLA_R > 1) 
			pNodes_toCheck = new HashSet<>();

		// for each layer
		for (int layer = 0; layer < Config.TINLA_R; layer++) {
			// get nodes to be checked
			for (PatternNode pn : pNodes) {
				// check their adjacency
				for (PatternNode trg : pn.adjacency) {
					// update this label adjacency for layer
					labelAdjacency.get(layer).add(trg.label);
					
					// if there is a further layer
					if (layer + 1 < Config.TINLA_R)
						pNodes_toCheck.add(trg);
				}
			}
			
			// update which nodes need to be checked
			if (layer + 1 < Config.TINLA_R) {
				pNodes = new HashSet<>(pNodes_toCheck);
				pNodes_toCheck.clear();
			}
		}
	}
}