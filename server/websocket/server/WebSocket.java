package websocket.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import websocket.common.Logger;

/**
 * WebSocker server implementation.
 * 
 * @author David
 */
public class WebSocket
{
	// need to use utf8 as charset
	public static final Charset utf8 = Charset.forName("UTF-8");

	private final Socket client;
	private final InputStream in;
	private final PrintStream out;
	
	protected final String clientIp;
	
	

	// serves a client at given socket
	public WebSocket(final Socket socket) throws IOException
	{
		client = socket;
		clientIp = client.getInetAddress().getHostAddress();
		
		in = new BufferedInputStream(client.getInputStream());
		out = new PrintStream(client.getOutputStream(), false, "UTF-8");
		
		Handshake.doIt(in, out);	
		
		Logger.log("websocket created for " + clientIp);
	}

	protected void close()
	{
		try
		{
			if (out != null)
				out.close();

			if (in != null)
				in.close();

			if (client != null)
				client.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	// TODO: test this
	@Override
	protected void finalize() throws Throwable
	{
		close();
		super.finalize();
	}
	
	/**
	 * read input stream and extract message from it messages from the client
	 * are send as [0x00 + utf-8 text + 0xFF]
	 * 
	 * @return a String containing the message without start/stop chars or null
	 *         if failed
	 */
	protected String receiveMessage()
	{
		int b;
		try
		{
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
			//	System.out.println((char) b + ", " + b);
				return null;
			}
		} catch (IOException e)
		{
			return null;
		}

	}

	// send message to the client as [0x00 + utf-8 text + 0xFF]
	protected void send(byte[] msg)
	{
		try
		{
			out.write(0x00);
			out.write(msg);
			out.write(0xFF);
			out.flush();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}