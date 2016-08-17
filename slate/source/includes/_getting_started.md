# Getting Started with Wasabi

The following steps will help you install the needed tools, then build and run a complete Wasabi stack. Note, at this time, only Mac OS X is supported.

## Bootstrap Your Environment

```bash
% /usr/bin/ruby \
  -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
% brew install git
% git clone https://github.com/intuit/wasabi.git
% cd wasabi
% ./bin/wasabi.sh bootstrap
```

Installed tools include: [homebrew 0.9](http://brew.sh), [git 2](https://git-scm.com),
[maven 3](https://maven.apache.org), [java 1.8](http://www.oracle.com/technetwork/java/javase/overview/index.html),
[docker 1.12](https://docker.com), [node 6](https://nodejs.org/en) and [python 2.7](https://www.python.org).

Similar tooling will work for Linux and Windows alike. Contribute a patch :)

## Start Wasabi

```bash
% ./bin/wasabi.sh build start
...
wasabi is operational:

  ui: % open http://localhost:8080     note: sign in as admin/admin
  ping: % curl -i http://localhost:8080/api/v1/ping
  debug: attach to localhost:8180

% curl -i http://localhost:8080/api/v1/ping
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

Congratulations! You are the proud owner of a newly minted Wasabi instance. :)

## Troubleshoot Wasabi

> Look at the current docker containers that have been successfully started.

```bash
% ./bin/wasabi.sh status

CONTAINER ID        IMAGE                    COMMAND                  CREATED             STATUS              PORTS                                                                     NAMES
8c12458057ef        wasabi-main              "entrypoint.sh wasabi"   25 minutes ago      Up 25 minutes       0.0.0.0:8080->8080/tcp, 0.0.0.0:8090->8090/tcp, 0.0.0.0:8180->8180/tcp    wasabi-main
979ecc885239        mysql:5.6                "docker-entrypoint.sh"   26 minutes ago      Up 26 minutes       0.0.0.0:3306->3306/tcp                                                    wasabi-mysql
2d33a96abdcb        cassandra:2.1            "/docker-entrypoint.s"   27 minutes ago      Up 27 minutes       7000-7001/tcp, 0.0.0.0:9042->9042/tcp, 7199/tcp, 0.0.0.0:9160->9160/tcp   wasabi-cassandra
```

* While starting Wasabi, if you see an error when the docker containers are starting up, you could do the following:

<div></div>
<div></div>
> E.g. if Cassandra and Wasabi containers have not started, then start them individually:

```bash
% ./bin/wasabi.sh start:cassandra

% ./bin/wasabi.sh start:wasabi
```
* The above shell output shows a successful start of 3 docker containers needed by Wasabi: wasabi-main (the Wasabi server), wasabi-mysql, and wasabi-cassandra. If any of these are not running, try starting them individually. For example, if the MySQL container is running, but Cassandra and Wasabi containers failed to start (perhaps due to a network timeout docker could not download the Cassandra image), do the following:


## Call Wasabi

These are the 3 common REST endpoints that you will use to instrument your client application with Wasabi.

Let's assume that you've created and started an experiment, 'BuyButton,' in the 'Demo_App' application with the following buckets:

* 'BucketA': green button (control bucket)
* 'BucketB': orange button bucket

> Assign a user to experiment and bucket:

```bash
% curl -H "Content-Type: application/json" \
    http://localhost:8080/api/v1/assignments/applications/Demo_App/experiments/BuyButton/users/userID1

{  
   "cache":true,
   "payload":"green",
   "assignment":"BucketA",
   "context":"PROD",
   "status":"NEW_ASSIGNMENT"
}
```

You can assign a user with a unique ID (e.g. 'userID1') to the experiment by making this HTTP request:

<div></div>

> Record an impression:

```bash
% curl -H "Content-Type: application/json" \
    -d "{\"events\":[{\"name\":\"IMPRESSION\"}]}" \
    http://localhost:8080/api/v1/events/applications/Demo_App/experiments/BuyButton/users/userID1
```

Now the 'userID1' user is assigned into the 'BucketA' bucket. Let's further record an impression, meaning the user has seen a given experience:
<div></div>

> Record an action:

```bash
% curl -H "Content-Type: application/json" \
    -d "{\"events\":[{\"name\":\"BuyClicked\"}]}" \
    http://localhost:8080/api/v1/events/applications/Demo_App/experiments/BuyButton/users/userID1
```

If the 'userID1' user performs an action such as clicking the Buy button, you'd record that action with the following request:
<div></div>

## Developer Resources

> API: Swagger API playground

```bash
% ./bin/wasabi.sh resource:api
```

> Javadoc

```bash
% ./bin/wasabi.sh resource:doc
```

> Wasabi UI

```bash
% ./bin/wasabi.sh resource:ui
```

> Cassandra: cqlsh shell

```bash
% ./bin/wasabi.sh resource:cassandra
```

> MySQL: mysql shell

```bash
% ./bin/wasabi.sh resource:mysql
```

> Java Debugger: Remote attach configuration

```bash
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8180
```


## Stop Wasabi

```bash
% ./bin/wasabi.sh stop
```

Alas, all good things must come to an end. Let's clean things up a bit stop the newly created Wasabi stack:

At this point in time, we now have all the requisite tools installed, and subsequent invocations of Wasabi will
start up much more quickly.

<div></div>

## Get Familiar with wasabi.sh
```bash
% ./bin/wasabi.sh --help

  usage: wasabi.sh [options] [commands]

  options:
    -e | --endpoint [ host:port ]          : api endpoint; default: localhost:8080
    -v | --verify [ true | false ]         : verify installation configuration; default: false
    -s | --sleep [ sleep-time ]            : sleep/wait time in seconds; default: 30
    -h | --help                            : help message

  commands:
    bootstrap                              : install dependencies
    build                                  : build project
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
<div></div>


## Develop Wasabi

### Build and Run Wasabi Server

```bash
% mvn package
% ./bin/wasabi.sh start:cassandra,mysql
% (cd modules/main/target; \
    WASABI_CONFIGURATION="-DnodeHosts=localhost -Ddatabase.url.host=localhost" ./wasabi-main-*-SNAPSHOT-development/bin/run) &
% curl -i http://localhost:8080/api/v1/ping
...
```

<div></div>

> Viewing runtime logs:

```bash
% tail -f modules/main/target/wasabi-main-*-SNAPSHOT-development/logs/wasabi-main-*-SNAPSHOT-development.log
```

The runtime logs can be accessed executing the following command in a another shell:
<div></div>

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

### Stop Wasabi

```bash
% ./bin/wasabi.sh stop
```

<div></div>
> Wasabi runtime configuration:

```bash
-DnodeHosts=localhost -Ddatabase.url.host=localhost
```

Now while that was fun, in all likelihood you will be using an IDE to work on Wasabi. In doing so, you need only
add the configuration information above to the JVM commandline prior to startup:


### Run Integration Tests
```bash
% ./bin/wasabi.sh start test stop
```

Code changes can readily be verified by running the growing collection of included integration tests:
<div></div>

## Package and Deploy at Scale

```bash
% ./bin/wasabi.sh package
% find ./modules -type f \( -name "*.rpm" -or -name "*.deb" \)
```

Wasabi can readily be packaged as installable *rpm* or *deb* distributions and deployed at scale as follows:
<div></div>

Note: [Java 8](http://www.oracle.com/technetwork/java/javase/overview/index.html) is a runtime dependency

## Integrate Wasabi

```xml
<dependency>
    <groupId>com.intuit.wasabi</groupId>
    <artifactId>wasabi</artifactId>
    <version>1.0.20160627213750<build_timestamp></version>
</dependency>
```
Wasabi is readily embeddable via the following *maven* dependency GAV family:
<div></div>

## Contribute

We greatly encourage contributions! You can add new features, report and fix existing bugs, write docs and
tutorials, or any of the above. Feel free to open issues and/or send pull requests.

The `master` branch of this repository contains the latest stable release of Wasabi, while snapshots are published to the `develop` branch. In general, pull requests should be submitted against `develop` by forking this repo into your account, developing and testing your changes, and creating pull requests to request merges. See the [Contributing to a Project](https://guides.github.com/activities/contributing-to-open-source/)
article for more details about how to contribute.

Extension projects such as browser plugins, client integration libraries, and apps can be contributed under the `contrib` directory.

Steps to contribute:

1. Fork this repository into your account on Github
2. Clone *your forked repository* (not our original one) to your hard drive with `git clone https://github.com/YOURUSERNAME/wasabi.git`
3. Design and develop your changes
4. Add/update unit tests
5. Add/update integration tests
6. Add/update documentation on `gh-pages` branch
7. Create a pull request for review to request merge
8. Obtain 2 approval _squirrels_ before your changes can be merged

Thank you for you contribution!


## Recap

Ok, that was a ground to cover. Here is a high level overview of the operational elements described above, either directly or indirectly:

![OverView](getting_started/landscape.png)

And lastly, here is a technology stack diagram of the elements of which a Wasabi instance is comprised of:

![TechStack](getting_started/stack.png)
