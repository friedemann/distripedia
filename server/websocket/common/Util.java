package websocket.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Util
{
	// important: removes all line breaks from the existing file. they are not
	// allowed in JSON notation (?)
	public static String getContents(File file)
	{
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

		return contents.toString();
	}
}
