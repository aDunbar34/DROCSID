const configuration = {
  iceServers: [
    { urls: "stun:stun.l.google.com:19302" },
    { urls: "stun:stun1.l.google.com:19302" },
  ],
};
const connection = new RTCPeerConnection(configuration);
const connectionStatus = document.getElementById("connection-status");

const args = window.location.href.split("?")[1].split(",");
if (args.length != 4) {
  connectionStatus.innerText = "Incorrect number of arguments!";
  throw new Error("Incorrect number of arguments");
}

const role = args[0];
const serverAddress = args[1];
const username = args[2];
const recipient = args[3];

let initiatorHeartbeatInterval;

const socket = new WebSocket(`ws://${serverAddress}:8081/socket/`);
window.addEventListener("unload", () => {
  socket.close();
});

socket.onopen = (event) => {
  sendIntroduction();
  if (role === "initiator") {
    initiatorCheckHeartbeat();
  }
};

socket.onmessage = (event) => {
  const response = JSON.parse(event.data);
  switch (response.type) {
    case "HEARTBEAT":
      console.log("Heartbeat received!");
  }
};

const sendIntroduction = function () {
  const message = {
    type: "INTRODUCTION",
    name: username,
  };
  socket.send(JSON.stringify(message));
};

const sendHeartbeat = function () {
  const message = {
    type: "HEARTBEAT",
    name: recipient,
  };
  socket.send(JSON.stringify(message));
};

const initiatorCheckHeartbeat = function () {
  sendHeartbeat();
};
