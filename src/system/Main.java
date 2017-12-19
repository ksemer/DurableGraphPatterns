package system;

import java.util.BitSet;

import graph.version.Graph;
import graph.version.loader.LoaderDBLP;
import graph.version.loader.LoaderProteins;
import graph.version.loader.LoaderWikipedia;
import graph.version.loader.LoaderYT;

/**
 * Main class
 * 
 * @author ksemer
 */
public class Main {

	/**
	 * Main
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Config.loadConfigs();
		Graph lvg;

		String dataset = Config.PATH_DATASET.toLowerCase();

		// for dblp dataset
		if (dataset.contains("dblp"))
			lvg = new LoaderDBLP().loadDataset();
		// for yt dataset
		else if (dataset.contains("yt"))
			lvg = new LoaderYT().loadDataset();
		else if (dataset.contains("wiki"))
			lvg = new LoaderWikipedia().loadDataset();
		// for proteins
		else
			lvg = new LoaderProteins().loadDataset();

		if (Config.RUN_DURABLE_QUERIES || Config.RUN_TOPK_QUERIES) {

			BitSet iQ = new BitSet(Config.MAXIMUM_INTERVAL);
			iQ.set(0, Config.MAXIMUM_INTERVAL, true);

			new Query(lvg, iQ).run();
		}
	}
}