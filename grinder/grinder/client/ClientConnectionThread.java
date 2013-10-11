package grinder.client;

import grinder.util.GrinderSettings;
import grinder.util.TestTicket;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * The Class ClientConnectionThread takes delivery of messages or objects from
 * the controller. It runs in an own thread, keeping the client-thread free to
 * build and control the >users<.
 *
 * @author Friedemann
 */
public class ClientConnectionThread extends Thread {

	/** The client. */
	private final Client client;

	/** The client socket. */
	private final Socket clientSocket;

	/** The ois. */
	private ObjectInputStream ois;

	/** The oos. */
	private ObjectOutputStream oos;

	/**
	 * Instantiates a new client connection thread.
	 *
	 * @param client the client
	 * @param clientSocket the client socket
	 * @param string
	 */
	public ClientConnectionThread(final Client client, final Socket clientSocket,
			final String name) {
		super(name);

		this.client = client;
		this.clientSocket = clientSocket;

		client.cgui.logMessage("client connection thread initialized...");
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		try
		{
			// initialize streams
			oos = new ObjectOutputStream(clientSocket.getOutputStream());
			oos.flush(); // establish oos-ois connection

			ois = new ObjectInputStream(clientSocket.getInputStream());

			// listen for incoming data
			while (client.isListening())
			{
				try
				{
					ClientCommand cmd = null;

					try
					{
						cmd = (ClientCommand) ois.readObject();
					} catch (final ClassCastException e)
					{
						client.cgui
								.logMessage("(�`�._.�(�`�._.receiving alien signals._.���)�._.���)");
					}

					client.cgui.logMessage("in: <" + cmd.toString() + ">");

					/** see @ClientCommand */
					switch (cmd)
					{

					case SEND_INFORMATION:

						// send client details
						oos.writeObject(client.getClientID());
						oos.flush();
						oos.writeObject(client.getState());
						oos.flush();
						break;

					case RECEIVE_TESTTICKET:

						final TestTicket t = (TestTicket) ois.readObject();

						// immediately respond with ticket id
						client.setState(ClientState.RECEIVE_TICKET_OK);

						oos.writeObject(t.getTestID());
						oos.flush();

						client.cgui.logMessage("test ticket (" + t.getTestID() + ") received");
						client.setTestTicket(t);
						break;

					case SEND_LOGFILE:

						final FileInputStream fis = new FileInputStream(client.getLogFile());
						final long totalBytesAvailable = client.getLogFile().length();

						client.cgui.logMessage("sending logfile (" + totalBytesAvailable
								+ " bytes)");

						client.setState(ClientState.SENDING_LOGFILE);

						// send logfile size
						oos.writeLong(totalBytesAvailable);
						oos.flush();

						final int bufsize = GrinderSettings.LOGFILE_SEND_BUFFER_SIZE;
						final byte[] buf = new byte[bufsize];

						int byteRead = 0;
						while ((byteRead = fis.read(buf)) > 0)
						{
							oos.write(buf, 0, byteRead);
							oos.flush();
						}
						
						/*
						// write buffer overhead to 1 byte exact
						if (byteRead > 0)
						{
							oos.write(buf, 0, byteRead);
						}
						*/						
						
						fis.close();

						break;

					case SEND_LOGFILE_OK:
						client.setState(ClientState.LOGFILE_SENT);
						client.closeLogFile();
						break;

					case PANIC:
						client.panic("controller");
						break;

					case BYE:
						// clean up
						ois.close();
						oos.close();
						clientSocket.close();
						client.cgui.logMessage("socket closed");
						break;
					}

				} catch (final ClassNotFoundException e)
				{
					client.cgui.logMessage("unkown object received!");
				}
			}

		} catch (final IOException e)
		{
			client.cgui.logMessage("server connection lost!");
			client.cgui.resetClientGUIControls();
		}
	}

	/**
	 * Writes directly to object output stream.
	 *
	 * @param obj the obj (e.g. ClientState)
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
				client.cgui.logMessage("Can't write object to output stream! " + e);
			}
		} else
		{
			client.cgui.logMessage("Can't write object to output stream! (oos==null)");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {

		oos.close();
		ois.close();
		clientSocket.close();
		super.finalize();
	}

}
