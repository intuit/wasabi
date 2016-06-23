#Event Hierarchy
There are different types of event interfaces. Some are of functional nature, some are purely for tagging purposes.
This file is just to describe the hierarchy between those events and what their benefits are. Also it gives a quick
overview about already implemented Events and what they should be used for.

##Event Interface Hierarchy
Indentation denotes inheritance.

* `EventLogEvent`: The base interface for all events
    * `ChangeEvent`: Events which indicate item attribute changes.
    * `CreateEvent`: Events which indicate item creations.
    * `ApplicationEvent`: Events which effect applications.
        * `ExperimentEvent`: Events which effect experiments.
            * `BucketEvent`: Events which effect buckets.

##Event Implementation Hierarchy
Indentation denotes inheritance.

* `AbstractEvent`: An event without a description which just handles the time and user.
    * `AbstractChangeEvent`: An event without a description which additionally handles property changes.
        * `BucketChangeEvent implements BucketEvent`: An event denoting bucket changes.
        * `ExperimentChangeEvent implements ExperimentEvent`: An event denoting experiment changes. 
    * `BucketCreateEvent implements BucketEvent, CreateEvent`: An event denoting bucket creations.
    * `ExperimentCreateEvent implements ExperimentEvent, CreateEvent`: An event denoting experiment creations.
    * `SimpleEvent`: An arbitrary event. Allows for events with just a time, description and user.
