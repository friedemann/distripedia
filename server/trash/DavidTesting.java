package trash;


import grinder.client.user.LocalStorage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;


import websocket.client.DistripediaClient;
import websocket.client.DistripediaClient.ArticleReceivedEvent;
import websocket.client.DistripediaClient.ArticleReceivedHandler;
import websocket.client.WebSocket.CloseEvent;
import websocket.client.WebSocket.CloseHandler;
import websocket.client.WebSocket.MessageEvent;
import websocket.client.WebSocket.MessageHandler;
import websocket.client.WebSocket.OpenEvent;
import websocket.client.WebSocket.OpenHandler;
import websocket.server.NameProvider;
import websocket.server.WebSocketServer;

public class DavidTesting
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		//distriClient();
		NameProvider np = new NameProvider();
	
	}

	private static void distriClient()
	{
		// start websocket server
		Thread wsServer = new Thread(new WebSocketServer("/"), "websocket-server");
		wsServer.start();
		
		LocalStorage ls = new LocalStorage();
		
		try
		{
			for (int i = 0; i < 1; i++)
			{
				final DistripediaClient client = new DistripediaClient(new URI("ws://localhost:8383/"), ls);
				client.setCachedArticlesPreferred(true);
				
				client.addArticleReceivedHandler(new ArticleReceivedHandler()
				{
					@Override
					public void onArticleReceived(ArticleReceivedEvent e)
					{
						System.out.println(e.getContent());
						System.out.println(e.isArticleFromCache());
					}
				});
				
				client.getArticle("article1");
			}
		
	
		
		} catch (URISyntaxException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
