package system;

import java.util.BitSet;
import java.util.Map;

import graph.version.Graph;
import graph.version.loader.LoaderDBLP;
import graph.version.loader.LoaderProteins;
import graph.version.loader.LoaderYT;
import utils.Storage;

/**
 * Main class
 * 
 * @author ksemer
 */
public class Main {
	// =================================================================
	public static long TIME;
	private static final long MEGABYTE = 1024L * 1024L;
	// =================================================================

	/**
	 * Main
	 * 
	 * @param args
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		Config.loadConfigs();
		//TODO na enhmerwnw iQ otan kanw load to arxeio
		// na koitaw analoga ta config an yparxoun oi domes
		Graph lvg;
		BitSet iQ = new BitSet(Config.MAXIMUM_INTERVAL);
		iQ.set(0, Config.MAXIMUM_INTERVAL, true);

		if (Config.LOAD_OBJECT) {
			lvg = (Graph) Storage.deserialize(Config.PATH_OBJECT);
			Config.PATH_DATASET = Config.PATH_OBJECT;

			if (Config.PATH_DATASET.toLowerCase().contains("dblp"))
				LoaderDBLP.setAuthors((Map<Integer, String>) Storage.deserialize(Config.PATH_OBJECT + "_authors_ids"));
		} else {
			String dataset = Config.PATH_DATASET.toLowerCase();

			// for dblp dataset
			if (dataset.contains("dblp")) {
				lvg = new LoaderDBLP().loadDataset();

				if (Config.STORE_OBJECT)
					Storage.serialize(LoaderDBLP.getAuthors(), Config.PATH_OBJECT + "_authors_ids");
			}
			// for yt dataset
			else if (dataset.contains("yt"))
				// TODO number of changes for yt
				lvg = new LoaderYT(9).loadDataset();
			// for proteins
			else
				lvg = new LoaderProteins().loadDataset();

			if (Config.STORE_OBJECT)
				Storage.serialize(lvg, Config.PATH_OBJECT);
		}

		if (Config.RUN_DURABLE_QUERIES || Config.RUN_TOPK_QUERIES)
			new Query(lvg, iQ).run();
	}

	/**
	 * Convert bytes to megabytes
	 * 
	 * @param bytes
	 * @return
	 */
	public static long bytesToMegabytes(long bytes) {
		return bytes / MEGABYTE;
	}
}