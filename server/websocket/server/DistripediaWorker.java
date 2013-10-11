package websocket.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import websocket.common.Logger;
import websocket.common.Message;
import websocket.common.Util;

public class DistripediaWorker extends WebSocket implements Runnable
{
	// holds all article titles that the server assumes are stored on the client
	private final Set<String> clientArticles = new HashSet<String>();

	// holds the stratus client if the client is a stratus client (256bit hash)
	private String stratusPeerId = null;

	// this objects id
	private int id = -1;
	
	private String name = "null";

	public void setId(int id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}

	private boolean hasStratusClient()
	{
		return stratusPeerId != null;
	}

	// serves a client at given socket
	public DistripediaWorker(final Socket socket) throws IOException
	{
		super(socket);
	}

	public void run()
	{
		try
		{
			// send name to client
			sendClientName();
			
			// method blocks until something bad happens
			listenForMessages();

			// not listening anymore, close streams/connection
			close();

			// if we reach this point the client has (been) disconnected
			log("client disconnected.");
			logActivity(108, getName(), "Server", hasStratusClient(), "client logging off");

			// remove all links to articles
			removeAllArticleLinks();
		} catch (Exception e)
		{
			Logger.log("SEVERE! distripedia worker crash!");
			e.printStackTrace();
		}
	}

	// removes all links to the articles that the client has from the server
	// hashmaps
	private void removeAllArticleLinks()
	{
		for (String article : clientArticles)
		{
			// remove articles from hashmap for all workers
			removeArticleLinkFromMap(article, WebSocketServer.allWorkersWithArticles);

			// also remove from hashmap for stratus clients if client was one
			if (this.hasStratusClient())
			{
				removeArticleLinkFromMap(article, WebSocketServer.stratusWorkersWithArticles);
			}
		}

		// empty the set now
		clientArticles.clear();

		// Logger.log("article/worker hash map information:");
		// Logger.log("| stratus: " + WebSocketServer.stratusWorkersWithArticles.toString());
		// Logger.log("| all: " + WebSocketServer.allWorkersWithArticles.toString());
	}

	// removes an article from from the server hashmap
	private void removeArticleLinkFromMap(String article, Map<String, List<DistripediaWorker>> map)
	{
		List<DistripediaWorker> articleList = map.get(article);

		synchronized (articleList)
		{
			articleList.remove(this);
		}
	}

	// adds one article and this worker instance to a server hashmap
	private void addArticleLinkToMap(String article,
			ConcurrentHashMap<String, List<DistripediaWorker>> map)
	{
		List<DistripediaWorker> articleList = map.get(article);

		if (articleList == null)
		{
			articleList = new ArrayList<DistripediaWorker>();
			map.put(article, articleList);
		}

		// lock the list because arraylist is not synchronized and other threads
		// might be accessing it too
		synchronized (articleList)
		{
			articleList.add(this);
		}
	}

	// copies all articles that the client has locally to the map of stratus
	// workers
	private void copyArticleLinksToStratusWorkerMap()
	{
		for (String article : clientArticles)
		{
			addArticleLinkToMap(article, WebSocketServer.stratusWorkersWithArticles);
		}

	}

	private void sendClientName() throws JSONException
	{
		JSONObject jsonSendName = new JSONObject();
		jsonSendName.put(Message.Key.ACTION, Message.Action.SEND_NAME);
		jsonSendName.put(Message.Key.CONTENT, getName());
		sendMessage(jsonSendName);
	}

	// this methods blocks until client closes connection
	private void listenForMessages()
	{
	
		// loop and read input
		while (true)
		{
			// get message from client
			final String received = receiveMessage();

			if (received == null)
				break;
						
			// we received a message from the client, now parse it and decide on
			// how to react
			try
			{			
				// parse json data sent to server
				final JSONObject jsonMessage = new JSONObject(received);
				final int action = jsonMessage.getInt(Message.Key.ACTION);

				// action defines the reason why the client sent us something
				switch (action)
				{
				case Message.Action.REQUEST_ARTICLE:
					handleClientRequestedArticle(jsonMessage);
					break;
				case Message.Action.SEND_ARTICLE:
					handleClientSentArticle(jsonMessage);
					break;
				case Message.Action.CLIENT_ARTICLE_RECEIVED:
					handleClientSentArticleReceived(jsonMessage);
					break;
				case Message.Action.CLIENT_SEND_ARTICLE_IDS:
					handleClientSentArticleIds(jsonMessage);
					break;
				case Message.Action.CLIENT_STORE_STRATUS_ID:
					handleClientSentStratusId(jsonMessage);
					break;
				default:
					Logger.log("@DistripediaWorker#listenForMessages: "
							+ "message action not understood: " + action);
				}
			} catch (JSONException e)
			{
				// json malformed
				e.printStackTrace();
			}
		}
	}

	// client sent confirmation that he received the article
	private void handleClientSentArticleReceived(JSONObject jsonMessage) throws JSONException
	{
		final String article = jsonMessage.getString(Message.Key.ARTICLE);
		log("client received article: " + article);
		logActivity(205, getName(), "Server", hasStratusClient(), String.valueOf(clientArticles.size()));
	}

	// client sent us articles he has, store this information on the server
	private void handleClientSentArticleIds(final JSONObject jsonMessage) throws JSONException
	{
		final JSONArray articles = jsonMessage.getJSONArray(Message.Key.CONTENT);
		log("client sent Article IDs: " + articles.toString());

		// put articles in hashmap
		for (int i = 0; i < articles.length(); i++)
		{
			String article = articles.getString(i);
			
			// TODO workaround for main page: dont add to lists
			if (article.equalsIgnoreCase("Main_Page"))
				continue;

			// store article in the server-wide hashmap for all workers,
			// maybe on the map for stratus workers as well,
			// and on this worker's list of articles
			boolean isArticleAdded = clientArticles.add(article);

			// only add articles to the server maps if they haven't been
			// submitted before
			// e.g. client sent article id twice for some reason
			if (isArticleAdded)
			{
				addArticleLinkToMap(article, WebSocketServer.allWorkersWithArticles);

				if (this.hasStratusClient())
				{
					addArticleLinkToMap(article, WebSocketServer.stratusWorkersWithArticles);
				}
			}
		}

//		logActivity(205, getName(), "Server", hasStratusClient(),
//				String.valueOf(clientArticles.size()));
	}

	// server received stratus id from client
	private void handleClientSentStratusId(final JSONObject jsonMessage) throws JSONException
	{
		// 10/07/10: ID was sent twice, no one knows why
		// only copy articles if the ID is sent the first time, avoid duplicates
		if (stratusPeerId == null)
		{
			copyArticleLinksToStratusWorkerMap();
		}
		
		stratusPeerId = jsonMessage.getString(Message.Key.CONTENT);
		log("client sent Stratus ID: " + stratusPeerId);
	}

	// server received article from client, forwards it to the waiting client
	private void handleClientSentArticle(final JSONObject jsonMessage) throws JSONException
	{
		final int reqId = jsonMessage.getInt(Message.Key.ID);
		log("client sent article: " + jsonMessage.getString(Message.Key.ARTICLE));

		// get worker whose client is waiting
		final DistripediaWorker worker = WebSocketServer.reqIdsToWorkers.get(reqId);

		if (worker != null)
		{
			logActivity(205, getName(), "Server", false, String.valueOf(clientArticles.size()));
			log("server forwarding article to: " + worker.getName());
			worker.sendMessage(jsonMessage);
			// remove worker from the waiting list
			WebSocketServer.reqIdsToWorkers.remove(reqId);
		}

	}

	// client requested article
	private void handleClientRequestedArticle(final JSONObject jsonMessage) throws JSONException
	{
		final String article = jsonMessage.getString(Message.Key.ARTICLE);
		log("client requested article: " + article);
		logActivity(101, getName(), "Server", hasStratusClient(), article);

		// a) ask other client to send us the article directly through stratus
		if (hasStratusClient())
		{
			if (getArticleThroughStratus(article))
			{
				return;
			}
		}

		// b) ask other client to send us the article through the webserver
		if (getArticleThroughWebsocket(article))
		{
			return;
		}

		// c) load article from server
		if (getArticleFromServer(article))
		{
			return;
		}

		// d) load article directly from wikipedia
		if (getArticleFromWikipedia(article))
		{
			return;
		}

		// d) article not found
		log("article not found on server: " + article);
		logActivity(404, "Server", getName(), hasStratusClient(), article);
		
		// file doesnt exist on server, send error
		JSONObject jsonSendArticle = new JSONObject();
		jsonSendArticle.put(Message.Key.ACTION, Message.Action.ERROR_ARTICLE_NOT_FOUND);
		jsonSendArticle.put(Message.Key.ARTICLE, article);
		jsonSendArticle.put(Message.Key.CONTENT, "404: file not found on server");

		sendMessage(jsonSendArticle);

	}

	private boolean getArticleThroughStratus(String article) throws JSONException
	{
		List<DistripediaWorker> stratusWorkersWithArticle = WebSocketServer.stratusWorkersWithArticles
				.get(article);

		if (stratusWorkersWithArticle != null && !stratusWorkersWithArticle.isEmpty())
		{

			// get a random worker who has the article and send request through
			// him
			int randIndex = WebSocketServer.rand.nextInt(stratusWorkersWithArticle.size());
			DistripediaWorker worker = stratusWorkersWithArticle.get(randIndex);

			// TODO: just temporary fix
			if (worker.hasStratusClient())
			{
				// send information about farPeer to the nearPeer
				log("server sending to client the stratus id from: " + worker.getName());
				logActivity(203, "Server", getName(), hasStratusClient(), worker.getName());

				// prepare json and send the file
				JSONObject jsonSendArticle = new JSONObject();

				jsonSendArticle.put(Message.Key.ACTION, Message.Action.SEND_FAR_STRATUS_PEER_ID);
				jsonSendArticle.put(Message.Key.ARTICLE, article);
				jsonSendArticle.put(Message.Key.CONTENT, worker.stratusPeerId);

				sendMessage(jsonSendArticle);

				return true;
			}
		}

		return false;
	}

	private boolean getArticleThroughWebsocket(String article) throws JSONException
	{
		List<DistripediaWorker> workersWithArticle = WebSocketServer.allWorkersWithArticles
				.get(article);

		if (workersWithArticle != null && !workersWithArticle.isEmpty())
		{
			// get a random worker who has the article and send request through
			// him
			int randIndex = WebSocketServer.rand.nextInt(workersWithArticle.size());
			DistripediaWorker worker = workersWithArticle.get(randIndex);

			// we will forward this message to a client that has the article
			log("server requesting article from other worker: " + worker.getName());
			logActivity(101, "Server", worker.getName(), hasStratusClient(), article);

			// prepare json and send the file
			JSONObject jsonMessage = new JSONObject();

			// set a unique to this article request, so we will know
			// later on who issued the request
			int reqId = WebSocketServer.reqId.getAndIncrement();
			jsonMessage.put(Message.Key.ID, reqId);
			jsonMessage.put(Message.Key.ARTICLE, article);
			jsonMessage.put(Message.Key.ACTION, Message.Action.REQUEST_ARTICLE);

			// save the pair of ID and this object, so the worker that receives
			// the article knows which worker requested it
			WebSocketServer.reqIdsToWorkers.put(reqId, this);

			worker.sendMessage(jsonMessage);

			// TODO what happens if other client cant deliver the article? this
			// client will wait forever

			return true;
		}

		return false;
	}

	private boolean getArticleFromServer(String article) throws JSONException
	{
		File file = new File(WebSocketServer.path + "/wiki/" + article + ".html");

		if (file.exists())
		{
			log("server sending article via file system: " + article);

			StringBuilder contents = new StringBuilder();

			try
			{
				BufferedReader input = new BufferedReader(new FileReader(file));
				try
				{
					String line = null;
					while ((line = input.readLine()) != null)
					{
						contents.append(line);
					}
				} finally
				{
					input.close();
				}
			} catch (IOException ex)
			{
				ex.printStackTrace();
			}

			JSONObject jsonSendArticle = new JSONObject();
			jsonSendArticle.put(Message.Key.ACTION, Message.Action.SEND_ARTICLE);
			jsonSendArticle.put(Message.Key.ARTICLE, article);
			jsonSendArticle.put(Message.Key.CONTENT, contents);

			sendMessage(jsonSendArticle);

			return true;
		} else
		{
			return false;
		}
	}

	private boolean getArticleFromWikipedia(String article) throws JSONException
	{
		log("Server trying to get article from wikipedia: " + article);
		logActivity(101, "Server", "Wikipedia", false, article);

		try
		{
			String content = wikipedia.Main.get_article(article);

			JSONObject jsonSendArticle = new JSONObject();
			jsonSendArticle.put(Message.Key.ACTION, Message.Action.SEND_ARTICLE);
			jsonSendArticle.put(Message.Key.ARTICLE, article);
			jsonSendArticle.put(Message.Key.CONTENT, content);

			sendMessage(jsonSendArticle);
		} catch (IOException e)
		{
			return false;
		}

		return true;
	}

	// synchronized because another thread can access the method requesting an
	// article from the client
	private synchronized void sendMessage(JSONObject json)
	{
		super.send(json.toString().getBytes(utf8));
	}

	private void log(String msg)
	{
		Logger.log(name + " (" + id + ")" + ": " + clientIp + ": " + msg);
	}

	private void logActivity(int action, String from, String to, boolean isStratus, String content)
	{
		if (ActivityMonitoringServer.hasClient())
		{
			ActivityMonitoringServer.getWorker().log(action, from, to, isStratus, content);
		}
	}
}