package algorithm;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import algorithm.indexes.PatternPathIndex;
import graph.pattern.PatternGraph;
import graph.pattern.PatternNode;
import graph.version.Edge;
import graph.version.Graph;
import graph.version.Node;
import graph.version.loader.LoaderDBLP;
import system.Config;
import system.Main;

/**
 * DurableMatching Algorithm class
 * 
 * @author ksemer
 */
public class DurableMatching {
	// ===============================================================

	// pattern graph
	private PatternGraph pg;

	// query interval
	private BitSet iQ;

	// Ranking structure
	private Map<Integer, TreeMap<Integer, Set<Node>>> Rank;

	// threshold for time duration
	private int threshold = Integer.MAX_VALUE;

	// if the query is continuously
	private boolean continuously;

	// stores the matches
	private Set<MatchInfo> topMatches;

	// keeps the max duration for the current matches
	private int maxDuration;

	// total recursions
	private int totalRecursions = 0;

	// size of rank
	private int sizeOfRank = 0;

	// recursions per run
	private int tempRec;

	// total time of algorithm
	private long totalTime;

	private int rankingStrategy;

	// ===============================================================
	long start;

	/**
	 * Constructor
	 * 
	 * @param lvg
	 * @param pg
	 * @param iQ
	 * @param continuously
	 * @throws Exception
	 */
	public DurableMatching(Graph lvg, PatternGraph pg, BitSet iQ, boolean continuously, int rankingStrategy)
			throws Exception {

		this.pg = pg;
		this.iQ = iQ;
		this.continuously = continuously;
		this.rankingStrategy = rankingStrategy;

		// initial C
		Map<Integer, Set<Node>> initC;
		start = System.currentTimeMillis();

		System.out.println("DurableMatching is Running...");

		// to start counting the time
		Main.TIME = System.currentTimeMillis();

		topMatches = new HashSet<MatchInfo>();
		Rank = new HashMap<>();

		if (Config.TIPLA_ENABLED)
			filterCandidatesByPath(lvg, pg, iQ);
		else
			filterCandidates(lvg, pg, iQ);

		TreeMap<Integer, Set<Node>> ranking;
		NavigableMap<Integer, Set<Node>> submap;
		int sc, pn_id;

		for (PatternNode p : pg.getNodes()) {
			ranking = Rank.get(p.getID());

			sc = ranking.lastKey();

			if (threshold > sc)
				threshold = sc;
		}

		TreeMap<Integer, Set<Node>> tree;

		// default ranking is running
		if (rankingStrategy == Config.ZERO_RANKING)
			threshold = 2;

		while (threshold > 1) {

			initC = new HashMap<>();

			for (Entry<Integer, TreeMap<Integer, Set<Node>>> entry : Rank.entrySet()) {
				pn_id = entry.getKey();
				tree = entry.getValue();
				initC.put(pn_id, new HashSet<>());

				submap = tree.subMap(tree.ceilingKey(threshold), true, tree.lastKey(), true);

				for (Entry<Integer, Set<Node>> entry1 : submap.entrySet())
					initC.get(pn_id).addAll(entry1.getValue());
			}

			System.out.print("Algoterm: " + threshold + "\tCan_size: " + initC.get(0).size());

			sizeOfRank++;

			initC = DUALSIM(initC);

			try {
				tempRec = 0;
				searchPattern(initC, 0);
				System.out.print("\tTermRec: " + tempRec + "\n");
			} catch (Exception e) {
				System.out.println("\nHAVE A LOOK: " + e.getMessage());
			}

			// matches found
			if (topMatches.size() != 0)
				break;

			// get new threshold
			if (rankingStrategy == Config.HALFWAY_RANKING)
				threshold = getBinaryBasedThreshold();
			else if (rankingStrategy == Config.MAX_RANKING)
				threshold = getMinMaxBasedThreshold();
		}

		System.out.println("sizeOfRank: " + sizeOfRank);
		System.out.println("Total Recursions: " + totalRecursions);

		// print matches
		printTopMatches();
	}

	/**
	 * Compute the next threshold based on binary ranking
	 * 
	 * @return
	 */
	private int getBinaryBasedThreshold() {
		int sc, oldT = threshold;
		TreeMap<Integer, Set<Node>> ranking;

		threshold /= 2 + 1;

		if (oldT == threshold)
			return 1;

		for (PatternNode p : pg.getNodes()) {
			ranking = Rank.get(p.getID());

			if (ranking.floorKey(threshold) == null)
				continue;

			sc = ranking.floorKey(threshold);

			if (threshold > sc)
				threshold = sc;
		}

		if (threshold == 1 && oldT != 2)
			return 2;

		return threshold;
	}

	/**
	 * Get next threshold based on minMax ranking
	 * 
	 * @return
	 */
	private int getMinMaxBasedThreshold() {
		int oldT = threshold, sc;
		TreeMap<Integer, Set<Node>> ranking;

		for (PatternNode p : pg.getNodes()) {
			ranking = Rank.get(p.getID());

			if (ranking.floorKey(threshold) == null)
				continue;

			sc = ranking.floorKey(threshold);

			if (threshold > sc)
				threshold = sc;
		}

		if (oldT == threshold)
			threshold--;

		return threshold;
	}

	/**
	 * Dual-based isomorphism algorithm
	 * 
	 * @param c
	 * @param depth
	 * @throws Exception
	 */
	private void searchPattern(Map<Integer, Set<Node>> c, int depth) throws Exception {
		// increase the counters for recursions
		totalRecursions++;
		tempRec++;

		if (System.currentTimeMillis() > (start + 3600000))
			throw new Exception("Reach time limit");
		// max matches limit can be used only in the below strategies
		// zero strategy may find more matches than the limit that are not
		// the best solution at the current step
		else if (topMatches.size() == Config.MAX_MATCHES
				&& (rankingStrategy == Config.MAX_RANKING || rankingStrategy == Config.HALFWAY_RANKING))
			throw new Exception("Reach maxMatches");
		else if (depth == pg.size() && c.size() != 0) {
			computeMatchesTime(c);
		} else if (!c.isEmpty()) {

			for (Node u : c.get(depth)) {

				if (!contains(c, u, depth)) {
					Map<Integer, Set<Node>> cCopy = new HashMap<Integer, Set<Node>>(c.size());

					// copy
					for (Entry<Integer, Set<Node>> entry : c.entrySet())
						cCopy.put(entry.getKey(), new HashSet<>(entry.getValue()));

					// set cCopy(depth) = u
					cCopy.get(depth).clear();
					cCopy.get(depth).add(u);

					searchPattern(refine(cCopy), depth + 1);
				}
			}
		}
	}

	/**
	 * Compute for each match the minimum time
	 * 
	 * @param match
	 */
	private void computeMatchesTime(Map<Integer, Set<Node>> match) {
		BitSet inter = (BitSet) iQ.clone();
		Edge e;

		if (Config.LABEL_CHANGE) {
			// check labels intersection
			for (Entry<Integer, Set<Node>> entry : match.entrySet()) {
				for (Node n : entry.getValue()) {
					// intersect labels lifespan
					inter.and(n.getLabel(pg.getNode(entry.getKey()).getLabel()));

					if (continuously) {
						BitSet shifted = (BitSet) inter.clone();
						int count = 0;

						while (!shifted.isEmpty()) {
							shifted.and(shifted.get(1, shifted.length()));
							count++;
						}

						if (count < threshold)
							return;

					} else
					// there intersection is less than algoTerm or topScore
					if (inter.cardinality() < threshold)
						return;
				}
			}
		}

		// check the edges
		for (PatternNode pn : pg.getNodes()) {

			// get adjacency of pn
			for (PatternNode child : pn.getAdjacency()) {

				// get the nodes that have same label as pn
				for (Node n : match.get(pn.getID())) {

					for (Node c : match.get(child.getID())) {

						if ((e = n.getEdge(c)) != null) {
							inter.and(e.getLifetime());

							if (continuously) {
								BitSet shifted = (BitSet) inter.clone();
								int count = 0;

								while (!shifted.isEmpty()) {
									shifted.and(shifted.get(1, shifted.length()));
									count++;
								}

								if (count < threshold)
									return;

							} else // check if the cardinality is less than
									// topScore or algoTerm
							if (inter.cardinality() < threshold)
								return;
						}
					}
				}
			}
		}

		// duration of match
		int duration = inter.cardinality();

		// if duration equals to max duration
		if (duration == maxDuration)
			topMatches.add(new MatchInfo(duration, inter, match));
		else if (duration > maxDuration) {

			// update the max duration
			maxDuration = duration;

			// update threshold
			threshold = maxDuration;

			// clean the old matches
			topMatches.clear();

			// add match
			topMatches.add(new MatchInfo(duration, inter, match));
		}
	}

	/**
	 * Dual Simulation Algorithm
	 * 
	 * @param c
	 * @return
	 */
	private Map<Integer, Set<Node>> DUALSIM(Map<Integer, Set<Node>> c) {
		// variables
		boolean changed = true;
		Node phiNode;
		List<Node> phiTemp;
		Set<Node> newC;
		Set<Node> phiqNode;

		while (changed) {
			changed = false;

			// for each node of pattern graph
			for (PatternNode qNode : pg.getNodes()) {
				phiqNode = c.get(qNode.getID());

				// for each node of pattern graph get the adjacency
				for (PatternNode qChild : qNode.getAdjacency()) {

					// newPhi corresponds to phi(qChild). This update
					// will ensure that phi(qChild) will contain only
					// nodes which have a parent in phi(qNode)
					newC = new HashSet<Node>();

					// for all phi(qNode)
					for (Iterator<Node> i = phiqNode.iterator(); i.hasNext();) {
						// phiTemp corresponds to the children of
						// phiNode which are contained in phi(qChild).
						// This checks both if phiNode has children in
						// phi(qChild) (of which it must have at least one)
						// and also builds newPhi to contain only those
						// nodes in phi(qChild) which also have a parent
						// in phi(qNode)
						phiNode = i.next();

						phiTemp = timeJoin(phiNode, qNode, qChild, c);

						if (phiTemp.isEmpty()) {
							// remove phiNode from phi(qNode)
							i.remove();

							// if phi(u) is empty then return an empty set
							if (phiqNode.isEmpty())
								return Collections.emptyMap();

							changed = true;
						} else
							// F'(u') = F'(u') UNION F_{v}(u')
							newC.addAll(phiTemp);
					}

					// if any phi(i) is empty, then there is no
					// isomorphic subgraph.
					if (newC.isEmpty())
						return Collections.emptyMap();

					// if F'(i') is smaller than F(u')
					if (newC.size() < c.get(qChild.getID()).size())
						changed = true;

					// every node in phi(qChild) must have at least one parent
					// in phi(qNode)
					// newPhi.retainAll(phi.get(qChild.getID()));
					c.put(qChild.getID(), newC);
				}
			}
		}
		return c;
	}

	/**
	 * Refinement procedure
	 * 
	 * @param c
	 * @return
	 */
	public Map<Integer, Set<Node>> refine(Map<Integer, Set<Node>> c) {
		Node phiNode;
		List<Node> phiTemp;
		Set<Node> c_;

		for (PatternNode qNode : pg.getNodes()) {

			for (PatternNode qChild : qNode.getAdjacency()) {
				c_ = new HashSet<Node>();

				for (Iterator<Node> j = c.get(qNode.getID()).iterator(); j.hasNext();) {
					phiNode = j.next();
					phiTemp = timeJoin(phiNode, qNode, qChild, c);

					if (phiTemp.isEmpty())
						j.remove();
					else
						c_.addAll(phiTemp);
				}

				if (c_.isEmpty())
					return Collections.emptyMap();

				// newPhi.retainAll(phi.get(qChild.getID()));
				c.put(qChild.getID(), c_);
			}
		}
		return c;
	}

	/**
	 * Intersection between Nodes that are live during the interval iQ
	 * 
	 * @param n
	 * @param p
	 * @param chil
	 * @return
	 */
	public List<Node> timeJoin(Node n, PatternNode p, PatternNode chil, Map<Integer, Set<Node>> phi) {
		List<Node> intersection = new ArrayList<Node>();
		BitSet inter, labelLife = (BitSet) iQ.clone();

		if (Config.LABEL_CHANGE) {
			labelLife.and(n.getLabel(p.getLabel()));

			if (continuously) {
				BitSet shifted = (BitSet) labelLife.clone();
				int count = 0;

				while (!shifted.isEmpty()) {
					shifted.and(shifted.get(1, shifted.length()));
					count++;
				}

				if (count < threshold)
					return intersection;

			} else if (labelLife.cardinality() < threshold)
				return intersection;
		}

		if (n.getAdjacency().size() < phi.get(chil.getID()).size()) {
			for (Edge e : n.getAdjacency()) {
				if (phi.get(chil.getID()).contains(e.getTarget())) {

					inter = (BitSet) labelLife.clone();

					// intersection between edge lifespan and interval I
					inter.and(e.getLifetime());

					if (Config.LABEL_CHANGE)
						inter.and(e.getTarget().getLabel(chil.getLabel()));

					if (continuously) {
						BitSet shifted = (BitSet) inter.clone();
						int count = 0;

						while (!shifted.isEmpty()) {
							shifted.and(shifted.get(1, shifted.length()));
							count++;
						}

						if (count < threshold)
							continue;

					} else
					// check if target is pruned or it is not alive during
					// interval
					if (inter.cardinality() < threshold)
						continue;

					intersection.add(e.getTarget());
				}
			}
		} else {
			Edge e;

			for (Node ngb : phi.get(chil.getID())) {
				// if n has neighbor ngb
				if ((e = n.getEdge(ngb)) != null) {

					inter = (BitSet) labelLife.clone();

					// intersection between edge lifespan and interval I
					inter.and(e.getLifetime());

					if (Config.LABEL_CHANGE)
						inter.and(ngb.getLabel(chil.getLabel()));

					if (continuously) {
						BitSet shifted = (BitSet) inter.clone();
						int count = 0;

						while (!shifted.isEmpty()) {
							shifted.and(shifted.get(1, shifted.length()));
							count++;
						}

						if (count < threshold)
							continue;

					} else // check if target is pruned or it is not alive
							// during
							// interval
					if (inter.cardinality() < threshold)
						continue;

					intersection.add(ngb);
				}
			}
		}

		return intersection;
	}

	/**
	 * Check if node u is contained in at least one c
	 *
	 * @param c
	 * @param u
	 * @param depth
	 * @return
	 */
	private boolean contains(Map<Integer, Set<Node>> phi, Node u, int depth) {
		for (int i = 0; i < depth; i++)
			if (phi.get(i).contains(u))
				return true;

		return false;
	}

	/**
	 * Generates candidates per pattern node using TiLa or TiNLa or CTiNLa
	 * 
	 * @param lvg
	 * @param pg
	 * @param iQ
	 */
	private void filterCandidates(Graph lvg, PatternGraph pg, BitSet iQ) {
		Map<Integer, Set<Node>> phi = new HashMap<Integer, Set<Node>>(pg.size());

		// initialize
		for (PatternNode pn : pg.getNodes()) {
			phi.put(pn.getID(), new HashSet<Node>());
			Rank.put(pn.getID(), new TreeMap<>());
		}

		pg.createLabelAdjacency();

		boolean found;
		BitSet lifespan;
		int label, sc;
		Set<Node> pnode_candidates, current_candidates;
		Map<Integer, Set<Node>> rankingBasedOnlifespanScore;
		Node n;

		for (PatternNode pn : pg.getNodes()) {
			pnode_candidates = phi.get(pn.getID());

			// get pattern's node label
			label = pn.getLabel();

			for (Iterator<Integer> it = iQ.stream().iterator(); it.hasNext();)
				pnode_candidates.addAll(lvg.getTiLaNodes(it.next(), label));

			rankingBasedOnlifespanScore = Rank.get(pn.getID());

			for (Iterator<Node> it = pnode_candidates.iterator(); it.hasNext();) {
				n = it.next();

				lifespan = (BitSet) iQ.clone();
				lifespan.and(n.getLabel(label));

				// if TiNLa is enabled
				if (Config.TINLA_ENABLED) {
					found = true;

					// for each r
					for (int r = 0; r < Config.TINLA_R; r++) {
						for (int l : pn.getLabelAdjacency(r)) {
							// if there is not a neighbor with that label
							// remove it
							if (n.getTiNLa(r, l) == null) {
								found = false;
								it.remove();
								break;
							} else { // otherwise get the lifespan and intersect
								lifespan.and(n.getTiNLa(r, l));

								// check if the neighborhood lifespan
								// intersection
								// is not empty
								if (lifespan.isEmpty()) {
									found = false;
									it.remove();
									break;
								}
							}
						}
						if (!found)
							break;
					}

					if (!found)
						continue;
				} else if (Config.CTINLA_ENABLED) {
					found = true;

					// for each r
					for (int r = 0; r < Config.TINLA_R; r++) {

						for (Entry<Integer, Integer> l : pn.getLabelAdjacency_C(r).entrySet()) {

							// if there is not a neighbor with that label
							// remove it
							if (n.getTiNLa_C(r, l.getKey()) == null) {
								found = false;
								it.remove();
								break;
							} else { // otherwise get the lifespan and intersect
								lifespan.and(n.getTiNLa_C(r, l.getKey()));

								// check if the neighborhood lifespan
								// intersection
								// is not empty
								if (lifespan.isEmpty()) {
									found = false;
									it.remove();
									break;
								} else {
									lifespan.and(n.getTiNLa_C(r, l.getKey(), l.getValue(), lifespan));
									if (lifespan.isEmpty()) {
										found = false;
										it.remove();
										break;
									}
								}
							}
						}
						if (!found)
							break;
					}

					if (!found)
						continue;
				} else if (lifespan.isEmpty()) { // check if Node n lifespan
													// does not contain any bit
					it.remove();
					continue;
				}

				// remove node from candidates since with score 1 it is not durable
				if ((sc = lifespan.cardinality()) == 1)
					it.remove();
				else {
					if ((current_candidates = rankingBasedOnlifespanScore.get(sc)) == null) {
						current_candidates = new HashSet<>();
						rankingBasedOnlifespanScore.put(sc, current_candidates);
					}
					// add candidate node
					current_candidates.add(n);
				}
			}
		}
	}

	/**
	 * Generates candidates per pattern node using TiPLa
	 * 
	 * @param lvg
	 * @param pg
	 * @param iQ
	 */
	private void filterCandidatesByPath(Graph lvg, PatternGraph pg, BitSet iQ) {

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
					if ((currentCandidates = lvg.getTiPLa().get(t).get(path)) != null) {

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
	 * Print topK Matches
	 * 
	 * @throws IOException
	 */
	private void printTopMatches() throws IOException {
		totalTime = (System.currentTimeMillis() - Main.TIME);

		// stores the result
		String result = "";

		for (MatchInfo mI : topMatches) {

			result += "------ Match ------\n";

			if (continuously) {
				BitSet shifted = (BitSet) mI.getLifespan().clone();
				int count = 0;

				while (!shifted.isEmpty()) {
					shifted.and(shifted.get(1, shifted.length()));
					count++;
				}
				result += "Duration : " + count + "\n";
			} else
				result += "Duration : " + mI.getLifespan().cardinality() + "\n";
			result += "Lifetime : " + mI.getLifespan() + "\n";
			result += "------ Nodes ------\n";

			for (Entry<Integer, Set<Node>> mg : mI.getMatch().entrySet()) {
				// pattern node id
				result += "pg_id: " + mg.getKey() + "\n";

				for (Node n : mg.getValue())
					// graph node id
					result += "g_id: " + n.getID() + "\n";
			}

			// print the edges
			for (PatternNode pn : pg.getNodes())
				for (PatternNode child : pn.getAdjacency())
					for (Node n : mI.getMatch().get(pn.getID()))
						for (Edge e : n.getAdjacency())
							if (mI.getMatch().get(child.getID()).contains(e.getTarget())) {

								if (Config.PATH_DATASET.contains("dblp"))
									result += LoaderDBLP.getAuthors().get(n.getID()) + ": ";

								result += "(" + pn.getID() + ") ---> (" + child.getID() + ")";

								if (Config.PATH_DATASET.contains("dblp"))
									result += " " + LoaderDBLP.getAuthors().get(e.getTarget().getID());

								result += "\n";
							}

			result += "-------------------\n";
		}

		String outputPath = Config.PATH_OUTPUT + "pq=" + pg.getID();

		if (continuously)
			outputPath += "cont_";

		if (Config.TINLA_ENABLED)
			outputPath += "tinla(" + Config.TINLA_R + ")_";
		else if (Config.CTINLA_ENABLED)
			outputPath += "tinla_c(" + Config.CTINLA_R + ")_";
		else if (Config.TIPLA_ENABLED)
			outputPath += "tipla_";
		else
			outputPath += "tila_";

		if (rankingStrategy == Config.HALFWAY_RANKING)
			outputPath += "t=half";
		else if (rankingStrategy == Config.MAX_RANKING)
			outputPath += "t=max";
		else if (rankingStrategy == Config.ZERO_RANKING)
			outputPath += "t=zero";

		FileWriter w = new FileWriter(outputPath);
		w.write("Total matches: " + topMatches.size() + "\n");
		w.write("Recursive Time: " + totalTime + " (ms)\n");
		w.write(result);
		w.close();
	}

	/**
	 * Return the durable matchings
	 * 
	 * @return
	 */
	public Set<MatchInfo> getMatches() {
		return topMatches;
	}

	/**
	 * Returns the durable patterns duration
	 * 
	 * @return
	 */
	public int getMaxDuration() {
		return maxDuration;
	}

	/**
	 * Return algorithm execution time
	 * 
	 * @return
	 */
	public long getTotalExecutionTime() {
		return totalTime;
	}

	/**
	 * Support Class for filter candidates by path
	 * 
	 * @author ksemer
	 */
	class nodeScore {
		public int score;

		public nodeScore() {
			score = 1;
		}
	}
}