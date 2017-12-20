package graph.version.index;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import graph.version.Edge;
import graph.version.Graph;
import graph.version.Node;
import system.Config;
import utils.Storage;

/**
 * TiPLa
 * 
 * @author ksemer
 */
public class TimePathIndex implements Serializable {

	// ====================================================================

	private static final long serialVersionUID = 1L;

	private Map<Integer, Map<String, Set<Node>>> timePathIndexWT;
	private Set<Node> set;

	// ====================================================================

	/**
	 * Constructor
	 * 
	 * @param max_depth
	 * @throws Exception
	 */
	public TimePathIndex() {

		this.timePathIndexWT = new HashMap<>();

		for (int i = 0; i < Config.MAXIMUM_INTERVAL; i++)
			this.timePathIndexWT.put(i, new HashMap<>());
	}

	/**
	 * Create path index Return for each time instant all combinations of paths ->
	 * nodes with that combination
	 * 
	 * @param g
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public Map<Integer, Map<String, Set<Node>>> createPathIndex(Graph g) throws IOException {

		System.out.println("Time Path Index is running");
		long time = System.currentTimeMillis();

		for (Node n : g.getNodes())
			traversePath(n);

		if (Config.SHOW_MEMORY) {

			Runtime runtime = Runtime.getRuntime();

			// Run the garbage collector
			runtime.gc();

			// Calculate the used memory
			long memory = runtime.totalMemory() - runtime.freeMemory();

			System.out.println("TiPLa memory: " + Storage.bytesToMegabytes(memory));
		}

		System.out.println("TiPLa time: " + (System.currentTimeMillis() - time) + " (ms)");

		return timePathIndexWT;
	}

	/**
	 * Traverse paths in depth
	 * 
	 * @param n
	 */
	private void traversePath(Node n) {

		Deque<NInfo> toBeVisited = new ArrayDeque<>();
		BitSet l;
		NInfo info = new NInfo(n, null, new BitSet(), 0);
		toBeVisited.add(info);

		while (!toBeVisited.isEmpty()) {

			info = toBeVisited.poll();

			// if we are in the last node defined by the depth
			if (info.depth == Config.TIPLA_MAX_DEPTH) {

				if (!info.lifespan.isEmpty())
					storePath(info);

				continue;
			}

			boolean addNew = false;

			// for all neighbors
			for (Edge e : info.n.getAdjacency()) {

				// if we are in src node we take as lifespan the edge's lifespan
				if (info.father == null) {
					addNew = true;
					toBeVisited.add(new NInfo(e.getTarget(), info, (BitSet) e.getLifetime().clone(), info.depth + 1));

				} else if (!info.father.n.equals(e.getTarget())) {

					// else we and the edge's lifespan with the info.lifespan
					l = (BitSet) info.lifespan.clone();
					l.and(e.getLifetime());

					if (!l.isEmpty()) {
						addNew = true;
						toBeVisited.add(new NInfo(e.getTarget(), info, l, info.depth + 1));
					}
				}
			}

			// if the path ends before maxDepth
			if (!addNew && (info.depth + 1) < Config.TIPLA_MAX_DEPTH)
				storePath(info);
		}
	}

	/**
	 * Corrects the order of the path and create the label paths
	 * 
	 * @param info
	 */
	private void storePath(NInfo info) {

		BitSet life = info.lifespan;
		List<Node> path = new ArrayList<>();

		while (true) {

			if (info.father == null) {
				path.add(info.n);
				break;
			}

			// add the node to the path and update father
			path.add(info.n);
			info = info.father;
		}

		Collections.reverse(path);

		// call recursive
		rec_labelComp(path, path.get(0), life, "", 0);
	}

	/**
	 * Recursive function
	 * 
	 * @param path
	 * @param src
	 * @param life
	 * @param label
	 * @param depth
	 */
	private void rec_labelComp(List<Node> path, Node src, BitSet life, String label, int depth) {

		Node n = path.get(depth);
		BitSet lifespan;
		String Path;

		for (Entry<Integer, BitSet> entry : n.getLabels().entrySet()) {
			int l = entry.getKey();

			if (life.isEmpty())
				lifespan = (BitSet) entry.getValue().clone();
			else {
				lifespan = (BitSet) life.clone();
				lifespan.and(entry.getValue());
			}

			if (!lifespan.isEmpty()) {

				if (depth == 0) {

					// if we are in the last node defined by the depth
					if (Config.ISDIRECTED) {

						for (int t = lifespan.nextSetBit(0); t != -1; t = lifespan.nextSetBit(t + 1)) {

							if ((set = timePathIndexWT.get(t).get("" + l)) == null) {
								set = new HashSet<>();
								timePathIndexWT.get(t).put("" + l, set);
							}

							set.add(src);
						}
					}

					if (depth + 1 != path.size())
						rec_labelComp(path, src, lifespan, label + "" + l, depth + 1);
				} else {

					// i is the next label in path
					// we use integers to denote labels
					Path = label + " " + l;

					for (int t = lifespan.nextSetBit(0); t != -1; t = lifespan.nextSetBit(t + 1)) {

						if ((set = timePathIndexWT.get(t).get(Path)) == null) {
							set = new HashSet<>();
							timePathIndexWT.get(t).put(Path, set);
						}

						set.add(src);
					}

					if (depth + 1 != path.size())
						rec_labelComp(path, src, lifespan, Path, depth + 1);
				}
			}
		}
	}
}
