package graph.version.index;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import graph.version.Edge;
import graph.version.Graph;
import graph.version.Node;
import system.Config;
import utils.Storage;

/**
 * TimePathBloomIndex class
 * 
 * @author ksemer
 */
public class TimePathBloomIndex {

	/**
	 * Create TiPLaBloom index For each node store its path bloom index
	 * 
	 * @param g
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void createIndex(Graph g) throws IOException, InterruptedException {

		System.out.println("TiPLaBloom Index is running");
		long time = System.currentTimeMillis();

		ExecutorService executor = Executors.newFixedThreadPool(Config.THREADS);

		for (Node n : g.getNodes()) {
			executor.submit(setCallablePath(n, Config.TIPLA_MAX_DEPTH));
		}

		executor.shutdown();

		while (!executor.isTerminated()) {
			executor.awaitTermination(60, TimeUnit.SECONDS);
		}

		if (Config.SHOW_MEMORY) {

			Runtime runtime = Runtime.getRuntime();

			// Run the garbage collector
			runtime.gc();

			// Calculate the used memory
			long memory = runtime.totalMemory() - runtime.freeMemory();

			System.out.println("TiPLaBloom memory: " + Storage.bytesToMegabytes(memory));
		}

		System.out.println("TiPLaBloom time: " + (System.currentTimeMillis() - time) + " (ms)");

	}

	/**
	 * Callable for computing all paths to maxDepth
	 * 
	 * @param n
	 * @param maxDepth
	 * @return
	 */
	private Callable<?> setCallablePath(Node n, int maxDepth) {

		Callable<?> c = () -> {
			try {
				n.initiliazeTiPLaBloom();

				for (int depth = maxDepth; depth >= 1; depth--) {
					traversePath(n, depth);
				}

				if (Config.ISDIRECTED) {

					Set<String> paths;
					Map<Integer, Set<String>> paths_per_t = n.getTiPLaAux();

					// to contain also the paths of size 0, node label itself
					for (Entry<Integer, BitSet> entry : n.getLabels().entrySet()) {
						int label = entry.getKey();

						for (Iterator<Integer> it = n.getLabel(label).stream().iterator(); it.hasNext();) {
							int t = it.next();

							if ((paths = paths_per_t.get(t)) == null) {
								paths = new HashSet<>();
								paths_per_t.put(t, paths);
							}

							paths.add("" + label);
						}
					}
				}

				n.createTiPLaBloom();

			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		};
		return c;
	}

	/**
	 * Traverse paths in depth
	 * 
	 * @param n
	 * @param maxDepth
	 */
	private void traversePath(Node n, int maxDepth) {

		Deque<NInfo> toBeVisited = new ArrayDeque<>();
		List<Node> path;
		BitSet l;
		NInfo info = new NInfo(n, null, new BitSet(), 0);
		toBeVisited.add(info);

		while (!toBeVisited.isEmpty()) {

			info = toBeVisited.poll();

			// if we are in the last node defined by the depth
			if (info.depth == maxDepth) {

				if (!info.lifespan.isEmpty()) {

					BitSet life = null;
					path = new ArrayList<>();

					while (true) {

						if (info.father == null) {
							path.add(info.n);
							break;
						}

						if (life == null)
							life = info.lifespan;

						// add the node to the path and update father
						path.add(info.n);
						info = info.father;
					}

					Collections.reverse(path);

					// call recursive
					rec_labelComp(path, path.get(0), life, "", 0);
				}
				continue;
			}

			// for all neighbors
			for (Edge e : info.n.getAdjacency()) {

				// if we are in src node we take as lifespan the edge's lifespan
				if (info.father == null) {

					toBeVisited.add(new NInfo(e.getTarget(), info, (BitSet) e.getLifetime().clone(), info.depth + 1));

				} else if (!info.father.n.equals(e.getTarget())) {

					// else we and the edge's lifespan with the info.lifespan
					l = (BitSet) info.lifespan.clone();
					l.and(e.getLifetime());

					if (!l.isEmpty())
						toBeVisited.add(new NInfo(e.getTarget(), info, l, info.depth + 1));
				}
			}
		}
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
		Set<String> paths;
		Map<Integer, Set<String>> paths_per_t = src.getTiPLaAux();

		for (Entry<Integer, BitSet> entry : n.getLabels().entrySet()) {
			int l = entry.getKey();

			lifespan = (BitSet) life.clone();
			lifespan.and(entry.getValue());

			if (!lifespan.isEmpty()) {

				if (depth + 1 < path.size()) {
					if (depth == 0)
						rec_labelComp(path, src, lifespan, label + "" + l, depth + 1);
					else
						rec_labelComp(path, src, lifespan, label + " " + l, depth + 1);
				} else {
					// i is the next label in path
					// we use integers to denote labels
					Path = label + " " + l;

					for (int t = lifespan.nextSetBit(0); t != -1; t = lifespan.nextSetBit(t + 1)) {

						if ((paths = paths_per_t.get(t)) == null) {
							paths = new HashSet<>();
							paths_per_t.put(t, paths);
						}

						paths.add(Path);
					}
				}
			}
		}
	}
}