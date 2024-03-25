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
const peerUsername = args[3];

let localStreamElem = document.getElementById("local-stream");
let remoteStreamElem = document.getElementById("remote-stream");
const webcamRadio = document.getElementById("webcam-radio");
const videoRadio = document.getElementById("video-radio");
const filePicker = document.getElementById("file-picker");

let initiatorHeartbeatInterval;
let localMediaStream;
let finished = 0;
let webcamFlag = true;

let socket;

connection.oniceconnectionstatechange = (event) => {
  if (connection.iceConnectionState === "connected") {
    connectionStatus.innerText = "Connection established";
  }
};

connection.onicecandidate = (event) => {
  console.log("Ice Candidate Event");
  if (event.candidate) {
    sendICECandidate(event.candidate);
    console.log(event.candidate);
  } else if (event.candidate === null) {
    console.log("ICE gathering finished.");
    sendFinish();
    finished += 1;
    if (finished == 2) {
      socket.close();
      console.log("Socket closed.");
    }
  }
};

connection.ontrack = (event) => {
  console.log("Track event fired");
  if (event.streams[0]) {
    const newRemoteStreamElem = document.createElement("video");
    newRemoteStreamElem.id = "remote-stream";
    newRemoteStreamElem.autoplay = true;

    remoteStreamElem.style.display = "none";
    remoteStreamElem.parentNode.appendChild(newRemoteStreamElem);
    remoteStreamElem.parentNode.removeChild(remoteStreamElem);

    remoteStreamElem = newRemoteStreamElem;
    remoteStreamElem.srcObject = event.streams[0];
    remoteStreamElem.play();
  }
};

const setupWebSockets = function () {
  socket = new WebSocket(`ws://${serverAddress}:8081/socket/`);
  window.addEventListener("unload", () => {
    socket.close();
    connection.close();
  });

  socket.onopen = () => {
    sendIntroduction();
    if (role === "initiator") {
      initiatorHeartbeatInterval = setInterval(() => sendHeartbeat(), 1000);
    }
  };

  socket.onmessage = (event) => {
    const response = JSON.parse(event.data);
    switch (response.type) {
      case "HEARTBEAT":
        console.log("Heartbeat received");
        console.log(response);
        if (response.name === peerUsername && response.connected === true) {
          console.log("Recipient is connected");
          clearInterval(initiatorHeartbeatInterval);
          sendOffer();
        }
        break;
      case "SDP":
        const sdp = response.content;
        connection.setRemoteDescription(sdp);
        console.log("Received SDP");
        if (role === "recipient") {
          sendAnswer();
        }
        break;
      case "ICE":
        const candidate = response.content;
        connection.addIceCandidate(candidate);
        console.log("Received new ICE candidate");
        break;
      case "FINISH":
        if (response.sender === peerUsername) {
          finished += 1;
          console.log("Finish received");
          if (finished == 2) {
            socket.close();
            console.log("Socket closed.");
          }
        }
    }
  };
};

navigator.mediaDevices
  .getUserMedia({ video: true, audio: true })
  .then((stream) => {
    localMediaStream = stream;
    localStreamElem.srcObject = localMediaStream;
    localMediaStream
      .getTracks()
      .forEach((track) => connection.addTrack(track, localMediaStream));
  })
  .then(setupWebSockets);

webcamRadio.addEventListener("click", () => {
  if (webcamFlag) {
    return;
  }
  localStreamElem.muted = true;
  localStreamElem.srcObject = localMediaStream;

  connection.getSenders().forEach((sender) => {
    connection.removeTrack(sender);
  });

  localMediaStream
    .getTracks()
    .forEach((track) => connection.addTrack(track, localMediaStream));

  webcamFlag = true;
});

videoRadio.addEventListener("click", () => {
  if (!webcamFlag) {
    return;
  }

  if (filePicker.files.length === 0) {
    return;
  }

  const file = filePicker.files[0];
  if (!file.type.startsWith("video/")) {
    return;
  }

  const newLocalStreamElem = document.createElement("video");
  newLocalStreamElem.id = "local-stream";
  newLocalStreamElem.autoplay = true;

  localStreamElem.style.display = "none";
  localStreamElem.parentNode.insertBefore(
    newLocalStreamElem,
    localStreamElem.nextSibling
  );
  localStreamElem.parentNode.removeChild(localStreamElem);

  localStreamElem = newLocalStreamElem;
  localStreamElem.src = URL.createObjectURL(file);

  connection.getSenders().forEach((sender) => {
    connection.removeTrack(sender);
  });

  localStreamElem.addEventListener("loadedmetadata", async () => {
    console.log("Metadata Load Reached");

    const stream = localStreamElem.captureStream();

    stream.getTracks().forEach((track) => {
      connection.addTrack(track, stream);
    });
  });

  localStreamElem.play();
});

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
    name: peerUsername,
  };
  socket.send(JSON.stringify(message));
  console.log("Sent heartbeat");
};

const sendOffer = function () {
  connection
    .createOffer()
    .then((offer) => {
      connection.setLocalDescription(offer);
      const message = {
        type: "SDP",
        sender: username,
        recipient: peerUsername,
        content: offer,
      };
      socket.send(JSON.stringify(message));
    })
    .then(console.log("Sent offer"));
};

const sendAnswer = function () {
  connection
    .createAnswer()
    .then((answer) => {
      connection.setLocalDescription(answer);
      const message = {
        type: "SDP",
        sender: username,
        recipient: peerUsername,
        content: answer,
      };
      socket.send(JSON.stringify(message));
    })
    .then(console.log("Sent answer"));
};

const sendICECandidate = function (candidate) {
  const message = {
    type: "ICE",
    sender: username,
    recipient: peerUsername,
    content: candidate,
  };
  socket.send(JSON.stringify(message));
  console.log("Sent ICE candidate");
};

const sendFinish = function () {
  const message = {
    type: "FINISH",
    sender: username,
    recipient: peerUsername,
  };
  socket.send(JSON.stringify(message));
  console.log("Sent finish");
};
