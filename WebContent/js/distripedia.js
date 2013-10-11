
// define the layout type of the website
var DEVELOPER = 1;
var WERKSCHAU = 2;

// set the layout type
var mode = (window.location.href.indexOf("developer") != -1) ? DEVELOPER : WERKSCHAU;

// indexOf is -1 if the url does not contain the parameter
var flashIsEnabled = (window.location.href.indexOf("disableflash") == -1);

// global handle to websocket
var myWebSocket = null;

var homepage = "Main_Page";
var localStorageArticlePrefix = "distripedia.article.";

// action status codes
var Action = { 
	REQUEST_ARTICLE: 				101,
	SEND_ARTICLE: 					201,
	CLIENT_ARTICLE_RECEIVED: 		200,
	CLIENT_SEND_ARTICLE_IDS: 		202,
	SEND_FAR_STRATUS_PEER_ID: 		203,
	CLIENT_STORE_STRATUS_ID:		204,
	SEND_NAME:						206,
	ERROR_ARTICLE_NOT_FOUND: 		404
};

// open socket connection when dom tree is loaded
$(document).ready(function() {
	// inititalize the error log
	initScrollPane();
	updateScrollPaneWidth();

	// hide not needed fields
	$("#worker-info").hide();
	$("#local-storage-control").hide();
	if(mode == WERKSCHAU)
	{
		$("#articles").hide();
		$("#server-log").hide();
	}
	else {
		toggleLocalStorage();
	}
    
    // inititalize websockets  
    updateLocalStorageContents();
	openSocket();
	
	// if websockets were not opened, redirect to error page
	if(myWebSocket == null)
	{
		window.location.replace('no_js.html');
	}
});

//when the browser window is resized, update scroll pane width
$(window).bind('resize', function() {
	updateScrollPaneWidth();
});


/**
 *	websocket functions 
 */

//opens a websocket and registers event handlers
function openSocket(){
	if (myWebSocket == null) {
		try {
			var host = window.location.host;
			var idx = host.indexOf(':');
			if (idx != -1)
				host = host.substring(0, idx);
			
            myWebSocket = new WebSocket("ws://" + host + ":8383");
			
			// send list of articles when connection is established
			myWebSocket.onopen = function(evt){
				if(mode == DEVELOPER)
					log("websocket open");

				var localArticles = getArticlesInLocalStorage();
				sendArticleIds(localArticles);

				$("#websocket-control").removeClass("disabled").addClass("enabled");
				
				// initialize flash and define the minimum required flash
				// version
				// initilaize here, to avaoid flash to be ready before
				// websockets are opened
				if(flashIsEnabled)
				{
					swfobject.embedSWF("distripedia.swf", "flash-content", "1", "1", "10.0.0");					
				}
				
				// start with wiki main page
				getArticle(homepage);
			};
			
			myWebSocket.onerror = function(evt){
				if(mode == DEVELOPER)
					log("ws error: " + evt.data);
			};
			
			myWebSocket.onmessage = function(evt){
				handleMessages(evt.data);
			};
			
			myWebSocket.onclose = function(evt){
				if(mode == DEVELOPER)
					log("websocket closed");
				$("#websocket-control").removeClass("enabled").addClass("disabled");
			};
		} 
		catch (ex) {
			log(ex);
		}
	}
}

function closeSocket() {
	$("#websocket-control").removeClass("enabled").addClass("disabled");
	
	myWebSocket.close();
	myWebSocket = null;
}

//handles the onmessage event
function handleMessages(data) {
	var msg = JSON.parse(data);
	 
	 switch(msg.action) {
	 	// server sent article
	 case Action.SEND_ARTICLE:
	 	handleClientReceivedArticle(msg);
		break;
	
	 // server sent name
	 case Action.SEND_NAME:
	 	handleClientReceivedName(msg);
		break;

	 	// send article to stratus
	 case Action.SEND_ARTICLE_TO_STRATUS:
	 	handleStratusRequestedArticle(msg);
		break;
		
		// servers requests article
	case Action.REQUEST_ARTICLE:
		handleServerRequestedArticle(msg);
		break;
		
		// server sent me far stratus peer id that has the article I want to get
	case Action.SEND_FAR_STRATUS_PEER_ID:
		sendArticleRequestToStratusPeer(msg);
		break;
		
	case Action.ERROR_ARTICLE_NOT_FOUND:
		displayArticle(msg.article, msg.content);
		log("error: " + msg.content);
		break;
	 }
}

function sendAsJson(object) {
	myWebSocket.send(JSON.stringify(object));
}

function updateWebsocketState(){
	if($("#websocket-control").hasClass('enabled'))
	{
		closeSocket();
	}
	else
	{
		openSocket();
	}
}


/**
 *	local storage functions 
 */

function toggleLocalStorage() {
	if($("#local-storage-control").is(':hidden'))
	{
		$("#local-storage").css('background-image', 'url(images/bullet_toggle_minus.png)');
	}
	else
	{
		$("#local-storage").css('background-image', 'url(images/bullet_toggle_plus.png)');
	}
	
	$("#local-storage-control").slideToggle(600);
}

function storeArticleInLocalStorage(msg) {
	try {
		localStorage.setItem(localStorageArticlePrefix + msg.article, msg.content);
	
		if(mode == DEVELOPER)
			log("Storing article '" + msg.article + "' in local storage.");
		
		updateLocalStorageContents();	
		return true;
	} catch (ex) {
		if(mode == DEVELOPER)
			log("Your local storage is full!");
		return false;
	}
	
}

//retrieves all keys from the local storage
function getLocalStorageKeys() {
	var keys = [];
	
	for (var i = 0; i < localStorage.length; i++) {
		keys.push(localStorage.key(i));
	}
	
	return keys;
}

function getArticleFromLocalStorage(article)
{
    return localStorage.getItem(localStorageArticlePrefix + article);
}

// returns all dp articles from the local storage
function getArticlesInLocalStorage() {
	var keys = getLocalStorageKeys();
	var articles = [];
	
	for (var i in keys) {
		var key = keys[i];
		if (key.indexOf(localStorageArticlePrefix) == 0) {
			articles.push(key.substr(localStorageArticlePrefix.length));
		} 
	}
	
	return articles;
}

//deletes all content from the local storage
function clearLocalStorage() {
	localStorage.clear();
	window.location.reload(true);
}

// update the local storage contents shown in the left navigation
function updateLocalStorageContents() {
	var temp = getArticlesInLocalStorage().sort();
	
	var str_out = "";
	for (var i = 0; i < temp.length; i++) {		
		var title = unescape(temp[i]);
		
		str_out += "<a href=\"#\" onclick=\"getArticle('" + title + "')\">" +title.substr(0, 13);
		// if title is too long, show just the first characters and append dots
		if(title.length > 13)
			str_out += "...";
		str_out += "</a><br />"
	}
	
	$("#localstorage-content").html(str_out);
}

function toggleLog() {
	if($("#footer").height() == 200)
	{
		$("body").css('padding-bottom', '100px');
		$("#footer").animate({ height: "53px" }, 1000 );
		$("#log-slider").animate({ height: "62px" }, 1000 );
		$("#log-control-btn").css('background-image', 'url(images/bullet_arrow_up.png)');
		$(".jScrollPaneContainer").css('height', '40px');
		initScrollPane();		
	}
	else
	{
		$("body").css('padding-bottom', '250px');
		$("#footer").animate({ height: "200px" }, 1000 );
		$("#log-slider").animate({ height: "209px" }, 1000 );
		$("#log-control-btn").css('background-image', 'url(images/bullet_arrow_down.png)');
		$(".jScrollPaneContainer").css('height', '187px');
		initScrollPane();
	}
}


/**
 *	article management
 */

function getArticle(article){
    if (mode == WERKSCHAU) {
      	if(article.toLowerCase() == "distripedia")
      	  article = "Wikipedia";
    }

	
	showLoader();
    // make the website accessible again after 25 seconds, even if nothing was
	// returned
    
	// if we already have the article in storage, dont send server request
	// instead get article from storage.
	var content = getArticleFromLocalStorage(article);
	if (content) {
		log("The article '" + article + "' was loaded from localstorage.");
		displayArticle(article, content);
		return;
	}
	
	// send article request to server
	var data = {};
	data.action = Action.REQUEST_ARTICLE;
	data.article = article;
	
	sendAsJson(data);

	log("You are requesting article '" + article + "'.");
}

function displayArticle(article, content) {
    // for WERKSCHAU mode, change Wikipedia to distripedia
    title = displayWikiTitle(article);
    if(mode == WERKSCHAU) {
        content = content.replace(/([^\./":\w\d])(wikipedia)/gi, "$1distripedia");
        title = title.replace(/wikipedia/gi, "distripedia");
    }
  
	$("#content").html("<h1>" + title + "</h1>" + content);
	
	
	interceptWikiLinks();
	hideLoader();
	
	$('html, body').animate({scrollTop: 0}, 'slow');
}

function handleClientReceivedArticle(msg) {
	if(msg.stratus == undefined)
		log("You received article '" + msg.article + "'.");
	
	// display article
	displayArticle(msg.article, msg.content);
	
	// dont store main_page
	if (msg.article != homepage) {
		// store article
		if (storeArticleInLocalStorage(msg)) {
			// if enough localstorage space was available
			// notify server that client has this article now
			sendArticleId(msg.article);
		}
	}
	
	// confirm article received
	var data = {};
	data.article = msg.article;
	data.action = Action.CLIENT_ARTICLE_RECEIVED;
	sendAsJson(data);
}

function handleClientReceivedName(msg) {
	// save the name
	$("#worker-name").html(msg.content);
	$("#worker-info").show();

}

function handleServerRequestedArticle(msg) {
    var response = {};
    response.article = msg.article;
    response.id = msg.id;
    
    var content = getArticleFromLocalStorage(msg.article);
    
    // article not present in localstorage?
    if (!content) {
        response.action = Action.ERROR_ARTICLE_NOT_FOUND;
		log("The article '" + msg.article + "' was not found.");
    }
    else {
        response.action = Action.SEND_ARTICLE;
        response.content = content;
		log("Sending article '" + msg.article + "' to server.");
    }
    
    sendAsJson(response);
}

function sendArticleId(article) {
	sendArticleIds([article]);
}

// send a list of all articles that the client has in store to the server
function sendArticleIds(arrIds) {
	var data = {};
	data.action = Action.CLIENT_SEND_ARTICLE_IDS;
	data.content = arrIds;
	
	sendAsJson(data);
	if(mode == DEVELOPER)
		log("sending article IDs..");
}


/**
 *	stratus helper functions
 */

//stores stratusId in the worker object
function storeStratusId(stratusId){
	var data = {};
	data.action = Action.CLIENT_STORE_STRATUS_ID;
	data.content = stratusId;
		
	sendAsJson(data);
	if(mode == DEVELOPER)
		log("You are now ready to send/receive articles through stratus service.");
	$("#flash-control").removeClass("disabled").addClass("enabled");
}

// establish peer to peer connection to stratus client and request article
function sendArticleRequestToStratusPeer(msg) {
	getFlashApp('flash-content').getArticleFromFarStratusPeer(msg.content, msg.article);
}

// send article back to stratus, function is invoked from flash
function sendArticleToStratusPeer(article)
{
	var content = getArticleFromLocalStorage(article);

	// article not present in localstorage?
    if (!content) {
		log("article not found " + article);
        return "article_not_found";
    }

    return content; 
}

// Internet Explorer and Mozilla-based browsers refer to the Flash application
// object differently.This function returns the appropriate reference, depending
// on the browser.
function getFlashApp(appName) {
	if (navigator.appName.indexOf ("Microsoft") !=-1) {
		return window[appName];
	} else {
		return document[appName];
	}
}


/**
 *	others
 */

// logs messages in footer
function log(text) {
	var currentTime = new Date();
	
	var hours = currentTime.getHours();
	var minutes = currentTime.getMinutes();
	var seconds = currentTime.getSeconds();
	
	// append leading zero for single digits
	if (hours < 10)
		hours = "0" + hours;
	
	if (minutes < 10)
		minutes = "0" + minutes;
	
	if (seconds < 10)
		seconds = "0" + seconds;
	
	var timeString = hours + ":" + minutes + ":" + seconds;
	
	// add milisecs for developer view
	if (mode == DEVELOPER) {
		var milisecs = currentTime.getMilliseconds();
		timeString += "-" + milisecs;
	}

	$("#log").append("&nbsp;&nbsp;" + timeString + " > " + text + "<br/>");
	initScrollPane();
}

function search() {
	var input = $('#searchbox').val();
	$('#searchbox').val("");
	
	article = normalizeInput(input) || homepage;
	getArticle(article); 
}


/**
 *	helper methods
 */

function initScrollPane(){
	$('#log').jScrollPane();
	$('#log')[0].scrollTo($('#log').height());
}

function updateScrollPaneWidth() {
	var windowWidth = $(window).width();
	$(".jScrollPaneContainer").css('width', (windowWidth - 30) + 'px');
	$("#log").css('width', (windowWidth - 50) + 'px');
}

function updateFlashState(){
    closeSocket();
    
	if($("#flash-control").hasClass('enabled'))
	{
		// disable flash
		window.location.href = "index.html?disableflash"; 	
	}
	else
	{
		// enable flash if possible
		window.location.href = "index.html"; 
	}
}

// overlay the website with grey layer and show loader
function showLoader() {
	$('<div id="overlay"></div>').appendTo($('BODY'));
	$('<div id="loading-circle"><img src="images/loading.gif" /><br /><br />Please wait...</div>').appendTo("#overlay");
	$("#overlay").fadeIn(700);
}

// hide the loader and make website accessible again
function hideLoader() {
	$("#overlay").fadeOut(500);
	$("#overlay").remove();
}

//normalizes a string in the following manner:
//- remove all blanks and underscores at the beginning
//- remove all blanks and underscores at the end
//- replace all sequences of blanks and/or underscores with a single underscore
//- uppercase first letter of input
//example: " new_ _York_" becomes "New_York"
function normalizeInput(input) {
	input = input.replace(/^[\s_]+/g, "");
	input = input.replace(/[\s_]+$/g, "");
	input = input.replace(/[\s_]+/g, "_");
	input = input.charAt(0).toUpperCase() + input.slice(1);
	
	return input;
}

//rewrite url-safe title to regular title
function displayWikiTitle(title) {
	return unescape(title.replace(/_/g, ' '));
}

//intercept links to wiki articles and gets them through websockets instead
function interceptWikiLinks() {
	$('a').click(function() { 
	    var hrefLoc = $(this).attr('href'); 
	    var prefix = "http://en.wikipedia.org/wiki/";
	   
	   // skip anchor thingies
	   if (hrefLoc.indexOf('#') == 0) {
	   		return true;
	   }
	   // intercept links that start with /prefix/
	   else if (hrefLoc.indexOf(prefix) == 0) {
			var article = hrefLoc.substr(prefix.length);
			getArticle(decodeURI(article));
			
			return false;
		}
		else {
			// open external links in new window to keep tab open
			$(this).attr('target', '_blank');
		}
	});
}
