package graph.version.loader;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import graph.version.Edge;
import graph.version.Graph;
import graph.version.Node;
import system.Config;

/**
 * Loader abstract class
 * 
 * @author ksemer
 */
public abstract class Loader {

	abstract Graph loadDataset() throws IOException;

	protected abstract void loadAttributes(Graph lvg) throws IOException;

	/**
	 * Create TiNLa && CTiNLa index
	 * 
	 * @param lvg
	 */
	public static void createNeighborIndex(Graph lvg) {
		int R = -1, label;
		Node trg;
		BitSet lifetime;

		if (Config.TINLA_ENABLED) {
			System.out.println("TiNLa(" + Config.TINLA_R + ") construction is starting...");
			R = Config.TINLA_R;
		} else if (Config.CTINLA_ENABLED) {
			System.out.println("CTiNLa(" + Config.CTINLA_R + ") construction is starting...");
			R = Config.CTINLA_R;
		}

		long time = System.currentTimeMillis();

		// for all nodes
		for (Node n : lvg.getNodes()) {

			Set<Node> visited = new HashSet<>();
			Set<Edge> temp_edges = new HashSet<>();
			Set<Edge> adjacency = new HashSet<>(n.getAdjacency());

			// for each r
			for (int r = 0; r < R; r++) {

				// for each adjacent node of radius r
				for (Edge e : adjacency) {
					trg = e.getTarget();

					// for avoiding cycles
					if (R > 2 && visited.contains(trg))
						continue;

					// for each label of trg node
					for (Entry<Integer, BitSet> entry : trg.getLabels().entrySet()) {
						// label
						label = entry.getKey();

						// lifetime of the label
						lifetime = entry.getValue();

						// for each active time instant update TiNLa index
						for (Iterator<Integer> it = lifetime.stream().iterator(); it.hasNext();) {
							int t = it.next();

							if (Config.TINLA_ENABLED)
								n.updateTiNLa(r, label, t);
							else if (Config.CTINLA_ENABLED)
								n.updateCTiNLa(r, label, t);
						}
					}

					// store in temp set the edges of the next hop
					temp_edges.addAll(trg.getAdjacency());

					if (R > 2) {
						// add node to visited set
						visited.add(trg);
					}
				}

				// clear and set adjacency to show the next hop
				adjacency.clear();
				adjacency.addAll(temp_edges);
				temp_edges.clear();
			}
		}

		if (Config.TINLA_ENABLED)
			System.out.println(
					"TiNLa(" + Config.TINLA_R + ") time: " + (System.currentTimeMillis() - time) / 1000 + " (sec)");
		else if (Config.CTINLA_ENABLED)
			System.out.println(
					"CTiNLa(" + Config.CTINLA_R + ") time: " + (System.currentTimeMillis() - time) / 1000 + " (sec)");
	}
}
