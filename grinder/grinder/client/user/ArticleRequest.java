/**
 *
 */
package grinder.client.user;

/**
 * Hands back the request data from the browser in a handy format.
 *
 * @author Friedemann
 */
public class ArticleRequest {

	public String articleIdReceived = "";
	public String receivedFrom = "";
	public int bytesReceived = 0;

	public ArticleRequest() {}

	public ArticleRequest(final String articleIdReceived, final int bytesReceived) {
		this.articleIdReceived = articleIdReceived;
		this.bytesReceived = bytesReceived;
	}

	public ArticleRequest(final String articleIdReceived, final int bytesReceived,
				final String receivedFrom) {
		this.articleIdReceived = articleIdReceived;
		this.bytesReceived = bytesReceived;
		this.receivedFrom = receivedFrom;
	}
}
