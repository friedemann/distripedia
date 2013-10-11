package grinder.util;

/**
 * A simple timer.
 *
 * @author Friedemann
 */
public class Timer {

	/** is the timer active? */
	private boolean running = false;

	/** The start time. */
	private long startTime;

	/** The stop time. */
	private long endTime;

	/**
	 * Instantiates a new timer.
	 */
	public Timer() {
		startTime = 0;
		endTime = 0;
	}

	/**
	 * Start the timer.
	 */
	public void start() {
		startTime = System.currentTimeMillis();
		running = true;
	}

	/**
	 * Stop the timer.
	 */
	public void stop() {
		endTime = System.currentTimeMillis();
		running = false;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	/**
	 * Gets the time. Stops the timer automatically if not done before.
	 *
	 * @return the measured time in miliseconds
	 */
	public int getTime() {
		if (running)
		{
			stop();
		}
		return (int) (endTime - startTime);
	}
}
