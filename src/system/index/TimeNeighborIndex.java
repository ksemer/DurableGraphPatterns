package system.index;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import system.Config;

public class TimeNeighborIndex {
	// label -> lifetime
	private Map<Integer, BitSet> TiNLa;
	// label -> [t--> c]
	private Map<Integer, Map<Integer, Integer>> counter;
	
	public TimeNeighborIndex() {
		TiNLa = new HashMap<>();
		counter = new HashMap<>();
	}
	
	public BitSet getTiNLa(int l) {
		return TiNLa.get(l);
	}
	
	public Map<Integer, Integer> getCounter(int label) {
		return counter.get(label);
	}

	public void update(int label, int t) {
		BitSet lifetime;
		Map<Integer, Integer> tmpCounter;
		Integer tmpC;

		if ((lifetime = TiNLa.get(label)) == null) {
			lifetime = new BitSet(Config.MAXIMUM_INTERVAL);
			TiNLa.put(label, lifetime);
		}
		
		lifetime.set(t);
		
		if ((tmpCounter = counter.get(label)) == null) {
			tmpCounter = new HashMap<>();
			tmpCounter.put(t, 1);
			counter.put(label, tmpCounter);
		}
		
		if ((tmpC = tmpCounter.get(t)) == null) {
			tmpCounter.put(t, 1);
		} else {
			tmpCounter.put(t, tmpC.intValue() + 1);
		}
		
	}
}