package system;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents the System's Configurations
 * 
 * @author ksemer
 */
public final class Config {
	// ====================================================

	public final static int MAX_RANKING = 1;

	public final static int HALFWAY_RANKING = 2;

	public final static int ZERO_RANKING = 3;

	// ====================================================

	public static boolean LOAD_OBJECT;

	public static boolean STORE_OBJECT;

	public static String PATH_OBJECT;

	// path of graph dataset
	public static String PATH_DATASET;

	// path of labels
	public static String PATH_LABELS;

	// output directory path
	public static String PATH_OUTPUT;

	// path with all input queries
	public static String PATH_QUERY;

	// dataset direction
	public static boolean ISDIRECTED;

	public static boolean CONTIGUOUS_MATCHES;

	public static boolean RUN_RANDOM;

	public static boolean RUN_TOPK_QUERIES;

	public static boolean RUN_DURABLE_QUERIES;

	// maximum interval of graph lifespan
	public static int MAXIMUM_INTERVAL;

	// # of labels in the dataset
	public static int SIZE_OF_LABELS;

	public static int RANDOM_ITERATIONS;

	// show the memory info of structures
	public static boolean SHOW_MEMORY;

	// for zero ranking
	public static boolean ZERO_RANKING_ENABLED;

	// for binary/halfway ranking execution
	public static boolean HALFWAY_RANKING_ENABLED;

	// for max ranking execution
	public static boolean MAX_RANKING_ENABLED;

	// maximum matches that we want to get
	public static int MAX_MATCHES;

	// nodes' labels change
	public static boolean LABEL_CHANGE;

	// time indexes
	public static boolean TINLA_ENABLED;
	public static boolean CTINLA_ENABLED;
	public static boolean TIPLA_ENABLED;
	public static int TIPLA_MAX_DEPTH;
	public static int TINLA_R;
	public static int CTINLA_R;

	private static final Logger _log = Logger.getLogger(Config.class.getName());

	// ====================================================

	/**
	 * Check for config correctness
	 * 
	 * @throws Exception
	 */
	public static void loadConfigs() throws Exception {

		final String SETTINGS_FILE = "./config/settings.properties";

		try {
			Properties Settings = new Properties();
			InputStream is = new FileInputStream(new File(SETTINGS_FILE));
			Settings.load(is);
			is.close();

			PATH_OBJECT = Settings.getProperty("ObjectPath", "");
			PATH_DATASET = Settings.getProperty("DataPath", "");
			PATH_LABELS = Settings.getProperty("LabelPath", "");
			PATH_OUTPUT = Settings.getProperty("OutputPath", "");
			PATH_QUERY = Settings.getProperty("QueryPath", "");
			RUN_TOPK_QUERIES = Boolean.parseBoolean(Settings.getProperty("TopkQueries", "false"));
			RUN_DURABLE_QUERIES = Boolean.parseBoolean(Settings.getProperty("DurableQueries", "false"));
			LOAD_OBJECT = Boolean.parseBoolean(Settings.getProperty("LoadObject", "false"));
			STORE_OBJECT = Boolean.parseBoolean(Settings.getProperty("StoreObject", "false"));
			ISDIRECTED = Boolean.parseBoolean(Settings.getProperty("Directed", "false"));
			CONTIGUOUS_MATCHES = Boolean.parseBoolean(Settings.getProperty("ContiguousMatches", "false"));
			MAX_RANKING_ENABLED = Boolean.parseBoolean(Settings.getProperty("MaxRanking", "false"));
			HALFWAY_RANKING_ENABLED = Boolean.parseBoolean(Settings.getProperty("HalfwayRanking", "false"));
			ZERO_RANKING_ENABLED = Boolean.parseBoolean(Settings.getProperty("ZeroRanking", "false"));
			SHOW_MEMORY = Boolean.parseBoolean(Settings.getProperty("ShowMemory", "false"));
			TINLA_ENABLED = Boolean.parseBoolean(Settings.getProperty("TiNLa", "false"));
			CTINLA_ENABLED = Boolean.parseBoolean(Settings.getProperty("CTiNLa", "false"));
			TIPLA_ENABLED = Boolean.parseBoolean(Settings.getProperty("TiPLa", "false"));
			MAX_MATCHES = Integer.parseInt(Settings.getProperty("MaxMatches", "1"));
			MAXIMUM_INTERVAL = Integer.parseInt(Settings.getProperty("MaximumInterval", "-1"));
			TINLA_R = Integer.parseInt(Settings.getProperty("TiNLa_r", "1"));
			CTINLA_R = Integer.parseInt(Settings.getProperty("CTiNLa_r", "1"));
			TIPLA_MAX_DEPTH = Integer.parseInt(Settings.getProperty("TiPLa_depth", "2"));
			RUN_RANDOM = Boolean.parseBoolean(Settings.getProperty("Random", "false"));
			RANDOM_ITERATIONS = Integer.parseInt(Settings.getProperty("RandomIterations", "5"));
			LABEL_CHANGE = Boolean.parseBoolean(Settings.getProperty("LabelChange", "false"));

			boolean stop = false;

			if (PATH_DATASET.isEmpty()) {
				_log.log(Level.SEVERE, "dataset path is empty." + ". Abborted.", new Exception());
				stop = true;
			} else if (PATH_LABELS.isEmpty()) {
				_log.log(Level.SEVERE, "label path is empty." + ". Abborted.", new Exception());
				stop = true;
			} else if (PATH_OUTPUT.isEmpty()) {
				_log.log(Level.SEVERE, "output path is empty." + ". Abborted.", new Exception());
				stop = true;
			} else if (PATH_QUERY.isEmpty()) {
				_log.log(Level.SEVERE, "query path is empty." + ". Abborted.", new Exception());
				stop = true;
			} else if (MAXIMUM_INTERVAL == -1) {
				_log.log(Level.SEVERE, "Interval is empty or wrong configured." + ". Abborted.", new Exception());
				stop = true;
			} else if ((LOAD_OBJECT || STORE_OBJECT) && PATH_OBJECT.isEmpty()) {
				_log.log(Level.SEVERE, "object path is empty while load/store object is true", new Exception());
				stop = true;
			} else if (TINLA_ENABLED && TINLA_R == 0) {
				_log.log(Level.SEVERE, "TiNLa settings are wrong", new Exception());
				stop = true;
			} else if (TIPLA_ENABLED && TIPLA_MAX_DEPTH == 0) {
				_log.log(Level.SEVERE, "TiPLa settings are wrong", new Exception());
				stop = true;
			} else if ((TIPLA_ENABLED && (TINLA_ENABLED || CTINLA_ENABLED)) || (TINLA_ENABLED && CTINLA_ENABLED)) {
				_log.log(Level.SEVERE, "Only one index must be enabled", new Exception());
				stop = true;
			}

			if (stop)
				System.exit(0);
		} catch (

		Exception e) {
			_log.log(Level.SEVERE, "Failed to Load " + SETTINGS_FILE + " File.", e);
			System.exit(0);
		}
	}
}