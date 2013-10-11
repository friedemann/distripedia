package websocket.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class LogViewerServlet
 */
public class LogViewerServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;
	private static final Format formatter = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LogViewerServlet()
	{
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException
	{
		// TODO Auto-generated method stub
	}

	/**
	 * @see Servlet#destroy()
	 */
	public void destroy()
	{
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException
	{
		// quick and rather expensive hack to display whole contents of the current
		// log file from catalina out

		// load today's log file
		final String todayStr = formatter.format(new Date());
		final String filename = "/var/log/tomcat5/catalina_" + todayStr + ".log";

		File file = new File(filename);
		BufferedReader input = null;
		
		try
		{
			input = new BufferedReader(new FileReader(file));
			response.setContentType("text/html");

			// js: scroll to bottom onload
			String top = "<html><head><script>function go(){setTimeout(window.location='#bottom', 2000);}</script></head><body onload='go()' style=\"font-family: monospace;\">";
			String bottom = "<br/><a name='bottom'>&nbsp;</a></body></html>";

			response.getWriter().print(top);

			while (input.ready())
			{
				response.getWriter().print(input.readLine());
				response.getWriter().print("<br/>");
			}
			response.getWriter().print(bottom);

		} catch (FileNotFoundException e)
		{
			return;
		}
		finally
		{
			if (input != null)
				input.close();
		}

	}
}
