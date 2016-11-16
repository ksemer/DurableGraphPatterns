package graph.version.loader;

import system.Config;
import system.Main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import graph.version.Graph;
import graph.version.Node;

/**
 * Loader of Proteins graph Datasets from Performance and Scalability of Indexed
 * Subgraph Query Processing Methods
 * 
 * @author ksemer
 */
public class LoaderProteins extends Loader {
	// =================================================================
	private Map<String, Integer> labels = new HashMap<>();
	// =================================================================

	/**
	 * Create a labeled version graph in memory from a given DataSet nodeID \t
	 * nodeID \t time and TiLa index
	 * 
	 * @throws IOException
	 */
	public Graph loadDataset() throws IOException {

		System.out.println("Creating Labeled Version Graph...");
		BufferedReader br = new BufferedReader(new FileReader(Config.PATH_DATASET));
		String line = null;
		String[] edge;
		Node node;
		int n1, n2, lID = 0, time = -1;
		long executionTime = System.currentTimeMillis();

		Graph lvg = new Graph();

		boolean nodes = false, edges = false;
		int sizeOfNodes = 0, nID = 0;

		while ((line = br.readLine()) != null) {
			// new graph instance
			if (line.contains("#")) {
				time++;
				sizeOfNodes = Integer.parseInt(br.readLine());
				nodes = true;
				edges = false;
				continue;
			}

			if (nodes) {
				lvg.addNode(nID);
				node = lvg.getNode(nID++);

				line = line.trim();

				if (!labels.containsKey(line)) {
					labels.put(line, lID++);
				}

				node.updateLabelLifetime(0, time);
				lvg.udpateTiLa(time, 0, node);

				sizeOfNodes--;

				if (sizeOfNodes == 0) {
					nodes = false;
					edges = true;
					continue;
				}
			} else if (edges) {
				// edge
				edge = line.split("\\s+");

				// src node
				n1 = Integer.parseInt(edge[0]);

				// trg node
				n2 = Integer.parseInt(edge[1]);

				// src -> trg time label
				lvg.addEdge(n1, n2, time);

				// trg -> src time label
				lvg.addEdge(n2, n1, time);
			}
		}
		br.close();

		System.out.println("TiLa time: " + (System.currentTimeMillis() - time) / 1000);

		// For displaying memory usage
		if (Config.TINLA_ENABLED || Config.TINLA_C_ENABLED) {
			Runtime runtime = null;
			long memory;

			if (Config.SHOW_MEMORY) {
				runtime = Runtime.getRuntime();

				// Run the garbage collector
				runtime.gc();

				// Calculate the used memory
				memory = runtime.totalMemory() - runtime.freeMemory();

				if (Config.TINLA_ENABLED)
					System.out.println("Used memory is megabytes without TiNLa: " + Main.bytesToMegabytes(memory));
				else
					System.out.println("Used memory is megabytes without TiNLa_C: " + Main.bytesToMegabytes(memory));
			}

			if (Config.TINLA_ENABLED)
				createNeighborIndex(lvg);
			else if (Config.TINLA_C_ENABLED)
				createNeighborCIndex(lvg);

			if (Config.SHOW_MEMORY) {
				// Run the garbage collector
				runtime.gc();
				// Calculate the used memory
				memory = runtime.totalMemory() - runtime.freeMemory();

				if (Config.TINLA_ENABLED)
					System.out.println("Used memory is megabytes with (TiNLa): " + Main.bytesToMegabytes(memory));
				else if (Config.TINLA_C_ENABLED)
					System.out.println("Used memory is megabytes with (TiNLa_C): " + Main.bytesToMegabytes(memory));
			}
		}

		System.out.println("Loadtime of lvg(all): " + (System.currentTimeMillis() - executionTime) / 1000 + " (sec)");

		return lvg;
	}

	/**
	 * Return labels structure string -> id
	 * 
	 * @return
	 */
	public Map<String, Integer> getLabels() {
		return this.labels;
	}

	@Override
	protected void loadAttributes(Graph lvg) throws IOException {
		// implemented in LoadDataset() method
	}
}