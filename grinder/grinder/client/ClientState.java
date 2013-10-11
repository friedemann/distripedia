package grinder.client;

/**
 * The Enum ClientState determines all states a client can be in.
 * 
 * @author Friedemann
 */
public enum ClientState {

	/** as it says */
	IDLE, //

	/** after receiving ticket, start time not reached yet */
	ARMED,

	/** while testing in progress */
	TESTING,

	/** after test duration expired */
	FINISHED,

	/** sending gathered log data - do not disturb! :) */
	SENDING_LOGFILE,

	/** after successfully sending logfile data */
	LOGFILE_SENT,

	/** cleaning up, e.g. after panic command */
	CLEANUP,

	/** closing connection */
	BYE,

	/** response before resending test ticket id */
	RECEIVE_TICKET_OK;

}
