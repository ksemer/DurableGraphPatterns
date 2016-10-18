package algorithm;

import graph.pattern.PatternGraph;
import graph.pattern.PatternNode;
import graph.version.Node;
import graph.version.LVGraph;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import system.index.PatternPathIndex;

/**
 * DurableMatching using TiPLa
 * @author ksemer
 */
public class DurableMatchingPath extends DurableMatching {

	public DurableMatchingPath(LVGraph lvg, PatternGraph pg, BitSet iQ, boolean continuously,
			Map<Integer, Map<String, Set<Node>>> timePathIndex) throws Exception {
		super(lvg, pg, iQ, timePathIndex, continuously, false);
	}

	@Override
	public void filterCandidates(LVGraph lvg, PatternGraph pg, BitSet iQ) {
		
		// create candidates set pattern node--> set of lvg nodes
		Map<Integer, Set<Node>> candidates = new HashMap<Integer, Set<Node>>(pg.size());
		
		// support variables
		Set<Node> currentCandidates = null;
		nodeScore sc;
		
		// ranking for each pattern node
		Map<PatternNode, Map<Integer, nodeScore>> score = new HashMap<>();
				
		// create pattern path index
		Map<Integer, Set<String>> patternPathIndex = new PatternPathIndex(2).createPathIndex(pg);
		
		// initialize
		for (PatternNode pn : pg.getNodes()) {
			candidates.put(pn.getID(), new HashSet<Node>());		
			Rank.put(pn.getID(), new TreeMap<>());
			// required for directed graphs
			pn.createLabelAdjacency();
		}
		
		// for each pattern node
		for (PatternNode pn : pg.getNodes()) {
			// initiate score structure
			score.put(pn, new HashMap<>());
			
			// for each iQ true bit
			for (Iterator<Integer> it = iQ.stream().iterator(); it.hasNext();) {
				int t = it.next();
				Set<Node> intersection = null;

				// for all pattern node pn paths
				for (String path : patternPathIndex.get(pn.getID())) {
					
					// get the candidates from the time path index
					if ((currentCandidates = TiPLa.get(t).get(path)) != null) {
						
						if (intersection == null) {
							intersection = new HashSet<>();
							intersection.addAll(currentCandidates);
						} else
							intersection.retainAll(currentCandidates);
					} else
						break;

					if (intersection.isEmpty())
						break;
				}
				
				if (intersection != null) {
					for (Node n : intersection) {
												
						if ((sc = score.get(pn).get(n.getID())) == null) {
							sc = new nodeScore();
							score.get(pn).put(n.getID(), sc);
						} else
							sc.score++;
					}
				}
			}		
		}

		// support variables
		int durScore;
		PatternNode pn;
		TreeMap<Integer, Set<Node>> patternNodeRank;
		
		for (Entry<PatternNode, Map<Integer, nodeScore>> entry : score.entrySet()) {
		    pn = entry.getKey();
		    patternNodeRank = Rank.get(pn.getID());
		    
			// for each candidate node
			for (Entry<Integer, nodeScore> entry1 : entry.getValue().entrySet()) {
				durScore = entry1.getValue().score;
				
				// a node must have duration > 1
				if (durScore == 1)
					continue;
				
				if ((currentCandidates = patternNodeRank.get(durScore)) == null) {
					currentCandidates = new HashSet<>();
					patternNodeRank.put(durScore, currentCandidates);
				}

				currentCandidates.add(lvg.getNode(entry1.getKey()));
			}
		}
	}
	
	/**
	 * Support Class
	 * @author ksemer
	 */
	class nodeScore {
		public int score;	
		public nodeScore() { score = 1; }
	}
}