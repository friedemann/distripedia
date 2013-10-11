package grinder.util;

import java.util.Random;

/**
 * A simple wrapper for java.util.Random
 */
public class Randomizer {

	/** The Random Generator */
	private Random r;
	
	/**
	 * Instantiates a new randomizer.
	 */
	public Randomizer()
	{
		this.r = new Random();
	}
		
	/**
	 * Gets the int.
	 * @return pseudorandom int
	 */
	public synchronized int getInt(){
		return this.r.nextInt();
	}	
	
	/**
	 * Gets a pseudorandom int between two
	 * specified ints.
	 *
	 * @param lo the lower bound
	 * @param hi the upper bound
	 * @return pseudorandom int
	 */
	public synchronized int getInt(int lo, int hi)
	{
		int i = this.r.nextInt(hi-lo);		
		return lo + i;
	}
	
	/**
	 * Reinitialize the Random Generator
	 */
	public synchronized void reinitialize()
	{
		this.r = new Random();
	}
	
}
