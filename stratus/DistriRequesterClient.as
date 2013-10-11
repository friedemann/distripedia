package stratus
{	
	public class DistriRequesterClient
	{
		import flash.events.NetStatusEvent;
		import flash.external.ExternalInterface;
		import flash.net.NetConnection;
		import flash.net.NetStream;

		// net connection handle
		private var netConnection:NetConnection;
		
		// net stream for incoming and outgoing data
		private var receivingStream:NetStream;
		private var sendingStream:NetStream;
		private var listenerStream:NetStream;
		
		private var myId:String = null;
		private var farId:String = null;
		private var requestedArticle:String = null;
		
		private  var connectionIsEstablished:Boolean = false;
		
		private static var distriRequesterClients:Vector.<DistriRequesterClient> = new Vector.<DistriRequesterClient>();
		
		// constructor
		public function DistriRequesterClient(netConnection:NetConnection, farId:String, requestedArticle:String)
		{
			this.netConnection = netConnection;
			this.myId = netConnection.nearID;
			this.farId = farId;
			this.requestedArticle = requestedArticle;
		
			// init stream for outgoing data
			sendingStream = new NetStream(this.netConnection, NetStream.DIRECT_CONNECTIONS);
			sendingStream.addEventListener(NetStatusEvent.NET_STATUS, sendingStreamHandler);
			
			var c:Object = new Object();
			c.onPeerConnect = function(caller:NetStream):Boolean
			{
				connectToPeer();
				return true;
			}
			sendingStream.client = c;
			
			// TODO: unique identifier for each stream
			sendingStream.publish("sending" + this.farId);

			listenerStream = new NetStream(this.netConnection, this.farId);
			listenerStream.addEventListener(NetStatusEvent.NET_STATUS, listenerStreamHandler);
			listenerStream.play("control");
			
			distriRequesterClients.push(this);
		}
		
		private function connectToPeer():void
		{			
			// init stream for incoming data
			receivingStream = new NetStream(this.netConnection, this.farId);
			receivingStream.addEventListener(NetStatusEvent.NET_STATUS, receivingStreamHandler);
			
			// TODO: unique name for stream
			receivingStream.play("sending" + this.myId);
			receivingStream.client = this;
		}
		
		private function sendingStreamHandler(vEvent:NetStatusEvent):void
		{
		}

		private function listenerStreamHandler(vEvent:NetStatusEvent):void
		{			
			if(vEvent.info.code ==  "NetStream.Play.Failed")
			{
				listenerStream.removeEventListener("NetStatusEvent.NET_STATUS", listenerStreamHandler);
				listenerStream.close();
				listenerStream = null;
			}
		}
		
		private function receivingStreamHandler(vEvent:NetStatusEvent):void
		{			
			// if far peer connected to my receiving stream, I can request the article
			if(vEvent.info.code ==  "NetStream.Play.Start")
			{
				requestArticle();
			}
		}
		
		// send an article request to a far peer
		private function requestArticle():void
		{		
			sendingStream.send("receivedArticleRequest", this.requestedArticle);
		}
		
		// got article from far peer, so I can store it in my local storage
		public function receivedRequestedArticle(article:String, content:String):void
		{
			log("You received article '" + article + "' through stratus.");
			var msg:Object = { };
			msg.article = article;
			msg.content = content;
			msg.stratus = "true";
			
			ExternalInterface.call("handleClientReceivedArticle", msg);
			removeMeFromVector();
		}
		
		// get the position of me in the vector and remove me
		private function removeMeFromVector():void
		{
			receivingStream.close();
			sendingStream.close();

			distriRequesterClients.splice(distriRequesterClients.indexOf(this), 1);
		}
		
		// logger function for JavaScript
		private function log(text:String):void {
			ExternalInterface.call("log", "<span class=\"stratus-msg\">stratus</span> > " + text);
		}
	}
}