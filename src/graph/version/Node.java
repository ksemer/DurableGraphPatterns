package graph.version;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import algorithm.indexes.TimeNeighborIndex;
import system.Config;

/**
 * This class represents the Node objects
 * 
 * @author ksemer
 */
public class Node implements Serializable {

	private static final long serialVersionUID = 1L;

	// =================================================================
	private int id;
	private Map<Integer, BitSet> labels;
	private Map<Node, Edge> adjacencies;
	private List<Map<Integer, BitSet>> TiNLa;
	private List<TimeNeighborIndex> CTiNLa;
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

		if (Config.TINLA_ENABLED) {
			TiNLa = new ArrayList<>(Config.TINLA_R);

			for (int i = 0; i < Config.TINLA_R; i++)
				TiNLa.add(i, new HashMap<Integer, BitSet>());
		}

		if (Config.CTINLA_ENABLED) {
			CTiNLa = new ArrayList<>(Config.CTINLA_R);

			for (int i = 0; i < Config.CTINLA_R; i++)
				CTiNLa.add(i, new TimeNeighborIndex());
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
	 * Update label lifespan
	 * 
	 * @param label
	 * @param t
	 */
	public void updateLabelLifetime(int label, int t) {
		BitSet lifespan;

		if ((lifespan = labels.get(label)) == null) {
			lifespan = new BitSet(Config.MAXIMUM_INTERVAL);
			labels.put(label, lifespan);
		}

		lifespan.set(t);
	}

	/**
	 * Return for Node the lifespan of given label in the neighborhood in radius
	 * r
	 * 
	 * @param r
	 * @param label
	 * @return
	 */
	public BitSet getTiNLa(int r, int label) {
		return TiNLa.get(r).get(label);
	}

	/**
	 * Given a label, the radius and the c value Return a lifespan that denotes
	 * that there are at least c neighborhoods with the given label in the given
	 * lifespan
	 * 
	 * @param r
	 * @param label
	 * @param c
	 * @param lifespan
	 * @return
	 */
	public BitSet getTiNLa_C(int r, int label, int c, BitSet lifespan) {
		int t;
		BitSet life = (BitSet) lifespan.clone();
		Map<Integer, Integer> index = CTiNLa.get(r).getCounter(label);

		for (Iterator<Integer> it = lifespan.stream().iterator(); it.hasNext();) {
			t = it.next();

			// if node doesn't contaiin at least c neighbors then disable the t
			// instant
			if (index.get(t) < c)
				life.set(t, false);
		}

		return life;
	}

	/**
	 * Return for radius r the
	 * 
	 * @param r
	 * @param label
	 * @return
	 */
	public BitSet getTiNLa_C(int r, int label) {
		return CTiNLa.get(r).getTiNLa(label);
	}

	/**
	 * Update TiNLa(r) set for label the time instant t
	 * 
	 * @param r
	 * @param label
	 * @param t
	 */
	public void updateTiNLa(int r, int label, int t) {
		BitSet lifespan;

		if ((lifespan = TiNLa.get(r).get(label)) == null) {
			lifespan = new BitSet(Config.MAXIMUM_INTERVAL);
			TiNLa.get(r).put(label, lifespan);
		}

		lifespan.set(t);
	}

	/**
	 * Update CTiNLa(r) set for label the time instant t
	 * 
	 * @param r
	 * @param label
	 * @param t
	 */
	public void updateCTiNLa(int r, int label, int t) {
		CTiNLa.get(r).update(label, t);
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
	 * Returns node's id
	 * 
	 * @return
	 */
	public int getID() {
		return id;
	}
}