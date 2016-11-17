package queries;

import java.io.FileWriter;
import java.util.BitSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import algorithm.DurableMatching;
import algorithm.DurableMatchingPath;
import graph.pattern.PatternGraph;
import graph.pattern.PatternNode;
import graph.version.Graph;
import graph.version.Node;
import system.Config;
import system.Main;

/**
 * Query abstract class
 * @author ksemer
 */
public abstract class Query {
	//=================================================================
	protected Graph lvg;
	private PatternGraph pg;
	protected BitSet iQ;
	public Map<Integer, Map<String, Set<Node>>> TiPLa;

	// used when the results are printed to know the label and query size
	public static int label;
	public static int size;
	private Runtime runtime;
	
	private FileWriter resultW, resultPerIt;
	private double[] TIME_TiLa;
	private double[] TIME_TiNLa1;
	private double[] TIME_TiNLa2;
	private double[] TIME_TiPLa;
	private int[] DURATION;
	private int[] MATCHES;
	public static boolean DEGREE;
	//=================================================================
	
	/**
	 * Constructor
	 */
	public Query() {
		runtime = Runtime.getRuntime();
	}
	
	/**
	 * Clique query method
	 * @throws Exception
	 */
	public void runClique() throws Exception {

		for (label = Config.SIZE_OF_LABELS - 1; label >= 0; label--) {
			System.out.println("Label clique: " + label);
			for (size = 2; size < Config.QUERY_SIZE; size++) {
				System.out.println("Clique size: " + size);

				pg = new PatternGraph();

				// create node with label
				for (int j = 0; j < size; j++)
					pg.addNode(j, label);

				// create edges
				for (int k = 0; k < size; k++) {
					for (int q = k + 1; q < size; q++) {
						pg.addEdge(k, q);
						pg.addEdge(q, k);
					}
				}
				
				// call durable matching algorithms
				if (TiPLa != null) {
					new DurableMatchingPath(lvg, pg, iQ, true, TiPLa);
				} else if (lvg.getTiLa() != null) {
					//TiNLa (r = 1)
					new DurableMatching(lvg, pg, iQ, null, true, Main.TiNLa){};
				}
			}
			
			if (label == 3)
				label = 1;
			else
				break;
		}
	}
	
	public void runCliqueYT() throws Exception {
		label = 0;

		for (;;) {
			System.out.println("Label clique: " + label);

			for (size = 2; size <= Config.QUERY_SIZE; size++) {
				System.out.println("Clique size: " + size);

				pg = new PatternGraph();

				// create node with label
				for (int j = 0; j < size; j++)
					pg.addNode(j, label);

				// create edges
				for (int k = 0; k < size; k++) {
					for (int q = k + 1; q < size; q++) {
						pg.addEdge(k, q);
						pg.addEdge(q, k);
					}
				}
				
				// call durable matching algorithms
				if (TiPLa != null) {
					new DurableMatchingPath(lvg, pg, iQ, false, TiPLa);
				} else if (lvg.getTiLa() != null) {
					//TiNLa (r = 1)
					new DurableMatching(lvg, pg, iQ, null, false, Main.TiNLa){};
				}
			}
			
			if (label == 0)
				label = 7;
			else
				break;
		}		
	}
	
	/**
	 * Run random queries
	 * @throws Exception
	 */
	public void runRandom() throws Exception {
		resultW = new FileWriter("Random_final_result");
		resultPerIt = new FileWriter("Random_iterations_result");
		resultPerIt.write("Query Size\tTiLa (ms)\tTiNLa(1) (ms)\tTiNLa(2)\tTiPLa (ms)\tduration\tmatches\n");
		resultW.write("Query Size\tTiLa (ms)\tTiNLa (ms)\tTiPLa (ms)\tduration\tmatches\n");
		TIME_TiLa = new double[Config.QUERY_SIZE];
		TIME_TiNLa1 = new double[Config.QUERY_SIZE];
		TIME_TiNLa2 = new double[Config.QUERY_SIZE];
		TIME_TiPLa = new double[Config.QUERY_SIZE];
		MATCHES = new int[Config.QUERY_SIZE];
		DURATION = new int[Config.QUERY_SIZE];
		
		DurableMatching dm;
		DurableMatchingPath dmp;
		PatternGraph pg;

		for (int i = 0; i < Config.RANDOM_ITERATIONS; i++) {
			Random r = new Random();
			System.out.println("ITERATION: " + (i+1));

			for (int size = 2; size <= Config.QUERY_SIZE; size++) {

				int n = r.nextInt(lvg.size() - 1);
				Node node = lvg.getNode(n);
				
				// node do not exist search for the same size
				if (node == null) {
					size--;
				} else if (queryGenerator.dfs(node, size)) {
					// get random query as a pattern graph
					pg = queryGenerator.getQuery(size);

					Config.TINLA_R = 1;
					dm = new DurableMatching(lvg, pg, iQ, null, false, false){};
					TIME_TiNLa1[size - 2]+= dm.getTotalExecutionTime();
					resultPerIt.write(size + "\t" + dm.getTotalExecutionTime() + "\t");

					Config.TINLA_R = 2;
					dm = new DurableMatching(lvg, pg, iQ, null, false, false){};
					TIME_TiNLa2[size - 2]+= dm.getTotalExecutionTime();
					resultPerIt.write(dm.getTotalExecutionTime() +"\t");

					resultPerIt.write(dm.getMaxDuration() + "\t" + dm.getMatches().size() + "\n");
					
					resultPerIt.flush();
				}
			}
		}
		
		// write the results
		for (int size = 2; size <= Config.QUERY_SIZE; size++) {
			resultW.write(size + "\t" + TIME_TiLa[size - 2] / Config.RANDOM_ITERATIONS + "\t"
					+ TIME_TiNLa1[size - 2] / Config.RANDOM_ITERATIONS + "\t"
					+ TIME_TiNLa2[size - 2] / Config.RANDOM_ITERATIONS + "\t"
					+ TIME_TiPLa[size - 2] / Config.RANDOM_ITERATIONS + "\t"
					+ DURATION[size - 2] / Config.RANDOM_ITERATIONS + "\t"
					+ MATCHES[size - 2] / Config.RANDOM_ITERATIONS + "\n");
		}

		resultW.close();
		resultPerIt.close();
	}
	
	public void runStar1() throws Exception {
		DEGREE = false;
		
		while (true) {
			System.out.println("Size: " + Config.QUERY_SIZE);
			
			pg = new PatternGraph();
	
			for (int i = 0; i < Config.QUERY_SIZE; i++) {
				// create node with label
				pg.addNode(i, 0);
			}
			
			for (PatternNode p : pg.getNodes())
				System.out.println(p.getID());
			
			pg.addNode(Config.QUERY_SIZE, 3);
			
			// create edges
			for (int k = 0; k < Config.QUERY_SIZE; k++) {
				pg.addEdge(k, Config.QUERY_SIZE);
				pg.addEdge(Config.QUERY_SIZE, k);
			}
	
			if (TiPLa == null) {

				// call durable matching algorithms
				if (Config.TINLA_ENABLED) {
					new DurableMatching(lvg, pg, iQ, null, true, true){};
				}
			
				if (new DurableMatching(lvg, pg, iQ, null, true, false){}.getMatches().size() == 0) {
					break;
				}
			} else {
				if (new DurableMatchingPath(lvg, pg, iQ, true, TiPLa).getMatches().size() == 0)
					break;
			}
			
			if (Config.QUERY_SIZE == 8)
				System.exit(0);
			
			Config.QUERY_SIZE++;
		}
	}
	
	public void runStar() throws Exception {
		DEGREE = true;
		
		while (true) {
			System.out.println("Size: " + Config.QUERY_SIZE);
			
			pg = new PatternGraph();
			pg.addNode(0, 3);

			for (int i = 1; i <= Config.QUERY_SIZE; i++) {
				// create node with label
				pg.addNode(i, 0);
			}
			
			for (PatternNode p : pg.getNodes())
				System.out.println(p.getID());
			
			
			// create edges
			for (int k = 1; k <= Config.QUERY_SIZE; k++) {
				pg.addEdge(k, 0);
				pg.addEdge(0, k);
			}
	
			if (TiPLa == null) {

				// call durable matching algorithms
				if (Config.TINLA_ENABLED) {
					new DurableMatching(lvg, pg, iQ, null, true, true){};
				}
			
				if (new DurableMatching(lvg, pg, iQ, null, true, false){}.getMatches().size() == 0) {
					break;
				}
			} else {
				if (new DurableMatchingPath(lvg, pg, iQ, true, TiPLa).getMatches().size() == 0)
					break;
			}
			
			if (Config.QUERY_SIZE == 8)
				System.exit(0);
			
			Config.QUERY_SIZE++;
		}
	}
	
	/**
	 * Show system's memory occupation
	 * @param text
	 */
	protected void getMemory(String text) {
		
		// Run the garbage collector
		runtime.gc();
		
    	// Calculate the used memory
    	long memory = runtime.totalMemory() - runtime.freeMemory();
    	System.out.println("Used memory is megabytes of (" + text + ") : " + Main.bytesToMegabytes(memory));
	}
}