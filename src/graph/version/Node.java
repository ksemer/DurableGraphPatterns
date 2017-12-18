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
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import orestes.bloomfilter.FilterBuilder;
import orestes.bloomfilter.memory.CountingBloomFilterMemory;
import system.Config;

/**
 * This class represents the Node objects
 * 
 * @author ksemer
 */
public class Node implements Serializable {

	// =================================================================

	private static final long serialVersionUID = 1L;

	// node id
	private int id;

	// label -> [lifespan]
	private Map<Integer, BitSet> labels;

	// trg -> [Edge]
	private Map<Node, Edge> adjacencies;

	// r -> label -> [lifespan]
	private List<Map<Integer, BitSet>> TiNLa;

	// r -> [bloom -> (t l]
	private List<BloomFilter<String>> TiNLaBloom;

	// r -> label -> [t --> c]
	private List<Map<Integer, Map<Integer, Integer>>> CTiNLa;

	// r -> [bloom -> t l c]
	private List<CountingBloomFilterMemory<String>> CTiNLaBloom;

	// bloom -> t p
	private BloomFilter<String> TiPLaBloom;

	// auxiliary structure for TiPLaBloom
	private Map<Integer, Set<String>> TiPLaBloomAux;

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
	 * Add new edge or update one with lifespan ranging from start to end - 1
	 * 
	 * @param node
	 * @param start
	 * @param end
	 */
	public void addEdge(Node node, int start, int end) {
		Edge e = adjacencies.get(node);

		if (e == null) {
			e = new Edge(node, new BitSet(Config.MAXIMUM_INTERVAL));
			adjacencies.put(node, e);
		}

		e.updateLifetime(start, end);
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
	 * Update label lifespan ranging from start to end - 1
	 * 
	 * @param label
	 * @param start
	 * @param end
	 */
	public void updateLabelLifespan(int label, int start, int end) {
		BitSet lifespan;

		if ((lifespan = labels.get(label)) == null) {
			lifespan = new BitSet(Config.MAXIMUM_INTERVAL);
			labels.put(label, lifespan);
		}

		lifespan.set(start, end + 1);

		if (Config.ENABLE_STAR_LABEL_PATTERNS) {
			labels.get(Config.STAR_LABEL).set(start, end);
		}
	}

	/**
	 * Initialize TiNLa index for radius r
	 * 
	 * @param r
	 */
	public void initializeTiNLa(int r) {

		if (TiNLa == null)
			TiNLa = new ArrayList<>();

		TiNLa.add(r, new HashMap<Integer, BitSet>());
	}

	/**
	 * Initialize CTiNLa index for radius r
	 * 
	 * @param r
	 */
	public void initializeCTiNLa(int r) {

		if (CTiNLa == null)
			CTiNLa = new ArrayList<>();

		CTiNLa.add(r, new HashMap<>());
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
	 * Create TiNLaBloom in radius r
	 * 
	 * @param r
	 */
	public void createTiNLaBloom(int r) {

		BitSet lifespan;
		int times = 0;

		if (TiNLaBloom == null)
			TiNLaBloom = new ArrayList<>();

		// no neighbors exist, thus do not initialize a bloom
		if (TiNLa.get(r).isEmpty())
			return;

		for (Entry<Integer, BitSet> entry : TiNLa.get(r).entrySet()) {
			lifespan = entry.getValue();

			for (int t = lifespan.nextSetBit(0); t != -1; t = lifespan.nextSetBit(t + 1)) {
				times++;
			}
		}

		TiNLaBloom.add(BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), times, 0.01));

		int label;
		BloomFilter<String> bloom = TiNLaBloom.get(r);

		for (Entry<Integer, BitSet> entry : TiNLa.get(r).entrySet()) {

			label = entry.getKey();
			lifespan = entry.getValue();

			// for each active bit add t l
			for (int t = lifespan.nextSetBit(0); t != -1; t = lifespan.nextSetBit(t + 1))
				bloom.put(t + " " + label);
		}
	}

	/**
	 * Create CTiNLaBloom in radius r
	 * 
	 * @param r
	 */
	public void createCTiNLaBloom(int r) {

		int times = 0, max = 0;

		if (CTiNLaBloom == null)
			CTiNLaBloom = new ArrayList<>();

		// no neighbors exist, thus do not initialize a bloom
		if (CTiNLa.get(r).isEmpty())
			return;

		for (Entry<Integer, Map<Integer, Integer>> entry : CTiNLa.get(r).entrySet()) {

			times += entry.getValue().size();

			for (Entry<Integer, Integer> entry1 : entry.getValue().entrySet()) {

				if (max < entry1.getValue())
					max = entry1.getValue();
			}
		}

		int bloom_bits;

		if (max == 1)
			bloom_bits = 1;
		else
			bloom_bits = (int) Math.ceil(Math.log(max) / Math.log(2));

		CTiNLaBloom.add(new CountingBloomFilterMemory<String>(new FilterBuilder(times, 0.01).countingBits(bloom_bits)));

		int label, t;
		CountingBloomFilterMemory<String> bloom = CTiNLaBloom.get(r);

		for (Entry<Integer, Map<Integer, Integer>> entry : CTiNLa.get(r).entrySet()) {

			label = entry.getKey();

			for (Entry<Integer, Integer> entry1 : entry.getValue().entrySet()) {

				t = entry1.getKey();
				bloom.add(t + " " + label, entry1.getValue());
			}
		}
	}

	/**
	 * Create TiPLaBloom
	 */
	public void createTiPLaBloom() {

		int totalEntries = 0;

		// no paths exists, thus do not initialize a bloom
		if (TiPLaBloomAux.isEmpty()) {
			TiPLaBloomAux = null;
			return;
		}

		for (Entry<Integer, Set<String>> entry : TiPLaBloomAux.entrySet())
			totalEntries += entry.getValue().size();

		TiPLaBloom = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), totalEntries, 0.01);

		int t;

		for (Entry<Integer, Set<String>> entry : TiPLaBloomAux.entrySet()) {

			t = entry.getKey();

			for (String p : entry.getValue())
				TiPLaBloom.put(t + " " + p);
		}

		TiPLaBloomAux = null;
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
	 * Return TiNLaBloom index
	 * 
	 * @return
	 */
	public List<BloomFilter<String>> getTiNLaBloom() {
		return TiNLaBloom;
	}

	/**
	 * Return CTiNLaBloom index
	 * 
	 * @return
	 */
	public List<CountingBloomFilterMemory<String>> getCTiNLaBloom() {
		return CTiNLaBloom;
	}

	/**
	 * Returns the time instances where there are at least c neighborhoods with the
	 * given label
	 * 
	 * @param r
	 * @param label
	 * @param c
	 * @param lifespan
	 * @return
	 */
	public BitSet getCTiNLa(int r, int label, int c, BitSet lifespan) {

		Map<Integer, Integer> cT;

		if ((cT = CTiNLa.get(r).get(label)) == null)
			return null;

		BitSet life = (BitSet) lifespan.clone();

		for (int t = lifespan.nextSetBit(0); t != -1; t = lifespan.nextSetBit(t + 1)) {

			// if node doesn't contain at least c neighbors then disable the time instant
			if (cT.get(t) == null || cT.get(t) < c)
				life.set(t, false);
		}

		return life;
	}

	/**
	 * Returns the time instances where a neighbor with label l exists
	 * 
	 * @param r
	 * @param label
	 * @param lifespan
	 * @return
	 */
	public BitSet getTiNLaBloom(int r, int label, BitSet lifespan) {

		if (TiNLaBloom.size() - 1 < r)
			return null;

		BitSet life = (BitSet) lifespan.clone();
		BloomFilter<String> bloom = TiNLaBloom.get(r);

		for (int t = lifespan.nextSetBit(0); t != -1; t = lifespan.nextSetBit(t + 1)) {

			if (!bloom.mightContain(t + " " + label))
				life.set(t, false);
		}

		return life;
	}

	/**
	 * Returns the time instances where there are at least c neighborhoods with the
	 * given label
	 * 
	 * @param r
	 * @param label
	 * @param c
	 * @param lifespan
	 * @return
	 */
	public BitSet getCTiNLaBloom(int r, int label, int c, BitSet lifespan) {

		if (CTiNLaBloom.size() - 1 < r)
			return null;

		CountingBloomFilterMemory<String> cT = CTiNLaBloom.get(r);
		BitSet life = (BitSet) lifespan.clone();

		for (int t = lifespan.nextSetBit(0); t != -1; t = lifespan.nextSetBit(t + 1)) {

			// if node doesn't contain at least c neighbors then disable the time instant
			if (cT.getEstimatedCount(t + " " + label) < c)
				life.set(t, false);
		}

		return life;
	}

	/**
	 * Returns the time instances where the given label path exist
	 * 
	 * @param labelPath
	 * @param lifespan
	 * @return
	 */
	public BitSet TiPLaBloomContains(String labelPath, BitSet lifespan) {

		if (TiPLaBloom == null)
			return null;

		BitSet life = (BitSet) lifespan.clone();

		for (int t = lifespan.nextSetBit(0); t != -1; t = lifespan.nextSetBit(t + 1)) {

			// if node does not contain the given label path then disable the time instant
			if (!TiPLaBloom.mightContain(t + " " + labelPath))
				life.set(t, false);
		}

		return life;
	}

	/**
	 * Return TiPLaBloomAux
	 *
	 * @return
	 */
	public Map<Integer, Set<String>> getTiPLaAux() {
		return TiPLaBloomAux;
	}

	/**
	 * Remove TiNLa for radius r
	 * 
	 * @param r
	 */
	public void clearTiNLa(int r) {
		TiNLa.get(r).clear();
	}

	/**
	 * Remove CTiNLa for radius r
	 * 
	 * @param r
	 */
	public void clearCTiNLa(int r) {
		CTiNLa.get(r).clear();
	}

	/**
	 * Remove TiNLa
	 */
	public void clearTiNLa() {
		TiNLa = null;
	}

	/**
	 * Remove CTiNLa
	 */
	public void clearCTiNLa() {
		CTiNLa = null;
	}

	/**
	 * Initialize auxiliary TiPLa bloom structure
	 */
	public void initiliazeTiPLaBloom() {
		TiPLaBloomAux = new HashMap<>();
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
}