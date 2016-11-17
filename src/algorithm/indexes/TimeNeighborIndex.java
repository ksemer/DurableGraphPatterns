package algorithm.indexes;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import system.Config;

/**
 * TiNLa && CTiNLa indexes
 * 
 * @author ksemer
 *
 */
public class TimeNeighborIndex {
	// label -> lifespan
	private Map<Integer, BitSet> TiNLa;

	// label -> [t--> c]
	private Map<Integer, Map<Integer, Integer>> CTiNLa;

	/**
	 * Constructor
	 */
	public TimeNeighborIndex() {
		TiNLa = new HashMap<>();
		CTiNLa = new HashMap<>();
	}

	/**
	 * Return for given label l the info from TiNLa index
	 * 
	 * @param label
	 * @return
	 */
	public BitSet getTiNLa(int label) {
		return TiNLa.get(label);
	}

	/**
	 * Return for givel label the info from CTiNLa index
	 * 
	 * @param label
	 * @return
	 */
	public Map<Integer, Integer> getCounter(int label) {
		return CTiNLa.get(label);
	}

	/**
	 * Update TiNLa && CTiNLa indexes
	 * 
	 * @param label
	 * @param t
	 */
	public void update(int label, int t) {
		Integer tmpC;
		BitSet lifespan;
		Map<Integer, Integer> tmpCounter;

		if (Config.TINLA_ENABLED) {
			if ((lifespan = TiNLa.get(label)) == null) {
				lifespan = new BitSet(Config.MAXIMUM_INTERVAL);
				TiNLa.put(label, lifespan);
			}

			lifespan.set(t);
		} else if (Config.CTINLA_ENABLED) {
			if ((tmpCounter = CTiNLa.get(label)) == null) {
				tmpCounter = new HashMap<>();
				tmpCounter.put(t, 1);
				CTiNLa.put(label, tmpCounter);
			}

			if ((tmpC = tmpCounter.get(t)) == null) {
				tmpCounter.put(t, 1);
			} else {
				tmpCounter.put(t, tmpC.intValue() + 1);
			}
		}
	}
}