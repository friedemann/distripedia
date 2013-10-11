package grinder.client;

import java.io.Serializable;

/**
 * List of commands the controller can send to the connected clients.
 * 
 * @author Friedemann
 */
public enum ClientCommand implements Serializable {

	/** send client information like ip */
	SEND_INFORMATION,

	/** followed by a fully set test ticket */
	RECEIVE_TESTTICKET,

	/** stop immediately with all testing action and return to idle state */
	PANIC,

	/** send collected log file */
	SEND_LOGFILE,

	/** respond with ok when logfile received */
	SEND_LOGFILE_OK,

	/** close connection */
	BYE;

}
