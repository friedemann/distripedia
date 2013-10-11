package websocket.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import websocket.common.Logger;

// quick and dirty......
public class ActivityMonitoringServer implements Runnable
{
	private final static int PORT = 8484;

	private ServerSocket masterSocket = null;
	private static ActivityWorker worker = null;
	
	public static boolean hasClient()
	{
		return worker != null;
	}
	
	public static ActivityWorker getWorker()
	{
		return worker;
	}
	
	public ActivityMonitoringServer()
	{
		init();
	}

	
	public void init()
	{
		try
		{
			masterSocket = new ServerSocket(PORT);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		Logger.log("Activity monitor online!");
	}

	public void run()
	{
		try
		{
			while (true)
			{
				// accept connections and start a new thread for each
				// DistripediaWorker
				Socket client = masterSocket.accept();
				Logger.log("activity client incoming from " + client.getInetAddress().getHostAddress());
				

				try
				{
					worker = new ActivityWorker(client);
				} catch (Exception e)
				{
					e.printStackTrace();
					client.close();
					continue;
				}

				// if the worker was successfully constructed, start thread
				if (worker != null)
				{
				//	new Thread(worker, "activity-worker").start();
				}

			} // Loop again, waiting for the next connection
		} catch (Exception e)
		{
			Logger.log("SEVERE! activity server crash!");
			e.printStackTrace();
			
		} finally
		{
			stop();
		}
	}

	public void stop()
	{
		try
		{
			if (masterSocket != null && !masterSocket.isClosed())
			{
				Logger.log("Closing server socket!");
				masterSocket.close();
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
