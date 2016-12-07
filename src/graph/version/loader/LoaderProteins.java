package graph.version.loader;

import system.Config;
import utils.Storage;

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
public class LoaderProteins {
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
		int n1, n2, lID = 0, time = -1, label;
		long executionTime = System.currentTimeMillis();
		boolean nodes = false, edges = false;
		int sizeOfNodes = 0, nID = 0, numberOfInstances = 0;

		while ((line = br.readLine()) != null) {
			// new graph instance
			if (line.contains("#")) {
				numberOfInstances++;
			}
		}
		br.close();

		Config.MAXIMUM_INTERVAL = numberOfInstances;
		System.out.println("Max Interval: " + Config.MAXIMUM_INTERVAL);

		Graph lvg = new Graph();

		br = new BufferedReader(new FileReader(Config.PATH_DATASET));
		while ((line = br.readLine()) != null) {
			// new graph instance
			if (line.contains("#")) {
				time++;
				sizeOfNodes = Integer.parseInt(br.readLine());
				nID = 0;
				nodes = true;
				edges = false;
				continue;
			}

			if (nodes) {
				lvg.addNode(nID);
				node = lvg.getNode(nID++);

				line = line.trim();

				if (!labels.containsKey(line)) {
					label = lID;
					labels.put(line, lID++);
				} else
					label = labels.get(line);

				node.updateLabelLifespan(label, time);
				lvg.udpateTiLa(time, label, node);

				sizeOfNodes--;

				if (sizeOfNodes == 0) {
					nodes = false;
					edges = true;
					continue;
				}
			} else if (edges) {
				// edge
				edge = line.split("\\s+");

				if (edge.length < 2)
					continue;

				// src node
				n1 = Integer.parseInt(edge[0]);

				// trg node
				n2 = Integer.parseInt(edge[1]);

				// src -> trg time label
				lvg.addEdge(n1, n2, time);

				if (!Config.ISDIRECTED)
					// trg -> src time label
					lvg.addEdge(n2, n1, time);
			}
		}
		br.close();

		Config.SIZE_OF_LABELS = labels.size();
		System.out.println("Labels: " + labels.size());

		System.out.println("TiLa time: " + (System.currentTimeMillis() - executionTime) + " (ms)");

		long memory;
		Runtime runtime = Runtime.getRuntime();

		if (Config.TINLA_ENABLED || Config.CTINLA_ENABLED) {

			// For displaying memory usage
			if (Config.SHOW_MEMORY) {

				// Run the garbage collector
				runtime.gc();

				// Calculate the used memory
				memory = runtime.totalMemory() - runtime.freeMemory();

				if (Config.TINLA_ENABLED)
					System.out.println("Used memory without (TiNLa): " + Storage.bytesToMegabytes(memory));
				else if (Config.CTINLA_ENABLED)
					System.out.println("Used memory without (CTiNLa): " + Storage.bytesToMegabytes(memory));
			}

			lvg.createTimeNeighborIndex();

			if (Config.SHOW_MEMORY) {

				// Run the garbage collector
				runtime.gc();

				// Calculate the used memory
				memory = runtime.totalMemory() - runtime.freeMemory();

				if (Config.TINLA_ENABLED)
					System.out.println("Used memory with (TiNLa): " + Storage.bytesToMegabytes(memory));
				else if (Config.CTINLA_ENABLED)
					System.out.println("Used memory with (CTiNLa): " + Storage.bytesToMegabytes(memory));
			}
		} else if (Config.TIPLA_ENABLED) {

			if (Config.SHOW_MEMORY) {
				// Run the garbage collector
				runtime.gc();

				// Calculate the used memory
				memory = runtime.totalMemory() - runtime.freeMemory();

				System.out.println("Used memory without (TiPLa): " + Storage.bytesToMegabytes(memory));
			}

			lvg.createTiPLa();

			if (Config.SHOW_MEMORY) {

				// Run the garbage collector
				runtime.gc();

				// Calculate the used memory
				memory = runtime.totalMemory() - runtime.freeMemory();

				System.out.println("Used memory with (TiPLa): " + Storage.bytesToMegabytes(memory));
			}
		}

		System.out.println("Loadtime of lvg(all): " + (System.currentTimeMillis() - executionTime) + " (ms)");

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
}