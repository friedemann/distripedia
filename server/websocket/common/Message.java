package websocket.common;

/**
 * wrapper class for the client-server communication message standard
 * @author David
 *
 */
public class Message
{
	/**
	 * possible key values
	 * @author David
	 *
	 */
	public static class Key
	{
		/**
		 * defines the purpose of this message. 
		 * see {@link Action} for expected values 
		 */
		public static final String ACTION 	= "action";
		
		/**
		 * the filename of the article in question. 
		 * expected value of type string (e.g. article1)
		 */
		public static final String ARTICLE 	= "article";
		
		/**
		 * an identifier for this message if necessary. 
		 * expected value of type int
		 */
		public static final String ID 		= "id";
		
		/**
		 * the content part of this message. can be an arbitrary text 
		 * (e.g. the article itself or a detailed error message)
		 * expected value of type string
		 */
		public static final String CONTENT 	= "content";
	}

	/**
	 * possible values for the key "action". 
	 * values are sent as integer status codes.
	 * @author David
	 *
	 */
	public static class Action
	{
		/**
		 * request an article from the messaging partner
		 */
		public static final int REQUEST_ARTICLE 		= 101;
		
		/**
		 * send an article to the partner
		 */
		public static final int SEND_ARTICLE 			= 201;
		
		/**
		 * from client only: client sends OKAY, received article
		 */
		public static final int CLIENT_ARTICLE_RECEIVED = 200;
		
		/**
		 * from client only: client sends a list of articles he has in local storage
		 */
		public static final int CLIENT_SEND_ARTICLE_IDS = 202;
		
		/**
		 * send the id of the other stratus client that has the article to the partner
		 */
		public static final int SEND_FAR_STRATUS_PEER_ID = 203;
		
		/**
		 * from client only: client sends its own stratus id
		 */
		public static final int CLIENT_STORE_STRATUS_ID = 204;
	
		/**
		 * send the own name to the client
		 */
		public static final int SEND_NAME = 206;
				
		/**
		 * inform partner that article is unavailable
		 */
		public static final int ERROR_ARTICLE_NOT_FOUND = 404;
	}
}
