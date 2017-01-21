package graph.version.loader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import graph.version.Graph;
import graph.version.Node;
import system.Config;
import utils.Storage;

/**
 * Loader of YT graph
 * 
 * @author ksemer
 */
public class LoaderYT {
	// when a label change value
	private static final int numberOfchanges = 9;

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
		String[] edge, time;
		int n1_id, n2_id;
		long executionTime = System.currentTimeMillis();

		Graph lvg = new Graph();

		br.readLine();
		while ((line = br.readLine()) != null) {
			edge = line.split("\t");
			n1_id = Integer.parseInt(edge[0]);
			n2_id = Integer.parseInt(edge[1]);

			lvg.addNode(n1_id);
			lvg.addNode(n2_id);

			// edge[2] has the year/time
			time = edge[2].split(",");
			for (int t = Integer.parseInt(time[0]); t <= Integer.parseInt(time[1]); t++) {
				lvg.addEdge(n1_id, n2_id, t);

				if (!Config.ISDIRECTED)
					// src -> trg time label
					lvg.addEdge(n2_id, n1_id, t);
			}
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

		System.out.println("ViLa time: " + (System.currentTimeMillis() - executionTime) / 1000 + " (sec)");

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
		String[] token, attributes;
		Node node;
		int label;
		Set<Integer> labels = new HashSet<>();

		while ((line = br.readLine()) != null) {

			token = line.split("\t");

			// has the attribute value per time instance of the interval
			attributes = token[1].split(",");

			// get node
			node = lvg.getNode(Integer.parseInt(token[0]));
			int pos = 0;

			for (int t = 0; t < Config.MAXIMUM_INTERVAL; t++) {
				label = Integer.parseInt(attributes[pos]);

				node.updateLabelLifespan(label, t);
				lvg.udpateTiLa(t, label, node);

				if ((t + 1) % numberOfchanges == 0)
					if (pos + 1 != attributes.length)
						pos++;
			}
		}
		br.close();

		Config.SIZE_OF_LABELS = labels.size();
	}
}