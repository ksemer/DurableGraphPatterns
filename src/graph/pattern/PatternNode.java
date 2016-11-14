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
 * @author ksemer
 */
public class PatternNode {
	//=================================================================
	private int id;
	private int label;
	private List<PatternNode> adjacency;
	private List<Set<Integer>> labelAdjacency;
	private List<Map<Integer, Integer>> labelAdjacency_C;
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
		
		if (Config.TINLA_ENABLED) {
			this.labelAdjacency = new ArrayList<>();

			for (int i = 0; i < Config.TINLA_R; i++) 
				this.labelAdjacency.add(i, new HashSet<Integer>());
		}
		else if (Config.TINLA_C_ENABLED) {
			this.labelAdjacency_C = new ArrayList<>();

			for (int i = 0; i < Config.TINLA_R; i++) 
				this.labelAdjacency_C.add(i, new HashMap<>());
		}
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
	
	public Map<Integer, Integer> getLabelAdjacency_C(int r) {
		return labelAdjacency_C.get(r);
	}

	/**
	 * Create label adjacency for TiNLa index
	 */
	public void createLabelAdjacency() {
		
		if (Config.TINLA_ENABLED) {
			Set<PatternNode> pNodes = null;
			
			//TODO support for more layers
			if (Config.TINLA_R == 2)
				pNodes = new HashSet<>();
			
			for (PatternNode trg : this.adjacency) {
				labelAdjacency.get(0).add(trg.label);
				
				//TODO support for more layers
				if (pNodes != null)
					pNodes.add(trg);
			}		
			
			//TODO support for more layers
			if (pNodes != null) {
				for (PatternNode pn : pNodes)
					for (PatternNode trg : pn.adjacency)
						labelAdjacency.get(1).add(trg.label);
			}
		} else if (Config.TINLA_C_ENABLED) {
			Set<PatternNode> pNodes = null;
			
			//TODO support for more layers
			if (Config.TINLA_R == 2)
				pNodes = new HashSet<>();

			Integer in;
			
			for (PatternNode trg : this.adjacency) {
				
				if ((in = labelAdjacency_C.get(0).get(trg.label)) == null)
					labelAdjacency_C.get(0).put(trg.label, 1);
				else
					labelAdjacency_C.get(0).put(trg.label, in.intValue() + 1);
				
				//TODO support for more layers
				if (pNodes != null)
					pNodes.add(trg);
			}
			
			//TODO support for more layers
			if (pNodes != null) {

				for (PatternNode pn : pNodes)
					for (PatternNode trg : pn.adjacency) {
						if ((in = labelAdjacency_C.get(1).get(trg.label)) == null)
							labelAdjacency_C.get(1).put(trg.label, 1);
						else
							labelAdjacency_C.get(1).put(trg.label, in.intValue() + 1);
					}
			}		
		}
		
	}
}