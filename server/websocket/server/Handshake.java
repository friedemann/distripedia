package websocket.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that establishes the handshake between client and server.
 */
public class Handshake
{

	private final static Pattern pathPattern 	= Pattern.compile("(?<=GET )(.*?)(?= HTTP)");
	private final static Pattern hostPattern 	= Pattern.compile("(?<=Host: )(.*?)(?=\r\n)");
	private final static Pattern originPattern 	= Pattern.compile("(?<=Origin: )(.*?)(?=\r\n)");
	private final static Pattern key1Pattern 	= Pattern.compile("(?<=Sec-WebSocket-Key1: )(.*?)(?=\r\n)");
	private final static Pattern key2Pattern 	= Pattern.compile("(?<=Sec-WebSocket-Key2: )(.*?)(?=\r\n)");

	/**
	 * Converts an int to byte array.
	 * 
	 * @param value the value
	 * @return the byte array containing 4 bytes
	 */
	private static byte[] intToByteArray(int value)
	{
		return new byte[] { 
				(byte) (0xFF & (value >> 24)), 
				(byte) (0xFF & (value >> 16)),
				(byte) (0xFF & (value >> 8)), 
				(byte) (0xFF & value) };
	}

	/**
	 * Gets the key number from the secret key string.
	 * 
	 * @param key the key
	 * @return the key number
	 */
	private static int getKeyNumber(String key)
	{
		long number = 0;
		int spaces = 0;

		for (char c : key.toCharArray())
		{
			if (Character.isDigit(c))
				number = number * 10 + (c - '0');
			else if (c == ' ')
				spaces++;
		}
		return (int) (number / spaces);
	}

	/**
	 * Do the funky handshake dance.
	 * 
	 * @param in the in
	 * @param out the out
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static synchronized void doIt(InputStream in, PrintStream out) throws IOException
	{
		try
		{
			byte[] header = new byte[1024];
			int headerSize = in.read(header);

			String host = "";
			String path = "";
			String origin = "";
			String key1 = "";
			String key2 = "";
			boolean isNewWSProtocol = false;

			// convert header array to string and load it into the matcher, load first pattern
			Matcher matcher = pathPattern.matcher(new String(header, 0, headerSize, WebSocket.utf8));

			// get path
			if (matcher.find())
				path = matcher.group();
			matcher.reset();

			// get host
			matcher.usePattern(hostPattern);
			if (matcher.find())
				host = matcher.group();
			matcher.reset();

			// get origin
			matcher.usePattern(originPattern);
			if (matcher.find())
				origin = matcher.group();
			matcher.reset();

			// get key1
			matcher.usePattern(key1Pattern);

			// if we found a key1 that means the client is using the new
			// protocol version v76
			if (matcher.find())
			{
				key1 = matcher.group();
				isNewWSProtocol = true;
			}
			matcher.reset();

			String location = "ws://" + host + path;

			if (isNewWSProtocol)
			{
				// get key2
				matcher.usePattern(key2Pattern);
				if (matcher.find())
					key2 = matcher.group();

				// commence the hixie hixie shake
				int keyNumber1 = getKeyNumber(key1);
				int keyNumber2 = getKeyNumber(key2);

				byte[] keyNumBytes1 = intToByteArray(keyNumber1);
				byte[] keyNumBytes2 = intToByteArray(keyNumber2);

				// get key3 (last 8 bytes of header)
				byte[] key3 = Arrays.copyOfRange(header, headerSize - 8, headerSize);

				// create md5 hash from the keys
				MessageDigest md = MessageDigest.getInstance("MD5");
				md.reset();
				md.update(keyNumBytes1);
				md.update(keyNumBytes2);
				md.update(key3);
				byte[] md5result = md.digest();

				String response = "HTTP/1.1 101 Web Socket Protocol Handshake\r\n"
						+ "Upgrade: WebSocket\r\n" + "Connection: Upgrade\r\n"
						+ "Sec-WebSocket-Origin: " + origin + "\r\n" + "Sec-WebSocket-Location: "
						+ location + "\r\n\r\n";

				out.write(response.getBytes(WebSocket.utf8));
				out.write(md5result);
				out.flush();
				// System.out.println(response);
			}
			else
			{
				// old version
				String response = "HTTP/1.1 101 Web Socket Protocol Handshake\r\n"
						+ "Upgrade: WebSocket\r\n" + "Connection: Upgrade\r\n"
						+ "WebSocket-Origin: " + origin + "\r\n" + "WebSocket-Location: "
						+ location + "\r\n\r\n";

				out.write(response.getBytes(WebSocket.utf8));
				out.flush();
				// System.out.println(response);
			}
		} catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}
}
