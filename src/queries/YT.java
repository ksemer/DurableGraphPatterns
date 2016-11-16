package queries;

import java.util.BitSet;

import algorithm.indexes.TimePathIndex;
import graph.version.loader.LoaderYT;
import system.Config;

/**
 * YT query
 * @author ksemer
 */
public class YT extends Query {
	
	public void run() throws Exception {
		Config.sizeOfLabels = 10;
		Config.PATH_DATASET = "/experiments/files/yt_graph";
		Config.PATH_LABELS = "/experiments/files/yt_label_" + Config.sizeOfLabels;
		Config.ISDIRECTED = true;
		Config.MAXIMUM_INTERVAL = 37;
		int numberOfChanges = 9;
		iQ = new BitSet(Config.MAXIMUM_INTERVAL);
		iQ.set(0, Config.MAXIMUM_INTERVAL, true);
	    
		lvg = new LoaderYT(numberOfChanges).loadDataset();
		
		if (Config.SHOW_MEMORY)
			getMemory("LVG");
		
		if (!Config.TILA_ENABLED) {
			lvg.getTiLa().clear();			
			
			if (Config.SHOW_MEMORY)
				getMemory("without TiLaL");
		}
		
		if (Config.TIPLA_ENABLED) {
			TiPLa = new TimePathIndex(2).createPathIndex(lvg);
			
			if (Config.SHOW_MEMORY)
				getMemory("TiPLa");		
		}

		if (Config.RUN_CLIQUES) 
			runCliqueYT();
		
		if (Config.RUN_RANDOM)
			runRandom();
	}
}