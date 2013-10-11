package wikipedia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import websocket.common.Logger;
import websocket.server.WebSocket;
import websocket.server.WebSocketServer;
import wikipedia.utils.URLUtils;

public class Main
{
	final static PrintStream ps = System.out;

	final static String wikipedia_url = "http://en.wikipedia.org/w/index.php";
	final static String wikipedia_args = "?action=render&printable=yes&title=";

	final static String article_file_path = "/wiki/";
	final static int articles_max = 5000;
	static String articles_counter_file = "/WEB-INF/properties/articles_counter.properties";
	static Properties articles_prop;

	/**
	 * Get the article as XML from Wikipedia
	 * 
	 * @param article_title
	 * @return
	 * @throws IOException 
	 */
	private static String get_xmlFromWikipedia(String article_title) throws IOException
	{
		String encodedArticle = null;
		
		try
		{
			encodedArticle = URLEncoder.encode(article_title, "UTF-8");
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		}
		
		URLConnection conn = URLUtils.getURLConnection(wikipedia_url + wikipedia_args + encodedArticle);
		

		return URLUtils.getSource(conn);
	}

	/**
	 * Save the XML file
	 * 
	 * @param file_name
	 * @param content
	 */
	private static void save_xmlFile(String file_name, String content)
	{
		BufferedWriter bw = null;
		try
		{

			bw = new BufferedWriter(new FileWriter(file_name));
			bw.write(content);

		} catch (IOException exc)
		{
			// Logger.getLogger (Main.class.getName ()).log (Level.SEVERE, null,
			// exc);
			exc.printStackTrace();
		} finally
		{

			try
			{
				if (bw != null)
					bw.close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get total amount of articles on own server
	 * 
	 * @return
	 */
	private static int get_totalArticles()
	{
		try
		{
			FileInputStream propInFile = new FileInputStream(WebSocketServer.path
					+ articles_counter_file);
			articles_prop = new Properties();

			articles_prop.load(propInFile);
			String articles = articles_prop.getProperty("articles", "0");

			return Integer.valueOf(articles).intValue();
		} catch (IOException e)
		{
			e.printStackTrace();
			ps.println("I/O failed.");
		}

		return 0;
	}

	/**
	 * Delete one random XML
	 */
	private static void delete_oneArticle()
	{
		File folder = new File(WebSocketServer.path + article_file_path);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++)
		{
			if (listOfFiles[i].isFile() && listOfFiles[i].canWrite())
			{
				if (listOfFiles[i].getName().contains(".xml"))
				{
					if (listOfFiles[i].delete())
						break;
					else
						throw new IllegalArgumentException("Delete failed");
				}
			}
		}
	}

	/**
	 * Update the property file with the articles amount
	 * 
	 * @param articles_total
	 */
	private static void update_totalArticles(int articles_total)
	{
		try
		{
			FileOutputStream propOutFile = new FileOutputStream(WebSocketServer.path
					+ articles_counter_file);

			articles_prop.setProperty("articles", String.valueOf(articles_total).toString());
			articles_prop.store(propOutFile, "Contains the total amount of articles");
		} catch (IOException e)
		{
			e.printStackTrace();
			ps.println("I/O failed.");
		}
	}

	/**
	 * Execute the article extraction
	 * 
	 * @param article_title
	 * @return
	 * @throws IOException 
	 */
	private static String do_extraction(String article_title) throws IOException
	{
		// Get article XML from Wikipedia
		String article_xml = get_xmlFromWikipedia(article_title);

		// Check how many articles there are and delete one if necessary
		int articles_total = get_totalArticles();

		if (articles_total >= articles_max)
			delete_oneArticle();
		else
			update_totalArticles(++articles_total);

		// Save XML
		save_xmlFile(WebSocketServer.path + article_file_path + article_title + ".html",
				article_xml);

		return article_xml;
	}

	/**
	 * Get the Wikipedia article
	 * 
	 * @param article_title
	 * @return
	 * @throws IOException 
	 */
	
	
	public static synchronized String get_article(String article_title) throws IOException
	{
		return do_extraction(article_title);
	}

	public static void main(String[] args)
	{
		// if (args.length < 1) // Kill app if there isn't any article to look
		// for
		// {
		// ps.println ("Please pass the title of the article as argument.");
		// System.exit (0);
		// }

	//	get_article("football");
		ps.println();
	}
}
