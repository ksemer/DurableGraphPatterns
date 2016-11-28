package algorithm;

import graph.version.Node;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to keep match information
 * 
 * @author ksemer
 */
public class Match {
	// =====================================
	// keeps duration of match
	private int duration;

	// lifespan
	private BitSet lifespan;

	// match pattern id --> node object
	private Map<Integer, Set<Node>> match;
	// =====================================

	/**
	 * Constructor
	 * 
	 * @param duration
	 * @param lifespan
	 * @param match
	 */
	public Match(int duration, BitSet lifespan, Map<Integer, Set<Node>> match) {
		this.duration = duration;
		this.lifespan = lifespan;
		this.match = match;
	}

	/**
	 * Return match's duration
	 * 
	 * @return
	 */
	public int getDuration() {
		return duration;
	}

	/**
	 * Return match lifespan
	 * 
	 * @return
	 */
	public BitSet getLifespan() {
		return lifespan;
	}

	/**
	 * Return match as a graph in a map structure
	 * 
	 * @return
	 */
	public Map<Integer, Set<Node>> getMatch() {
		return match;
	}
}