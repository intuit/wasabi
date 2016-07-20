#EventLog
The EventLog is an event distribution system which runs in a separate thread and receives as well as 
distributes events. The EventLog will collect events and pass them on to their subscribed listeners through a 
ThreadPoolExecutor which creates `Runnable`s for each event-listener pair and runs them whenever possible.

##Post events
To post events you should use guice-injections to inject the EventLog into your module. Then you can just call 
`postEvent(EventLogEvent event)` on it. Have a look into the event package for existing events and feel free
to add new events. Also please tag them with tagging interfaces accordingly, so that existing subscribers do not miss
out on them!

##Subscribe
You can subscribe to certain kinds of events (usually by subscribing to their interfaces or even actual event
implementations) by just adding the EventLog as a guice-injected property to your module to get access to the EventLog,
and by writing an implementation of the `EventLogListener`. That listener has to `register()` itself at the eventLog.
For examples on how to do it please refer to the email module or the auditlog module, which make use of this. 

##Configuration
You can adjust these parameters in the main pom.xml:

```xml
<eventlog.class.name>com.intuit.wasabi.eventlog.impl</eventlog.class.name>
<eventlog.poolsize.core>2</eventlog.poolsize.core>
<eventlog.poolsize.max>4</eventlog.poolsize.max>
```

The poolsize gives the core and maximum number of threads for the ThreadPoolExecutor, while the class hints to the
EventLog implementation. This makes it easy to replace it with your own Implementation.

##Events
For a detailed list of events and their hierarchy refer to the README.md in the corresponding `events` package.
