# Getting Started with Wasabi

Installing Wasabi is straightforward. The following steps will install the needed tools, build and run a complete stack.

## Bootstrapping Your Environment

```bash
% /usr/bin/ruby \
  -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
% brew install git
% git clone https://github.com/idea/wasabi.git
% cd wasabi
% ./bin/wasabi.sh bootstrap
```

For OSX, you can get started as shown.

Installed tools include: [homebrew 0.9](http://brew.sh), [git 2](https://git-scm.com),
[maven 3](https://maven.apache.org), [java 1.8](http://www.oracle.com/technetwork/java/javase/overview/index.html),
[node 6](https://nodejs.org/en) and [python 2.7](https://www.python.org).

Similar tooling will work for Linux and Windows alike. Contribute a patch :)

## Starting Wasabi

```bash
% ./bin/wasabi.sh -b true start
...
=== Wasabi successfully started. ===
Wasabi UI: % open http://192.168.99.100:8080
ping: % curl -i http://192.168.99.100:8080/api/v1/ping
debug: attach debuger to 192.168.99.100:8180

% curl -i http://$(docker-machine ip wasabi):8080/api/v1/ping
HTTP/1.1 200 OK
Date: Wed, 25 May 2016 00:25:47 GMT
...
X-Application-Id: wasabi-api-20151215171929-SNAPSHOT-development
Content-Type: application/json
Transfer-Encoding: chunked
Server: Jetty(9.3.z-SNAPSHOT)

{
  "componentHealths":[
    {
      "componentName":"Experiments Cassandra",
      "healthy":true
    },
    {
      "componentName":"MySql","healthy":true
    }
  ],
  "wasabiVersion":"wasabi-api-20151215171929-SNAPSHOT-development"
}
```

Now that we have the necessary tools in place let's move on to build and start Wasabi followed by issuing a _ping_
command to verify the build:
<div></div>

Congratulations! You are the proud owner of a newly minted and personalized full stack Wasabi instance :)

Ok, enough celebration ... let's get back to business.

## Calling Wasabi

These are the 3 common API's that you'd use to instrument your client application with Wasabi API's.

Let's assume that you've created and started a sample experiment 'buyButtonTest' in 'myApp' application with 'orangeButton'
and 'greenButton' buckets via the Admin UI or by calling the API's. 

```bash
Assign a user to experiment and bucket:
% curl -H "Content-Type: application/json" http://192.168.99.100:8080/api/v1/assignments/applications/myApp/experiments/buyButtonTest/users/userID1
{"cache":true,"payload":null,"assignment":"orangeButton","context":"PROD","status":"NEW_ASSIGNMENT"}
```

You can assign a user with a unique ID (e.g. 'userID1') to an experiment by calling this API Request:
<div></div>

```bash
Record an impression:
% curl -H "Content-Type: application/json" -d "{\"events\":[{\"name\":\"IMPRESSION\"}]}" http://192.168.99.100:8080/api/v1/events/applications/myApp/experiments/buyButtonTest/users/userID1
```

Now the 'userID1' user is assigned into the 'orangeButton' bucket. Let's record an impression of their experience with this API Request:
<div></div>

```bash
Record an action:
% curl -H "Content-Type: application/json" -d "{\"events\":[{\"name\":\"BuyClicked\"}]}" http://192.168.99.100:8080/api/v1/events/applications/myApp/experiments/buyButtonTest/users/userID1
```

If the 'userID1' user does an action, such as clicking the buy button, you'd record it with this API Request: 
<div></div>

## Stopping Wasabi

```bash
% ./bin/wasabi.sh stop
```

Alas, all good things must come to an end. Let's clean things up a bit stop the newly created Wasabi stack:

At this point in time we now have all the requisite tools installed and as such subsequent invocations of Wasabi will
start up much more quickly. Additionally there is no further need to include the _-b true_ or _--build true_ option.

<div></div>
```bash
% ./bin/wasabi.sh -h
  
  usage: wasabi.sh [options] [commands]
  
  options:
    -b | --build [ true | false ]          : build; default: false
    -e | --endpoint [ host:port ]          : api endpoint; default: localhost:8080
    -v | --verify [ true | false ]         : verify installation configuration; default: false
    -s | --sleep [ sleep-time ]            : sleep/wait time in seconds; default: 30
    -h | --help                            : help message
  
  commands:
    bootstrap                              : install dependencies
    start[:cassandra,mysql,wasabi]         : start all, cassandra, mysql, wasabi
    test                                   : test wasabi
    stop[:wasabi,cassandra,mysql]          : stop all, wasabi, cassandra, mysql
    resource[:ui,api,doc,cassandra,mysql]  : open resource api, javadoc, cassandra, mysql
    status                                 : display resource status
    remove[:wasabi,cassandra,mysql]        : remove all, wasabi, cassandra, mysql
    package                                : build deployable packages
    release[:start,finish]                 : promote release
```

Further, there are a number of additional wasabi.sh options available you should become familiar with:



## Developing Wasabi

Let's turn it up to 11. To facilitate developing Wasabi contributions you can easily build and run from source and readily connect to the a fore mentioned infrastructure:

### Build and Run Wasabi Server

```bash
% mvn package
% ./bin/wasabi.sh start:cassandra,mysql
% dmip=$(docker-machine ip wasabi)
% (cd modules/main/target; \
    WASABI_CONFIGURATION="-DnodeHosts=${dmip} -Ddatabase.url.host=${dmip}" ./wasabi-main-*-SNAPSHOT-development/bin/run) &
% curl -i http://localhost:8080/api/v1/ping
...
```

<div></div>

> Viewing runtime logs:

```bash
% tail -f modules/main/target/wasabi-main-*-SNAPSHOT-development/logs/wasabi-main-*-SNAPSHOT-development.log
```

The runtime logs can be accessed executing the following command in a another shell:

### Build and Run Wasabi UI

```bash
% cd modules/ui
% grunt build
```

> Edit Gruntfile.js with the following change to the apiHostBaseUrlValue value, since you would be running the
Wasabi server on localhost.

```javascript
development: {
                constants: {
                    supportEmail: 'you@example.com',
                    apiHostBaseUrlValue: 'http://localhost:8080/api/v1'
                }
            }
```

```bash
% grunt serve
```

### Resources

The following developer resources are available:

  * API

```bash
% ./bin/wasabi.sh resource:api
```

  * Javadoc
  
```bash
% ./bin/wasabi.sh resource:doc
```

  * UI
  
```bash
% ./bin/wasabi.sh resource:ui
```

  * Cassandra

```bash
% ./bin/wasabi.sh resource:cassandra
```

  * MySql

```bash
% ./bin/wasabi.sh resource:mysql
```

  * Java Debugger Remote Attach Configuration

```bash
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8180
```

### Stopping Wasabi

```bash
% ./bin/wasabi.sh stop
```

<div></div>
> Wasabi runtime configuration:

```bash
-DnodeHosts=$(docker-machine ip wasabi) -Ddatabase.url.host=$(docker-machine ip wasabi)
```

Now while that was fun, in all likelihood you will be using an IDE to develop Wasabi features. In doing so you need only
add the aforementioned configuration information to your Wasabi JVM runtime prior to startup:

<div></div>
Awesome! You are well on your way at this point in time.

### Running Functional Tests
```bash
% ./bin/wasabi.sh start test stop
```

Code changes can readily be verified by running the growing collection of included functional tests:



## Packaging and Deploying Wasabi at Scale

```bash
% ./bin/wasabi.sh package
% find ./modules -type f \( -name "*.rpm" -or -name "*.deb" \)
```
Wasabi can readily be packaged as installable *rpm* or *deb* distributions and deployed at scale as follows:

Note: [Java 8](http://www.oracle.com/technetwork/java/javase/overview/index.html) is a runtime dependency

## Integrating Wasabi

```xml
<dependency>
    <groupId>com.intuit.wasabi</groupId>
    <artifactId>wasabi</artifactId>
    <version>1.0.TBD</version>
</dependency>
```
Wasabi is readily embeddable via the following *maven* dependency GAV family:


## Recap

Ok, that was a ground to cover. Here is a high level overview of the operational elements described above, either directly or indirectly:

![OverView](getting_started/landscape.png)

And lastly, as a teaser, here is a technology stack diagram of the elements of which Wasabi is comprised of:

![TechStack](getting_started/stack.png)