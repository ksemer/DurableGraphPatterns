package utils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;

import graph.pattern.PatternGraph;
import graph.pattern.PatternNode;
import graph.version.Edge;
import graph.version.Node;
import system.Config;

/**
 * Pattern graph query generator
 * 
 * @author ksemer
 */
public class QueryGenerator {
	// ===============================================
	private static Map<Node, Integer> labels;
	private static int patternGraphID = 0;
	private static Set<Node> hasBeenVisited;
	// ===============================================

	/**
	 * DFS traversal
	 * 
	 * @param src
	 * @param size
	 * @return
	 */
	public static boolean dfs(Node src, int size) {
		hasBeenVisited = new HashSet<Node>();
		Stack<Node> st = new Stack<Node>();
		st.push(src);
		labels = new HashMap<>();
		labels.put(src, getMaxLabel(src));

		while (!st.isEmpty()) {
			Node v = st.pop();

			if (!hasBeenVisited.contains(v)) {
				hasBeenVisited.add(v);

				for (Edge w : v.getAdjacency()) {

					if (!hasBeenVisited.contains(w.getTarget())) {
						labels.put(w.getTarget(), getMaxLabel(w.getTarget()));
						st.push(w.getTarget());
					}
				}
			}

			if (hasBeenVisited.size() == size)
				break;
		}

		if (hasBeenVisited.size() < size)
			return false;

		return true;
	}

	/**
	 * Get label with the highest duration
	 * 
	 * @param src
	 * @return
	 */
	private static int getMaxLabel(Node src) {
		int max = 0, label = 0;
		BitSet lifespan;

		for (Entry<Integer, BitSet> entry : src.getLabels().entrySet()) {
			lifespan = entry.getValue();

			if (lifespan.cardinality() > max) {
				max = lifespan.cardinality();
				label = entry.getKey();
			}
		}

		return label;
	}

	/**
	 * Return query as a pattern graph
	 * 
	 * @param size
	 * @return
	 */
	public static PatternGraph getQuery(int size) {

		PatternGraph pg = new PatternGraph(patternGraphID++);
		PatternNode pn1, pn2;
		List<Node> nodes = new ArrayList<>(hasBeenVisited);

		for (int i = 0; i < nodes.size(); i++) {
			pg.addNode(i, labels.get(nodes.get(i)));
		}

		System.out.println("Pattern graph id: " + pg.getID());

		for (int i = 0; i < nodes.size(); i++) {
			pn1 = pg.getNode(i);

			for (int j = i + 1; j < nodes.size(); j++) {

				if (nodes.get(i).getEdge(nodes.get(j)) != null) {
					pn2 = pg.getNode(j);

					if (!pn1.getAdjacency().contains(pn2)) {
						pn1.addEdge(pn2);

						if (!Config.ISDIRECTED) {
							pn2.addEdge(pn1);
							System.out.println(pn1.getID() + " (" + pn1.getLabel() + ") <--> " + pn2.getID() + " ("
									+ pn2.getLabel() + ")");
						} else
							System.out.println(pn1.getID() + " (" + pn1.getLabel() + ") --> " + pn2.getID() + " ("
									+ pn2.getLabel() + ")");
					}
				}
			}
		}

		System.out.println("---------------------");
		return pg;
	}
}