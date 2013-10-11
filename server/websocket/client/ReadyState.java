package websocket.client;

/**
 * 
 * The readyState attribute represents the state of the connection.
 * 
 * @author David
 * 
 */
public enum ReadyState
{

	/**
	 * The connection has not yet been established.
	 */
	Connecting,

	/**
	 * The WebSocket connection is established and communication is possible.
	 */
	Open,

	/**
	 * The connection is going through the closing handshake.
	 */
	Closing,

	/**
	 * The connection has been closed or could not be opened.
	 */
	Closed
}
