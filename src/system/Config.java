package system;

/**
 * This class represents the System's Configurations
 * @author K.Semertzidis
 */
public final class Config {
	//====================================================
	// path of graph dataset
	public static String PATH_DATASET;
	
	// path of labels
	public static String PATH_LABELS;
	
	// dataset direction
	public static boolean ISDIRECTED;
	
	// maximum interval of graph lifetime
	public static int MAXIMUM_INTERVAL;
	
	public static int SIZE_OF_LABELS;
	public static int RANDOM_ITERATIONS;
	public static int QUERY_SIZE;
	public static boolean LABELS_CHANGE;
	public static boolean WRITE_FILE;
	
	// show the memory info of structures
	public static boolean SHOW_MEMORY;
	
	// for binary ranking execution
	public static boolean BINARY_RANKING;
	
	// for minmax ranking execution
	public static boolean MINMAX_RANKING;
	
	// which query type to execute
	public static boolean RUN_CLIQUES;
	public static boolean RUN_RANDOM;
	public static boolean RUN_STAR;
	
	// maximum matches that we want to get
	public static int MAX_MATCHES;

	// time indexes
	public static boolean TILA_ENABLED;
	public static boolean TINLA_ENABLED;
	public static boolean CTINLA_ENABLED;
	public static boolean TIPLA_ENABLED;
	public static int TIPLA_MAX_DEPTH;
	public static int TINLA_R;
	public static int CTINLA_R;
	//====================================================
	
	/**
	 * Check for config correctness
	 * @throws Exception
	 */
	public static void loadConfigs() throws Exception {
		TILA_ENABLED = true;

		if (TINLA_ENABLED && TINLA_R == 0)
			throw new Exception("TiNLa settings are wrong");
		
		if (BINARY_RANKING && MINMAX_RANKING)
			throw new Exception("Only one type of ranking must be enabled");
		
		if (TIPLA_ENABLED && TIPLA_MAX_DEPTH == 0)
			throw new Exception("TiPLa settings are wrong");
	}
}