package wikipedia.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.net.www.http.HttpClient;

import wikipedia.Main;

public class URLUtils
{
	/**
     * Create connection to server
     * @param urlstring
     * @return
     */
    public static URLConnection getURLConnection (String urlstring) 
    {
        try
        {
        
            URL url = new URL (urlstring);
            URLConnection conn = url.openConnection ();
           
            conn.setRequestProperty ("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; en-GB; rv:1.9.1.4) Gecko/20091016 Firefox/3.5.4");	// API Hack
            conn.setRequestProperty ("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty ("Accept-Language", "en-gb,en;q=0.5");
            conn.addRequestProperty ("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            
            return conn;
         
        }
        catch (MalformedURLException ex)
        {  
            ex.printStackTrace();
            return null;
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Get data form server
     * @param conn
     * @return
     */
    public static String getSource (URLConnection conn) throws IOException
    {
        StringBuffer sb = new StringBuffer ("");
   
            BufferedReader in = new BufferedReader(new InputStreamReader (conn.getInputStream (), "UTF-8"));
            String line;
            
            while ((line = in.readLine ()) != null)
            {
                sb.append (line);
            }


        return sb.toString ();
    }
}
