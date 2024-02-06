from socket import *
import threading
import sys


def inputHandler(socket):
    while True:
        buffer = b""
        byte = b""
        while byte != b"\n":
            byte = socket.recv(1)
            if not byte:
                break
            buffer += byte
        print("\033[K", end="")
        print("\rchum> " + buffer.decode("utf-8"), end="")
        print("\033[Kyou> ", end="", flush=True)


def outputHandler(socket):
    while True:
        message = input("you> ")
        message += "\n"
        socket.send(message.encode("utf-8"))


serverName = sys.argv[1]
serverPort = int(sys.argv[2])

clientSocket = socket(AF_INET, SOCK_STREAM)
clientSocket.connect((serverName, serverPort))

inputThread = threading.Thread(target=inputHandler, args=(clientSocket,))
outputThread = threading.Thread(target=outputHandler, args=(clientSocket,))

inputThread.start()
outputThread.start()

while inputThread.is_alive() and outputThread.is_alive():
    pass
