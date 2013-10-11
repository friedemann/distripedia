package grinder.controller;

import grinder.util.GrinderSettings;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * @author Friedemann
 */
public class CtrlServer implements Runnable {

	private final Controller ctrl;

	private final int port;
	private ServerSocket serverSocket = null;
	
	private int connCount = 0;

	private boolean serverListening;

	private final ArrayList<CtrlServerThread> serverThreads;

	/**
	 * Instantiates a new controller server, which takes care about the client
	 * connections.
	 *
	 * @param ctrl the ctrl
	 * @param port the port
	 */
	public CtrlServer(final Controller ctrl, final int port) {
		this.ctrl = ctrl;
		this.port = port;
		serverThreads = new ArrayList<CtrlServerThread>();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {

		try
		{
			serverSocket = new ServerSocket(port);
			ctrl.cgui.logMessage("server socket started on port " + port + "! "
					+ "waiting for client connections...");

			serverListening = true;

		} catch (final IOException e)
		{

			ctrl.cgui.logMessage("ERRROR: Could not listen on port " + port + "!");
			serverListening = false;
		}

		while (serverListening)
		{
			Socket socket = null;

			/*
			// idle around if clients connected equals max clients
			while(serverThreads.size() == GrinderSettings.CONTROLLER_MAX_CLIENTS)
			{
				try
				{
					Thread.sleep(1000);
				} catch (final InterruptedException e)
				{
					ctrl.cgui.logMessage("interrupted while CONTROLLER_MAX_CLIENTS idling");
				}
			}
			*/


			try
			{
				// blocks
				socket = serverSocket.accept();

			} catch (final IOException e)
			{
				ctrl.cgui.logMessage("ERRROR: Can't initialize client connection!");
			}

			ctrl.cgui.logMessage("client connection accepted from " + socket.getInetAddress());

			final CtrlServerThread cst = new CtrlServerThread(ctrl, this, "ct-"
					+ connCount, socket);
			getServerThreads().add(cst);
			cst.start();
			
			connCount++;
		}
	}

	/**
	 * Stops the controller server which listens for new connections.
	 */
	public synchronized void stopServer() {

		serverListening = false;

		try
		{
			serverSocket.close();
			ctrl.cgui.logMessage("server shut down. no more connections will be accepted.");
			serverListening = false;

		} catch (final IOException e)
		{
			ctrl.cgui.logMessage("ERRROR: Can't close server socket! O_o");
		}
	}

	/**
	 * Removes the thread from thE INterNaL ServER list.
	 *
	 * @param t the thread to be removed
	 */
	public synchronized void removeThreadFromList(final Thread t) {
		getServerThreads().remove(t);
		ctrl.cgui.updateClientTableData();
	}

	/**
	 * returns a list of all running server threads
	 *
	 * @return the serverThread list
	 */
	public ArrayList<CtrlServerThread> getServerThreads() {
		return serverThreads;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		serverSocket.close();
		super.finalize();
	}

}
