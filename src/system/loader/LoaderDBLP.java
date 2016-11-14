package system.loader;

import graph.version.Node;
import system.Config;
import system.Main;
import graph.version.LVGraph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loader of DBLP graph
 * @author ksemer
 */
public class LoaderDBLP extends Loader {
	//=================================================================
	private final int BEGINNER = 2;
	private final int JUNIOR = 5;
	private final int SENIOR = 10;
	private final int PROF = 11;
	private static Map<Integer, String> authorsNames;
	//=================================================================

	/**
	 * Create a labeled version graph in memory from a given DataSet
	 * nodeID \t nodeID \t time
	 * @throws IOException
	 */
	public LVGraph loadDataset() throws IOException {

		System.out.println("Creating Labeled Version Graph...");
		BufferedReader br = new BufferedReader(new FileReader(Config.PATH_DATASET));      
		String line = null;
		String[] edge;
		int n1_id, n2_id, time;
		long executionTime = System.currentTimeMillis();

		LVGraph lvg = new LVGraph(Config.sizeOfNodes);

		while ((line = br.readLine()) != null) {
			edge = line.split("\t");
			n1_id = Integer.parseInt(edge[0]);
			n2_id = Integer.parseInt(edge[1]);
			
			lvg.addNode(n1_id);
			lvg.addNode(n2_id);
			
			// edge[2] has the year/time
			time = convert(edge[2]);
			
			// src -> trg time label
			lvg.addEdge(n1_id, n2_id, time);
			
			if (!Config.ISDIRECTED)
				// src -> trg time label
				lvg.addEdge(n2_id, n1_id, time);
		}
		br.close();
		
		// load attributes
		loadAttributes(lvg);
		loadNames(lvg.size());

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
		
		System.out.println("Loadtime of lvg(all): " + (System.currentTimeMillis() - executionTime)/ 1000 + " (sec)");

		return lvg;
	}

	/**
	 * Load nodes attributes
	 * @param lvg 
	 * @throws IOException
	 */
	protected void loadAttributes(LVGraph lvg) throws IOException {
		System.out.println("Loading labels...");

		BufferedReader br = new BufferedReader(new FileReader(Config.PATH_LABELS));      
		String line = null;
		String[] token, attributes;
		Node node;
		int value;
		long time = System.currentTimeMillis();

		while ((line = br.readLine()) != null) {

			token = line.split("\\|");

			// has the attribute value per time instance of the interval
			attributes = token[1].split(",");

			// get node
			node = lvg.getNode(Integer.parseInt(token[0]));

			// author without co-authors in a paper
			if (node == null)
				continue;

			for (int t = 0; t < attributes.length; t++) {
				value = Integer.parseInt(attributes[t]);

				if (value <= BEGINNER) {
					node.updateLabelLifetime(0, t);
					lvg.udpateTiLa(t, 0, node);
				} else if (value <= JUNIOR) {
					node.updateLabelLifetime(1, t);
					lvg.udpateTiLa(t, 1, node);
				} else if (value <= SENIOR) {
					node.updateLabelLifetime(2, t);
					lvg.udpateTiLa(t, 2, node);
				} else if (value >= PROF) {
					node.updateLabelLifetime(3, t);
					lvg.udpateTiLa(t, 3, node);
				}
			}
		}
		br.close();

		System.out.println("TiLa time: " + (System.currentTimeMillis() - time) / 1000);
	}
	
	
	/**
	 * Load DBLP authors names
	 * @param size
	 * @throws IOException
	 */
	private static void loadNames(int size) throws IOException {
		System.out.println("Loading authors names in memory...");
		
		BufferedReader br = new BufferedReader(new FileReader("/experiments/files/DBLP_Authors_MAP"));      
		String line = null;
		String[] token;

		authorsNames = new HashMap<>(size);

		while ((line = br.readLine()) != null) {
			token = line.split("\t");
			authorsNames.put(Integer.parseInt(token[1]), token[0]);
		}
		br.close();
	}
	
	/**
	 * Return map of authors id-->name
	 * @return
	 */
	public static Map<Integer, String> getAuthors() {
		return authorsNames;
	}
	
	/**
	 * Convert a string time format to an integer value that it will be used in a BitSet
	 * @param time
	 * @return
	 */
	private static int convert(String time) {
		return (Integer.parseInt(time) - 1959);
	}
}