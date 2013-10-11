package websocket.server;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.json.JSONException;
import org.json.JSONObject;



import websocket.common.Logger;
import websocket.server.WebSocketServer;

/**
 * Application Lifecycle Listener implementation class ContextListener
 * 
 */
public class ContextListener implements ServletContextListener
{

	Thread distriServerThread = null;
	WebSocketServer distriServer = null;

	Thread activityMonitorThread = null;
	ActivityMonitoringServer activityServer = null;

	/**
	 * Default constructor.
	 * @throws JSONException 
	 * @throws UnsupportedEncodingException 
	 */
	public ContextListener() throws JSONException, UnsupportedEncodingException
	{
		System.out.println("Default Charset:");
		System.out.println(Charset.defaultCharset());
		System.out.println("file.encoding=" + System.getProperty("file.encoding"));
	}

	/**
	 * @see ServletContextListener#contextInitialized(ServletContextEvent)
	 */
	public void contextInitialized(ServletContextEvent arg0)
	{
		Logger.log("Tomcat context started!");

		String path = arg0.getServletContext().getRealPath("/");
		
		distriServer = new WebSocketServer(path);
		distriServerThread = new Thread(distriServer, "websocket-server");
		distriServerThread.start();

		activityServer = new ActivityMonitoringServer();
		activityMonitorThread = new Thread(activityServer, "activity-monitor");
		activityMonitorThread.start();
	}

	/**
	 * @see ServletContextListener#contextDestroyed(ServletContextEvent)
	 */
	public void contextDestroyed(ServletContextEvent arg0)
	{
		if (distriServer != null)
			distriServer.stop();
		
		if (activityServer != null)
			activityServer.stop();

		Logger.log("Tomcat context destroyed!");
	}

}
