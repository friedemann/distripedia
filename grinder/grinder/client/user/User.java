package grinder.client.user;

import grinder.client.Client;
import grinder.util.Randomizer;
import grinder.util.Timer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.UUID;


/**
 * Emulates a User. The User gets an article list and "surfs around" on the
 * webpage based on random choice, of course influenced by the article's
 * trendiness. The User is as long active as the client tells the monkey to stop
 * reading around in teh interwebz (we all know what thats like, don't we?)
 *
 * @author Friedemann
 */
public class User extends Thread {

	/** The user id given by the client. */
	private final UUID userID;

	/** The user's browser instance. */
	private final Browser b;

	/** True if the user acts with default behaviour. */
	private boolean isActive = true;

	/** The article list. */
	private ArrayList<String> articleList;

	/** The timestamp the user started. */
	private Timestamp timeStarted = null;

	/** The time to live in seconds. */
	private final int timeToLive;

	/** Timer to measure request durations. */
	private final Timer tmr;

	/** The random. */
	private final Randomizer random;

	/** The client reference. */
	private final Client client;

	/**
	 * Instantiates a new user.
	 *
	 * @param userID the user id
	 */
	public User(final Client c, final BrowserType type, final int ttl) {

		client = c;
		timeToLive = ttl;

		// UUID is not thread safe
		synchronized (client.lf) {
			userID = UUID.randomUUID();
		}
		this.setName("user-" + userID.toString());

		b = new Browser(type, client.testTicket.getWebServerURI(), client.testTicket
				.getWebSocketURI());
		tmr = new Timer();
		random = new Randomizer();
	}

	/**
	 * Gets the user id.
	 *
	 * @return the user id
	 */
	public UUID getUserID() {
		return userID;
	}

	/**
	 * Sets the article list.
	 *
	 * @param list the new article list
	 */
	public void setArticleList(final ArrayList<String> list) {
		articleList = list;
	}

	/**
	 * Sets the user (in-)active.
	 *
	 * @param act
	 */
	public void setActive(final boolean act) {
		isActive = act;
	}

	/**
	 * Gets the time the user is running in seconds.
	 *
	 * @return the time running
	 */
	public int getTimeRunning() {

		if (timeStarted != null)
			return (int) ((System.currentTimeMillis() - timeStarted.getTime()) / 1000);
		else
			return 0;

	}

	/**
	 * Gets user's time to live in seconds (tab open time)
	 *
	 * @return int the time in seconds
	 */
	public int getTimeToLive() {
		return timeToLive;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		// keep start time
		if (timeStarted == null)
		{
			timeStarted = new Timestamp(System.currentTimeMillis());
		}

		String nextArticleID;

		while (isActive)
		{
			// #1 BLOCKING until all users are initialized
			/*
			while((client.isAllUsersInitialized() == false) && isActive)
			{
				try {
					Thread.sleep(500);
				} catch (final InterruptedException e) {
					client.cgui.logMessage("Interrupted while waiting for all users! "+e);
				}
			}
			*/

			// #2 Goto sleep while reading (assuming the user is reading the main page first :-)
			if (isActive)
			{
				try
				{
					Thread.sleep(random.getInt(client.testTicket.getMinReadingTime(),
							client.testTicket.getMaxReadingTime()) * 1000);

				} catch (final InterruptedException e)
				{
					setActive(false);
				}
			}
			
			if (isActive)
			{

				// #3 choose and request next article
				nextArticleID = this.chooseNextArticle();
				client.cgui.logMessage(userID + " fetching " + nextArticleID);
	
				tmr.start();
				final ArticleRequest ar = b.retrieveArticle(nextArticleID);
				tmr.stop();
	
				/** @see logfileformat.txt */
				final String logline = userID + "|" + b.getBrowserID().toString() + "|"
						+ b.getBrowserType() + "|" + tmr.getStartTime() + "|" + tmr.getEndTime()
						+ "|" + nextArticleID + "|" + ar.articleIdReceived + "|"
						+ ar.bytesReceived + "|" + ar.receivedFrom;
	
				client.logUserAction(logline);
			}
		}

		//shutdown ws connection
		b.close();

		// running out...
		client.cgui.logMessage("USER TAB CLOSE " + userID);

	} // run end

	/**
	 * Choose next article.
	 *
	 * @return String the unique next article ID
	 */
	private String chooseNextArticle() {

		final int i = random.getInt(0, articleList.size() - 1);
		final String nextArticle = articleList.get(i);

		return nextArticle;
	}

}
