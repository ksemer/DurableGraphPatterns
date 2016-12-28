package system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import algorithm.DurableMatching;
import algorithm.DurableTopkMatching;
import graph.pattern.PatternGraph;
import graph.version.Graph;
import graph.version.loader.LoaderDBLP;
import graph.version.loader.LoaderProteins;
import graph.version.loader.LoaderYT;

public class Experiments {
	private static String out;
	private static boolean runTopk = true;
	private static boolean runMost = false;

	/**
	 * Main
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Config.loadConfigs();

		Config.K = 10;
		Config.ADAPTIVE_THETA = 0.5;
		out = Config.PATH_OUTPUT;
		runIndex();
	}

	private static void runIndex() throws Exception {
		Config.TINLA_ENABLED = false;
		Config.CTINLA_ENABLED = false;
		Config.TIPLA_ENABLED = false;

		runQ();

		Config.TINLA_ENABLED = true;
		Config.CTINLA_ENABLED = false;
		Config.TIPLA_ENABLED = false;

		Config.TINLA_R = 1;
		runQ();

		Config.TINLA_R = 2;
		runQ();

		Config.TINLA_ENABLED = false;
		Config.CTINLA_ENABLED = true;
		Config.TIPLA_ENABLED = false;

		Config.CTINLA_R = 1;
		runQ();

		Config.CTINLA_R = 2;
		runQ();

		Config.TINLA_ENABLED = false;
		Config.CTINLA_ENABLED = false;
		Config.TIPLA_ENABLED = true;

		runQ();
	}

	public static void runQ() throws Exception {
		String dataset = Config.PATH_DATASET.toLowerCase();
		Graph lvg = null;

		// for dblp dataset
		if (dataset.contains("dblp")) {
			Config.MAXIMUM_INTERVAL = 58;
			lvg = new LoaderDBLP().loadDataset();
		} else if (dataset.contains("yt")) { // youtube
			Config.MAXIMUM_INTERVAL = 37;
			lvg = new LoaderYT().loadDataset();
		} else {
			// proteins
			lvg = new LoaderProteins().loadDataset();
		}

		if (runTopk)
			runQ(lvg, true, false);

		if (runMost)
			runQ(lvg, false, true);
	}

	public static void runQ(Graph lvg, boolean runTop, boolean runMost) throws Exception {
		String dataset = Config.PATH_DATASET.toLowerCase();
		Config.RUN_TOPK_QUERIES = runTop;
		Config.RUN_DURABLE_QUERIES = runMost;

		// for dblp dataset
		if (dataset.contains("dblp")) {

			Config.MAX_RANKING_ENABLED = true;
			Config.ADAPTIVE_RANKING_ENABLED = false;
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_prof.txt", "prof");
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_senior.txt", "senior");
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_junior.txt", "junior");
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_begin.txt", "begin");

			Config.MAX_RANKING_ENABLED = false;
			Config.ADAPTIVE_RANKING_ENABLED = true;
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_prof.txt", "prof");
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_senior.txt", "senior");
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_junior.txt", "junior");
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_begin.txt", "begin");
		} else if (dataset.contains("yt")) { // youtube

			Config.MAX_RANKING_ENABLED = true;
			Config.ADAPTIVE_RANKING_ENABLED = false;
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_least.txt", "least");
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_most.txt", "most");

			Config.MAX_RANKING_ENABLED = false;
			Config.ADAPTIVE_RANKING_ENABLED = true;
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_least.txt", "least");
			run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_most.txt", "most");
		} else { // proteins

			String[] x = dataset.split("/");
			String dataName = x[x.length - 1].replace(".gfu", "");

			Config.MAX_RANKING_ENABLED = true;
			Config.ADAPTIVE_RANKING_ENABLED = false;
			// run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_" +
			// dataName + ".txt", dataName);
			run_for_proteins(lvg, dataName);

			// Config.MAX_RANKING_ENABLED = true;
			// Config.ADAPTIVE_RANKING_ENABLED = false;
			// run(lvg, "/home/ksemer/workspaces/tkde_data/queries/queries_" +
			// dataName + ".txt", dataName);
			// run_for_proteins(lvg, dataName);
		}
	}

	public static void run_for_proteins(Graph lvg, String outputPr) throws Exception {

		for (Entry<String, Integer> entry : LoaderProteins.labels.entrySet()) {
			int label = entry.getValue();
			
			ExecutorService executor = Executors.newCachedThreadPool();
			List<Callable<?>> callables = new ArrayList<>();

			for (int i = 2; i <= 6; i++) {
				BitSet iQ;
				Config.PATH_OUTPUT = out + outputPr + "1/" + entry.getKey() + "-";
				iQ = new BitSet(Config.MAXIMUM_INTERVAL);
				iQ.set(0, Config.MAXIMUM_INTERVAL, true);

				PatternGraph pg = new PatternGraph(i);

				// create node with label
				for (int j = 0; j < i; j++) {
					pg.addNode(j, label);
				}

				// create edges
				for (int k = 0; k < i; k++) {
					for (int q = k + 1; q < i; q++) {
						pg.addEdge(k, q);
						pg.addEdge(q, k);
					}
				}
				
				if (Config.RUN_DURABLE_QUERIES) {

					if (Config.MAX_RANKING_ENABLED)
						callables.add(setCallableDurQ(lvg, pg, iQ, Config.MAX_RANKING));

					if (Config.ADAPTIVE_RANKING_ENABLED)
						callables.add(setCallableDurQ(lvg, pg, iQ, Config.ADAPTIVE_RANKING));

					if (Config.ZERO_RANKING_ENABLED)
						callables.add(setCallableDurQ(lvg, pg, iQ, Config.ZERO_RANKING));
				}

				if (Config.RUN_TOPK_QUERIES) {

					if (Config.MAX_RANKING_ENABLED)
						callables.add(setCallableTopkQ(lvg, pg, iQ, Config.MAX_RANKING));

					if (Config.ADAPTIVE_RANKING_ENABLED)
						callables.add(setCallableTopkQ(lvg, pg, iQ, Config.ADAPTIVE_RANKING));

					if (Config.ZERO_RANKING_ENABLED)
						callables.add(setCallableTopkQ(lvg, pg, iQ, Config.ZERO_RANKING));
				}
			}
			
			for (Callable<?> c : callables)
				executor.submit(c);

			executor.shutdown();

			while (!executor.isTerminated()) {
			}

			executor.shutdownNow();
			System.out.println("shutdown finished");
		}

		System.out.println("shutdown finished");
		lvg = null;

		Runtime runtime = Runtime.getRuntime();

		// Run the garbage collector
		runtime.gc();
	}

	public static void run(Graph lvg, String queryInput, String outputPr) throws Exception {
		BitSet iQ;
		Config.PATH_OUTPUT = out + outputPr + "/";
		iQ = new BitSet(Config.MAXIMUM_INTERVAL);
		iQ.set(0, Config.MAXIMUM_INTERVAL, true);
		String[] edge;
		PatternGraph pg = null;
		String line = null;
		boolean nodes = false;
		int sizeOfNodes = 0, id = 0, n1, n2;

		ExecutorService executor = Executors.newCachedThreadPool();
		List<Callable<?>> callables = new ArrayList<>();

		BufferedReader br = new BufferedReader(new FileReader(queryInput));

		while ((line = br.readLine()) != null) {

			if (line.contains("--") && pg != null) {

				if (Config.RUN_DURABLE_QUERIES) {

					if (Config.MAX_RANKING_ENABLED)
						callables.add(setCallableDurQ(lvg, pg, iQ, Config.MAX_RANKING));

					if (Config.ADAPTIVE_RANKING_ENABLED)
						callables.add(setCallableDurQ(lvg, pg, iQ, Config.ADAPTIVE_RANKING));

					if (Config.ZERO_RANKING_ENABLED)
						callables.add(setCallableDurQ(lvg, pg, iQ, Config.ZERO_RANKING));
				}

				if (Config.RUN_TOPK_QUERIES) {

					if (Config.MAX_RANKING_ENABLED)
						callables.add(setCallableTopkQ(lvg, pg, iQ, Config.MAX_RANKING));

					if (Config.ADAPTIVE_RANKING_ENABLED)
						callables.add(setCallableTopkQ(lvg, pg, iQ, Config.ADAPTIVE_RANKING));

					if (Config.ZERO_RANKING_ENABLED)
						callables.add(setCallableTopkQ(lvg, pg, iQ, Config.ZERO_RANKING));
				}
			} else if (line.contains("#")) {

				id = 0;
				nodes = true;
				sizeOfNodes = Integer.parseInt(br.readLine());
				line = line.trim().replace("#", "");
				pg = new PatternGraph(Integer.parseInt(line));
				continue;
			} else if (nodes) {

				pg.addNode(id++, Integer.parseInt(line.trim()));
				sizeOfNodes--;

				if (sizeOfNodes == 0) {
					nodes = false;
					continue;
				}
			} else {
				// edge
				edge = line.split("\\s+");

				// src node
				n1 = Integer.parseInt(edge[0]);

				// trg node
				n2 = Integer.parseInt(edge[1]);

				// src -> trg
				pg.addEdge(n1, n2);

				if (!Config.ISDIRECTED)
					// trg -> src
					pg.addEdge(n2, n1);
			}
		}
		br.close();

		for (Callable<?> c : callables)
			executor.submit(c);

		executor.shutdown();

		while (!executor.isTerminated()) {
		}

		executor.shutdownNow();
		System.out.println("shutdown finished");
		lvg = null;

		Runtime runtime = Runtime.getRuntime();

		// Run the garbage collector
		runtime.gc();
	}

	/**
	 * Set callable Durable query execution
	 * 
	 * @param lvg
	 * @param pg
	 * @param iQ
	 * @param rankingStrategy
	 * @return
	 */
	private static Callable<?> setCallableDurQ(Graph lvg, PatternGraph pg, BitSet iQ, int rankingStrategy) {
		Callable<?> c = () -> {
			try {
				new DurableMatching(lvg, pg, iQ, Config.CONTIGUOUS_MATCHES, rankingStrategy);
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
			return true;
		};
		return c;
	}

	/**
	 * Set callable Topk Durable query execution
	 * 
	 * @param lvg
	 * @param pg
	 * @param iQ
	 * @param rankingStrategy
	 * @return
	 */
	private static Callable<?> setCallableTopkQ(Graph lvg, PatternGraph pg, BitSet iQ, int rankingStrategy) {
		Callable<?> c = () -> {
			try {
				new DurableTopkMatching(lvg, pg, iQ, Config.CONTIGUOUS_MATCHES, Config.K, rankingStrategy);
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
			return true;
		};
		return c;
	}
}
