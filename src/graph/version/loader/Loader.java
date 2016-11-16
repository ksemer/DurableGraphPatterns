package graph.version.loader;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import graph.version.Edge;
import graph.version.Graph;
import graph.version.Node;
import system.Config;

/**
 * Loader abstract class
 * @author ksemer
 */
public abstract class Loader {
	
	abstract Graph loadDataset() throws IOException;
	protected abstract void loadAttributes(Graph lvg) throws IOException;
	
	/**
	 * Create TiNLa index
	 * @param lvg
	 */
	public static void createNeighborIndex(Graph lvg) {
		System.out.println("TiNLa(" + Config.TINLA_R + ") construction is starting...");
		Set<Node> nodes = null;

		long time = System.currentTimeMillis();
		Node trg;
		BitSet lifetime;

		// for all nodes
		for (Node n : lvg.getNodes()) {
			//TODO support more layers
			if (Config.TINLA_R == 2)
				nodes = new HashSet<>();

			// get adjacency
			for (Edge e : n.getAdjacency()) {
				trg = e.getTarget();
				
				//TODO support more layers
				if (Config.TINLA_R == 2)
					nodes.add(trg);

				// for the edge lifetime
				for (Iterator<Integer> it = e.getLifetime().stream().iterator(); it.hasNext();) {
					int t = it.next();
					
					//FIXME
					// check which label is active
//					for (int label = 0; label < Config.sizeOfLabels; label++)
					for (int label = 0; label < 8; label+=7)
						if ((lifetime = trg.getLabel(label)) != null && lifetime.get(t))
							n.updateTiNLa(0, label, t);
				}
			}
			
			//TODO support more layers
			if (Config.TINLA_R == 2) {
				for (Node n1 : nodes) {
					for (Edge e : n1.getAdjacency()) {
						trg = e.getTarget();
						
						for (Iterator<Integer> it = e.getLifetime().stream().iterator(); it.hasNext();) {
							int t = it.next();
							
							//FIXME
							// check which label is active
							for (int label = 0; label < 8; label+=7)
								if ((lifetime = trg.getLabel(label)) != null && lifetime.get(t))
									n.updateTiNLa(1, label, t);
						}
					}	
				}
			}
		}

		System.out.println("TiNLa(" + Config.TINLA_R + ") time: " + (System.currentTimeMillis() - time) / 1000 + " (sec)");
	}
	
	/**
	 * Create TiNLa_C index
	 * @param lvg
	 */
	public static void createNeighborCIndex(Graph lvg) {
		System.out.println("TiNLa_C(" + Config.TINLA_R + ") construction is starting...");
		Set<Node> nodes = null;

		long time = System.currentTimeMillis();
		Node trg;
		BitSet lifetime;

		// for all nodes
		for (Node n : lvg.getNodes()) {
			//TODO support more layers
			if (Config.TINLA_R == 2)
				nodes = new HashSet<>();

			// get adjacency
			for (Edge e : n.getAdjacency()) {
				trg = e.getTarget();
				
				//TODO support more layers
				if (Config.TINLA_R == 2)
					nodes.add(trg);

				// for the edge lifetime
				for (Iterator<Integer> it = e.getLifetime().stream().iterator(); it.hasNext();) {
					int t = it.next();
					
					//FIXME
					// check which label is active
					for (int label = 0; label < 8; label+=7)
						if ((lifetime = trg.getLabel(label)) != null && lifetime.get(t))
							n.updateTiNLa_C(0, label, t);
				}
			}
			
			//TODO support more layers
			if (Config.TINLA_R == 2) {
				for (Node n1 : nodes) {
					for (Edge e : n1.getAdjacency()) {
						trg = e.getTarget();
						
						for (Iterator<Integer> it = e.getLifetime().stream().iterator(); it.hasNext();) {
							int t = it.next();
							
							//FIXME
							// check which label is active
							for (int label = 0; label < 8; label+=7)
								if ((lifetime = trg.getLabel(label)) != null && lifetime.get(t))
									n.updateTiNLa_C(1, label, t);
						}
					}	
				}
			}
		}

		System.out.println("TiNLa_C(" + Config.TINLA_R + ") time: " + (System.currentTimeMillis() - time) / 1000 + " (sec)");		
	}
}
