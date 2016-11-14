package system.index;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import graph.pattern.PatternGraph;
import graph.pattern.PatternNode;
import system.Config;

/**
 * PatternPath Index class
 * @author ksemertz
 */
public class PatternPathIndex {
	//===============================================
	private Map<String, Set<PatternNode>> pathIndexWT;
	//temp variable
	private Map<Integer, boolean[]> hasBeenVisited;
	private int max_depth;

	// help variables
	private Set<PatternNode> set;
	//===============================================
	
	/**
	 * Constructor
	 * @param max_depth
	 * @throws Exception
	 */
	public PatternPathIndex(int max_depth) {
		this.max_depth = max_depth;
		this.pathIndexWT = new HashMap<>();
	}

	/**
	 * Create path index
	 * Return for each pattern node the set of paths started from it
	 * @param pg
	 * @return
	 */
	public Map<Integer, Set<String>> createPathIndex(PatternGraph pg) {
		System.out.println("Pattern Path Index is running");
		hasBeenVisited = new HashMap<>();

		for (int depth = max_depth; depth >= 1; depth--)
			for (PatternNode n : pg.getNodes()) {
				if (!hasBeenVisited.containsKey(n.getID()))
					hasBeenVisited.put(n.getID(), new boolean[pg.size()]);
				
				traversePath(n, depth);
			}
		
		// key -> pattern node id, 
		// value -> set of string which includes all the paths from pattern node
		Map<Integer, Set<String>> in = new HashMap<>();
		
		// initialize
		for (PatternNode p : pg.getNodes()) {
			in.put(p.getID(), new HashSet<>());
			
			if (Config.ISDIRECTED)
				// path size of 0 (contains only it self)
				if (p.getAdjacency().isEmpty())
					in.get(p.getID()).add("" + p.getLabel());
		}
		
		// iterate path index
		for (Entry<String, Set<PatternNode>> entry : pathIndexWT.entrySet()) {
		    String path = entry.getKey();
		    
		    // update in for each pattern node add the paths
		    for (PatternNode p : entry.getValue()) 
		    	in.get(p.getID()).add(path);	
		}
		
		// print for each pattern node its paths
		for (PatternNode p : pg.getNodes())
			System.out.println("PNodeID: " + p.getID() + "->" + in.get(p.getID()));
		
		return in;
	}
	
	/**
	 * TraversePath
	 * @param n
	 * @param max_depth
	 */
	private void traversePath(PatternNode n, int max_depth) {
		
		Deque<n_info> toBeVisited = new ArrayDeque<>();
		List<PatternNode> path;
		
		n_info info = new n_info(n, null, 0);
		toBeVisited.add(info);
		
		while (!toBeVisited.isEmpty()) {
			info = toBeVisited.poll();
			
			if (info.depth == max_depth) {
				path = new ArrayList<>();
					
				while (true) {
					if (info.father == null) {
						if (max_depth == this.max_depth)
							hasBeenVisited.get(n.getID())[path.get(1).getID()] = true;

						path.add(info.n);
						break;
					}	
						
					path.add(info.n);
					info = info.father;
				}
				
				Collections.reverse(path);
				
				// call recursive
				rec_labelComp(path, path.get(0), "", 0);
			
				continue;
			}
							
			for (PatternNode trg : info.n.getAdjacency()) {
				if (hasBeenVisited.get(n.getID())[trg.getID()])
					continue;
				
				if (info.father == null) {		
						toBeVisited.add(new n_info(trg, info, info.depth + 1));
				} else if (!info.father.n.equals(trg)) {
					toBeVisited.add(new n_info(trg, info, info.depth + 1));
				}
			}
		}
	}

	/**
	 * Recursive function
	 * @param path
	 * @param src
	 * @param label
	 * @param depth
	 */
	private void rec_labelComp(List<PatternNode> path, PatternNode src, String label, int depth) {
		PatternNode n = path.get(depth);

		for (int i = 0; i < Config.sizeOfLabels; i++) {
			if (n.getLabel() == i) {
				if (depth + 1 < path.size())
					rec_labelComp(path, src, label + "|" + i, depth + 1);
				else {		
					// i is the next label in path
					// we use integers to denote labels
					String Path = label + "|" + i;
	
					if ((set = pathIndexWT.get(Path)) == null) {
						set = new HashSet<>();
						pathIndexWT.put(Path, set);
					}
	
					set.add(src);
				}	
			}	
		}
	}

	class n_info {
		PatternNode n;
		n_info father;
		int depth;
		
		public n_info(PatternNode n, n_info father, int depth) {
			this.n = n;
			this.father = father;
			this.depth = depth;
		}
	}

}
