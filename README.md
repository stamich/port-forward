# Port Forwarding Server Application

## General Information

This is a simple port forwarding server application that forwards incoming TCP connections to a specified destination address and port. 
The application is written in Scala and uses Akka Streams for handling the TCP connections.
When a client connects to the server, the server establishes a connection to the specified destination address and port and forwards data between the client and the destination.

Technologies used:
- Scala 2.13
- ScalaFx 22.0.0-R33

- Akka Actor 2.8.8
- Akka Streams 2.8.8
- Akka HTTP 10.2.4

- Cats 2.13.0
- Cats Effect 3.5.7

- SBT 1.10.7

## Usage

### Build the application

To build the application you need to have installed on your machine:
- Java 17
- Scala 2.13
- SBT 1.10.7

When you have all the required tools installed, go to the root directory of the project and run the following command:

`sbt clean assembly`

After the command finishes, you will find the executable jar file in the `target/scala-2.13` directory.

Application is named `port-forward-server.jar`.

### Run the application

To run the application, you need to have Java 17 installed on your machine.

To start the application, go to the `target/scala-2.13` folder run the following command:

`java -jar port-forward-server.jar --cli` - for starting the application in the command line interface mode.
`java -jar port-forward-server.jar --gui` - for starting the application in the graphical user interface mode.

In the command line interface mode, application is configured in attached `application.conf` file.

In the graphical user interface mode, you can configure the application using the GUI.