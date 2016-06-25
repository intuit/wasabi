# Wasabi - AB Testing Platform

**Documentation:** [User Guide](https://intuit.github.io/wasabi/v1/guide/index.html), [JavaDocs](https://intuit.github.io/wasabi/v1/javadocs/latest/index.html) <br/>
**Continuous Integration:** [![Build Status](https://api.travis-ci.org/intuit/wasabi.svg?branch=master)](https://travis-ci.org/intuit/wasabi)
[![Build Status](https://api.travis-ci.org/intuit/wasabi.svg?branch=develop)](https://travis-ci.org/intuit/wasabi)
[![Coverage Status](https://coveralls.io/repos/github/intuit/wasabi/badge.svg?branch=develop)](https://coveralls.io/github/intuit/wasabi?branch=develop)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.intuit.wasabi/wasabi/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.intuit.wasabi/wasabi) <br/>
**License:** [![Apache 2](http://img.shields.io/badge/license-Apache%202-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0)


## Project

Wasabi A/B Testing Service is a real-time, enterprise-grade, 100% API driven project. Users are empowered to own their own data, and run experiments across web, mobile, and desktop. It’s fast, easy to use, it’s chock full of features, and instrumentation is minimal.

Learn more about how Wasabi can empower your team to move from hunches to actionable, data-driven user insights with our simple, flexible, and scalable experimentation platform.


### Features

* **Own your own data** - Don’t ship your data to 3rd party services. Manage your own data and protect your users to maintain your competitive advantage.
* **Enterprise Grade** - Wasabi has been battle tested in production in fin-tech for 2 years in TurboTax, QuickBooks, Mint.com, etc., powering over 1,000 experiments across Intuit.
* **High Performance** - Consistent performance response times within 30ms server side.
* **100% API Driven** - Compatible with any language and environment.
* **Platform Agnostic** - A uniform, consistent experience across web, mobile, desktop, and also front-end, back-end.
* **Get up and running in minutes** - Spin up your Wasabi docker in 5 minutes and be in production with the platform, instrumentation, and experiments within a day.
* **It’s Free** - Don’t pay an arm and a leg for an enterprise-grade platform – the platform is free! 
* **Real-time assignments** - Assign users into experiments in real-time, to preserve traffic for other parallel A/B tests.
* **Cloud and on-premise** - Designed to live in the cloud or in your own data center.
* **Analytics** - Core analytics functionality and metrics visualization out of the box. 
* **Administrative UI** - Manage and setup your experiments using our user friendly interface.

## Getting Started

#### Requirements

The Wasabi server requires some tools to be available in order to run. The following steps will initialize an operational development environment.

For OSX, you can get started as follows:

```bash
% /usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
% brew install git
% git clone https://github.com/intuit/wasabi
% cd wasabi
% ./bin/wasabi.sh bootstrap
```

Installed tools include: [homebrew 0.9](http://brew.sh), [git 2](https://git-scm.com), [maven 3](https://maven.apache.org), [java 1.8](http://www.oracle.com/technetwork/java/javase/overview/index.html), [node 6](https://nodejs.org/en) and [python 2.7](https://www.python.org).

#### Start, Test and Stop

Note: Make sure your default JDK is set to 1.8

```bash
% ./bin/wasabi.sh -b true start
...
=== Wasabi successfully started. ===
Wasabi UI: % open http://192.168.99.100:8080
Default user: admin/admin

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

{"componentHealths":[{"componentName":"Experiments Cassandra","healthy":true},{"componentName":"MySql","healthy":true}],"wasabiVersion":"wasabi-api-20151215171929-SNAPSHOT-development"}
% ./bin/wasabi.sh stop
```

Note: The initial invocation of Wasbi can take up to 8m as the system is configured, built and provisioned. Second and subsequent invocations occur much faster, taking on average 1m20s.

#### Call Wasabi

These are the 3 common API's that you'd use to instrument your client application with Wasabi API's.

Let's assume that you've created and started a sample experiment 'buyButtonTest' in 'myApp' application with 'orangeButton'
and 'greenButton' buckets via the Admin UI or by calling the API's. 

You can assign a user with a unique ID (e.g. 'userID1') to an experiment by calling this API Request:
```bash
Assign a user to experiment and bucket:
% curl -H "Content-Type: application/json" http://192.168.99.100:8080/api/v1/assignments/applications/myApp/experiments/buyButtonTest/users/userID1
{"cache":true,"payload":null,"assignment":"orangeButton","context":"PROD","status":"NEW_ASSIGNMENT"}
```

Now the 'userID1' user is assigned into the 'orangeButton' bucket. Let's record an impression of their experience with this API Request:
```bash
Record an impression:
% curl -H "Content-Type: application/json" -d "{\"events\":[{\"name\":\"IMPRESSION\"}]}" http://192.168.99.100:8080/api/v1/events/applications/myApp/experiments/buyButtonTest/users/userID1
```

If the 'userID1' user does an action, such as clicking the buy button, you'd record it with this API Request: 
```bash
Record an action:
% curl -H "Content-Type: application/json" -d "{\"events\":[{\"name\":\"BuyClicked\"}]}" http://192.168.99.100:8080/api/v1/events/applications/myApp/experiments/buyButtonTest/users/userID1
```

## Development

Additionally one can opt to build and deploy Wasabi locally, using an IDE for example, and connect to the required services as follows:

#### Build and Run

```bash
% ./bin/wasabi.sh start:cassandra,mysql
% wasabi_ip=$(docker-machine ip wasabi)
% wasabi_configuration="-DnodeHosts=${wasabi_ip} -Ddatabase.url.host=${wasabi_ip}"
% (cd modules/main/target; \
    WASABI_CONFIGURATION=${wasabi_configuration} ./wasabi-main-*-SNAPSHOT-development/bin/run) &
% curl -i http://localhost:8080/api/v1/ping
# server log
% tail -f modules/main/target/wasabi-main-*-SNAPSHOT-development/logs/wasabi-main-*-SNAPSHOT-development.log &
% ./bin/wasabi.sh stop:cassandra,mysql
```

See *wasabi help* for more options:

```bash
% ./bin/wasabi.sh -h

usage: wasabi.sh [options] [commands]

...
```

#### Running Functional Tests

In addition to locally compiling the code, running unit tests and the like it is trivial to run the entire collection of included functional tests as follows:

```bash
% ./bin/wasabi.sh start test stop
```

## Package

Further, we can build and deploy *rpm* or *deb* distribution packages as follows:

Note: [Java 8](http://www.oracle.com/technetwork/java/javase/overview/index.html) is a runtime dependency

```bash
% ./bin/wasabi.sh package
% find ./modules -type f \( -name "*.rpm" -or -name "*.deb" \)
```

As noted above, configure these Wasabi instances to connect to the required services via the following JVM configurations:

```bash
% ... -DnodeHosts=${cassandra_ip(s)} -Ddatabase.url.host=${mysql_ip} -Ddatabase.url.port=${mysql_port} -Ddatabase.url.dbname=${mysql_dbname} -Ddatabase.url.args={mysql_args}"
```

## Integration / Maven

Lastly Wasabi is embeddable via the following *maven* dependency family:

```xml
<dependency>
    <groupId>com.intuit.data.autumn</groupId>
    <artifactId>autumn</artifactId>
    <version>1.0.20160515205816</version>
</dependency>
```

## Resources

* [docs](link)
* [issues](link)

## Contributing

All contributions are highly encouraged! You can add new features, report and fix existing bugs and write docs and
tutorials. Feel free to open issue or send pull request!

The `master` branch of this repository contains the latest stable release of Wasabi while and snapshots are published to
the `develop` branch. In general pull requests should be submitted against `develop` in the form of `feature` branch
pull requests. See the [Contributing to a Project](https://guides.github.com/activities/contributing-to-open-source/)
article for more details about how to contribute.

## Roadmap

* implement modules/main/src/test/integration/rollup_exp_stats.py
* tbd

## License

The code is published under the terms of the [Apache License v2.0](http://www.apache.org/licenses/LICENSE-2.0).
