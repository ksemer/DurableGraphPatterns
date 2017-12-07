package graph.version.loader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import graph.version.Graph;
import graph.version.Node;
import system.Config;
import utils.Storage;

/**
 * Loader of Wiki graph
 * 
 * @author ksemer
 */
public class LoaderWikipedia {
	private Map<Integer, Integer> n_min = new HashMap<>();
	private Map<Integer, Integer> n_max = new HashMap<>();

	/**
	 * Create a labeled version graph in memory from a given DataSet nodeID \t
	 * nodeID \t time
	 * 
	 * @throws IOException
	 */
	public Graph loadDataset() throws IOException {

		System.out.println("Creating Labeled Version Graph...");
		BufferedReader br = new BufferedReader(new FileReader(Config.PATH_DATASET));
		String line = null;
		String[] edge;
		int n1_id, n2_id, time;
		long executionTime = System.currentTimeMillis();

		Graph lvg = new Graph();

		br.readLine();
		while ((line = br.readLine()) != null) {
			edge = line.split("\t");
			n1_id = Integer.parseInt(edge[0]);
			n2_id = Integer.parseInt(edge[1]);

			// edge[2] has the year/time
			time = Integer.parseInt(edge[2]);

			if (n_min.containsKey(n1_id)) {
				if (time < n_min.get(n1_id)) {
					n_min.put(n1_id, time);
				}

				if (time > n_max.get(n1_id)) {
					n_max.put(n1_id, time);
				}
			} else {
				n_min.put(n1_id, time);
				n_max.put(n1_id, time);
			}

			if (n_min.containsKey(n2_id)) {
				if (time < n_min.get(n2_id)) {
					n_min.put(n2_id, time);
				}

				if (time > n_max.get(n2_id)) {
					n_max.put(n2_id, time);
				}
			} else {
				n_min.put(n2_id, time);
				n_max.put(n2_id, time);
			}

			lvg.addNode(n1_id);
			lvg.addNode(n2_id);

			lvg.addEdge(n1_id, n2_id, time, Config.MAXIMUM_INTERVAL);
			// lvg.addEdge(n1_id, n2_id, time);

			if (!Config.ISDIRECTED)
				// src -> trg time label
				// lvg.addEdge(n2_id, n1_id, time);
				lvg.addEdge(n1_id, n2_id, time, Config.MAXIMUM_INTERVAL);
		}
		br.close();

		// load attributes
		loadAttributes(lvg);

		// For displaying memory usage
		if (Config.SHOW_MEMORY) {
			Runtime runtime = Runtime.getRuntime();

			// Run the garbage collector
			runtime.gc();

			// Calculate the used memory
			long memory = runtime.totalMemory() - runtime.freeMemory();
			System.out.println("Used memory with ViLa: " + Storage.bytesToMegabytes(memory));
		}

		System.out.println("ViLa time: " + (System.currentTimeMillis() - executionTime) + " (ms)");

		if (Config.TINLA_ENABLED || Config.CTINLA_ENABLED)
			lvg.createTimeNeighborIndex();
		else if (Config.TIPLA_ENABLED)
			lvg.createTiPLa();

		System.out.println("Loadtime of all: " + (System.currentTimeMillis() - executionTime) + " (ms)");

		return lvg;
	}

	/**
	 * Load nodes attributes
	 * 
	 * @param vg
	 * @param numberOfchanges
	 * @throws IOException
	 */
	private void loadAttributes(Graph lvg) throws IOException {
		System.out.println("Loading attributes in memory...");

		BufferedReader br = new BufferedReader(new FileReader(Config.PATH_LABELS));
		String line = null;
		String[] token;
		Node node;
		int label;
		Set<Integer> labels = new HashSet<>();

		while ((line = br.readLine()) != null) {

			token = line.split("\\s++");

			// has the attribute value per time instance of the interval
			label = Integer.parseInt(token[1]);
			labels.add(label);

			// get node
			node = lvg.getNode(Integer.parseInt(token[0]));

			if (node == null)
				continue;

			int firstTime = n_min.get(node.getID());

			int range = firstTime + (int) (firstTime * 0.2);

			if (range > Config.MAXIMUM_INTERVAL)
				range = Config.MAXIMUM_INTERVAL;

			if (range > n_max.get(node.getID()))
				range = n_max.get(node.getID());

			node.updateLabelLifespan(label, firstTime, range);
			lvg.udpateTiLa(firstTime, range, label, node);
		}
		br.close();

		Config.SIZE_OF_LABELS = labels.size();
	}
}