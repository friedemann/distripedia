package grinder.client;

import grinder.client.user.BrowserType;
import grinder.client.user.User;
import grinder.util.GrinderSettings;
import grinder.util.Randomizer;
import grinder.util.TestTicket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.UUID;

/**
 * The Client is first controlled by the client gui until a server connection is
 * set up. From then on the server instance controls the client, and the client
 * only reports back to the gui. Of course the gui can always shut down its
 * client, but that could result in a complete loss of data (!).
 *
 * @author Friedemann
 */
public class Client {

	/** The client socket. */
	private Socket clientSocket;

	/** The unique client id. */
	private final UUID clientID;

	/** The controller/server ip. */
	private InetAddress controllerIP;

	/** The current client status . */
	private ClientState state;

	/** True as long as the client is listening for incoming commands */
	private boolean listening = true;

	/** The object outsourcing the client/controller connection. */
	private ClientConnectionThread cct;

	/** The thread controlling the actual grinding. */
	private GrinderThread gt;

	/** true if a server connection is established. */
	public boolean connectionEstablished = false;

	/** The Client GUI the Client itself can talk to. */
	public ClientGUI cgui;

	/** The Testcase data received from the server. */
	public TestTicket testTicket;

	/** Storage for the active users, http type */
	public Hashtable<UUID, User> userHTTP;

	/** Storage for the active users, websocket type */
	public Hashtable<UUID, User> userWS;

	/** Storage for the active users, stratus type */
	public Hashtable<UUID, User> userSTRATUS;

	/** The client's logfiler */
	public LogFiler lf;

	private boolean allUsersInitialized;

	private Properties props;


	/**
	 * Instantiates a new client.
	 *
	 * @param clientGUI the client gui
	 */
	public Client(final ClientGUI clientGUI) {

		cgui = clientGUI;
		clientID = UUID.randomUUID();
		state = ClientState.IDLE;
		
		userHTTP = new Hashtable<UUID, User>(50);
		userWS = new Hashtable<UUID, User>(50);
		userSTRATUS = new Hashtable<UUID, User>(50);

		loadProperties("client.properties");
		cgui.logMessage("...initialized. id: " + getClientID());
	}

	/**
	 * Load properties.
	 *
	 * @param propFileName the prop file name
	 * @return the properties
	 */
	private Properties loadProperties(final String propFileName) {

		final Properties p = new Properties();

		try
		{
			final FileInputStream stream = new FileInputStream(propFileName);
			p.load(stream);
			stream.close();

			props = p;

		} catch (final FileNotFoundException e)
		{
			cgui.logMessage("preset properties file not found - skipping...");
		} catch (final IOException e)
		{
			cgui.logMessage("property reading in failure - skipping...");
		}

		return p;
	}

	/**
	 * @return the props
	 */
	public Properties getProps() {
		return props;
	}

	/**
	 * Gets the client's current state.
	 *
	 * @return the state
	 */
	public synchronized ClientState getState() {
		return state;
	}

	/**
	 * @param state the state to set
	 */
	public synchronized void setState(final ClientState state) {
		this.state = state;

		// update the gui
		cgui.setStatus(this.state);

		// inform the controller about the state change
		cct.writeToOutputStream(this.state);
	}

	/**
	 * Gets the client's uuid
	 *
	 * @return the clientID
	 */
	public UUID getClientID() {
		return clientID;
	}


	/**
	 * Panic. Stops all ongoing testing, deletes logs and client returns to idle
	 * state immediately.
	 *
	 * @param String reason
	 */
	public synchronized void panic(final String reason) {
		cgui.logMessage("PANIC! (" + reason + ")");
		this.setState(ClientState.CLEANUP);

		cleanupUsers();
		
		if (lf != null)
		{
			lf.closeLogFile();
			lf.deleteLogFile();	
		}
		
		this.setState(ClientState.IDLE);
	}

	/**
	 * Sets the ontroller ip the client connects to.
	 *
	 * @param inetAddress the controller ip
	 */
	public void setControllerIP(final InetAddress inetAddress) {
		controllerIP = inetAddress;
	}

	/**
	 * Connect to controller - pass socket to connection handling thread
	 */
	public void connectToController() {

		try
		{
			clientSocket = new Socket(controllerIP, GrinderSettings.CONTROLLER_DEFAULT_PORT);
			cct = new ClientConnectionThread(this, clientSocket, "ClientConnectionThread");

			cgui.logMessage("connection accepted by controller!");

			this.setListening(true);
			cct.start();

		} catch (final IOException e)
		{
			cgui.logMessage("connection attempt failed! controller online? firewalls?");
			this.setListening(false);
		}
	}

	/**
	 * Disconnect client from controller
	 */
	public void disconnectFromController() {

		cgui.logMessage("disconnecting...");
		this.setListening(false);
		cct.writeToOutputStream(ClientState.BYE);
	}

	/**
	 * Checks if the client is currently connected.
	 *
	 * @return true, if is connected
	 */
	public boolean isConnected() {
		if (clientSocket != null)
			return clientSocket.isConnected();
		else
			return false;
	}

	/**
	 * Checks if the client is currently listening. If set to false the client
	 * connection thread will be shut down.
	 *
	 * @return the listening
	 */
	public boolean isListening() {
		return listening;
	}

	/**
	 * Sets the test ticket.
	 *
	 * @param ticket the new test ticket
	 */
	public void setTestTicket(final TestTicket ticket) {

		if (ticket == null)
		{
			testTicket = null;
			return;
		}

		if (state == ClientState.TESTING)
		{
			// in case a test is going on
			this.panic("testing in progress");
			this.setTestTicket(ticket);
		} else
		{
			testTicket = ticket;
			this.setState(ClientState.ARMED);

			beginTestRun();
		}
	}

	/**
	 * Adds a user and starts the thread
	 *
	 * @return UUID the userID
	 */
	public synchronized UUID addUser(final BrowserType type) {

		final int timeToLive = (new Randomizer()).getInt(testTicket.getMinTabOpenTime(),
				testTicket.getMaxTabOpenTime());

		User usr = null;
		try
		{
			usr = new User(this, type, timeToLive);
		} catch (Exception e)
		{
			// TODO better ex handling
			cgui.logMessage("user creation fail! (timeout?)");
			return null;
		}
		final UUID userID = usr.getUserID();

		switch (type)
		{
		case HTTP:
			userHTTP.put(userID, usr);
			break;
		case WEBSOCKET:
			userWS.put(userID, usr);
			break;
		case STRATUS:
			userSTRATUS.put(userID, usr);
			break;
		}

		usr.setArticleList(testTicket.getArticleList());

		usr.start();
		cgui.logMessage("USER TAB OPEN: " + userID.toString());
		cgui.updateUserCount();

		return userID;
	}

	/**
	 * Destroys a user.
	 *
	 * @param ID the user ID
	 */
	public synchronized void destroyUser(final UUID userID) {

		User usr = null;

		// remove user from one of the hashmaps
		usr = userHTTP.remove(userID);

		if (usr == null)
		{
			usr = userWS.remove(userID);
		}
		if (usr == null)
		{
			usr = userSTRATUS.remove(userID);
		}

		if (usr != null)
		{
			usr.setActive(false);

			// in case user is sleeping
			usr.interrupt();

			cgui.updateUserCount();
		} else
		{
			cgui.logMessage("cannot destroy user: " + userID);
		}
	}

	/**
	 * Perform cleanup action: Destroy all users still active.
	 */
	public synchronized void cleanupUsers() {

		for (final BrowserType type : BrowserType.values())
		{			
			Enumeration<User> users = null;

			switch (type)
			{
			case HTTP:
				users = userHTTP.elements();
				break;
			case WEBSOCKET:
				users = userWS.elements();
				break;
			case STRATUS:
				users = userSTRATUS.elements();
				break;
			}

			while (users.hasMoreElements())
			{
				final User usr = users.nextElement();
				destroyUser(usr.getUserID());
			}
		}
	}

	/**
	 * Logs user action.
	 *
	 * @param msg the msg
	 */
	public synchronized void logUserAction(final String msg) {

		// forward to file logging object
		lf.log(clientID, msg);

		// increase request counter
		cgui.increaseRequestCounter();
	}

	/**
	 * Gets the log file from the logfiler instance.
	 *
	 * @return byte[] the log file byte array
	 */
	public byte[] getLogFileByteArray() {
		if (lf != null)
		{
			try
			{
				return lf.getFileByteArray();
			} catch (final IOException e)
			{
				cgui.logMessage(e.toString());
			}
		}
		return null;
	}

	/**
	 * Gets the log file from the logfiler instance.
	 *
	 * @return File the log file
	 */
	public File getLogFile() {
		if (lf != null) return lf.getFile();
		return null;
	}

	/**
	 * closes log file and releases handle.
	 */
	public synchronized void closeLogFile() {
		if (lf != null)
		{
			lf.closeLogFile();
		}
	}


	/**
	 * @param listening the listening status
	 */
	private void setListening(final boolean listening) {
		this.listening = listening;
	}

	/**
	 * Wait until start time and begin testing cycle (Thread: generate users and
	 * start grinding.)
	 */
	private void beginTestRun() {

		if (getState() == ClientState.ARMED)
		{
			cgui.resetRequestCounter();

			// reset file logger
			if (lf != null){
				lf.deleteLogFile();
				lf = null;
			}

			try
			{
				lf = new LogFiler(this, GrinderSettings.CLIENT_LOG_FILE_FOLDER
						+ testTicket.getTestID() + "_" + getClientID().toString() + ".log");

				gt = new GrinderThread(this, "GrinderThread");
				gt.start();

			} catch (final IOException e)
			{
				cgui.logMessage("Client log file cannot be created! " + e);
				cgui.logMessage("aborting test run...");
				panic("could not create log file");
			}
		}
	}

	/**
	 * @return the allUsersInitialized
	 */
	public synchronized boolean isAllUsersInitialized() {
		return allUsersInitialized;
	}

	/**
	 * @param allUsersInitialized the allUsersInitialized to set
	 */
	public synchronized void setAllUsersInitialized(final boolean allUsersInitialized) {
		this.allUsersInitialized = allUsersInitialized;
	}

	/**
	 * returns the client's IP address.
	 *
	 * @return the IP address
	 */
	public String getOwnIPAddress() {
		String ip = null;
		try
		{
			final InetAddress localaddr = InetAddress.getLocalHost();
			ip = localaddr.getHostAddress();

		} catch (final UnknownHostException e)
		{
			return "Can't detect localhost : " + e;
		}
		return ip;
	}

}
