from socket import socket, AF_INET, SOCK_STREAM
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
        print("\rDROCSID user> " + buffer.decode("utf-8"), end="")
        print("\033[Kyou> ", end="", flush=True)


def outputHandler(socket):
    while True:
        message = input("you> ")
        message += "\n"
        socket.send(message.encode("utf-8"))


print(
    """
██████╗ ██████╗  ██████╗  ██████╗███████╗██╗██████╗
██╔══██╗██╔══██╗██╔═══██╗██╔════╝██╔════╝██║██╔══██╗
██║  ██║██████╔╝██║   ██║██║     ███████╗██║██║  ██║
██║  ██║██╔══██╗██║   ██║██║     ╚════██║██║██║  ██║
██████╔╝██║  ██║╚██████╔╝╚██████╗███████║██║██████╔╝
╚═════╝ ╚═╝  ╚═╝ ╚═════╝  ╚═════╝╚══════╝╚═╝╚═════╝
    """
)

print("You are now experiencing DROCSID in PYTHON PEER MODE")


if len(sys.argv) >= 3:
    hostname = sys.argv[1]
    port_no = int(sys.argv[2])
    socket = socket(AF_INET, SOCK_STREAM)
    socket.connect((hostname, port_no))
elif len(sys.argv) == 2:
    port_no = int(sys.argv[1])
    server_socket = socket(AF_INET, SOCK_STREAM)
    server_socket.bind(("", port_no))
    server_socket.listen()
    socket, addr = server_socket.accept()

inputThread = threading.Thread(target=inputHandler, args=(socket,))
outputThread = threading.Thread(target=outputHandler, args=(socket,))

inputThread.start()
outputThread.start()

while inputThread.is_alive() and outputThread.is_alive():
    pass
