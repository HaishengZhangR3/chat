
// ui switch tab control
function openTab(evt, tabName) {
  if (document.getElementById(tabName).style.display === 'block') {
    hideTab(evt);
    return;
  }

  var i, tabcontent, tablinks;
  tabcontent = document.getElementsByClassName("tabcontent");
  for (i = 0; i < tabcontent.length; i++) {
    tabcontent[i].style.display = "none";
  }
  tablinks = document.getElementsByClassName("tablinks");
  for (i = 0; i < tablinks.length; i++) {
    tablinks[i].className = tablinks[i].className.replace(" active", "");
  }
  document.getElementById(tabName).style.display = "block";
  evt.currentTarget.className += " active";
}

function hideTab(evt) {
  var i, tabcontent, tablinks;
  tabcontent = document.getElementsByClassName("tabcontent");
  for (i = 0; i < tabcontent.length; i++) {
    tabcontent[i].style.display = "none";
  }
}

// socket for chat notification
let socket = new WebSocket(wsUrl);

// remember the last updated chat ID so that the parser can fix it on top
var chatLastUpdated = '';

// WebSocket open ready, then refresh page
socket.onopen = function(event) {
  refresh();
};

// handle incoming messages
socket.onmessage = function(event) {
  let incomingMessage = event.data;
  var popupMsg = parseNotification(incomingMessage)
  showPopUp(popupMsg);

  setTimeout(function(){
    refresh();
  }, 1000);
};

socket.onclose = event => console.log(`Closed ${event.code}`);

// clear message in div#messages
function clearIt() {
  let content = document.getElementById('messages')
  content.innerHTML = ''
}

// show message in div#messages
function showMessage(message) {
  clearIt()
  let content = document.getElementById('messages')
  let messageElem = document.createElement('div');
  messageElem.textContent = message;
  content.append(messageElem);
}

// for notification parser
function parseNotification(data){
  var message = JSON.parse(data);
  switch(message[0]){
    case "CreateCommand":
    case "ReplyCommand":
      var chatMsg = message[1];
      chatLastUpdated = chatMsg.chatId.id
      return '<B><font color="' + getColor(chatMsg.sender) + '">[' + chatMsg.sender + ']</font></B> ' + chatMsg.content

    case "CloseCommand":
      var chatMeta = message[1]
      chatLastUpdated = chatMeta.linearId.id
      return '<B><font color="' + getColor('Title') + '">[' + chatLastUpdated + ']</font></B> ' + ' is closed.'

    case "AddParticipantsCommand":
      var chatMeta = message[1]
      chatLastUpdated = chatMeta.linearId.id
      return '<B><font color="' + getColor('Title') + '">[' + chatLastUpdated + ']</font></B> ' + ' added one participant.'

    case "RemoveParticipantsCommand":
      var chatMeta = message[1]
      chatLastUpdated = chatMeta.linearId.id
      return '<B><font color="' + getColor('Title') + '">[' + chatLastUpdated + ']</font></B> ' + ' removed one participant.'

    default:
      return "";
      break;
  }
}

// show pop up message for notification
function showPopUp(message) {

  var popupDiv = document.createElement("popup");
  popupDiv.innerHTML = '<div class="chat-popup new-message">' + message + '</div>';

  var bottomRightElm = document.getElementById("myPopUp");
  bottomRightElm.appendChild(popupDiv);

  setTimeout(() => {
    bottomRightElm.removeChild(popupDiv);
  }, 5000);
}

// server help function-->
function post(path, data){
  var xhr = new XMLHttpRequest();
  xhr.withCredentials = true;
  xhr.addEventListener("readystatechange", function () {
    if (this.readyState === 4) {
      if (this.status == 200) {
      } else {
        alert('Error occurs in server side: \nstatus code:\n' + this.status);
      }
    }
  });

  xhr.open("POST", serviceUrl + path);
  xhr.setRequestHeader("Accept", "*/*");
  xhr.setRequestHeader("Content-Type", "application/json");
  if (data == null){
    xhr.send();
  } else {
    xhr.send(data);
  }
}

function get(path, needParseChatMessage){
  var xhr = new XMLHttpRequest();
  xhr.withCredentials = true;
  xhr.addEventListener("readystatechange", function () {
    if (this.readyState === 4) {
      if (this.status == 200) {
        clearIt()
        if (needParseChatMessage) {
          parseChatMessage(this.responseText)
        } else {
          showMessage(this.responseText);
        }
      } else {
        alert('Error occurs in server side: \nstatus code:\n' + this.status);
      }
    }
  });

  xhr.open("GET", serviceUrl + path);
  xhr.setRequestHeader("Accept", "*/*");
  xhr.send(null);
}

// parser for "get" message from server
function parseChatMessage(data){
  var messages = JSON.parse(data);
  messages.sort((a, b) => (a.chatId.id > b.chatId.id) ? 1 : (a.chatId.id === b.chatId.id) ? ((a.created.epochSecond > b.created.epochSecond) ? 1 : -1) : -1 );

  // display the last updated chats
  var msgsLastUpdated = messages.filter(it => it.chatId.id === chatLastUpdated);
  displayChats(msgsLastUpdated)

  // display other chats
  var msgsNormal = messages.filter(it => it.chatId.id !== chatLastUpdated);
  displayChats(msgsNormal)
}

function displayChats(messages){
  var previousId = '';
  let container = document.getElementById('messages')
  var i;

  for (i = 0; i < messages.length; i++) {
    var currentId = messages[i].chatId.id;
    if (currentId !== previousId){
      previousId = currentId;
      displayChatTitle(container, messages[i]);
    }
    displayChatMessage(container, messages[i]);
  }
}

function displayChatTitle(container, message){
  var active = (message.status === 'Active');
  let messageElem = document.createElement('div');
  var titleColor = getColor('Title')
  if (!active){
    titleColor = getColor('Closed')
  }
  messageElem.innerHTML = '<p align="center"><B><font color="' + titleColor + '">[' + message.chatId.id + ']</font></B></p>';
  container.append(messageElem);
}

function displayChatMessage(container, message){
  var active = (message.status === 'Active');
  let messageElem = document.createElement('div');
  var msgColor = getColor(message.sender)
  if (!active){
    msgColor = getColor('Closed')
  }
  messageElem.innerHTML = '<B><font color="' + msgColor + '">[' + message.sender + ']</font></B> ' + message.content;
  container.append(messageElem);
}


function getColor(key) {
  var colors = {
    'Title':  '#0020C2', // Cobalt Blue
    'PartyA': '#7B08DF', // purple
    'PartyB': '#66B35C', // green
    'PartyC': '#0000FF', // blue
    'PartyD': '#1D1F5F', // dark blue
    'Closed': '#B6B6B4'  // Gray Cloud
  };
  return colors[key];
}

function getInput(elementId) {
  return document.getElementById(elementId).value;
}

// check chat
function getValidId(ele) {
  var chatId = getInput(ele)
  if (chatId.length == 36){ // uuid like: b624ee0b-2dfa-484a-a419-8ab5b79e0bc8
    return chatId;
  }
  return ''
}
function alertInvalidId(){
  alert('Please provide valid chat ID, like:\n' + 'b624ee0b-2dfa-484a-a419-8ab5b79e0bc8');
}
// user click event
function createChat() {
  var subject = getInput("basicSubject")
  if (!subject){
    alert('Please provide subject to chat');
    return
  }
  var content = getInput("basicContent")
  if (!content){
    alert('Please provide content to chat');
    return
  }
  var receivers = getInput("basicParticipants")
  if (!receivers){
    alert('Please provide parties to chat, separated by comma (,)');
    return
  }
  var data = JSON.stringify({
    "subject": subject,
    "content": content,
    "receivers": receivers.split(",")
  });

  post("/chat", data)
}
function sendMessage() {
  var content = getInput("basicContent")
  if (!content){
    alert('Please provide content to send');
    return
  }

  var data = JSON.stringify({
    "content": content
  });

  var id = getValidId("basicChatId")
  if (!id){
    alertInvalidId()
    return
  }
  post("/chat/" + id, data)
}

function closeChat() {
  var id = getValidId("basicChatId")
  if (!id){
    alertInvalidId()
    return
  }
  post("/chat/" + id + "/close")
}

function addParticipants() {
  var toAdd = getInput("updateParticipants")
  if (!toAdd){
    alert('Please provide parties to add, separated by comma (,)');
    return
  }
  var data = JSON.stringify({
    "receivers": toAdd.split(",")
  });
  var id = getValidId("updateChatId")
  if (!id){
    alertInvalidId()
    return
  }
  post("/chat/" + id + "/participants/add", data)
}

function removeParticipants() {
  var toRemove = getInput("updateParticipants")
  if (!toRemove){
    alert('Please provide parties to remove, separated by comma (,)');
    return
  }
  var data = JSON.stringify({
    "receivers": toRemove.split(",")
  });

  var id = getValidId("updateChatId")
  if (!id){
    alertInvalidId()
    return
  }
  post("/chat/" + id + "/participants/remove", data)
}

function getAllChatIDs() {
  get("/chats/ids")
}
function getAllChats() {
  get("/chats/messages", true)
}

function getChatAllMessages() {
  var id = getValidId("queryChatId")
  if (!id){
    alertInvalidId()
    return
  }
  get("/chat/" + id, true)
}
function getChatCurrentStatus() {
  var id = getValidId("queryChatId")
  if (!id){
    alertInvalidId()
    return
  }
  get("/chat/" + id + "/status")
}
function getChatParticipants() {
  var id = getValidId("queryChatId")
  if (!id){
    alertInvalidId()
    return
  }
  get("/chat/" + id + "/participants")
}

function refresh(){
  getAllChats()
}
