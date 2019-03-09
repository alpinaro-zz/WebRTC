WebRTCTest Tool is a java project for testing Ant Media Server webrtc capabilities.

* WebRTCTest is compatiple with Ant Media Server signalling protocol. 
* There is two modes of WebRTCTest: publisher and player. (-m flag determines the mode)
* WebRTCTest two options with UI or without UI. (-u flag determines the UI od/off)
* You can save recieved(in player mode) or published(in publisher mode) frames as png. (-p flag determines the saving periof of frames. If you set 100 it saves every 100 frames.)

## Run
It can be run from command promt with the following options.
```
./run.sh -f output.mp4 -m publisher -n 1
```

### Parameters
```
Flag 	 Name      	 Default   	 Description                 
---- 	 ----      	 -------   	 -----------   
f    	 File Name 	 -         	 media file in same directory.Mandatory in publishing*            
s    	 Server Ip 	 localhost 	 server ip                   
q    	 Sequrity  	 false     	 true(wss) or false(ws)      
l    	 Label     	 nolabel   	 window lable                
i    	 Stream Id 	 myStream  	 id for stream               
m    	 Mode      	 player    	 publisher or player         
u    	 Show GUI  	 true      	 true or false               
p    	 Period    	 0         	 frame period to save as png 
v    	 Verbose   	 false     	 true or false 
n        Count   	 1         	 Number of player/publisher connctions   
```

*File should be in mp4 format and h264, opus encoded