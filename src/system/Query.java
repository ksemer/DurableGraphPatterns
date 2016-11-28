package system;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import algorithm.DurableMatching;
import algorithm.DurableTopkMatching;
import graph.pattern.PatternGraph;
import graph.version.Graph;

/**
 * Query class
 * 
 * @author ksemer
 */
public class Query {
	// =================================================================
	private Graph lvg;
	private BitSet iQ;
	// =================================================================

	/**
	 * Constructor
	 * 
	 * @param iQ
	 * @param lvg
	 * 
	 * @throws IOException
	 */
	public Query(Graph lvg, BitSet iQ) throws IOException {
		this.lvg = lvg;
		this.iQ = iQ;
	}

	public void run() throws Exception {
		String[] edge;
		PatternGraph pg = null;
		String line = null;
		boolean nodes = false;
		int sizeOfNodes = 0, id = 0, n1, n2;
		
		ExecutorService executor = Executors.newCachedThreadPool();

		BufferedReader br = new BufferedReader(new FileReader(Config.PATH_QUERY));

		while ((line = br.readLine()) != null) {
			
			if (line.contains("--") && pg != null) {
				
				if (Config.RUN_DURABLE_QUERIES) {
					
					List<Callable<?>> callables = new ArrayList<>();
					
					if (Config.MAX_RANKING_ENABLED)					
						callables.add(setCallableDurQ(lvg, pg, iQ, Config.MAX_RANKING));
					
					if (Config.HALFWAY_RANKING_ENABLED)
						callables.add(setCallableDurQ(lvg, pg, iQ, Config.HALFWAY_RANKING));
				
					if (Config.ZERO_RANKING_ENABLED)
						callables.add(setCallableDurQ(lvg, pg, iQ, Config.ZERO_RANKING));			

					for (Callable<?> c : callables)
						executor.submit(c);
				}
				
				if (Config.RUN_TOPK_QUERIES) {
					List<Callable<?>> callables = new ArrayList<>();

					if (Config.MAX_RANKING_ENABLED)					
						callables.add(setCallableTopkQ(lvg, pg, iQ, Config.MAX_RANKING));
					
					if (Config.HALFWAY_RANKING_ENABLED)
						callables.add(setCallableTopkQ(lvg, pg, iQ, Config.HALFWAY_RANKING));
				
					if (Config.ZERO_RANKING_ENABLED)
						callables.add(setCallableTopkQ(lvg, pg, iQ, Config.ZERO_RANKING));			

					for (Callable<?> c : callables)
						executor.submit(c);					
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
		
		executor.shutdown();
	}
	
	/**
	 * Set callable Durable query execution
	 * @param lvg
	 * @param pg
	 * @param iQ
	 * @param rankingStrategy
	 * @return
	 */
	private Callable<?> setCallableDurQ(Graph lvg, PatternGraph pg, BitSet iQ, int rankingStrategy) {
		Callable<?> c = () -> {
			new DurableMatching(lvg, pg, iQ, Config.CONTIGUOUS_MATCHES, rankingStrategy);
			return true;
		};
		return c;
	}
	
	/**
	 * Set callable Topk Durable query execution
	 * @param lvg
	 * @param pg
	 * @param iQ
	 * @param rankingStrategy
	 * @return
	 */
	private Callable<?> setCallableTopkQ(Graph lvg, PatternGraph pg, BitSet iQ, int rankingStrategy) {
		Callable<?> c = () -> {
			new DurableTopkMatching(lvg, pg, iQ, Config.CONTIGUOUS_MATCHES, Config.K, rankingStrategy);
			return true;
		};
		return c;
	}
}