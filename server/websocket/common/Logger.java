package websocket.common;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class Logger
{
	private static Format formatter = new SimpleDateFormat("k:mm:ss-S");

	public synchronized static void log(String s)
	{
		final String nowDateStr = formatter.format(new Date());
		System.out.println(nowDateStr + " > " + s);;
	}
}
