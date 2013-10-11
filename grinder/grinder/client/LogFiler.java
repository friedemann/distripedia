package grinder.client;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * The class LogFiler provides a simple way of generating logfiles.
 */
public class LogFiler {

	/** The log file name. */
	private final String fileName;

	/** The BufferedWriter. */
	private BufferedWriter bw = null;

	/** Reference to the client. */
	private final Client client;

	/**
	 * Instantiates a new log file handler.
	 *
	 * @param fileName the logfile name
	 * @throws IOException
	 */
	public LogFiler(final Client c, final String fileName) throws IOException {
		client = c;
		this.fileName = fileName;

		bw = new BufferedWriter(new FileWriter(fileName));
	}

	/**
	 * Write a grinder message to the log.
	 *
	 * @param clientID the client id
	 * @param content the preformatted content string
	 * @see logfileformat.txt
	 */
	public synchronized void log(final UUID clientID, final String content) {

		final Timestamp t = new Timestamp(System.currentTimeMillis());

		if (bw != null)
		{
			try
			{
				bw.write(clientID.toString() + "|" + t + "|" + content);
				bw.newLine();
				bw.flush();

			} catch (final IOException e)
			{
				client.cgui.logMessage("LogFiler BufferedWriter IO Error! " + e);
			}
		}
	}

	public void closeLogFile() {
		// shutdown writer
		try
		{
			if (bw != null)
			{
				bw.flush();
				bw.close();
				bw = null;
			}
		} catch (final IOException ex)
		{
			client.cgui.logMessage("Cannot close log file! " + ex);
		}
	}

	/**
	 * Gets the file.
	 *
	 * @return the file
	 */
	public File getFile() {
		return new File(fileName);
	}

	/**
	 * Gets the logfile name
	 *
	 * @return the file
	 */
	public String getFilename() {
		return fileName;
	}

	/**
	 * Delete log file.
	 */
	public void deleteLogFile() {
		(new File(getFilename())).delete();
	}

	/**
	 * Gets the bytes from a file.
	 *
	 * @param file the file
	 * @return the bytes from file
	 * @throws IOException
	 */
	public byte[] getFileByteArray() throws IOException {

		final File file = new File(fileName);

		final InputStream is = new FileInputStream(file);

		// Get the size of the file
		final long length = file.length();

		// Create the byte array to hold the data
		final byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)
		{
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length)
			throw new IOException("Could not completely read file " + file.getName());

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

}
