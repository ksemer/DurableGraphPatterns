package system.loader;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import graph.version.Edge;
import graph.version.LVGraph;
import graph.version.Node;
import system.Config;

/**
 * Loader abstract class
 * @author ksemer
 */
public abstract class Loader {
	
	abstract LVGraph loadDataset() throws IOException;
	protected abstract void loadAttributes(LVGraph lvg) throws IOException;
	
	/**
	 * Create TiNLa index
	 * @param lvg
	 */
	public static void createNeighborIndex(LVGraph lvg) {
		System.out.println("TiNLa(" + Config.TINLA_R + ") construction is starting...");
		
		long time = System.currentTimeMillis();
		Node trg;
		BitSet lifetime;
		Set<Node> nodes = new HashSet<>(), nodes_toCheck = null;

		// for all nodes
		for (Node n : lvg.getNodes()) {
				
			nodes.add(n);
			
			if (Config.TINLA_R > 1)
				nodes_toCheck = new HashSet<>();
			
			// for each layer
			for (int layer = 0; layer < Config.TINLA_R; layer++) {
				
				// get nodes to be checked
				for (Node n1 : nodes) {
					// check adjacency
					for (Edge e : n1.getAdjacency()) {
						trg = e.getTarget();
						
						// for the edge lifetime
						for (Iterator<Integer> it = e.getLifetime().stream().iterator(); it.hasNext();) {
							int t = it.next();
							
							// check which label is active
							//for (int label = 0; label < Config.sizeOfLabels; label++)
							for (int label = 0; label < Config.sizeOfLabels; label++)
								if ((lifetime = trg.getLabel(label)) != null && lifetime.get(t))
									n.updateTiNLa(layer, label, t);
						}
						
						// if there is a further layer
						if (layer + 1 < Config.TINLA_R)
							nodes_toCheck.add(trg);
					}
				}
			
				// update which nodes need to be checked
				if (layer + 1 < Config.TINLA_R) {
					nodes = new HashSet<>(nodes_toCheck);
					nodes_toCheck.clear();
				}
			}
			
			nodes.clear();
		}

		System.out.println("TiNLa(" + Config.TINLA_R + ") time: " + (System.currentTimeMillis() - time) / 1000 + " (sec)");
	}
}
