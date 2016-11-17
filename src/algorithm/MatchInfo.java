package algorithm;

import graph.version.Node;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to keep match information
 * @author ksemer
 */
public class MatchInfo {
	//=====================================
	// keeps duration of match
	private int duration;
	
	// lifespan
	private BitSet lifespan;
	
	// match pattern id --> node object
	private Map<Integer, Set<Node>> match;
	//=====================================

	/**
	 * Constructor
	 * @param duration
	 * @param lifespan
	 * @param match
	 */
	public MatchInfo(int duration, BitSet lifespan, Map<Integer, Set<Node>> match) {
		this.duration = duration;
		this.lifespan = lifespan;

		this.match = match;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public BitSet getLifespan() {
		return lifespan;
	}
	
	public Map<Integer, Set<Node>> getMatch() {
		return match;
	}
}