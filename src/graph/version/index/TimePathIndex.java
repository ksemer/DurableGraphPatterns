package graph.version.index;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
public class TimePathIndex {

	// ========================================================================

	private Map<Integer, Map<String, Set<Node>>> TiPLa;

	// ========================================================================

	/**
	 * Constructor
	 * 
	 * @param max_depth
	 * @throws Exception
	 */
	public TimePathIndex() {

		if (!Config.BLOOM_ENABLED) {
			TiPLa = new ConcurrentHashMap<>();

			for (int i = 0; i < Config.MAXIMUM_INTERVAL; i++)
				TiPLa.put(i, new ConcurrentHashMap<>());
		}
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
	public Map<Integer, Map<String, Set<Node>>> createPathIndex(Graph g) throws IOException, InterruptedException {

		if (Config.BLOOM_ENABLED)
			System.out.print("TiPLaBloom is running");
		else
			System.out.println("TiPLa is running");
		long time = System.currentTimeMillis();

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		for (Node n : g.getNodes())
			executor.submit(setCallablePath(n));

		executor.shutdown();

		while (!executor.isTerminated()) {
			executor.awaitTermination(10, TimeUnit.SECONDS);
		}

		if (Config.SHOW_MEMORY) {

			Runtime runtime = Runtime.getRuntime();

			// Run the garbage collector
			runtime.gc();

			// Calculate the used memory
			long memory = runtime.totalMemory() - runtime.freeMemory();

			if (Config.BLOOM_ENABLED)
				System.out.println("TiPLaBloom memory: " + Storage.bytesToMegabytes(memory));
			else
				System.out.println("TiPLa memory: " + Storage.bytesToMegabytes(memory));
		}

		if (Config.BLOOM_ENABLED)
			System.out.println("TiPLaBloom time: " + (System.currentTimeMillis() - time) + " (ms)");
		else
			System.out.println("TiPLa time: " + (System.currentTimeMillis() - time) + " (ms)");

		return TiPLa;
	}

	/**
	 * Callable for computing all paths to maxDepth
	 * 
	 * @param n
	 * @return
	 */
	private Callable<?> setCallablePath(Node n) {

		Callable<?> c = () -> {
			try {

				if (Config.BLOOM_ENABLED)
					n.initiliazeTiPLaAux();

				traversePath(n);

				if (Config.BLOOM_ENABLED)
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
			if (!addNew && (info.depth + 1) <= Config.TIPLA_MAX_DEPTH)
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

		// call recursive label path
		if (Config.BLOOM_ENABLED)
			rec_labelCompB(path, path.get(0), life, "", 0);
		else
			rec_labelComp(path, path.get(0), life, "", 0);
	}

	/**
	 * Recursive function bloom
	 * 
	 * @param path
	 * @param src
	 * @param life
	 * @param label
	 * @param depth
	 */
	private void rec_labelCompB(List<Node> path, Node src, BitSet life, String label, int depth) {

		Node n = path.get(depth);
		BitSet lifespan;
		String Path;
		Set<String> paths;
		Map<Integer, Set<String>> paths_per_t = src.getTiPLaAux();

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

							if ((paths = paths_per_t.get(t)) == null) {
								paths = new HashSet<>();
								paths_per_t.put(t, paths);
							}

							paths.add("" + l);
						}
					}

					if (depth + 1 != path.size())
						rec_labelCompB(path, src, lifespan, label + "" + l, depth + 1);
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

					if (depth + 1 != path.size())
						rec_labelCompB(path, src, lifespan, Path, depth + 1);
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
		Set<Node> set;

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

							if ((set = TiPLa.get(t).get("" + l)) == null) {
								set = new HashSet<>();
								TiPLa.get(t).put("" + l, set);
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

						if ((set = TiPLa.get(t).get(Path)) == null) {
							set = new HashSet<>();
							TiPLa.get(t).put(Path, set);
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
