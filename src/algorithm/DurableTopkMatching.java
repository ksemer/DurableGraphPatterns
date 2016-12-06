package algorithm;

import java.util.BitSet;

import graph.pattern.PatternGraph;
import graph.version.Graph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import algorithm.indexes.PatternPathIndex;
import graph.pattern.PatternNode;
import graph.version.Edge;
import graph.version.Node;
import graph.version.loader.LoaderDBLP;
import system.Config;

/**
 * DurableMatching Algorithm class
 * 
 * @author ksemer
 */
public class DurableTopkMatching {
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

	// stores the k matches
	private PriorityQueue<Match> topkMatches;

	// stores all the matches
	private Set<String> matchesFound;

	// stores all chosen theta
	private Set<Integer> durationMaxRanking;

	// minimum checked threshold Max, Adaptive
	private int checkedTheta;

	// total recursions
	private int totalRecursions = 0;

	// size of rank
	private int sizeOfRank = 0;

	// k matches
	private int k;

	// utility value for adaptive ranking
	private int canDSize;

	// recursions per theta run
	private int recursionsPerTheta;

	// total time of algorithm
	private long totalTime;

	private int rankingStrategy;

	// time limit for algorithm execution
	private long timeLimit;

	// ===============================================================

	/**
	 * Constructor
	 * 
	 * @param lvg
	 * @param pg
	 * @param iQ
	 * @param continuously
	 * @param k
	 * @param rankingStrategy
	 * @throws IOException
	 */
	public DurableTopkMatching(Graph lvg, PatternGraph pg, BitSet iQ, boolean continuously, int k, int rankingStrategy)
			throws IOException {

		this.k = k;
		this.pg = pg;
		this.iQ = iQ;
		this.continuously = continuously;
		this.rankingStrategy = rankingStrategy;

		// initial C
		Map<Integer, Set<Node>> initC;
		timeLimit = System.currentTimeMillis();

		topkMatches = new PriorityQueue<Match>(11, new MatchComparator(false));
		Rank = new HashMap<>();

		// if TiPLa index is activated use the path filtering
		if (Config.TIPLA_ENABLED)
			filterCandidatesByPath(lvg, pg, iQ);
		else
			filterCandidates(lvg, pg, iQ);

		TreeMap<Integer, Set<Node>> ranking;
		int sc, pn_id;

		// for each pattern node
		for (PatternNode p : pg.getNodes()) {

			ranking = Rank.get(p.getID());

			// if ranking is empty then no matches
			if (ranking.isEmpty()) {
				threshold = -1;

				// write no matches
				writeTopMatches();
				return;
			}

			// zero ranking
			if (rankingStrategy == Config.ZERO_RANKING) {
				threshold = 2;
				continue;
			} else if (rankingStrategy == Config.ADAPTIVE_RANKING) {
				int cand = 0;

				// from highest key to lowest in ranking
				for (int th : ranking.descendingKeySet()) {
					cand += ranking.get(th).size();

					// there should be at least k candidates until th
					if (cand >= Config.K && threshold > th) {
						threshold = th;
						break;
					}
				}
			} else {
				sc = ranking.lastKey();

				if (threshold > sc)
					threshold = sc;
			}
		}

		// max ranking
		if (rankingStrategy != Config.ZERO_RANKING) {
			matchesFound = new HashSet<>();
			durationMaxRanking = new HashSet<>();

			// when there are not k candidates
			if (rankingStrategy == Config.ADAPTIVE_RANKING && threshold > Config.MAXIMUM_INTERVAL)
				threshold = 2;

			// store which threshold has been chosen
			durationMaxRanking.add(threshold);
			checkedTheta = threshold;
		}

		TreeMap<Integer, Set<Node>> tree;
		NavigableMap<Integer, Set<Node>> submap;

		canDSize = Integer.MAX_VALUE;
		Set<Node> c;

		while (threshold > 1) {

			initC = new HashMap<>();

			for (Entry<Integer, TreeMap<Integer, Set<Node>>> entry : Rank.entrySet()) {
				pn_id = entry.getKey();
				tree = entry.getValue();
				System.out.println(tree.keySet());

				c = new HashSet<>();
				initC.put(pn_id, c);

				submap = tree.subMap(tree.ceilingKey(threshold), true, tree.lastKey(), true);

				for (Entry<Integer, Set<Node>> en : submap.entrySet())
					c.addAll(en.getValue());

				// store the smaller candidate size
				if (canDSize > c.size())
					canDSize = c.size();
			}

			System.out.print("Threshold: " + threshold + "\tCan_size: " + initC.get(0).size());
			sizeOfRank++;

			initC = DUALSIM(initC);

			try {
				recursionsPerTheta = 0;
				searchPattern(initC, 0);
				System.out.print("\tRecursions: " + recursionsPerTheta + "\n");
			} catch (Exception e) {
				System.out.println("\nError: " + e.getMessage());
			}

			// topk matches found
			if (topkMatches.size() == k)
				break;

			// get new threshold
			if (rankingStrategy == Config.ADAPTIVE_RANKING)
				threshold = getAdaptiveThreshold();
			else if (rankingStrategy == Config.MAX_RANKING)
				threshold = getMaxThreshold();
		}

		// write matches
		writeTopMatches();
	}

	/**
	 * Compute the next threshold based on adaptive ranking
	 * 
	 * @return
	 */
	private int getAdaptiveThreshold() {
		int threshold = checkedTheta;
		TreeMap<Integer, Set<Node>> ranking;
		NavigableMap<Integer, Set<Node>> submap;

		int prevS = (int) (canDSize + Config.CP * canDSize);
		canDSize = Integer.MAX_VALUE;

		for (PatternNode p : pg.getNodes()) {

			ranking = Rank.get(p.getID());
			submap = ranking.subMap(ranking.firstKey(), true, ranking.floorKey(threshold) - 1, true);

			int cand = 0;

			// from highest key to lowest in ranking
			for (int th : submap.descendingKeySet()) {
				cand += submap.get(th).size();

				// there should be at least k candidates until th
				if (cand >= prevS) {
					threshold = th;

					if (canDSize > cand)
						canDSize = cand;

					break;
				}
			}
		}

		// if threshold has been already checked
		while (durationMaxRanking.contains(threshold)) {
			threshold -= Math.round(Config.ADAPTIVE_THETA * threshold);
		}

		// store threshold that have been analyzed
		durationMaxRanking.add(threshold);
		checkedTheta = threshold;

		return threshold;
	}

	/**
	 * Get next threshold based on max ranking
	 * 
	 * @return
	 */
	private int getMaxThreshold() {
		int sc;
		TreeMap<Integer, Set<Node>> ranking;
		threshold = checkedTheta;

		for (PatternNode p : pg.getNodes()) {
			ranking = Rank.get(p.getID());

			if (ranking.floorKey(threshold) == null)
				continue;

			// get the lower highest threshold
			sc = ranking.floorKey(threshold);

			if (threshold > sc)
				threshold = sc;
		}

		// if threshold has been already checked
		while (durationMaxRanking.contains(threshold)) {
			threshold--;
		}

		// store threshold that have been analyzed
		durationMaxRanking.add(threshold);
		checkedTheta = threshold;

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
		recursionsPerTheta++;

		if (System.currentTimeMillis() > (timeLimit + Config.TIME_LIMIT * 1000)) {
			throw new Exception("Reach time limit");
		} else if (depth == pg.size() && c.size() != 0) {
			computeMatchTime(c);
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
	private void computeMatchTime(Map<Integer, Set<Node>> match) {
		BitSet inter = (BitSet) iQ.clone();
		Node src, trg;
		String matchSign = null;
		int[] signAr = null;

		if (rankingStrategy != Config.ZERO_RANKING)
			signAr = new int[match.size()];

		// check the edges
		for (PatternNode pn : pg.getNodes()) {

			// get the node that have same label as pn
			src = match.get(pn.getID()).iterator().next();

			if (rankingStrategy != Config.ZERO_RANKING)
				signAr[pn.getID()] = src.getID();

			if (Config.LABEL_CHANGE)
				// intersect labels lifespan
				inter.and(src.getLabel(pn.getLabel()));

			// get adjacency of pn
			for (PatternNode child : pn.getAdjacency()) {

				// get the node that have the same label as child
				trg = match.get(child.getID()).iterator().next();

				inter.and(src.getEdge(trg).getLifetime());

				if (continuously) {
					BitSet shifted = (BitSet) inter.clone();
					int count = 0;

					while (!shifted.isEmpty()) {
						shifted.and(shifted.get(1, shifted.length()));
						count++;
					}

					if (count < threshold)
						return;

				} else if (inter.cardinality() < threshold) {
					// check if the cardinality is less than
					// topScore or algoTerm
					return;
				}
			}
		}

		// if match has already been found
		if (rankingStrategy != Config.ZERO_RANKING && matchesFound.contains((matchSign = Arrays.toString(signAr))))
			return;

		// duration of match
		int duration = inter.cardinality(), minDuration;

		// if ranking strategy is max or adaptive
		if (rankingStrategy == Config.MAX_RANKING || rankingStrategy == Config.ADAPTIVE_RANKING) {

			if (topkMatches.size() < k) {

				// add the match
				topkMatches.offer(new Match(duration, inter, match));

				// add the sign
				matchesFound.add(matchSign);

				durationMaxRanking.add(threshold);

			} else if (topkMatches.size() == k) {

				// if we have found k matches and the new match has higher
				// duration
				if (duration > (minDuration = topkMatches.peek().getDuration())) {

					// remove match with the min duration as and its sign
					matchesFound.remove(topkMatches.poll());

					// add the match
					topkMatches.offer(new Match(duration, inter, match));

					// add the sign
					matchesFound.add(matchSign);

					// update threshold
					threshold = topkMatches.peek().getDuration();

					durationMaxRanking.add(threshold);

					// update min checkedTheta
					if (checkedTheta > threshold)
						checkedTheta = threshold;

				} else if (duration == minDuration) {

					if (rankingStrategy == Config.ADAPTIVE_RANKING) {
						threshold = duration + 1;

						// if threshold has been already checked
						while (durationMaxRanking.contains(threshold)) {
							threshold++;
						}

					} else {
						// set the threshold to look for matches with duration
						// >=
						// duration of min element in heap
						threshold = duration + 1;
					}
				}
			}
		} else if (rankingStrategy == Config.ZERO_RANKING) { // ranking strategy
																// is zero

			if (topkMatches.size() < k) {

				// add the match
				topkMatches.offer(new Match(duration, inter, match));

			} else if (topkMatches.size() == k) {

				// if we have found k matches and the new match has higher
				// duration
				if (duration > (minDuration = topkMatches.peek().getDuration())) {

					// remove match with the min duration
					topkMatches.remove();

					// add the match
					topkMatches.offer(new Match(duration, inter, match));
					threshold = topkMatches.peek().getDuration();
				} else if (duration == minDuration) {

					// set the threshold to look for matches with duration >=
					// duration of min element in heap
					threshold = duration + 1;
				}
			}
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
	private Map<Integer, Set<Node>> refine(Map<Integer, Set<Node>> c) {
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
	private List<Node> timeJoin(Node n, PatternNode p, PatternNode chil, Map<Integer, Set<Node>> phi) {
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

					} else if (inter.cardinality() < threshold) {
						// check if target is pruned or it is not alive
						// during interval
						continue;
					}

					intersection.add(ngb);
				}
			}
		}

		return intersection;
	}

	/**
	 * Check if node u is contained in at least one candidate set
	 *
	 * @param phi
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
		int label;
		Set<Node> pnode_candidates, current_candidates, candidates;
		Map<Integer, Set<Node>> rankingBasedOnlifespanScore;

		// store for a label the candidate nodes
		Map<Integer, Set<Node>> labelCandidates = new HashMap<>();
		Node n;

		for (PatternNode pn : pg.getNodes()) {
			pnode_candidates = phi.get(pn.getID());

			// get pattern's node label
			label = pn.getLabel();

			// if label exist then retrieve its candidates for all iQ
			if ((candidates = labelCandidates.get(label)) != null)
				pnode_candidates.addAll(candidates);
			else {
				// for each time instant get the candidates and add them in one
				// set
				for (Iterator<Integer> it = iQ.stream().iterator(); it.hasNext();)
					pnode_candidates.addAll(lvg.getTiLaNodes(it.next(), label));

				// candidates for label in iQ
				labelCandidates.put(label, new HashSet<>(pnode_candidates));
			}

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
					Map<Integer, Integer> cT;

					// for each r
					for (int r = 0; r < Config.CTINLA_R; r++) {

						for (Entry<Integer, Integer> l : pn.getLabelAdjacency_C(r).entrySet()) {

							// if there is not a neighbor with that label
							// remove it
							if ((cT = n.getCTiNLa(r, l.getKey())) == null) {
								found = false;
								it.remove();
								break;
							} else { // otherwise get the lifespan and intersect

								lifespan = n.getCTiNLa(cT, l.getValue(), lifespan);

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
				} else if (lifespan.isEmpty()) { // check if Node n lifespan
													// does not contain any bit
					it.remove();
					continue;
				}

				int sc;

				// remove node from candidates since with score 1 it is not
				// durable
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
		Map<Integer, Set<String>> patternPathIndex = new PatternPathIndex(Config.TIPLA_MAX_DEPTH).createPathIndex(pg);

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
	 * Write Matches
	 * 
	 * @throws IOException
	 */
	private void writeTopMatches() throws IOException {
		totalTime = (System.currentTimeMillis() - timeLimit);

		String outputPath = Config.PATH_OUTPUT + "pq=" + pg.getID() + "_";

		if (continuously)
			outputPath += "cont_";

		if (Config.TINLA_ENABLED)
			outputPath += "tinla(" + Config.TINLA_R + ")_";
		else if (Config.CTINLA_ENABLED)
			outputPath += "ctinla(" + Config.CTINLA_R + ")_";
		else if (Config.TIPLA_ENABLED)
			outputPath += "tipla_";
		else
			outputPath += "tila_";

		if (rankingStrategy == Config.ADAPTIVE_RANKING)
			outputPath += "r=a";
		else if (rankingStrategy == Config.MAX_RANKING)
			outputPath += "r=m";
		else if (rankingStrategy == Config.ZERO_RANKING)
			outputPath += "r=z";

		FileWriter w = new FileWriter(outputPath);
		w.write("Top-" + k + " matches\n");
		w.write("Total matches: " + topkMatches.size() + "\n");
		w.write("Recursive Time: " + totalTime + " (ms)\n");
		w.write("sizeOfRank: " + sizeOfRank + "\n");
		w.write("Total Recursions: " + totalRecursions + "\n");
		w.write("-------------------\n");

		// no matches found
		if (threshold == -1) {
			w.write("No matches");
			w.close();
			return;
		}

		// stores the result
		String result = "";
		BitSet shifted;
		int count;

		// reverse minHeap to maxHeap in order to ouput from high to low
		PriorityQueue<Match> topkMatchesR = new PriorityQueue<Match>(11, new MatchComparator(true));

		while (!topkMatches.isEmpty())
			topkMatchesR.offer(topkMatches.poll());

		while (!topkMatchesR.isEmpty()) {
			Match mI = topkMatchesR.poll();

			result += "------ Match ------\n";

			if (continuously) {
				shifted = (BitSet) mI.getLifespan().clone();
				count = 0;

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

			// write the edges
			for (PatternNode pn : pg.getNodes()) {

				// for each adjacent node
				for (PatternNode trg : pn.getAdjacency()) {

					for (Node n : mI.getMatch().get(pn.getID())) {

						for (Edge e : n.getAdjacency()) {

							if (mI.getMatch().get(trg.getID()).contains(e.getTarget())) {

								if (Config.PATH_DATASET.contains("dblp"))
									result += LoaderDBLP.getAuthors().get(n.getID()) + ": ";

								result += "(" + pn.getID() + ") ---> (" + trg.getID() + ")";

								if (Config.PATH_DATASET.contains("dblp"))
									result += " " + LoaderDBLP.getAuthors().get(e.getTarget().getID());

								result += "\n";
							}
						}
					}
				}
			}

			result += "-------------------\n";
		}

		w.write(result);
		w.close();
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
