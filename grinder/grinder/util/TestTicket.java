package grinder.util;

import java.io.Serializable;
import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * This test ticket is setup by the controller for each client logged in. Based
 * on this ticket, the client can set its configuration and start load-testing.
 * All times are measured in seconds.
 *
 * @author Friedemann
 */
public class TestTicket implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -1448769937574314674L;

	/** The test id. */
	private String testID;

	/** The min tab open time. */
	private int minTabOpenTime;

	/** The max tab open time. */
	private int maxTabOpenTime;

	/** The min reading time. */
	private int minReadingTime;

	/** The max reading time. */
	private int maxReadingTime;

	/** The user count with html4 only browsers. */
	private int userCountHTTP;

	/** The user count web socket capable. */
	private int userCountWebSocketCapable;

	/** The user count web socket capable. */
	private int userCountStratusCapable;

	/** The start time. */
	private Timestamp startTime;

	/** The test duration. */
	private int testDuration;

	/** The web server address. */
	private URI webServerURI;

	/** The web socket address. */
	private URI webSocketURI;

	/** The article list. */
	private ArrayList<String> articleList;

	/**
	 * Instantiates a new test ticket.
	 */
	public TestTicket() {}

	/**
	 * @return the testID
	 */
	public synchronized String getTestID() {
		return testID;
	}

	/**
	 * Gets the min tab open time.
	 *
	 * @return the minTabOpenTime
	 */
	public synchronized int getMinTabOpenTime() {
		return minTabOpenTime;
	}

	/**
	 * Gets the max tab open time.
	 *
	 * @return the maxTabOpenTime
	 */
	public synchronized int getMaxTabOpenTime() {
		return maxTabOpenTime;
	}

	/**
	 * Gets the min reading time.
	 *
	 * @return the minReadingTime
	 */
	public synchronized int getMinReadingTime() {
		return minReadingTime;
	}

	/**
	 * Gets the max reading time.
	 *
	 * @return the maxReadingTime
	 */
	public synchronized int getMaxReadingTime() {
		return maxReadingTime;
	}

	/**
	 * Gets the user count.
	 *
	 * @return the users
	 */
	public synchronized int getUserCountHTTP() {
		return userCountHTTP;
	}

	/**
	 * Gets the user count with stratus capable browsers
	 *
	 * @return the usersStratusCapable
	 */
	public synchronized int getUserCountStratus() {
		return userCountStratusCapable;
	}

	/**
	 * Gets the user count web socket capable.
	 *
	 * @return the usersWebSocketCapable
	 */
	public synchronized int getUserCountWebSocket() {
		return userCountWebSocketCapable;
	}

	/**
	 * Gets the start time.
	 *
	 * @return the startTime
	 */
	public synchronized Timestamp getStartTime() {
		return startTime;
	}

	/**
	 * Gets the test duration.
	 *
	 * @return the testDuration
	 */
	public synchronized int getTestDuration() {
		return testDuration;
	}

	/**
	 * Gets the web server address.
	 *
	 * @return the webServerAddress
	 */
	public synchronized URI getWebServerURI() {
		return webServerURI;
	}

	/**
	 * @return the webSocketURI
	 */
	public synchronized URI getWebSocketURI() {
		return webSocketURI;
	}

	/**
	 * Gets the article list.
	 *
	 * @return the articleList
	 */
	public synchronized ArrayList<String> getArticleList() {
		return articleList;
	}

	/**
	 * @param testID the testID to set
	 */
	public synchronized void setTestID(final String testID) {
		this.testID = testID;
	}

	/**
	 * Sets the min tab open time.
	 *
	 * @param minTabOpenTime the minTabOpenTime to set
	 */
	public synchronized void setMinTabOpenTime(final int minTabOpenTime) {
		this.minTabOpenTime = minTabOpenTime;
	}

	/**
	 * Sets the max tab open time.
	 *
	 * @param maxTabOpenTime the maxTabOpenTime to set
	 */
	public synchronized void setMaxTabOpenTime(final int maxTabOpenTime) {
		this.maxTabOpenTime = maxTabOpenTime;
	}

	/**
	 * Sets the min reading time.
	 *
	 * @param minReadingTime the minReadingTime to set
	 */
	public synchronized void setMinReadingTime(final int minReadingTime) {
		this.minReadingTime = minReadingTime;
	}

	/**
	 * Sets the max reading time.
	 *
	 * @param maxReadingTime the maxReadingTime to set
	 */
	public synchronized void setMaxReadingTime(final int maxReadingTime) {
		this.maxReadingTime = maxReadingTime;
	}

	/**
	 * Sets the user count.
	 *
	 * @param users the users to set
	 */
	public synchronized void setUserCountHTTP(final int users) {
		userCountHTTP = users;
	}

	/**
	 * Sets the user count with stratus capable browsers.
	 *
	 * @param users the new user count web socket capable
	 */
	public synchronized void setUserCountStratus(final int users) {
		userCountStratusCapable = users;
	}

	/**
	 * Sets the user count web socket capable.
	 *
	 * @param users the new user count web socket capable
	 */
	public synchronized void setUserCountWebSocket(final int users) {
		userCountWebSocketCapable = users;
	}

	/**
	 * Sets the start time.
	 *
	 * @param ts the startTime to set
	 */
	public synchronized void setStartTime(final Timestamp ts) {
		startTime = ts;
	}

	/**
	 * Sets the test duration.
	 *
	 * @param testDuration the testDuration to set
	 */
	public synchronized void setTestDuration(final int testDuration) {
		this.testDuration = testDuration;
	}

	/**
	 * Sets the web server address.
	 *
	 * @param string the webServerAddress to set
	 */
	public synchronized void setWebServerURI(final URI uri) {
		webServerURI = uri;
	}

	/**
	 * @param webSocketURI the webSocketURI to set
	 */
	public synchronized void setWebSocketURI(URI webSocketURI) {
		this.webSocketURI = webSocketURI;
	}

	/**
	 * Sets the article list.
	 *
	 * @param strings the articleList to set
	 */
	public synchronized void setArticleList(final ArrayList<String> articles) {
		articleList = articles;
	}
}
