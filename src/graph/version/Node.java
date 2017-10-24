package graph.version;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import system.Config;

/**
 * This class represents the Node objects
 * 
 * @author ksemer
 */
public class Node implements Serializable {

	private static final long serialVersionUID = 1L;

	// =================================================================

	// node id
	private int id;

	// label -> [lifespan]
	private Map<Integer, BitSet> labels;

	// trg -> [Edge]
	private Map<Node, Edge> adjacencies;

	// r -> label -> [lifespan]
	private List<Map<Integer, BitSet>> TiNLa;

	// r -> label -> [t--> c]
	private List<Map<Integer, Map<Integer, Integer>>> CTiNLa;
	// =================================================================

	/**
	 * Constructor
	 * 
	 * @param id
	 */
	public Node(int id) {
		this.id = id;
		this.adjacencies = new HashMap<>();
		this.labels = new HashMap<>();

		if (Config.ENABLE_STAR_LABEL_PATTERNS) {
			BitSet lifespan = new BitSet(Config.MAXIMUM_INTERVAL);
			labels.put(Config.STAR_LABEL, lifespan);
		}

		if (Config.TINLA_ENABLED) {
			TiNLa = new ArrayList<>(Config.TINLA_R);

			for (int i = 0; i < Config.TINLA_R; i++)
				TiNLa.add(i, new HashMap<Integer, BitSet>());
		} else if (Config.CTINLA_ENABLED) {
			CTiNLa = new ArrayList<>(Config.CTINLA_R);

			for (int i = 0; i < Config.CTINLA_R; i++)
				CTiNLa.add(i, new HashMap<>());
		}
	}

	/**
	 * Add new edge or update existed
	 * 
	 * @param node
	 * @param time
	 */
	public void addEdge(Node node, int time) {
		Edge e = adjacencies.get(node);

		if (e == null) {
			e = new Edge(node, new BitSet(Config.MAXIMUM_INTERVAL));
			adjacencies.put(node, e);
		}

		e.updateLifetime(time);
	}

	/**
	 * Update label lifespan
	 * 
	 * @param label
	 * @param t
	 */
	public void updateLabelLifespan(int label, int t) {
		BitSet lifespan;

		if ((lifespan = labels.get(label)) == null) {
			lifespan = new BitSet(Config.MAXIMUM_INTERVAL);
			labels.put(label, lifespan);
		}

		lifespan.set(t);

		if (Config.ENABLE_STAR_LABEL_PATTERNS) {
			labels.get(Config.STAR_LABEL).set(t);
		}
	}

	/**
	 * Update TiNLa(r) labels contain info from an adjacent node of (this) node
	 * 
	 * @param r
	 * @param label
	 */
	public void updateTiNLa(int r, Map<Integer, BitSet> labels) {
		int label;
		BitSet lifespan, lifespanTrg;
		Map<Integer, BitSet> TiNLaR = TiNLa.get(r);

		// for each label
		for (Entry<Integer, BitSet> entry : labels.entrySet()) {
			// label
			label = entry.getKey();

			// label's lifespan
			lifespanTrg = entry.getValue();

			if ((lifespan = TiNLaR.get(label)) == null) {
				lifespan = new BitSet(Config.MAXIMUM_INTERVAL);
				TiNLaR.put(label, lifespan);
			}

			// update TiNLa index
			lifespan.or(lifespanTrg);
		}
	}

	/**
	 * Update CTiNLa in radius 1
	 * 
	 * @param r
	 * @param labels
	 */
	public void updateCTiNLa(int r, Map<Integer, BitSet> labels) {
		int t, label;
		BitSet lifespan;
		Integer tmpCounter;
		Map<Integer, Integer> CTiNLa_l;
		Map<Integer, Map<Integer, Integer>> CTiNLaR = CTiNLa.get(r);

		// for each label
		for (Entry<Integer, BitSet> entry : labels.entrySet()) {

			// label
			label = entry.getKey();

			// label's lifespan
			lifespan = entry.getValue();

			if ((CTiNLa_l = CTiNLaR.get(label)) == null) {
				CTiNLa_l = new HashMap<>();
				CTiNLaR.put(label, CTiNLa_l);
			}

			// for each active time instant update CTiNLa index
			for (Iterator<Integer> it = lifespan.stream().iterator(); it.hasNext();) {
				t = it.next();

				if ((tmpCounter = CTiNLa_l.get(t)) == null)
					CTiNLa_l.put(t, 1);
				else
					CTiNLa_l.put(t, tmpCounter.intValue() + 1);
			}
		}
	}

	/**
	 * Update CTiNLa in radius > 1
	 * 
	 * @param r
	 * @param trgCTiNLa
	 */
	public void updateCTiNLaR(int r, Map<Integer, Map<Integer, Integer>> trgCTiNLa) {
		int label, t;
		Integer tmpCounter;
		Map<Integer, Integer> CTiNLa_l;
		Map<Integer, Integer> counterPerTime;
		Map<Integer, Map<Integer, Integer>> CTiNLaR = CTiNLa.get(r);

		// for each label
		for (Entry<Integer, Map<Integer, Integer>> entry : trgCTiNLa.entrySet()) {

			// label
			label = entry.getKey();

			// per time instant the counter for the label
			counterPerTime = entry.getValue();

			if ((CTiNLa_l = CTiNLaR.get(label)) == null) {
				CTiNLa_l = new HashMap<>();
				CTiNLaR.put(label, CTiNLa_l);
			}

			// for each active time instant update TiNLa index
			for (Entry<Integer, Integer> et : counterPerTime.entrySet()) {
				t = et.getKey();

				if ((tmpCounter = CTiNLa_l.get(t)) == null)
					CTiNLa_l.put(t, et.getValue());
				else
					CTiNLa_l.put(t, tmpCounter.intValue() + et.getValue());
			}
		}
	}

	/**
	 * Returns node's id
	 * 
	 * @return
	 */
	public int getID() {
		return id;
	}

	/**
	 * Return label's lifespan
	 * 
	 * @param label
	 * @return
	 */
	public BitSet getLabel(int label) {
		return labels.get(label);
	}

	/**
	 * Return labels structure
	 * 
	 * @return
	 */
	public Map<Integer, BitSet> getLabels() {
		return labels;
	}

	/**
	 * Return nodes's adjacency
	 * 
	 * @return
	 */
	public Collection<Edge> getAdjacency() {
		return adjacencies.values();
	}

	/**
	 * Return edge object for neighbor node n
	 * 
	 * @param n
	 * @return
	 */
	public Edge getEdge(Node n) {
		return adjacencies.get(n);
	}

	/**
	 * Return for Node the intersection of TiNLa(r) and given lifespan
	 * 
	 * @param r
	 * @param label
	 * @param lifespan
	 * @return
	 */
	public BitSet getTiNLa(int r, int label, BitSet lifespan) {
		BitSet life;

		if ((life = TiNLa.get(r).get(label)) == null)
			return null;

		lifespan.and(life);

		return lifespan;
	}

	/**
	 * Return TiNLa index
	 * 
	 * @return
	 */
	public List<Map<Integer, BitSet>> getTiNLa() {
		return TiNLa;
	}

	/**
	 * Return CTiNLa index
	 * 
	 * @return
	 */
	public List<Map<Integer, Map<Integer, Integer>>> getCTiNLa() {
		return CTiNLa;
	}

	/**
	 * Given a cTiNLa index and a c value, return a lifespan that denotes that there
	 * are at least c neighborhoods with the given label in the given lifespan
	 * 
	 * @param r
	 * @param label
	 * @param c
	 * @return
	 */
	public BitSet getCTiNLa(int r, int label, int c, BitSet lifespan) {
		Map<Integer, Integer> cT;

		if ((cT = CTiNLa.get(r).get(label)) == null)
			return null;

		int t;
		BitSet life = (BitSet) lifespan.clone();

		for (Iterator<Integer> it = lifespan.stream().iterator(); it.hasNext();) {
			t = it.next();

			// if node doesn't contain at least c neighbors then disable the t
			// instant
			if (cT.get(t) == null || cT.get(t) < c)
				life.set(t, false);
		}

		return life;
	}
}