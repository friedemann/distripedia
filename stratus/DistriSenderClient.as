package stratus
{	
	public class DistriSenderClient
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
		
		private var myId:String = null;
		private var farId:String = null;
				
		private static var distriSenderClients:Vector.<DistriSenderClient> = new Vector.<DistriSenderClient>();
		
		// constructor
		public function DistriSenderClient(netConnection:NetConnection, farId:String)
		{
			this.netConnection = netConnection;
			this.myId = netConnection.nearID;
			this.farId = farId;
			
			// init stream for outgoing data
			sendingStream = new NetStream(this.netConnection, NetStream.DIRECT_CONNECTIONS);
			sendingStream.addEventListener(NetStatusEvent.NET_STATUS, sendingStreamHandler);
			
			var c:Object = new Object();
			c.onPeerConnect = function(caller:NetStream):Boolean
			{
				return true;
			}
			sendingStream.client = c;
			
			// TODO: unique identifier for each stream
			sendingStream.publish("sending" + this.farId);
			
			connectToPeer();
			
			distriSenderClients.push(this);
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
		
		private function receivingStreamHandler(vEvent:NetStatusEvent):void
		{			
			if(vEvent.info.code ==  "NetStream.Play.UnpublishNotify")
			{
				removeMeFromVector();
			}
			
		}
		
		// get article request from peer that wants my article
		public function receivedArticleRequest(article:String):void
		{
			log("You are sending article '" + article + "' through stratus.");
			var content:String = ExternalInterface.call("sendArticleToStratusPeer", article);
			
			sendingStream.send("receivedRequestedArticle", article, content);			
		}

		// get the position of me in the vector and remove me
		private function removeMeFromVector():void
		{
			receivingStream.close();
			sendingStream.close();

			distriSenderClients.splice(distriSenderClients.indexOf(this), 1);
		}
		
		// logger function for JavaScript
		private function log(text:String):void {
			ExternalInterface.call("log", "<span class=\"stratus-msg\">stratus</span> > " + text);
		}		
	}
}