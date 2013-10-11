package websocket.server;

import java.io.IOException;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import websocket.common.Message;

//quick and dirty......
public class ActivityWorker extends WebSocket
{
	public ActivityWorker(Socket socket) throws IOException
	{
		super(socket);
	}

	public void send(String msg)
	{
		super.send(msg.getBytes(WebSocket.utf8));
	}
	
	public synchronized void log(int action, String from, String to, boolean isStratus, String content)
	{
		JSONObject jsonMessage = new JSONObject();

		try
		{
			jsonMessage.put("action", action);
			jsonMessage.put("clientFromId", from);
			jsonMessage.put("clientToId", to);
			jsonMessage.put("isStratus", isStratus);
			jsonMessage.put("content", content);
			
			this.send(jsonMessage.toString());
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
	}
}
