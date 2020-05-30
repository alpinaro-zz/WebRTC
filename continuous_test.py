import subprocess
import os
import time
import signal
import sys
import requests
import json
import random

mainPublisher = None
mainPlayer = None
rtmpPublisher = None
session = requests.Session()
publishers = []
players = []

class TestTool:
  def __init__(self, streamId, process):
    self.streamId = streamId
    self.process = process

def createRtmpPublisher(appName, streamId):
  process = subprocess.Popen(["ffmpeg", "-re", "-stream_loop", "-1", "-i", "test_264_aac.mp4",
    "-codec", "copy", "-f", "flv", "rtmp://localhost/"+appName+"/"+streamId],
    cwd="webrtctest",
    stdin=open(os.devnull, 'wb'), stdout=open(os.devnull, 'wb'), stderr=open(os.devnull, 'wb'))
  return process

def createPublisher(streamId):
  process = subprocess.Popen(["java", "-cp", "webrtc-test.jar:libs/*", "-Djava.library.path=libs/native", "io.antmedia.Starter",
    "-m", "publisher", "-i", streamId, "-s", "localhost", "-f", "test.mp4", "-r", "true"],
    cwd="webrtctest",
    stdout=open(os.devnull, 'wb'), stderr=open(os.devnull, 'wb'))
  return process

def createPlayer(streamId, load):
  process = subprocess.Popen(["java", "-cp", "webrtc-test.jar:libs/*", "-Djava.library.path=libs/native", "io.antmedia.Starter",
    "-i", streamId, "-s", "localhost", "-n", load, "-u", "false"],
    cwd="webrtctest",
    stdout=open(os.devnull, 'wb'), stderr=open(os.devnull, 'wb'))
  return process

def login():
  resp = session.post("http://localhost:5080/rest/authenticateUser", json={"email":"test@antmedia.io","password":"testtest"})
  #print(resp)

def streamInfo(streamId):
  resp = session.get("http://localhost:5080/rest/request?_path=WebRTCAppEE/rest/v2/broadcasts/"+streamId)
  jsonData = json.loads(resp.text)
  #print (json.dumps(jsonData, indent=2))
  return jsonData

def systemInfo(streamId):
  resp = session.get("http://localhost:5080/rest/getSystemResourcesInfo")
  jsonData = json.loads(resp.text)
  #print (json.dumps(jsonData, indent=2))
  return jsonData

def doAction():
  action = "no action"
  rand = random.randint(0, 4)
  if rand == 0:
    #create publisher
    streamId = "stream"+str(int(time.time()))
    process = createPublisher(streamId)
    action = "new publisher:"+streamId
    publishers.append(TestTool(streamId, process))
  elif rand == 1:
    #create player
    if (len(publishers) > 0):
      index = random.randint(0, len(publishers)-1)
      streamId = publishers[index].streamId
      process = createPlayer(streamId, "1")
      action = "new player:"+streamId
      players.append(TestTool(streamId, process))
  elif rand == 2:
    #remove publisher
    if (len(publishers) > 0):
      index = random.randint(0, len(publishers)-1)
      action = "remove publisher:"+publishers[index].streamId
      publishers[index].process.kill()
      del publishers[index]
  elif rand == 3:
    #remove player
    if (len(players) > 0):
      index = random.randint(0, len(players)-1)
      action = "remove player:"+players[index].streamId
      players[index].process.kill()
      del players[index]
  return action
  
def main():
#  sys.stdout = open('file', 'w')
  login()

  mainStreamId="test"
  global mainPublisher
  global mainPlayer
  global rtmpPublisher
  mainPublisher = createPublisher(mainStreamId)
  
  time.sleep(5)
  print("before create rtmp publisher")
  rtmpPublisher = createRtmpPublisher("LiveApp", "rtmp1")
  
#  time.sleep(5)
  print("after create rtmp publisher")
  mainPlayer = createPlayer(mainStreamId, "5")
  print ("Time in second\tCPU\tRAM\tStreams\tViewers\tAction")
  for x in range(1000):
    jsonData = systemInfo(mainStreamId)
    now = int(time.time())
    inUseMemory = jsonData["systemMemoryInfo"]["inUseMemory"]
    totalMemory = jsonData["systemMemoryInfo"]["totalMemory"]
    cpuUsage = jsonData["cpuUsage"]["systemCPULoad"]
    totalLiveStreamSize = jsonData["totalLiveStreamSize"]
    localWebRTCViewers = jsonData["localWebRTCViewers"]
    action = doAction()
    print(str(now)+"\t"+str(inUseMemory*100/totalMemory)+"\t"+str(cpuUsage)+"\t"+str(totalLiveStreamSize)+"\t"+str(localWebRTCViewers)+"\t"+action)
    time.sleep(5)

  clear_processes()
  print("leaving the main")


def clear_processes():
  rtmpPublisher.kill()
  mainPublisher.kill()
  mainPlayer.kill()
  for tool in publishers:
    tool.process.kill()
  for tool in players:
    tool.process.kill()

def signal_handler(sig, frame):
  print("Signal handler is running") 
  clear_processes()
    
  sys.exit(0)

signal.signal(signal.SIGINT, signal_handler)

#handler for kill command. It's not for -9. It's for just kill
signal.signal(signal.SIGTERM, signal_handler)


main()

