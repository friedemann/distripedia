package grinder.client.user;

import grinder.util.GrinderSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import websocket.client.DistripediaClient;
import websocket.client.DistripediaClient.ArticleReceivedEvent;
import websocket.client.DistripediaClient.ArticleReceivedHandler;

/**
 * Simulating a very stripped "browser". After initialized it can retrieve
 * distripedia webpages via a websocket connection or a regular http request.
 * The fallback to a regular http-request only counts the data received.
 *
 * @author Friedemann
 */
public class Browser implements Runnable, ArticleReceivedHandler {

	/** @see BrowserType */
	private final BrowserType type;

	/** The unique browser id. */
	private final UUID browserID;

	/** stores the http uri */
	private final URI baseURI;

	/** Stores the websocket uri */
	private final URI baseURIws;

	/** The local storage object. */
	private LocalStorage LS = null;

	/** The distripedia client object handling the websocket connections. */
	private DistripediaClient dpc;

	private Socket stratusSocket;

	private final boolean hasArticleReceived = false;

	private ArticleReceivedEvent articleReceivedEvent = null;

	private final Object waitingForArticleLock = new Object();

	public Browser(final BrowserType type, final URI uriHTTP, final URI uriWS) {
		browserID = UUID.randomUUID();
		this.type = type;

		baseURI = uriHTTP;
		baseURIws = uriWS;

		switch (type)
		{
		case WEBSOCKET:
			LS = new LocalStorage();
			dpc = new DistripediaClient(uriWS, LS);
			dpc.setCachedArticlesPreferred(true);
			dpc.addArticleReceivedHandler(this);
			break;

		case STRATUS:
			LS = new LocalStorage();
			break;
		}
	}

	/**
	 * Gets the browser id.
	 *
	 * @return the browser id
	 */
	public UUID getBrowserID() {
		return browserID;
	}

	/**
	 * Gets the browser type.
	 *
	 * @return BrowserType the browser type
	 * @see BrowserType
	 */
	public BrowserType getBrowserType() {
		return type;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * websocket.client.DistripediaClient.ArticleReceivedHandler#onArticleReceived
	 * (websocket.client.DistripediaClient.ArticleReceivedEvent)
	 */
	@Override
	public void onArticleReceived(final ArticleReceivedEvent e) {
		articleReceivedEvent = e;

		// new (david)
		synchronized (waitingForArticleLock)
		{
			waitingForArticleLock.notify();
		}
		// end new
	}

	public ArticleRequest retrieveArticle(final String articleID) {

		final ArticleRequest ar = new ArticleRequest();

		switch (type)
		{
		case HTTP:

			retrieveArticleViaHTTP(ar, baseURI.toString() + articleID + ".html");
			break;

		case WEBSOCKET:
			articleReceivedEvent = null;

			dpc.getArticle(articleID);

			// new (david)
			try
			{
				synchronized (waitingForArticleLock)
				{
					waitingForArticleLock
							.wait(GrinderSettings.CLIENT_WEBSOCKET_RECEIVE_TIMEOUT);
				}
			} catch (final InterruptedException e)
			{}
			// end new

			// final long endTime = System.currentTimeMillis() +
			// GrinderSettings.CLIENT_WEBSOCKET_RECEIVE_TIMEOUT;
			// while (!hasArticleReceived)
			// {
			//
			// if (endTime < System.currentTimeMillis())
			// {
			// break;
			// }
			// }

			if (articleReceivedEvent != null)
			{
				final int articleSize = articleReceivedEvent.getContent().length();
				final String articleTitle = articleReceivedEvent.getArticle();

				ar.articleIdReceived = articleTitle;
				ar.bytesReceived = articleSize;
			} else
			{
				ar.articleIdReceived = "TIMEOUT";
				ar.bytesReceived = -1;
			}
			break;

		case STRATUS:
			// TODO implement stratus emulating browser connections

			// get browserid from server
			// setup connection
			// receive article from peer user
			// close connection

			break;
		}

		return ar;
	}

	/**
	 * Retrieve article(-size) via http.
	 *
	 * @param address the address
	 * @return int bytes received
	 */
	public ArticleRequest retrieveArticleViaHTTP(final ArticleRequest ar,
			final String address) {
		URL url = null;
		try
		{
			url = new URL(address);
		} catch (final MalformedURLException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String html = "";

		try
		{
			final BufferedReader br = new BufferedReader(new InputStreamReader(url
					.openStream()));
			String line = br.readLine();

			while (line != null)
			{
				html = html.concat(line);
				line = br.readLine();
			}

		} catch (final IOException e)
		{
			e.printStackTrace();
		}

		String[] uriParts;
		uriParts = address.split("/");

		ar.articleIdReceived = uriParts[uriParts.length - 1].replaceAll("\\.html", "");
		ar.bytesReceived = html.length();

		return ar;
	}

	@Override
	public void run() {
		// TODO Stratus implementation
		if (type == BrowserType.STRATUS)
		{

		}
	}

	/**
	 * Closes the websocket connection
	 */
	public void close() {
		if (type == BrowserType.WEBSOCKET)
		{
			dpc.disconnect();
		}
	}
}
