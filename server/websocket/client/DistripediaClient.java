package websocket.client;



import grinder.client.user.LocalStorage;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import org.json.JSONException;
import org.json.JSONObject;

import websocket.client.WebSocket.CloseEvent;
import websocket.client.WebSocket.CloseHandler;
import websocket.client.WebSocket.MessageEvent;
import websocket.client.WebSocket.MessageHandler;
import websocket.client.WebSocket.OpenEvent;
import websocket.client.WebSocket.OpenHandler;
import websocket.common.Logger;
import websocket.common.Message;

/**
 * A Java Implementation of the Distripedia JS-Client.
 * 
 * @author David
 */
public class DistripediaClient implements OpenHandler, CloseHandler, MessageHandler
{
	private final WebSocket webSocket;
	private final LocalStorage localStorage;
	
	private ArticleReceivedHandler articleReceivedHandler;

	private boolean debug = false;
	private boolean isCachedArticlesPreferred = false;
	
	
	/**
	 * Sets whether local copies of articles are preferred over
	 * articles from the server.
	 *
	 * @param isCachedArticlesPreferred true for local, false for server
	 */
	public void setCachedArticlesPreferred(boolean isCachedArticlesPreferred)
	{
		this.isCachedArticlesPreferred = isCachedArticlesPreferred;
	}

	/**
	 * Checks if the client prefers articles from the local storage if available
	 *
	 * @return true, if local copies are preferred, false otherwise
	 */
	public boolean isCachedArticlesPreferred()
	{
		return isCachedArticlesPreferred;
	}

	/**
	 * Instantiates a new distripedia client, and connects to the given URI.
	 * 
	 * @param url the url
	 * @param localStorage the local storage
	 */
	public DistripediaClient(final URI url, final LocalStorage localStorage)
	{
		if (url == null || localStorage == null)
		{
			throw new IllegalArgumentException("Parameters cannot be null!");
		}

		this.localStorage = localStorage;

		webSocket = new WebSocket(url);
		webSocket.addOpenHandler(this);
		webSocket.addCloseHandler(this);
		webSocket.addMessageHandler(this);
		webSocket.connect();
	}

	/**
	 * Sends a request to the server to retrieve the article or tries to fetch
	 * it from the localStorage if preferCachedArticles is set to true.
	 * If the server can deliver the article an {@code ArticleEvent} will be
	 * fired.
	 * 
	 * @param article the article
	 */
	public void getArticle(String article)
	{
		if (article == null)
		{
			throw new IllegalArgumentException("Article cannot be null!");
		}

		if (isCachedArticlesPreferred())
		{
			String content = localStorage.getItem(article);
			
			if (content != null)
			{
				// fire event specifying that article came from LS
				handleArticleReceived(article, content, true);
				return;
			}
		}

		try
		{
			JSONObject request = new JSONObject();
			request.put(Message.Key.ACTION, Message.Action.REQUEST_ARTICLE);
			request.put(Message.Key.ARTICLE, article);

			webSocket.send(request.toString());
		} catch (JSONException e)
		{
			e.printStackTrace();
		}	
	}
	
	/**
	 * Disconnects the client from the server.
	 */
	public void disconnect()
	{
		webSocket.close();
	}

	@Override
	public void onOpen(OpenEvent ev)
	{
		debug("open");

		Collection<String> keys = localStorage.keys();
		sendArticlesIds(keys);
		getArticle("Main_Page");
	}

	@Override
	public void onClose(CloseEvent ev)
	{
		debug("close");
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * testing.WebSocket.MessageHandler#onMessage(testing.WebSocket.MessageEvent)
	 */
	@Override
	public void onMessage(MessageEvent ev)
	{
		debug("got message");

		try
		{
			final JSONObject data = new JSONObject(ev.getData());
			final int action = data.getInt(Message.Key.ACTION);

			// action defines the reason why the server sent us something
			switch (action)
			{
			case Message.Action.SEND_ARTICLE:
				handleClientReceivedArticle(data);
				break;
			case Message.Action.SEND_NAME:
				// do nuffin yet
				break;
			case Message.Action.REQUEST_ARTICLE:
				handleServerRequestedArticle(data);
				break;

			case Message.Action.ERROR_ARTICLE_NOT_FOUND:
				handleArticleNotFoundError(data);
				break;
			default:
				Logger.log("@DistripediaClient#onMessage: " + "message action not understood: "
						+ action);
			}

		} catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Handle the situation when the client receives an article:
	 * - Store article in localstorage
	 * - send back article id to server,
	 * - invoke ArticleHandler.
	 * 
	 * @param data the json-data
	 * @throws JSONException
	 */
	private void handleClientReceivedArticle(JSONObject data) throws JSONException
	{
		final String article = data.getString(Message.Key.ARTICLE);
		final String content = data.getString(Message.Key.CONTENT);

		localStorage.setItem(article, content);
		this.sendArticleId(article);
		
		// send article received
		//TODO make nicer
		final JSONObject res = new JSONObject();
		res.put(Message.Key.ACTION, 200);
		res.put(Message.Key.ARTICLE, article);

		webSocket.send(res.toString());

		// fire event
		handleArticleReceived(article, content, false);
	}

	/**
	 * Handles the situation when the server requests an article:
	 * Retrieve article from local storage and deliver it if possible,
	 * otherwise send error message
	 * 
	 * @param data the json-data
	 * @throws JSONException
	 */
	private void handleServerRequestedArticle(JSONObject data) throws JSONException
	{
		final JSONObject response = data;

		final String article = data.getString(Message.Key.ARTICLE);
		final String content = localStorage.getItem(article);

		if (content != null)
		{
			response.put(Message.Key.ACTION, Message.Action.SEND_ARTICLE);
			response.put(Message.Key.CONTENT, content);
		}
		else
		{
			response.put(Message.Key.ACTION, Message.Action.ERROR_ARTICLE_NOT_FOUND);
		}

		webSocket.send(response.toString());
	}

	// TODO add handler 
	private void handleArticleNotFoundError(JSONObject data) throws JSONException
	{
		final String article = data.getString(Message.Key.ARTICLE);

		log("article not found: " + article);
	}

	/**
	 * Sends a single article id back to the server.
	 * 
	 * @param article the article
	 */
	private void sendArticleId(String article)
	{
		this.sendArticlesIds(Arrays.asList(article));
	}

	/**
	 * Sends a collection of article ids to the server
	 * 
	 * @param ids the article ids
	 */
	private void sendArticlesIds(Collection<String> ids)
	{
		try
		{
			final JSONObject data = new JSONObject();
			data.put(Message.Key.ACTION, Message.Action.CLIENT_SEND_ARTICLE_IDS);
			data.put(Message.Key.CONTENT, ids);

			webSocket.send(data.toString());

		} catch (JSONException e)
		{
			e.printStackTrace();
		}
	}

	private void log(String string)
	{
		Logger.log("java distripedia client: " + string);
	}

	/**
	 * Debug logging.
	 * 
	 * @param str the message
	 */
	private void debug(String str)
	{
		if (debug)
			log(str);
	}

	/*
	 * EVENT HANDLER DEFINITIONS
	 */

	/**
	 * The ArticleReceivedHandler is called when an requested article arrives.
	 */
	public interface ArticleReceivedHandler
	{
		public void onArticleReceived(ArticleReceivedEvent e);
	}

	public static class ArticleReceivedEvent
	{
		private String article;
		private String content;
		private boolean isArticleFromCache;

		/**
		 * Checks if this article came from the local storage (cache).
		 *
		 * @return true, if article is from cache
		 */
		public boolean isArticleFromCache()
		{
			return isArticleFromCache;
		}
		
		/**
		 * Gets the article ID.
		 *
		 * @return the article
		 */
		public String getArticle()
		{
			return article;
		}

		/**
		 * Gets the content of the article.
		 *
		 * @return the content
		 */
		public String getContent()
		{
			return content;
		}
		
		public ArticleReceivedEvent(String article, String content, boolean isArticleFromCache)
		{
			this.article			= article;
			this.content 			= content;
			this.isArticleFromCache = isArticleFromCache; 
		}
	}

	public void addArticleReceivedHandler(ArticleReceivedHandler handler)
	{
		this.articleReceivedHandler = handler;
	}

	private void handleArticleReceived(String article, String content, boolean isArticleFromCache)
	{
		if (this.articleReceivedHandler != null)
		{
			articleReceivedHandler.onArticleReceived(new ArticleReceivedEvent(article, content, isArticleFromCache));
		}
	}
}
