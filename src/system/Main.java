package system;

import queries.DBLP;
import queries.YT;

/**
 * Main class
 * @author ksemer
 */
public class Main {
	//=================================================================
	public static long TIME;
	public static boolean TiNLa;
	private static final long MEGABYTE = 1024L * 1024L;
	//=================================================================
	
	private static void initiateSettings() throws Exception {
		
		Config.TINLA_ENABLED = true;
		Config.TINLA_R = 1;
		
		Config.TIPLA_MAX_DEPTH = 2;
		Config.LABELS_CHANGE = true;
		Config.SHOW_MEMORY = false;
		Config.WRITE_FILE = true;
		Config.MAX_MATCHES = 1000;
		Config.RUN_CLIQUES = false;
		Config.RUN_RANDOM = true;
		Config.RANDOM_ITERATIONS = 5;
		
		Config.loadConfigs();
	}

	/**
	 * Main
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		initiateSettings();
		
		Config.BINARY_RANKING = false;
		Config.MINMAX_RANKING = true;
		run("dblp", args);
//		
//		Config.MINMAX_RANKING = false;
//		Config.BINARY_RANKING = true;
//		run("dblp", args);
	}


	private static void run(String dataset, String[] args) throws Exception {
		Config.TINLA_R = 2;
		TiNLa = true;
		Config.TIPLA_ENABLED = true;
		if (dataset.equals("dblp"))
			new DBLP().run();
		else
			new YT().run();

//		Config.TINLA_R = 2;
//		TiNLa = true;
//		
//		if (dataset.equals("dblp"))
//			new DBLP().run();
//		else
//			new YT().run(args);
//				
//		Config.TINLA_R = 1;
//		TiNLa = false;
//		
//		if (dataset.equals("dblp"))
//			new DBLP().run();
//		else
//			new YT().run(args);
//
//		Config.TIPLA_ENABLED = true;
//		Config.TINLA_R = 1;
//		TiNLa = false;
//		
//		if (dataset.equals("dblp"))
//			new DBLP().run();
//		else
//			new YT().run(args);
	}

	/**
	 * Convert bytes to megabytes
	 * @param bytes
	 * @return
	 */
	public static long bytesToMegabytes(long bytes) {
		return bytes / MEGABYTE;
	}
}