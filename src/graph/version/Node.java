package graph.version;

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
public class Node {
	// =================================================================
	private int id;
	private Map<Integer, BitSet> labels;
	private Map<Node, Edge> adjacencies;
	private List<Map<Integer, BitSet>> TiNLa;
	private List<TimeNeighborIndex> TiNLa_C;
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

		if (Config.TINLA_C_ENABLED) {
			TiNLa_C = new ArrayList<>(Config.TINLA_R);

			for (int i = 0; i < Config.TINLA_R; i++)
				TiNLa_C.add(i, new TimeNeighborIndex());
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
	 * Return label's lifetime
	 * 
	 * @param label
	 * @return
	 */
	public BitSet getLabel(int label) {
		return labels.get(label);
	}

	/**
	 * Update label lifetime
	 * 
	 * @param label
	 * @param t
	 */
	public void updateLabelLifetime(int label, int t) {
		BitSet lifetime;

		if ((lifetime = labels.get(label)) == null) {
			lifetime = new BitSet(Config.MAXIMUM_INTERVAL);
			labels.put(label, lifetime);
		}

		lifetime.set(t);
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
	 * lifetime
	 * 
	 * @param r
	 * @param label
	 * @param c
	 * @param lifetime
	 * @return
	 */
	public BitSet getTiNLa_C(int r, int label, int c, BitSet lifetime) {
		int t;
		BitSet life = (BitSet) lifetime.clone();
		Map<Integer, Integer> index = TiNLa_C.get(r).getCounter(label);

		for (Iterator<Integer> it = lifetime.stream().iterator(); it.hasNext();) {
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
		return TiNLa_C.get(r).getTiNLa(label);
	}

	/**
	 * Update TiNLa(r) set for label the time instant t
	 * 
	 * @param r
	 * @param label
	 * @param t
	 */
	public void updateTiNLa(int r, int label, int t) {
		BitSet lifetime;

		if ((lifetime = TiNLa.get(r).get(label)) == null) {
			lifetime = new BitSet(Config.MAXIMUM_INTERVAL);
			TiNLa.get(r).put(label, lifetime);
		}

		lifetime.set(t);
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

	/**
	 * Update TiNLa_C(r) set for label the time instant t
	 * 
	 * @param r
	 * @param label
	 * @param t
	 */
	public void updateTiNLa_C(int r, int label, int t) {
		TiNLa_C.get(r).update(label, t);
	}
}