package system;

import queries.DBLP;
import queries.YT;

/**
 * Main class
 * @author ksemer
 */
public class Main {
	//=================================================================
	public static boolean TiNLa = false;
	public static long TIME;
	private static final long MEGABYTE = 1024L * 1024L;
	//=================================================================
	
	private static void initiateSettings() {
		Config.TILA_ENABLED = true;
		
		Config.TINLA_ENABLED = false;
		Config.CTINLA_ENABLED = true;
		
		Config.TIPLA_ENABLED = false;
		Config.TIPLA_MAX_DEPTH = 2;

		Config.LABELS_CHANGE = true;
		Config.SHOW_MEMORY = false;
		Config.WRITE_FILE = true;
		
		Config.MAX_MATCHES = 1000;
		Config.QUERY_SIZE = 6;
		
		Config.RUN_CLIQUES = false;
		
		Config.RUN_RANDOM = true;
		Config.RANDOM_ITERATIONS = 5;
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
		Config.TINLA_R = 2;
		new YT().run();
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