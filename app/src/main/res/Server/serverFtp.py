#!/usr/bin/python
from time import sleep, time
import socket, sys, threading
from os.path import exists, isfile
from os import system, makedirs, getcwd
from ftp_parent import MAIN
from sys import exit

# Global Stuff
global localhost, serverPort, conn, sock
serverPort = 5004
backlog = 5
credintials = [""]*3
APC_Downloads = ""
connectedOnce = False
totalConnections = 0
actionData = "0"
cwd = getcwd()

def initiatePath():
	from platform import platform
	if "linux" in platform().lower():    #for linux
		APC_Downloads = cwd+"/APC-Downloads/"
	elif "darwin" in platform().lower(): #for macos
		APC_Downloads = cwd+"/APC-Downloads/"
	elif "windows" in platform().lower():#for windows
		APC_Downloads = cwd+r'\ APC-Downloads\ '.replace(" ", "")
	else:
		APC_Downloads = "APC-Downloads"
		pass
	return APC_Downloads 

try:
	credintials = open("now-ip.txt", 'r').readlines()
except FileNotFoundError as e:
	print("FileNotFoundError: [Errno 2] No such file or directory: \'now-ip.txt\'")
	print("--->Using LocalHost IP")

def getLocalIP():
	s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
	s.connect(("8.8.8.8", 80))
	ip = s.getsockname()[0]
	s.close()
	return ip

class server(MAIN):
	''' Constructor to Establish Bind server once an object made'''
	def __init__(self, localhost, serverPort):	# Connect Tcp
		global bcklog, count, sock
		self.servSock = socket.socket()         #tcp
		try:
			self.servSock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
			self.servSock.bind((localhost, serverPort))# bind((host,Port))
		except Exception as e:
			print("[Bind ]", e)
			sys.exit()

	def accept(self):
		global conn, addr, connectedOnce, totalConnections
		while True:
			try:
				self.servSock.listen(backlog)
				self.conn, self.addr = self.servSock.accept()
				conn = self.conn
				connectedOnce = True
				totalConnections +=1
				print("Client's added Successfully")
				main()
			except Exception as e:
				print("[conn Error ]", e)

	def exit(self):
		self.conn.close()

	def down(self, name, conn):
		while True:
			MAIN.download(name, conn)

def main():
	buffer = 1024
	global localhost, serverPort, conn, sock, connectedOnce, actionData, totalConnections
	while True:
		reset = 0
		print("Waiting an Action.. totalConnections=", totalConnections)
	
		actionData = conn.recv(5).decode("UTF-8")
		if actionData != "1" and actionData != "2":
			actionData = "3"
		try:
			conn.send(str.encode("1"))
		except Exception as e:
			break
		print("actionData: ", actionData)

		fileSize = 0

		if str(actionData) == "1":# Download
			print("Client is uploading a file...")
			start = time()
			serverObj.download(conn, int(fileSize), APC_Downloads)
			end = time()
			actionData = "0"

		elif str(actionData) == "2":# Upload
			fileUpload  = input("Choose a file: ") # raw_input = string
			while (not isfile(fileUpload)): # to check if file exists.)
				fileUpload  = input("Choose a file: ") # raw_input = string
			start = time()
			serverObj.upload(fileUpload, conn, buffer)
			end = time()
			print("Uploaded in: {}".format(end-start))
			actionData = "0"
		elif str(actionData) == "3":# Upload
			break
		else:
			print("Something went Wrong!")
			actionData = "0"
			continue


if  __name__ == "__main__":
	finishThread = threading.Event()
	APC_Downloads = initiatePath()
	if not exists(APC_Downloads):
		makedirs(APC_Downloads)

	try:
		localhost = getLocalIP()
		serverObj = server(localhost, serverPort)

		print("({}, {})".format(localhost, serverPort))
		print("[+] Server is UP!")
		print("[+] Files will be downloaded to: "+APC_Downloads)
	except Exception as e:
		print ("[Main ] ",e)
		exit()

	acceptThread = threading.Thread(name = "Accepting Connections", target=serverObj.accept)
	acceptThread.start()
	while not connectedOnce:
		pass

