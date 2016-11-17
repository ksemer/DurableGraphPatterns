package graph.version.loader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import graph.version.Graph;
import graph.version.Node;
import system.Config;
import system.Main;

/**
 * Loader of YT graph
 * 
 * @author ksemer
 */
public class LoaderYT extends Loader {
	// when a label changes
	private int numberOfchanges;

	/**
	 * Constructor
	 * 
	 * @param numberOfchanges
	 */
	public LoaderYT(int numberOfchanges) {
		this.numberOfchanges = numberOfchanges;
	}

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

		if (Config.TINLA_ENABLED || Config.CTINLA_ENABLED) {
			Runtime runtime = null;
			long memory;

			// For displaying memory usage
			if (Config.SHOW_MEMORY) {
				runtime = Runtime.getRuntime();

				// Run the garbage collector
				runtime.gc();

				// Calculate the used memory
				memory = runtime.totalMemory() - runtime.freeMemory();

				if (Config.TINLA_ENABLED)
					System.out.println("Used memory is megabytes without TiNLa: " + Main.bytesToMegabytes(memory));
				else if (Config.CTINLA_ENABLED)
					System.out.println("Used memory is megabytes without CTiNLa: " + Main.bytesToMegabytes(memory));
			}

			createNeighborIndex(lvg);

			if (Config.SHOW_MEMORY) {
				// Run the garbage collector
				runtime.gc();
				// Calculate the used memory
				memory = runtime.totalMemory() - runtime.freeMemory();

				if (Config.TINLA_ENABLED)
					System.out.println("Used memory is megabytes with (TiNLa): " + Main.bytesToMegabytes(memory));				
				else if (Config.CTINLA_ENABLED)
					System.out.println("Used memory is megabytes with (CTiNLa): " + Main.bytesToMegabytes(memory));
			}
		}

		System.out.println("Loadtime of lvg(all): " + (System.currentTimeMillis() - executionTime) / 1000 + " (sec)");

		return lvg;
	}

	/**
	 * Load nodes attributes
	 * 
	 * @param vg
	 * @param numberOfchanges
	 * @throws IOException
	 */
	protected void loadAttributes(Graph lvg) throws IOException {
		System.out.println("Loading attributes in memory...");

		BufferedReader br = new BufferedReader(new FileReader(Config.PATH_LABELS));
		String line = null;
		String[] token, attributes;
		Node node;
		int value;
		long time = System.currentTimeMillis();

		while ((line = br.readLine()) != null) {

			token = line.split("\t");

			// has the attribute value per time instance of the interval
			attributes = token[1].split(",");

			// get node
			node = lvg.getNode(Integer.parseInt(token[0]));
			int pos = 0;

			for (int t = 0; t < Config.MAXIMUM_INTERVAL; t++) {
				value = Integer.parseInt(attributes[pos]);

				node.updateLabelLifetime(value, t);
				lvg.udpateTiLa(t, value, node);

				if ((t + 1) % numberOfchanges == 0)
					if (pos + 1 != attributes.length)
						pos++;
			}
		}
		br.close();

		System.out.println("TiLa time: " + (System.currentTimeMillis() - time) / 1000);
	}
}