package websocket.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class NameProvider
{
	private final String file = "/WEB-INF/names.txt";
	private final int NUM_NAMES = 2048;

	private final ArrayList<String> nameList = new ArrayList<String>(NUM_NAMES);
	private int index = 0;

	public NameProvider()
	{
		fillList();
		Collections.shuffle(nameList);
	}

	private void fillList()
	{
		BufferedReader bufferedReader = null;
		try
		{
			FileReader fileReader = new FileReader(WebSocketServer.path + file);
			bufferedReader = new BufferedReader(fileReader);

			while (bufferedReader.ready())
			{
				nameList.add(bufferedReader.readLine());
			}

		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		} finally
		{
			try
			{
				if (bufferedReader != null)
					bufferedReader.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns a name from the list of names.
	 * 
	 * @return a popular first name
	 */
	public String getName()
	{
		if (index >= nameList.size())
			index = 0;
		else
			index++;

		return nameList.get(index);
	}
}
