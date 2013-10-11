package websocket.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URI;



/**
 * An implementation of the websockets interface as
 * defined in {@link http://dev.w3.org/html5/websockets/}
 * and specified in {@link http://www.whatwg.org/specs/web-socket-protocol/}.
 * 
 */
public class WebSocket implements Runnable
{
	/** The thread that constantly reads from the input stream. */
	private Thread asyncReadingThread;

	/** The URL the client is connected to. */
	private final URI url;

	/** The socket. */
	private Socket socket;
	
	/** The input stream. */
	private InputStream in;
	
	/** The output stream. */
	private PrintStream out;

	/** The ready state attribute. */
	private ReadyState readyState;
	
	/** The open handler. */
	private OpenHandler openHandler;
	
	/** The message handler. */
	private MessageHandler messageHandler;
	
	/** The close handler. */
	private CloseHandler closeHandler;

	/**
	 * Gets the url.
	 *
	 * @return the url
	 */
	public URI getUrl()
	{
		return url;
	}

	/**
	 * Gets the ready state.
	 *
	 * @return the ready state
	 */
	public ReadyState getReadyState()
	{
		return readyState;
	}

	/**
	 * Instantiates a new web socket. 
	 * Does not automatically connect to the server.
	 *
	 * @param url the url
	 */
	public WebSocket(final URI url)
	{
		this.url = url;

		final String protocol = url.getScheme();
		final int port = url.getPort();

		if (!protocol.equals("ws"))
		{
			throw new IllegalArgumentException("Unsupported protocol: " + protocol);
		}

		if (port == -1)
		{
			throw new IllegalArgumentException("Port undefined!");
		}
		
		readyState = ReadyState.Connecting;
	}

	/**
	 * Establishes a websocket connection with the server.
	 *
	 */
	public void connect()
	{
		final String host = url.getHost();
		final int port = url.getPort();
		
		try
		{
			socket = new Socket(host, port);
			in = new BufferedInputStream(socket.getInputStream());
			out = new PrintStream(socket.getOutputStream(), false, "UTF-8");
			
			exchangeHandshakes();
		} catch (IOException e)
		{
			close();
			e.printStackTrace();
		}
		
		readyState = ReadyState.Open;
		handleOpen();
		

		// start reading the input stream
		asyncReadingThread = new Thread(this, "websocket client");
		asyncReadingThread.start();
	}

	/**
	 * Exchanges handshakes with the server.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void exchangeHandshakes() throws IOException
	{
		sendOpeningClientHandshake();
		retrieveOpeningServerHanshake();
	}

	/**
	 * Sends the opening client handshake to the server.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void sendOpeningClientHandshake() throws IOException
	{
		final String path = url.getPath();
		final String host = url.getHost();
		final String origin = "http://" + host;
	
		final String request = "GET " + path + " HTTP/1.1\r\n" + "Upgrade: WebSocket\r\n"
				+ "Connection: Upgrade\r\n" + "Host: " + host + "\r\n" + "Origin: " + origin
				+ "\r\n\r\n";
	
		out.write(request.getBytes("UTF-8"));
		out.flush();
	}

	/**
	 * Retrieves the server's opening handshake.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private void retrieveOpeningServerHanshake() throws IOException
	{
		final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		
		String header = reader.readLine();
		if (!header.equals("HTTP/1.1 101 Web Socket Protocol Handshake"))
		{
			throw new IOException("Invalid handshake response");
		}

		header = reader.readLine();
		if (!header.equals("Upgrade: WebSocket"))
		{
			throw new IOException("Invalid handshake response");
		}

		header = reader.readLine();
		if (!header.equals("Connection: Upgrade"))
		{
			throw new IOException("Invalid handshake response");
		}

		do
		{
			header = reader.readLine();
		} while (!header.equals(""));
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				String message = this.read();
				handleMessage(message);
		
			} catch (IOException e)
			{
				break;
			}
		}
		
		close();
	}

	/**
	 * Reads input stream and extract message from it. Messages from the client
	 * are send as [0x00 + utf-8 text + 0xFF].
	 *
	 * @return a String containing the message without start/stop chars or null
	 * if failed
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private String read() throws IOException
	{
		int b;

		if ((b = in.read()) == 0x00)
		{
			StringBuilder buffer = new StringBuilder(256);

			while ((b = in.read()) != 0xFF)
			{
				buffer.append((char) b);
			}

			return buffer.toString();
		}
		else
		{
			// input didnt start with 0x00 (something went wrong)
			System.out.println((char) b + ", " + b);
			throw new IOException();
		}

	}

	/**
	 * Sends a message to the server.
	 *
	 * @param message the message
	 * @return true, if successful
	 */
	public boolean send(String message)
	{
		switch (readyState)
		{
		case Connecting:
			throw new RuntimeException("Invalid state error");
			
		case Closing:
		case Closed:
			return false;
		}

		// only allow open state to send:
		try
		{
			out.write(0x00);
			out.write(message.getBytes("UTF-8"));
			out.write(0xFF);
			out.flush();
		} catch (IOException e)
		{
			e.printStackTrace();
			close();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Closes the websocket connection.
	 *
	 */
	public void close()
	{
		if (readyState == ReadyState.Closing || readyState == ReadyState.Closed)
		{
			return;
		}
		
		if (readyState == ReadyState.Open)
		{
			// send termination char to server (fake closing handshake)
			out.write(0xFF);
			out.flush();
		}
			
		readyState = ReadyState.Closing;			
	
		try
		{
			if (in != null)
				in.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			if (out != null)
				out.close();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		
		try
		{
			if (socket != null)
				socket.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		readyState = ReadyState.Closed;
		handleClose();
	}

	/*
	 * EVENT HANDLER DEFINITIONS
	 */

	/**
	 * The onopen handler is called when the connection is established.
	 *
	 * @param handler the handler
	 */
	public void addOpenHandler(final OpenHandler handler)
	{
		openHandler = handler;
	}

	public static class OpenEvent
	{
	}

	private void handleOpen()
	{
		if (openHandler != null)
			openHandler.onOpen(new OpenEvent());
	}

	public interface OpenHandler
	{
		void onOpen(OpenEvent ev);
	}

	/**
	 * The onmessage handler is called when data arrives.
	 *
	 * @param handler the handler
	 */
	public void addMessageHandler(final MessageHandler handler)
	{
		messageHandler = handler;
	}

	public static class MessageEvent
	{
		private final String data;

		public MessageEvent(String data)
		{
			this.data = data;
		}

		public String getData()
		{
			return data;
		}
	}

	public interface MessageHandler
	{
		/**
		 * The onmessage method is invoked when data arrives.
		 *
		 * @param ev the event
		 */
		void onMessage(MessageEvent ev);
	}

	protected void handleMessage(String data)
	{
		if (messageHandler != null)
			messageHandler.onMessage(new MessageEvent(data));
	}

	/**
	 * The onclose handler is called when the connection is terminated.
	 *
	 * @param handler the handler
	 */
	public void addCloseHandler(final CloseHandler handler)
	{
		closeHandler = handler;
	}

	public static class CloseEvent
	{
	}

	public interface CloseHandler
	{
		void onClose(CloseEvent ev);
	}

	protected void handleClose()
	{
		if (closeHandler != null)
			closeHandler.onClose(new CloseEvent());
	}
}
