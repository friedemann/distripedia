package websocket.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import websocket.common.Logger;

// listens on port 8383 and serves each client in a separate thread
public class WebSocketServer implements Runnable
{
	// TODO better implementation?
	// maps which article is hosted on which client: each key maps to a List of
	// objects whose clients
	// have the article stored locally
	protected final static ConcurrentHashMap<String, List<DistripediaWorker>> allWorkersWithArticles = new ConcurrentHashMap<String, List<DistripediaWorker>>();

	// TODO better implementation?
	// maps which article is hosted on which client in the stratus network: each
	// key maps to a List of objects whose clients
	// have the article stored locally
	protected final static ConcurrentHashMap<String, List<DistripediaWorker>> stratusWorkersWithArticles = new ConcurrentHashMap<String, List<DistripediaWorker>>();

	// maps req IDs to worker instances (e.g. workers waiting to deliver article
	// to client)
	protected final static ConcurrentHashMap<Integer, DistripediaWorker> reqIdsToWorkers = new ConcurrentHashMap<Integer, DistripediaWorker>();

	// shared random number generator
	protected final static Random rand = new Random();

	// threadsafe counter mechanism for the hashmap keys above
	protected final static AtomicInteger reqId = new AtomicInteger();

	private final static int PORT = 8383;
	private boolean debug = true;
	private int workerNumber = 0;
	private NameProvider nameProvider;

	public static String path = "/";
	
	ServerSocket masterSocket = null;

	public WebSocketServer(String path)
	{
		WebSocketServer.path = path;
		nameProvider = new NameProvider();

		init();
		Logger.log("WebSocket created.");
		
	}

	public void init()
	{
		try
		{
			masterSocket = new ServerSocket(PORT);
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
				Logger.log("ws-client incoming from " + client.getInetAddress().getHostAddress());
				
				DistripediaWorker worker = null;

				try
				{
					worker = new DistripediaWorker(client);
				} catch (Exception e)
				{
					e.printStackTrace();
					client.close();
					continue;
				}

				// if the worker was successfully constructed, start thread
				if (worker != null)
				{
					int id = workerNumber++;
					worker.setId(id);
					
					String name = nameProvider.getName();
					worker.setName(name);
					
					new Thread(worker, name + " (" + id + ")").start();
				}
				
				// TODO keep a thread pool for thread re-use

			} // Loop again, waiting for the next connection
		} catch (Exception e)
		{
			Logger.log("SEVERE! websocket server crash!");
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
