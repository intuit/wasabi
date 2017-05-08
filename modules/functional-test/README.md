This file shall help you running these tests and making your own tests. Feel free to add your tests and short 
descriptions at the beginning of this file, so that others may know what each test set is used for.

The tests use [TestNG](http://testng.org/doc/) and are implemented using 
[rest-assured](https://github.com/jayway/rest-assured/wiki/Usage) and 
[Gson](https://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/index.html).

#Running Integration Test in Vagrant

Prerequisite: 
* cd [workspace]/wasabi. mvn clean install
* Install Oracle VM VirtualBox (version 5.1.18 or higher) and Vagrant (version 1.9.3 or higher).

Mac installation instructions with brew
```shell
# install vagrant
% brew cask install vagrant
# install virtualbox
% brew cask install virtualbox
% brew cask install vagrant-manager
# install vagrant's omnibus
% vagrant plugin install vagrant-omnibus
```

To run all integration tests defined in testng.xml, simply do the following.
* cd [workspace]/wasabi/modules/functional-test
* vagrant up (this step will install dependencies (jdk, Cassandra, MySQL), install Wasabi app, create Cassandra tables, start up Wasabi app, run integration tests, stops Wasabi app and generate Jacoco file for code coverage)
* Test output will be saved in [workspace]/wasabi/modules/functional-test/target/integration-test.out
* vagrant destroy (this will destroy the VM)

**NOTE:** jacoco-it.exec will be stored at [workspace]/wasabi/modules/functional-test/target/jacoco

#Checklist

This checklist will help you writing new tests, and also documents which tests are covered how or where. If you want to 
know more about the tests and how to write them, read on.

| Test                         | Class                           | testng.xml | testng_{TEST}.xml | run-{TEST}.sh       | pom.xml | README.md | done  |
|------------------------------|---------------------------------|------------|-------------------|---------------------|---------|-----------|-------|
| SmokeTest                    | service.SmokeTest               | yes        | smokeTest         | functional-tests    | yes     | yes       | done  |
| AuthTest                     | service.AuthTest                | yes        | authTest          | authtest            | yes     | yes       | done  |
| IntegrationTests\*           | service.IntegrationExperiment   | yes        | integration       | --                  | yes     | yes       | 05/21 |
| ForcedFailure                | library.ForcedFailuresTest      | no         | forcedfailure     | forcedfailuretest   | yes     | yes       | done  |
| PreparePerformanceTest       | library.SetupPerformanceTest    | no         | prepPerfTest      | perfsetup           | yes     | yes       | done  |
| Priorities                   | service.PrioritiesTest          | yes        | prioritiesTest    | --                  | yes     | yes       | done  |
| SegmentOnHttpHeader          | service.SegmentOnHttpHeaderTest | yes        | segHttpHeader     | --                  | yes     | yes       | done  |
| SegmentationRuleCacheFix     | service.SegementationRuleChacheFix | yes     | segRuleFix        | --                  | yes     | yes       | done  |
| Teardown                     | library.TearDownTestExperiments | yes        | teardown          | teardown            | yes     | yes       | done  |
| StateInconsistency           | service.StateInconsistencyTest  | yes        | repeatStateInconsistency | --           | yes     | yes       | done  |
| RetryTest                    | library.RetryTestClass          | no         | retryTestExample  | --                  | yes     | yes       | done  |

\* The integration tests are a collection of many tests, collected in one big suite file (`testng_integrationTests.xml`). 
Most of your tests should probably go there. However it might be of use to have them in their own XML-files to
run them individually when working on a specific test or feature.


#Test list

Each xml-File contains one test-suite. Feel free to add more, but please describe then in a single sentence in this
file. If needed you can add more detailed descriptions of each test in their corresponding XML-Files or
your Testclasses JavaDoc. That also means that if you want to have more information about a specific test, please
refer to those points.

##Service tests

These are tests supposed to be run when a new successful build was done. It is recommended to run them in the listed
order (or just run `testng.xml`, as it employs all of the following and the teardown), but they should be independent 
from each other.

* Test preparation
  1. `testng_initialTeardown.xml`: Just a copy of the teardown to trick TestNG into running it twice.
  2. `testng_smokeTest.xml`: Tests the basic functionality of the core components with (hopefully) correct inputs.
* Integration tests (`testng_integrationTests.xml`) (p
  1. `IntegrationExperiment.java`: Experiment integration tests: Creation, Deletion, State transitions.
  2. `IntegrationBucket.java`: Bucket integration tests.
  3. `IntegrationMutualExclusion.java`: Creates experiments and adds/removes mutual exclusions subsequently.
  4. `IntegrationAuthentication.java`: Checks the authentication resource, i.e. the login.
* Other tests  
  1. `testng_authTest.xml`: Tries to create experiments with valid and invalid user credentials.
  2. `testng_prioritiesTest.xml`: Tests if priorities are handled correctly.
* Bug fix tests
  1. `testng_segHttpHeader.xml`: Tests segmentation by HTTP headers.
  2. `testng_segRuleFix.xml`: Bugfix-Test - Segmentation rules were cached and thus not properly retrieved.
  3. `testng_repeatStateInconsistency.xml`: Bugfix-Test - Creates experiments and iterates over some legal state changes.
      Uses the `service.factory.RepeatStateInconsistencyTestFactory`.


##Library tests

These tests are considered part of the library and do not actually test much but prepare for other tests or can be
used to tear down tests.

* `testng_forcedfailure.xml`: A test to check how Jenkins reports failures and skips. You probably do not want to run
    this on a server where statistics matter.
* `testng_preparePerfTest.xml`: Prepares the performance tests, can create multiple (optionally mutual exclusive) 
    experiments with a number of buckets and a fixed order of priorities.
* `testng_teardown.xml`: Terminates and deletes all experiments associated with applications starting with specified
    prefixes. Useful to be run after all tests are completed.
* `testng_retryTestExample.xml`: A test to demonstrate test with retrials and timeouts.



#Running Tests

Since the integration tests are made with the [TestNG](http://testng.org/) test framework, it is possible to run them 
in several ways. For Jenkins you want to be able to run them from the command line, which will briefly be explained 
here.

While writing tests and to run them locally, you might want to integrate them as run configurations in IntelliJ (or 
whichever IDE you use). IntelliJ is covered below, for other IDEs please refer to their documentations or see if
TestNG's [documentation got you covered](http://testng.org/doc/documentation-main.html#running-testng).

##Command line

To run the tests from the command line you can use this command from the `modules/functional_test/target` folder:

```bash
java -Dapi.server.name=${HOST}:${PORT} -Duser.name=${USER} -Duser.password=${PWD} -classpath ${LOGXMLDIR}/:${JAR} org.testng.TestNG -d ${OUTPUTDIR} ${TESTNGXML}
```

Replace the variables `${var}` as follows:

* `HOST`: The default is `localhost`.
* `PORT`: The default is `8080`.
* `USER`: The user name, the default is `wasabi@example.com`.
* `PWD`: The user password, the default is `wasabi01`.
* `LOGXMLDIR`: The directory with the `logback.xml` file, e.g. `classes`.
* `JAR`: The jar file (with dependencies) produced by the maven build, e.g. 
    `wasabi-functional-test-20150721051958-SNAPSHOT-jar-with-dependencies.jar`. Remember to replace the version number.
* `OUTPUTDIR`: The output directory, e.g. `functional-test-results/functional-testng-out`.
* `TESTNGXML`: The xml to use, usually prefixed with `classes/`, e.g. `classes/testng.xml`.

An example call would then look like:

```bash
java -Dapi.server.name=localhost:8080 -Duser.name=wasabi@example.com -Duser.password=wasabi01 -classpath classes/:wasabi-functional-test-20150721051958-SNAPSHOT-jar-with-dependencies.jar org.testng.TestNG -d functional-test-results/functional-testng-out classes/testng.xml
```

For some tests you can add more `-Dsomething` switches, but you have to dive into the files to learn more.

##Command line alternative

The project already creates scripts to make it more convenient to run tests. To use them just run
them, they are located in the `target/scripts/` folder. They also sometimes allow several arguments.

An example (Creates 10 Experiments with 4 Buckets each, starts them and enables mutual exclusions):

```bash
sh target/scripts/run-perfsetup.sh -m true -e 10 -b 4
```

Note that most scripts are not well documented according to their parameters, so having a look inside them to 
figure out what they set is worth it!

##IntelliJ

###Default test configurations

Right-click on the `*.xml` you want to run and choose `Run...`.

###Custom test configurations

You can also create Run configurations yourself by going to `Run -> Edit Configurations...`.
Click on the little `+` on the top left of the run configurations and choose `TestNG`. Choose `Suite` and enter
the `*.xml` you want to use.

**Do not use the "Parameters" tab to pass parameters to the tests**, it does not work correctly and might leave you
without tests running at all. To add parameters use the field *VM options* (where `-ea` is already entered) and add 
them like this: `-Dexperiment.count=5`.

As a last step provide a value for `Use classpath of module`: usually this is the `wassabi-functional-test` module.

You can then just run all configurations as you are used to. 


#Writing Tests

##The Test framework

This section will get you started with writing integration tests. To add a test to the build folders you need to
add it to the `modules/functional_tests/pom.xml`, in the section `build.resources` where the other XML files are:

```xml
<!-- ... -->
<resource>
    <directory>${basedir}</directory>
    <targetPath>${basedir}/target/classes</targetPath>
    <includes>
        <include>testng.xml</include>
        <include>testng_preparePerfTest.xml</include>
        <include>NEW TEST HERE</include>
<!-- ... -->
```

**Note that one very important limitation is that because of some limitations in the logging framework it is not possible
to run parallel tests, as some reused objects can make problems.**

###tests.library-Package

The library package contains useful helpers to make setting up a test fast and easy.
The most important class here is the `TestBase`. It has lots of convenience methods for most API endpoints and offers a 
default `APIServerConnector` which is created using the properties from `resources/config.properties`. Of course you can
overwrite the connector with your own and even supply one for custom API requests. The `TestBase` also pings the server 
and asserts everything (Wasabi, Cassandra, MySQL) is up before any further tests are run.

The `TestBase` should be the parent of each of your tests, unless it makes no sense to use it. It also includes
the `RetryListener`, which is necessary for `RetryTest`-annotations to work.

A typical test class should look like this:

```java
package com.intuit.wasabi.tests.service;

import com.intuit.wasabi.tests.library.TestBase;

/**
 * A simple integration test.
 */
public class MyIntegrationTest extends TestBase {
    
    /**
     * Constructs the integration test.
     */
    public MyIntegrationTest() {
        // init
    }
    
    /**
     * Actions to be taken before the tests are started.
     */
    @BeforeClass
    public void beforeTestClass(){
        // setup
    }

    /**
     * Actions to be taken after the tests are completed.
     */
    @AfterClass
    public void afterTestClass(){
        // teardown
    }
    
    ///////////////////
    // Example tests //
    ///////////////////
    
    /**
     * The first test depends on assertPingAPIServer, which is the only test in the group "ping".
     */
    @Test(dependsOnGroups = { "ping" })
    public void t_firstTest() {
        // first test
    }
    
    // ...
}
```

Also make sure to check out `library.util.Constants` and `library.util.TestUtils` as they provide useful constants and
helper methods. Especially the `PREFIX` constants are a good choice to mark integration test items in the application to
allow for easy cleanup with `TearDownTestExperiments` (via `testng_teardown.xml`). The `TestUtils` provide methods
for creating proper date strings and parsing csv-responses.

Additionally the library package contains the definitions of the `@RetryTest` annotation and the classes needed to 
allow for test retrials (`RetryAnalyzer` and `RetryListener`).

The last part inside the library package are tests which are not really meant to be run after each build but to 
support other tests or for other specific purposes. One example is the `JenkinsReportWithForcedFailuresTest` which
does exactly that: It forces failures and reports them. This test is just there to demonstrate how Jenkins reports 
failures and of no use for the actual application.

More useful is the `TearDownTestExperiments`, which should be used after all integration tests are finished. It cleans
up and removes all experiments. However since it is only guaranteed on suite-level or by `dependsOnMethods` when these
cleanups are performed and thus certain race conditions can apply, you should only use this carefully. It is better
to just add your cleanup experiments to the `TestBase().toCleanUp` experiment list, which will just clean up
those at the end of a test.

Please **note**: The first method *should* depend the group `ping`. If it depends on the method, then the ping test
will be executed twice - once as part of the super class `TestBase` and once as part of your test class. By allowing
the group dependency it will only be run once, as TestNG does not instantiate it again.

###testng.xml

This xml-File contains all tests which are to be run by Jenkins after a build. There are lots of other xml files
starting with `testng_`, those usually contain the tests individually plus the teardown. 
This is supposed to make it easy to run single test files without providing arguments to TestNG - 
just choose the correct XML and you are set. For more information on how the `testng.xml` files should look like, 
please refer to the [documentation](http://testng.org/doc/documentation-main.html#testng-xml).

The `testng.xml` basically just contains links to other suite-files:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">

<suite name="IntegrationTests" parallel="false">
    <suite-files>
        <suite-file path="testng_smokeTest.xml" />
        <!-- ... -->
        <suite-file path="testng_teardown.xml" />
    </suite-files>
</suite>
```

Each of those other suite files can then configure its own tests, define its own listeners, add parameters etc.
Usually this boils down to just a single test class, as shown below, but the core integration tests are all
in the same suite file.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">

<suite name="SmokeTest">
    <test name="SmokeTest">
        <classes>
            <class name="com.intuit.wasabi.tests.service.SmokeTest" />
        </classes>
    </test>
</suite>
```

Note that most often you probably just want to add your test class to the `testng_integrationTests.xml`. In that case, 
however, make sure to clean up after the test is completed (via `TestBase().cleanupAfter(...)`), so that it does not 
interfere with other tests. To make sure a test has a clean working environment, `TestBase().cleanupBefore(...)` can
be called. For these purposes use the `@BeforeClass`/`@AfterClass` annotations.

Also take care of the order of the tests, as it might be important. Sometimes it is enough to just make sure
that the dependencies work correctly.

###ModelItems

In the `model` package you will find lots of very basic Java classes. These describe items you will receive (or can 
send) to the API using the methods provided by the `TestBase`. Internally 
[Gson](https://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/index.html) and 
[rest-assured](https://github.com/jayway/rest-assured/wiki/Usage) are used to handle the conversion between the model
items and the JSON strings returned or send to the API.

To allow for easy comparisons, each of those items inherits from `ModelItem`. This leads to a common implementation
of the methods `public int hashCode()` and `public boolean equals(Object other)`, the latter allowing for easy logging
to track differences. To assert the state of an item the `library/util/ModelAssert` class provides several 
`public static void assertModelItems(...)` methods which can compare single `ModelItem`s, lists of `ModelItem`s and even
maps.

###SerializationStrategies

For some comparisons you want to exclude certain fields (for example the experiments' `modificationDate`s), or compare
just two (a bucket name and its state). To do this, you can either set a static SerializationStrategy for a specific 
class or just pass a temporary one to the `assertModelItems(..., SerializationStrategy)` methods.

There is a flaw in the API for these strategies. Since the `set/getSerializationStrategy` methods are defined in the 
`ModelItem` super class but are supposed to set static members, it can be confusing that you need to call instance 
methods to set static fields. So beware that you can not set different serialization strategies for two instances of 
the same class! (This is especially important to remember when running multiple tests, thus it is recommended to 
just supply `SerializationStrategies` for each assert individually, or taking care of a clean state after a more complex
call.

The SerializationStrategy also applies for the JSON conversions done by `Gson`, that means whenever instances are
created from a JSON, or written to a JSON (to be transferred to the server). Some `put...` and `post...` methods
might even add or exclude certain fields automatically to avoid problems - in case you do not want this to happen
you might have to use the `doPost/doPut/doGet/doDelete` provided by the `TestBase` or those from the 
`APIServerConnector` to perform difficult requests.

There are two default implementations for the `SerializationStrategy`, the `DefaultNameExclusionStrategy` and the
`DefaultNameInclusionStrategy`. These ex- or include fields depending on their names. It is also possible to 
write more elaborate serialization strategies which exclude all boolean fields or all fields which have a specific 
value, etc.

###Adding parameters

Most methods in the `TestBase` allow it to rapidly add certain parameters on the fly, usually those needed for
requests. However, in case those are not enough it is possible to create your own request with the 
`TestBase` methods `doPost/doPut/doGet/doDelete` which take parameter maps for POST- and GET-parameters. If even the
status codes are uncertain, feel free to use the same methods directly with the `APIServerConnector`.

###Automatically retrying tests

If tests shall be retried multiple times on failures without reporting them if they eventually succeed, it is possible
to annotate them correctly. See the `RetryTestClass.java` for a detailed example implementation.

You basically need to add the `@Listeners({ RetryListener.class })` annotation to one of your test classes (the
`TestBase` already does that, so you only need this in case you do not inherit from it).
Note that the annotation makes the listener *globally* (at the suite level) available, thus all your tests classes, 
even those without the annotation, will use it. That is how annotations (and listeners) in TestNG work.
Alternatively it is possible to add listeners via the TestNG xml at suite-level:

```xml
<suite name="RetryTest">
    <listeners>
        <listener class-name="com.intuit.wasabi.tests.library.util.RetryListener" />
    </listeners>
    <!-- ... -->
</suite>
```

Please **note**: If you supply a listener twice, that means once with the `@Listeners`-annotation and once via the xml,
TestNG will create two instances of that listener. Since the TestBase already has one, think twice before you add
the RetryListener to your classes.

To complete a retriable test, annotate your test methods for retry tests with the `@RetryTest` annotation. 
Also add the `retryAnalyzer` attribute to the `@Test` annotation as follows:

```java
@Test(retryAnalyzer = RetryAnalyzer.class)
@RetryTest(maxTries = 3, warmup = 1000)
public void myTest() {
    Assert.assertTrue(false);
}
```

The values mean:

* `maxTries`: The total number of tries. That means if `maxTries` is 3, there will be one try and two retries.
* `warmup`: The time before a test (re-)try.

If you forget the `retryAnalyzer` or the `RetryListener`, the `@RetryTest` annotation will not work properly. 

Please **note**: Retrying tests does **not** work with tests which use a `@DataProvider`. Also IntelliJ will report
them incorrectly, as it does reporting on the fly. The final report will be correct though.

###Tests depending on other tests

Some tests depend on other tests' outcomes or that they were running. For example to start an experiment it is 
important that it can be created and that buckets can be added to them. Thus a test which changes the state of an
experiment from `DRAFT` to `RUNNING` should depend on those tests.

If the dependency is in the same test class it is usually enough to add the `dependsOnMethods` parameter to the `@Test`
annotation:

```java
@Test
public void t_someTest() {
    // ...
}

@Test(dependsOnMethods = {"t_someTest"})
public void t_firstTest() {
    Assert.assertTrue(true);
}
```

This will ensure that `t_firstTest` will only be executed if `t_someTest` was successful. If the tests should just be
executed in that order, independent from the result of `t_someTest`, you can define `t_firstTest` like this:

```java
@Test(dependsOnMethods = {"t_someTest"}, alwaysRun = true)
public void t_firstTest() {
    Assert.assertTrue(true);
}
```

If your dependencies are spread over several classes (including subclasses), you can place them inside groups
and set group dependencies.
Back to the example from above, let's assume that that there are tests `createExperiment`, `startExperiment` in one
test class, and `createBuckets` in another class. The `dependsOnMethods` parameter will now work to cross-reference
`createBuckets` in the first class. But you can add groups:

```java
// ExperimentTests.java
@Test(groups = {"basicExperimentTests"})
public void createExperiment() {
    // ...
}

@Test(dependsOnGroups = {"basicBucketTests"})
public void startExperiment() {
    // ...
}

// BucketTests.java
@Test(dependsOnGroups = {"basicExperimentTests"}, groups = "basicBucketTests")
public void createBuckets() {
    // ...
}
```

Note that you have to care of circular dependencies and that you spell the groups correctly. TestNG will otherwise fail.
If you set up everything correctly TestNG will figure out an order of the tests and run them accordingly.

##Examples

The best examples are probably the `RetryTestClass` in the `library` package and the `SmokeTest` in the `service` 
package. Also you should have a look at their corresponding xml files, `testng_retryTestExample.xml` and 
`testng_smokeTest.xml`. 

For a complete test you should try to stick to the following pattern:

###`testng_myTest.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">

<suite name="myTestSuite">
    <!-- If you want to allow for test retries, supply the following listener: -->
    <listeners>
        <listener class-name="com.intuit.wasabi.tests.library.util.RetryListener" />
    </listeners>
    
    <!-- If you have parameters for the test, supply them like this: -->
    <parameter name="myParameter" value="someParamValue" />
    
    <!-- This is the real test -->
    <test name="myTest">
        <classes>
            <class name="com.intuit.wasabi.tests.service.MyTest" />
        </classes>
    </test>
</suite>
```

###`MyTest.java`

```java
package com.intuit.wasabi.tests.service;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import com.intuit.wasabi.tests.library.TestBase;
import com.intuit.wasabi.tests.library.util.RetryAnalyzer;

/**
 * MyTest is a small demo test.
 */
@Listeners({ RetryListener.class }) // if want RetryTest but the listener in the class but the xml
public class MyTest extends TestBase {
    
    /**
     * Passes always, waits for assertPingAPIServer to complete (which is the only test in the group "ping")
     */
    @Test(dependsOnGroups = { "ping" })
    public void firstTest() {
        Assert.assertTrue(true);
    }
    
    /**
     * Tests if the supplied parameter is "someParamValue".
     *
     * @param myParameter the parameter to test
     */
    @Test(dependsOnMethods = { "firstTest" })
	@Parameters({ "myParameter" })
    public void secondTest(@Optional String myParameter) {
        Assert.assertEquals(myParameter, "someParamValue", "Parameter not as expected.");
    }
    
    /**
     * Retries and has warmup and timeout.
     * Fails always.
     * 
     * This is the test which forces you to include the listener in the xml file.
     */
    @Test(dependsOnMethods = { "assertPingAPIServer" }, retryAnalyzer = RetryAnalyzer.class)
    @RetryTest(warmup = 300, maxTries = 2, timeout = 500)
    public void retryTest() {
        Assert.assertTrue(false);
    }
}
```

Of course you can play around with these as much as you like. 


## Extensions

### New fields

It is possible that model items get new fields, or some fields are removed. In those cases you should be able to
simply add those fields to the specific classes - as long as the classes inherit from ModelItem their `equals`, 
`hashCode`, `toJSONString` and `toString` methods should still work, as those use reflections.

However, adding or removing very volatile fields (as for example the `Experiment().modificationTime`) can result
in broken tests, as the serialization strategies might no longer work correctly. In that case you can either update
all effected tests or implement your own inheriting classes.

It is planned though that eventually serialization strategies are refactored and common strategies (as
`new DefaultNameExclusionStrategy("id", "creationTime", "modificationTime", "ruleJson")` for experiments) are accessible
through a Factory.

### New ModelItems

If new model items are added they should implement a few important methods.

A small checklist:

* [ ] `extends ModelItem`
* [ ] contains `private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();`
* [ ] implements `set/getSerializationStrategy`
* [ ] has sufficient public fields

Additionally it is a good idea to allow for a constructor which takes an instance of the new model item as parameter
and copies all attributes to the newly created instance. Also methods which set an attribute and return `this` are
nice to allow for chaining. Lastly most ModelItems benefit from a matching factory (inside the `model.factory` package)
to create simple default items with meaningful default values for all required fields.

*Note: It is usually not recommended to set default values for the public fields. But for some data types such 
as `double`, `boolean`, `int` and other primitives Java sets default values. For complex objects like `String` and
custom objects this is not the case, those default to null - and thus are ignored automatically for all serializations.*

A basic template is provided here:

```java
package com.intuit.wasabi.tests.model;

import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;

public class NewItem extends ModelItem {
    /** The serialization strategy for comparisons and JSON serialization. */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();

    // PUBLIC FIELDS (the fields present in the json, names have to be exactly as in the json)
    public String some_field;
    
    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        User.serializationStrategy = serializationStrategy;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return User.serializationStrategy;
    }

}
```


#TODO List/Possible feature additions
        
*These should be done at one point in the future.*

* [x] Finish JavaDoc in TestBase.
* [x] Finish this file.
* [ ] More test coverage.
* [ ] Refactor serialization strategies.
* [ ] Analytics ModelItems.

*Most of these are optional or of low importance.*

* [ ] analytics package has no factories or convenience methods.
* [ ] BucketStatistics.bucketComparisons is just an Object at the moment, could be modeled better.
