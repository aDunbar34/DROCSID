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

const localStreamElem = document.getElementById("local-stream");
const remoteStreamElem = document.getElementById("remote-stream");
const filePickerContainer = document.getElementById("file-picker-container");
const filePicker = document.createElement("input");
filePicker.type = "file";
filePicker.id = "file-picker";
filePicker.accept = "video/*";

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

const camStreamSetup = function () {
  navigator.mediaDevices
    .getUserMedia({ video: true, audio: true })
    .then((stream) => {
      localMediaStream = stream;
      localStreamElem.srcObject = localMediaStream;
      localStreamElem.play();
      localMediaStream
        .getTracks()
        .forEach((track) => connection.addTrack(track, localMediaStream));
    })
    .then(setupWebSockets);
};

const videoStreamSetup = function () {
  connectionStatus.innerText = "Select a video file to stream";
  filePickerContainer.append(filePicker);
};

filePicker.addEventListener("change", () => {
  if (filePicker.files.length === 0) {
    return;
  }

  const file = filePicker.files[0];
  if (!file.type.startsWith("video/")) {
    connectionStatus.innerText = "Invalid file. Please choose another.";
    return;
  }

  localStreamElem.loop = true;
  localStreamElem.muted = false;
  localStreamElem.src = URL.createObjectURL(file);

  localStreamElem.addEventListener("loadedmetadata", async () => {
    const stream = localStreamElem.captureStream();

    stream.getTracks().forEach((track) => {
      connection.addTrack(track, stream);
    });
  });

  localStreamElem.play();
  filePickerContainer.removeChild(filePicker);
  connectionStatus.innerText = "Establishing connection, please wait...";
  setupWebSockets();
});

let camVideoChoice;
while (camVideoChoice !== "cam" && camVideoChoice !== "video") {
  camVideoChoice = window.prompt(
    "Enter 'cam' to stream via a connected camera, or 'video' to stream a video file."
  );
}

if (camVideoChoice === "cam") {
  camStreamSetup();
} else {
  videoStreamSetup();
}

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
