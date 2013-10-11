package grinder.client.user;

import java.util.HashMap;
import java.util.Set;

/**
 * A simple implementation the local storage interface as defined in
 * {@link http://dev.w3.org/html5/webstorage/}
 *
 * @author David
 */
public class LocalStorage {
	/** hash map that serves as underlying storage. */
	private final HashMap<String, String> map = new HashMap<String, String>();

	/** defines the maximum amount of bytes we can store in the local storage */
	private static final int MAX_SIZE = 5 * 1024 * 1024;

	/** The current size of the local storage in bytes. */
	private int size = 0;

	/**
	 * Instantiates a new local storage.
	 */
	public LocalStorage() {}

	/**
	 * Gets an item from the storage.
	 *
	 * @param key the key
	 * @return the value to which the specified key is mapped, or null if the
	 *         storage contains no mapping for the key
	 */
	public String getItem(final String key) {
		return map.get(key);
	}

	/**
	 * Puts an item into the storage if there is sufficient space.
	 *
	 * @param key the key
	 * @param value the value
	 * @throws RuntimeException if the quota is exceeded
	 */
	public void setItem(final String key, final String value) {
		if (key != null && value != null)
		{
			if (size + value.length() > MAX_SIZE)
				throw new RuntimeException("Quota exceeded!");
			else
			{
				map.put(key, value);
				size += value.length();
			}
		}
	}

	/**
	 * Removes an item from the storage.
	 *
	 * @param key the key
	 */
	public void removeItem(final String key) {
		if (key != null)
		{
			final String value = map.remove(key);
			size -= value.length();
		}
	}

	/**
	 * Clears the local storage.
	 */
	public void clear() {
		map.clear();
		size = 0;
	}

	/**
	 * Returns a set of all keys from the storage. Replaces the key() method,
	 * that is defined in the LS interface due to java's hash map implementation
	 *
	 * @return the set of keys
	 */
	public Set<String> keys() {
		return map.keySet();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return map.toString();
	}

}
