<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Spring webserver

This project defines a simple Spring webserver that connects to a Corda node via RPC.

# Structure:

The Spring web server is defined in the `server` module, and has two parts:

* `src/main/resources/static`, which defines the webserver's frontend
* `src/main/kotlin/net/corda/server`, which defines the webserver's backend

The backend has two controllers, defined in `server/src/main/kotlin/net/corda/server/Controller.kt`:

* `StandardController`, which provides generic (non-CorDapp specific) REST endpoints
* `CustomController`, which the user can extend to provide CorDapp-specific REST endpoints

# Pre-requisites:

See https://docs.corda.net/getting-set-up.html.

# Usage

## Running the nodes:

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

## Running the webservers:

Once the nodes are running, there are several ways to run the webservers. All these approaches 
read their properties from the `src/main/resources/application.properties` file:

* `server.port`, which defines the HTTP port the webserver listens on
* `config.rpc.*`, which define the RPC settings the webserver uses to connect to the node

### With Gradle:

You can start the webserver for Party A using Gradle:

* Windows: `gradlew.bat runPartyAServer`
* Unix: `./gradlew runPartyAServer`

### As a self-contained JAR:

You can convert the webserver for Party A into a runnable JAR using:

* Windows: `gradlew.bat bootJar`
* Unix: `./gradlew bootJar`

And run the webserver using:

    java -jar build/libs/corda-webserver.jar
    
To specify your own property file:

    java –DApp.config.file=local.properties –jar build/libs/corda-webserver.jar


### With IntelliJ

You can also start the webserver for Party A using the `Run PartyA Server` IntelliJ run configuration.

## Interacting with the nodes:

Once the nodes are started, you can access the node's frontend at the following address:

    localhost:10055

And you can access the REST endpoints at:

    localhost:10055/[ENDPOINT]

For example, you can check the node's status using:

    localhost:10055/status

## Access through Chrome html/javascript web page:
You must open the Chrome with "discable CORS" mode
open -n -a /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --args --user-data-dir="/tmp/chrome_dev_test" --disable-web-security


# Letter of Credit with Chat SDK supported feature

## build: contract, flow, loc CorDapp, loc web wervice, deploy 4 nodes

    ./gradlew clean; 
    ./gradlew jar; 
    ./gradlew bootJar; 
    ./gradlew deployNodes
    
## run nodes:

    copy the CorDapp jars to nodes cordapp folder, i.e.: build/nodes/Durr/cordapps
    ./examples/letter-of-credit-webserver/build/nodes/runnodes

## run kotlin-shell command line to access via RPC:

    git/corda-kotlin-shell/startup.sh
    (in terminal) :load ./scripts/setupChat.kts
    (do anything, refer to ./scripts/setupChat.kts)
    
## run web server for all nodes to access via REST api:

    java -jar build/libs/letter-of-credit-webserver-1.0-RC01.jar --server.port=10055 --config.rpc.port=10011
    java -jar build/libs/letter-of-credit-webserver-1.0-RC01.jar --server.port=10056 --config.rpc.port=10021
    java -jar build/libs/letter-of-credit-webserver-1.0-RC01.jar --server.port=10057 --config.rpc.port=10006
    java -jar build/libs/letter-of-credit-webserver-1.0-RC01.jar --server.port=10058 --config.rpc.port=10016

### ports list:
    Name: Daimler, RPC port: 10011, websocket port: 20001 , web service: http://localhost:10055/loc
    Name: DaimlerBank, RPC port: 10021, websocket port: 20002 , web service: http://localhost:10056/loc
    Name: Durr, RPC port: 10006, websocket port: 20003 , web service: http://localhost:10057/loc
    Name: DurrBank, RPC port: 10016, websocket port: 20004 , web service: http://localhost:10058/loc


    

    