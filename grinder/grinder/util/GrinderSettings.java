/**
 *
 */
package grinder.util;

/**
 * Class providing general settings used globally by various grinder objects.
 *
 * @author Friedemann
 */
public final class GrinderSettings {


	// SHARED GLOBALS
	// ////////////////////////////////////////////////////////////

	/** The Constant LOGFILE_SEND_BUFFER_SIZE. */
	public static final int LOGFILE_SEND_BUFFER_SIZE = 1024;

	/** Format for GUI logging. */
	public static final String SIMPLE_DATE_FORMAT = "HH:mm:ss";

	/** regexp for evaluating IPv4 addresses */
	public static final String REGEX_IPV4 = "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";

	// CONTROLLER GLOBALS
	// ////////////////////////////////////////////////////////////

	/** regexp for evaluating article ids and weighting in article list */
	//public static final String REGEX_ARTICLELIST = "^[^#]([A-Za-z0-9])+\\|([0-9]+)$";
	public static final String REGEX_ARTICLELIST = "^([0-9])+\\|([\\S])+$";

	/*
	 * maximum client connections a controller will accept - equals # of max
	 * threads
	 */
	public static final int CONTROLLER_MAX_CLIENTS = 10;

	/**
	 * The first Port on which the controller listens for client connections.
	 */
	public static final int CONTROLLER_DEFAULT_PORT = 9111;

	/** The Constant CONTROLLER_LOG_FILE_FOLDER. */
	public static final String CTRL_LOG_FILE_FOLDER = "C:\\Temp\\";

	// CLIENT GLOBALS
	// ////////////////////////////////////////////////////////////

	/** Timeout in ms, after the Websocket client gives up receiving the article */
	public static final int CLIENT_WEBSOCKET_RECEIVE_TIMEOUT = 15 * 1000;

	/** The Constant CLIENT_LOG_FILE_FOLDER. */
	public static final String CLIENT_LOG_FILE_FOLDER = "C:\\Temp\\";

	/** The maximum user count the client will handle. */
	public static final int CLIENT_MAX_USER_COUNT = 500;

	/** Time in ms a client tries to connect to controller. */
	public static final int CLIENT_TO_CONTROLLER_TIMEOUT = 10000;


}
