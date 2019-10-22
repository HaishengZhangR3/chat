<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Spring webserver

This project defines a simple Spring webserver that connects to a Corda node via RPC.

# Structure:

The Spring web server is defined in the `server` module, and has two parts:

* `src/main/resources/static`, which defines the webserver's frontend
* `src/main/kotlin/net/corda/server`, which defines the webserver's backend

The backend has two controllers, defined in `server/src/main/kotlin/net/corda/server/Controller`:

* `ChatController`, which provides Chat function REST endpoints

# Pre-requisites:

See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

# A simple chat room with Chat SDK supported feature

## ports list:
    Name: PartyA, RPC port: 10011, websocket port: 20001 , web service: http://localhost:10055/loc
    Name: PartyB, RPC port: 10021, websocket port: 20002 , web service: http://localhost:10056/loc
    Name: PartyC, RPC port: 10006, websocket port: 20003 , web service: http://localhost:10057/loc
    Name: PartyD, RPC port: 10016, websocket port: 20004 , web service: http://localhost:10058/loc

## build: contract, flow, client CorDapp, client web web service, deploy all nodes

    ./gradlew clean; 
    ./gradlew jar; 
    ./gradlew bootJar; 
    ./gradlew deployNodes
    
## run nodes:

    copy the CorDapp jars to nodes cordapp folder (it is in examples/client-webserver), i.e.: build/nodes/PartyA/cordapps
    ./examples/client-webserver/build/nodes/runnodes

## run kotlin-shell command line to access via RPC:

    git/corda-kotlin-shell/startup.sh
    (in terminal) :load ./scripts/setupChat.kts
    (do anything, refer to ./scripts/setupChat.kts)
    
## run web server for all nodes to access via REST api:

    java -jar build/libs/client-webserver-1.0-RC01.jar --server.port=10055 --config.rpc.port=10011
    java -jar build/libs/client-webserver-1.0-RC01.jar --server.port=10056 --config.rpc.port=10021
    java -jar build/libs/client-webserver-1.0-RC01.jar --server.port=10057 --config.rpc.port=10006
    java -jar build/libs/client-webserver-1.0-RC01.jar --server.port=10058 --config.rpc.port=10016

## run client UI in Chrome with "discable CORS" mode:
You must open the Chrome with "discable CORS" mode, this way:

    open -n -a /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --args --user-data-dir="/tmp/chrome_dev_test" --disable-web-security

and then open the index page:

    open chat/examples/client-webserver/src/main/resources/static/index.html
