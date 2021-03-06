package grinder.controller;

import grinder.client.ClientCommand;
import grinder.client.ClientState;
import grinder.util.GrinderSettings;
import grinder.util.TestTicket;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JOptionPane;

/**
 * The Controller takes care of generating TestTickets based on the settings
 * made with the controller and send them to the clients, which are logged onto
 * the controller at that time. If the test cycle is finished, the controller
 * collects all the logfiles generated by the clients/users and assembles them
 * to a big file. The Analyzer parses and interprets the log afterwards.
 *
 * @author Friedemann
 * @see Analyzer
 */
public class Controller {

	/** Backward reference to the controller gui. */
	public ControllerGUI cgui;

	/**
	 * The controller server thread, listening and dispatching connections to own
	 * threads.
	 */
	private final CtrlServer cServer;

	private ArrayList<String> articleList;

	private Properties props;

	/**
	 * Instantiates a new controller and starts the server thread
	 */
	public Controller(final ControllerGUI cgui) {

		articleList = null;
		this.cgui = cgui;

		loadProperties("controller.properties");
		cgui.logMessage("controller initialized, starting server thread...");

		cServer = new CtrlServer(this, GrinderSettings.CONTROLLER_DEFAULT_PORT);
		new Thread(cServer, "CtrlServer").start();
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
	 * Generates test ticket with user values distributed for connected clients.
	 *
	 * @return the test ticket
	 * @throws URISyntaxException
	 */
	private TestTicket generateTestTicket(final int clientsConnected) throws URISyntaxException {

		final TestTicket ticket = cgui.getTestTicketCanvas();

		if (clientsConnected >= 1) // distribute users
		{
			ticket.setUserCountHTTP(ticket.getUserCountHTTP() / clientsConnected);
			ticket.setUserCountWebSocket(ticket.getUserCountWebSocket() / clientsConnected);
			ticket.setUserCountStratus(ticket.getUserCountStratus() / clientsConnected);
		}

		ticket.setArticleList(articleList);

		return ticket;
	}

	/**
	 * Deploy test tickets to connected clients.
	 *
	 * @return true, if successful, false if there are no clients!
	 */
	public boolean deployTestTickets() {

		final int clientsConnected = cServer.getServerThreads().size();

		TestTicket t = null;
		try
		{
			t = generateTestTicket(clientsConnected);
		} catch (final URISyntaxException e)
		{
			cgui.logMessage("Webserver address malformed: " + e);
			return false;
		}

		if (clientsConnected > 0)
		{
			for (final CtrlServerThread cst : cServer.getServerThreads())
			{
				cst.writeToOutputStream(ClientCommand.RECEIVE_TESTTICKET);
				cst.writeToOutputStream(t);
			}
			return true;
		} else
			return false;

	}

	/**
	 * Parses the article list into a string array.
	 *
	 * @param fileName
	 * @return int - number of parsed lines/articles, negative if article list
	 *         was wrongly formatted
	 * @throws FileNotFoundException
	 */
	public int parseArticleList(final String filePath) throws FileNotFoundException {

		final BufferedInputStream fileStream = new BufferedInputStream(new FileInputStream(
				filePath));
		final BufferedReader br = new BufferedReader(new InputStreamReader(fileStream));

		articleList = new ArrayList<String>(50);
		int articleCount = 0;
		String line;

		try
		{
			while ((line = br.readLine()) != null)
			{

				line.trim(); // remove ws

				// only consider correctly formatted lines
				if (line.matches(GrinderSettings.REGEX_ARTICLELIST))
				{
					final String[] article = line.split("\\|");

					final int weight = Integer.valueOf(article[0]);
					final String ID = article[1];

					for (int i = 0; i < weight; i++)
					{
						articleList.add(ID);
					}
					articleCount++;
				}
			}
		} catch (final IOException e)
		{
			cgui.logMessage("ERROR while parsing article list: " + e);
			return -1;
		}
		return articleCount;
	}

	/**
	 * Checks whether an article list is loaded.
	 *
	 * @return true, if successful
	 */
	public boolean isArticleListLoaded() {
		return articleList != null;
	}

	/**
	 * returns the controller's IP address.
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

	/**
	 * Send PANIC message to all connected clients
	 */
	public void panic() {
		for (final CtrlServerThread cst : cServer.getServerThreads())
		{
			cst.writeToOutputStream(ClientCommand.PANIC);
		}
	}

	/**
	 * Gather client data for the controller GUI's table of connected clients.
	 *
	 * @param rows the table row count
	 * @param cols the table column count
	 * @return String[][] array with client details
	 */
	public String[][] gatherClientData(final int rows, final int cols) {

		final ArrayList<CtrlServerThread> threadList = cServer.getServerThreads();
		final String[][] clientData = new String[rows][cols];

		for (int i = 0; i < threadList.size(); i++)
		{
			clientData[i][0] = threadList.get(i).getClientIP().toString();
			clientData[i][1] = threadList.get(i).getClientID().toString();
			clientData[i][2] = threadList.get(i).getDeployedTicketID();
			clientData[i][3] = threadList.get(i).getClientState().toString();
		}
		return clientData;
	}

	/**
	 * Check whether all clients are finished with testing
	 */
	public synchronized void checkForTestFinish() {
		final ArrayList<CtrlServerThread> threadList = cServer.getServerThreads();

		int clientsFinished = 0;

		for (final CtrlServerThread cst : threadList)
		{
			clientsFinished += (cst.getClientState() == ClientState.FINISHED) ? 1 : 0;
		}

		if (threadList.size() == clientsFinished)
		{
			final int answer = cgui.confirmDialog("Test finished!",
					"All clients are finished with testing. Would you like to "
							+ "gather and merge all collected logfile data now? "
							+ "\nPressing CANCEL will delete all gathered data and "
							+ "reset the clients to idle state!");

			if (answer == JOptionPane.OK_OPTION)
			{
				collectClientLogFiles();
			} else if (answer == JOptionPane.CANCEL_OPTION)
			{
				panic();
			}
		}
	}

	/**
	 * Collect client log files, display a progress monitor meanwhile. Happens in
	 * a sub-thread, so that the last client connection thread which triggers
	 * this method is free again to accept the log file!
	 *
	 * UPDATE: does now a recheck for the client being finished. This allows
	 * fetching the logfiles manually.
	 */
	public synchronized void collectClientLogFiles() {

		// foreach cst/client
		// send sendfile command
		// catch file in csthread (fwd it?)
		// update pm
		// done!

		final int clientsConnected = cServer.getServerThreads().size();

		final Thread thread = new Thread(new Runnable() {
			public void run() {

				int i = 1;

				for (final CtrlServerThread cst : cServer.getServerThreads())
				{

					cgui.logMessage("requesting log " + i + " of " + clientsConnected);

					if (cst.getClientState() == ClientState.FINISHED)
					{
						cst.writeToOutputStream(ClientCommand.SEND_LOGFILE);

						while (cst.getClientState() != ClientState.LOGFILE_SENT)
						{
							try
							{
								Thread.sleep(500);
							} catch (final InterruptedException e)
							{
								cgui.logMessage(e.toString());
							}
						}

						// increase client counter/ move on
						i++;
					} else
					{
						cgui.logMessage("client " + i + " not finished yet! skipping...");
					}
				}
			}
		}, "LogfileCollectorThread");
		thread.start();

	}
}
