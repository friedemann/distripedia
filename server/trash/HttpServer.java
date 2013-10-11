package trash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import websocket.common.Logger;

// server runs single-threaded, sufficient for now because there is only one, simple request from each client
// TODO this class needs some SERIOUS cleaning up to do
/**
 * @deprecated
 */
public class HttpServer implements Runnable
{
	private static final int PORT = 8080;
	private boolean debug = false;

	@Override
	public void run()
	{
		try
		{
			// Create a ServerSocket to listen on that port.
			final ServerSocket masterSocket = new ServerSocket(HttpServer.PORT);
			// Now enter an infinite loop, waiting for connections and handling
			// them.
			while (true)
			{
				Socket client = masterSocket.accept();
				if (debug)
					Logger.log("http-client connected");

				BufferedReader in = new BufferedReader(new InputStreamReader(client
						.getInputStream()));
				PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));

				// read GET
				String header = in.readLine();

				// TODO dirty hack to ignore favicon requests
				if (!header.equals("GET /favicon.ico HTTP/1.1"))
				{
					// TODO parse header, only sent requested file

					// print response headers
					out.println("HTTP/1.0 200 ");
					out.println("Content-Type: text/html");
					out.println();

					// open file to serve
					File f = new File("html/index.html");
					BufferedReader fileReader = new BufferedReader(new FileReader(f));

					// send index.html
					int i = 0;
					while ((i = fileReader.read()) != -1)
					{
						out.write(i);
					}
					out.flush();
					fileReader.close();

					if (debug)
						Logger.log("http - served html");
				}
				else
				{
					if (debug)
						Logger.log("http - skipped favicon");
				}

				out.close();
				in.close();
				client.close();

				if (debug)
					Logger.log("http-client disconnected.");

			} // Loop again, waiting for the next connection

		}
		catch (Exception e)
		{
			// TODO remove pokemon exception (catches em all =) )
			System.err.println(e);
		}

	}

}