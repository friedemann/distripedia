package grinder.controller;

import grinder.client.ClientCommand;
import grinder.client.ClientState;
import grinder.util.GrinderSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;

import javax.swing.ProgressMonitor;

/**
 * The Class CtrlServerThread handles the communication with the connected
 * client - one thread each connection.
 *
 * @author Friedemann
 */
public class CtrlServerThread extends Thread {

	// reference to papa controller
	private final Controller ctrl;
	// reference to papa server
	private final CtrlServer server;

	/** The client socket. */
	private final Socket clientSocket;

	/** Thread name. */
	private final String name;

	private ObjectOutputStream oos;
	private ObjectInputStream ois;

	// client information
	private UUID clientID;
	private InetAddress clientIP;
	private ClientState clientState;
	private String deployedTicketID = "";

	/** Controls the while loop within run() */
	private boolean serverThreadListening = true;

	/** The progress monitor. */
	private ProgressMonitor pMonitor;

	/**
	 * Instantiates a new server thread.
	 *
	 * @param ctrl reference to controller
	 * @param server reference to controlserver thread
	 * @param name thread name
	 * @param socket bound client socket
	 */
	public CtrlServerThread(final Controller ctrl, final CtrlServer server,
			final String name, final Socket socket) {
		super(name);

		this.ctrl = ctrl;
		this.server = server;
		clientSocket = socket;
		this.name = name;

		ctrl.cgui.logMessage("<" + name + "> server connection thread initialized");
	}

	/**
	 * Write to object output stream.
	 *
	 * @param obj the obj
	 */
	public void writeToOutputStream(final Object objOut) {
		if (oos != null)
		{
			try
			{
				oos.writeObject(objOut);
				oos.flush();

			} catch (final IOException e)
			{
				ctrl.cgui.logMessage("<" + name + "> Can't write object to output stream! "
						+ e);
				throw new RuntimeException("output stream fail");
			}
		} else
		{
			ctrl.cgui.logMessage("<" + name
					+ "> Can't write object to output stream! (oos==null)");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		try
		{
			// set up in and output streams
			ois = new ObjectInputStream(clientSocket.getInputStream());
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
			ctrl.cgui.logMessage("<" + name + "> server iostreams set up");

			requestClientInformation();

			// listen for incoming data
			while (serverThreadListening)
			{

				/* *********************************************
				 * the client generally only sends messages about his state - only
				 * exceptions: 1) when the server asks for the log file 2) when the
				 * client receives a test ticket
				 * *********************************************
				 */

				ClientState cstate = null;

				try
				{
					cstate = (ClientState) ois.readObject();
					ctrl.cgui.logMessage("<" + name + "> in: <" + cstate.toString() + ">");
					clientState = cstate;
					ctrl.cgui.updateClientTableData();

				} catch (final ClassCastException e)
				{
					ctrl.cgui
							.logMessage("(�`�._.�(�`�._.receiving alien signals._.���)�._.���)");
				}

				switch (cstate)
				{
				case RECEIVE_TICKET_OK:
					deployedTicketID = (String) ois.readObject();
					ctrl.cgui.updateClientTableData();
					break;

				case ARMED:
					break;

				case CLEANUP:
					deployedTicketID = "";
					ctrl.cgui.updateClientTableData();
					break;

				case TESTING:
					break;

				case FINISHED:
					ctrl.checkForTestFinish();
					break;

				case SENDING_LOGFILE:

					initializeProgressMonitor();

					long bytesReceived = 0;
					final long bytesTotal = ois.readLong(); // get size of total
					// bytes
					// from client

					// open file to write to
					final String fileName = GrinderSettings.CTRL_LOG_FILE_FOLDER
							+ deployedTicketID + ".log";
					final File completeLogFile = new File(fileName);

					// append data if file exists
					final FileOutputStream fos = new FileOutputStream(completeLogFile, true);

					// read in log file
					final int bufsize = GrinderSettings.LOGFILE_SEND_BUFFER_SIZE;
					final byte[] buf = new byte[bufsize];
					
					long bytesReadTotal = 0;
					int currentBytesRead = 0;
					
					while(bytesReadTotal < bytesTotal)
					{
						currentBytesRead = ois.read(buf);
						fos.write(buf, 0, currentBytesRead);
						bytesReadTotal += currentBytesRead;
						updateProgress(bytesReadTotal, bytesTotal);
					}
					
					
					/*
					final int timesToRead = (int) (bytesTotal / bufsize);
					final int overhead = (int) (bytesTotal % bufsize);

					for (int i = 0; i < timesToRead; i++)
					{
						ois.read(buf);
						fos.write(buf);
						bytesReceived += bufsize;

						updateProgress(bytesReceived, bytesTotal);
					}

					// read to 1 byte exact
					if (overhead > 0) {
						ois.read(buf, 0, overhead);
						fos.write(buf, 0, overhead);
						bytesReceived += overhead;
						updateProgress(bytesReceived, bytesTotal);
					}*/

					fos.flush();
					fos.close();

					ctrl.cgui.logMessage("<" + name + "> client log file received! ("
							+ bytesReadTotal + " bytes)");

					writeToOutputStream(ClientCommand.SEND_LOGFILE_OK);
					break;

				case LOGFILE_SENT:
					break;

				case BYE:
					writeToOutputStream(ClientCommand.BYE);
					break;
				}

			}

		} catch (final ClassNotFoundException e1) // error while casting to
		// ClientState
		{
			ctrl.cgui.logMessage("<" + name + "> class not found exception! " + e1);
		} catch (final OptionalDataException e)
		{
			ctrl.cgui.logMessage("<" + name + "> " + e +", "+ e.length);
			ctrl.cgui.updateClientTableData();
		} catch (final IOException e)
		{
			ctrl.cgui.logMessage("<" + name + "> client connection lost! " + e);
			server.removeThreadFromList(this);
			ctrl.cgui.updateClientTableData();
		} catch (RuntimeException re)
		{
			serverThreadListening = false;
		}


		// clean up
		try
		{
			oos.close();
			ois.close();
			clientSocket.close();

		} catch (final IOException e)
		{
			ctrl.cgui.logMessage("<" + name + "> cannot close streams or socket!");
		}

		ctrl.cgui.logMessage("<" + name + "> server connection thread stopped");
		server.removeThreadFromList(this);
	}

	/**
	 * Request client information. Receives clientID and initial status from the
	 * client.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException the class not found exception
	 */
	private void requestClientInformation() throws IOException, ClassNotFoundException {

		oos.writeObject(ClientCommand.SEND_INFORMATION);
		oos.flush();

		final Object objIn = ois.readObject();
		clientID = (UUID) objIn;

		final Object objIn2 = ois.readObject();
		clientState = (ClientState) objIn2;

		clientIP = clientSocket.getInetAddress();

		ctrl.cgui.logMessage("<" + name + "> client details received: " + clientID + ", "
				+ clientState + ", " + clientIP);

		ctrl.cgui.updateClientTableData();
	}

	/**
	 * Initialize a new progress monitor (needs to be renewed for each task!)
	 */
	private void initializeProgressMonitor() {
		pMonitor = new ProgressMonitor(ctrl.cgui, "<" + name
				+ "> Receiving Logfile...", "waiting", 0, 100);

		pMonitor.setMillisToDecideToPopup(0);
		pMonitor.setMillisToPopup(0);
	}

	/**
	 * Update the progress monitor
	 *
	 * @param bytesReceived the bytes received
	 * @param bytesTotal the bytes total to receive
	 */
	private void updateProgress(final long bytesReceived, final long bytesTotal) {

		final Double percent = ((double) bytesReceived * 100 / bytesTotal);
		pMonitor.setProgress(percent.intValue());
		pMonitor.setNote(String.format("%.1f%% (%d/%d bytes)", percent,
				bytesReceived, bytesTotal));
	}

	/**
	 * @return the clientID
	 */
	public final UUID getClientID() {
		return clientID;
	}

	/**
	 * @return the clientIP
	 */
	public final InetAddress getClientIP() {
		return clientIP;
	}

	/**
	 * @return the clientState
	 */
	public final ClientState getClientState() {
		return clientState;
	}

	/**
	 * @return the deployedTicketID
	 */
	public final String getDeployedTicketID() {
		return deployedTicketID;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		clientSocket.close();
		super.finalize();
	}
}
