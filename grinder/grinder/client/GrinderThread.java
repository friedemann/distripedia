/*
 *
 */
package grinder.client;

import grinder.client.user.BrowserType;
import grinder.client.user.User;

import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.UUID;

/**
 * Thread executing the actual grinding.
 */
public class GrinderThread extends Thread {

	Client client;
	private boolean testRunning;

	/**
	 * Instantiates a new grinder thread.
	 *
	 * @param client reference
	 */
	public GrinderThread(final Client c, final String name) {

		super(name);
		client = c;

		testRunning = false;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		// #1 BLOCKING until start-time is reached
		while (!testRunning)
		{
			// in case somebody panic'ed cleanup and stop immediately
			if ((client.getState() == ClientState.CLEANUP))
			{
				client.cleanupUsers();
				client.setState(ClientState.IDLE);
			}

			// if time to start is reached
			if ((client.getState() == ClientState.ARMED)
					&& client.testTicket.getStartTime().before(
							new Timestamp(System.currentTimeMillis())))
			{
				client.setState(ClientState.TESTING);
				client.cgui.logMessage("BEGINNING TESTING!");
				testRunning = true;
			}
		}
		
		client.setAllUsersInitialized(false);

		// #2 GRINDING
		while (client.getState() == ClientState.TESTING)
		{

			// generate/restock user threads
			try
			{
				while (client.userHTTP.size() < client.testTicket.getUserCountHTTP())
				{
					client.addUser(BrowserType.HTTP);
					Thread.sleep(10);
					if (client.getState() == ClientState.CLEANUP) break;
				}
				while (client.userWS.size() < client.testTicket.getUserCountWebSocket())
				{
					client.addUser(BrowserType.WEBSOCKET);
					Thread.sleep(10);
					if (client.getState() == ClientState.CLEANUP) break;
				}
				while (client.userHTTP.size() < client.testTicket.getUserCountStratus())
				{
					client.addUser(BrowserType.STRATUS);
					Thread.sleep(10);
					if (client.getState() == ClientState.CLEANUP) break;
				}
			} catch (final InterruptedException e)
			{
				client.cgui.logMessage(this.getName() + " epic fail.");
			}
			
			// "...who let the users out...."
			client.setAllUsersInitialized(true);

			// cycle through users, destroy expired ones
			for (final BrowserType type : BrowserType.values())
			{
				Enumeration<User> users = null;

				switch (type)
				{
				case HTTP:
					users = client.userHTTP.elements();
					break;
				case WEBSOCKET:
					users = client.userWS.elements();
					break;
				case STRATUS:
					users = client.userSTRATUS.elements();
					break;
				}

				while (users.hasMoreElements())
				{
					final User usr = users.nextElement();

					if ((usr.getTimeRunning() > usr.getTimeToLive()))
					{
						client.destroyUser(usr.getUserID());
					}

				}
			}

			// update gui
			client.cgui.updateUserCount();

			// test finished?
			final Timestamp testFinishedTime = new Timestamp(client.testTicket
					.getStartTime().getTime()
					+ (client.testTicket.getTestDuration() * 1000));

			final Timestamp now = new Timestamp(System.currentTimeMillis());

			if (testFinishedTime.before(now)) // time ran up
			{
				client.setState(ClientState.CLEANUP);
				client.cleanupUsers();
				client.setState(ClientState.FINISHED);
			}
			else if (client.getState() == ClientState.CLEANUP) // someone triggered panic while testing
			{
				client.cleanupUsers();
				client.setState(ClientState.IDLE);
			}
			
		} // end testing while loop

		client.cgui.logMessage("################### " + this.getName() + " stopped. ##################");
	}
}
