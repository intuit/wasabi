# Overview

The Wasabi AB Testing service is from Intuit Data Engineering and Analytics (IDEA)  based on a set of RESTful APIs that allow
you to instrument your product code for AB tests (also called split tests), and then create and manage experiments and
track and analyze results. We also provide a user interface for experiment management and tracking results. If you are
looking for a simple, flexible, and scalable way to run AB test experiments in your product, you're in the right place.


Wasabi enables you to:

* Create and manage experiments via API or through our web UI.
* Instrument your product code to run an AB test experiment via simple REST API calls:
* Assign users to a bucket (A, B. C, ...), then you show them the version of the experience you want them to see
(treatment).
* Log impressions when users have been exposed to the treatment.
* Log one or more kinds of actions the users may take in response to the treatment (your success metrics).
* View your experiment results to find the winning bucket and see statistical significance (via API or web UI).
* Immediately roll out your winning experience to all users until you can implement it in your product code.


### Where can I use AB testing? (almost anywhere)

* Web clients
* Mobile clients
* Desktop clients (with an internet connection)
* Server-side clients
* Can be called from client-side or server-side code
* Can run experiments across multiple platforms if you have a common user identifier


### What kinds of experiments can I run?

* Visual elements and content (e.g. red button vs. blue button)
* Multi-page work flows
* Dynamic single-page client experiences
* Back-end algorithms (e.g. compare recommendation algorithms)
and much, much more!


This document explains the features of the Wasabi AB Testing service and gives guidance on how to use them. Note that
everything that you can do within the User Interface (UI) is also available via the REST APIs.

## Overall Architecture

![Experiment Admin](overview/wasabiArchFlow.png)

## Core Concepts

### Bucket

Users going through an experiment are randomly assigned to a bucket (bucket A, bucket B, etc.) which determines which
user experience they will be offered. This is often called by other, equivalent names such as variation, treatment
group, or recipe.

### Assignment

The process by which a user who is a potential subject of the experiment is assigned to one of the buckets, or
determined not to be part of the experiment. We use a two roll system for assignment: “roll the dice” once and compare
to the Sampling % to determine if the user is in the experiment or not. If the user is in the experiment, then we roll
the dice again to determine which bucket the user is in, based on the bucket allocation percentages.

Assignments are durable – once a user (e.g. UserId) is assigned to a particular bucket for an experiment, the AB Testing
service will always return the same bucket assignment for that UserId, so the user will get the same experience in the
product.  Note that this is also true if the user is determined to not be in the experiment (sometimes called a "null
assignment"), that is, if you call the AB Testing service for that user's assignment, you will always be given the "null
assignment".

### Impression

Once a user is assigned to a bucket and has been shown the first part of the corresponding user experience or treatment,
you record an impression for that user. They become part of the denominator for calculating statistics. This is
logically separate from assignment, because in some situations you may assign a user to a bucket before they get to the
part of your product where they see the impression of the user experience.

### Action

Of all the users who see an impression of a particular user experience, some subset of those may take one or more
actions that you are interested in logging and measuring as metrics for the success of that particular treatment. Each
different kind of action gives rise to an action rate (roughly actions divided by impressions, usually restricting to
unique users). Measuring these action rates and determining when they differ in a statistically significant way is the
object of AB testing.



## Experiment Admin (Create,Edit, Start, Stop, Terminate)

The main administrative user interface of the AB Testing service shows a list of the current experiments, organized by
App and Experiment Name.

![Experiment Admin](overview/toplevel.png)



### Concepts and Definitions

#### App

Defines the application where the experiment runs. For example QBO, or TTO. This functions as a grouping for
experiments, and no two experiments within the same App can have the same Experiment Name.  This also defines the scope
of mutual exclusion between experiments.  Finally, access control to administration of the AB Testing service is
controlled at an App level.  For example, while all users with a Corp login can login to the AB Testing admin UI, only
users who have been granted access will be able to see or modify experiments in a given App, e.g., CTG.

#### Experiment

The name for the experiment, which must be unique within the same App, and should be descriptive. There is also an
Experiment UUID, which is not visible in the UI, but is used extensively in the APIs.

NOTE: an experiment name may be re-used after any experiments with that name have been archived.

#### Sampling %

This is the percentage of users for whom an assignment is requested who will be placed into one of the experiment
buckets. In most experiments, the sampling % is relatively small, and only a fraction of the user traffic is put into
one of the buckets or experiment variations. The rest of the users receive the default experience. Users who are not
assigned to a bucket will be given the assignment value “NULL” from the AB Testing service assignment API (they are
given the "null assignment").  Note that the assignment of "NULL" is actually "sticky", that is, if that user returns
to the app and you request their assignment into the experiment again, you will always get the "NULL" value for them.

#### Rapid Experiment Max Users

Usually, for an A/B test, you want to have users randomly assigned to buckets and run the test for a period of time to
try to get statistically significant results.

However, sometimes you just want to try out a test on a few people. Rather than picking a certain sampling percentage
and trying to monitor the experiment until you have a sufficiently large number of assignments, you can make the
experiment a "rapid experiment". Note that this is a very unusual situation, so normally, you will not use this feature.

If you do need to use this feature, however, specify the number of users you want in the experiment and when that number
of assignments have been made, the experiment will Stop (meaning users will stop being assigned to the experiment, but
users who have been assigned will still receive those assignments).

For a stopped experiment, you can increase the maximum number of users and restart the experiment. New users will then
start being assigned to the experiment, again, until you've reached the newly entered number of users.

#### Status

The current status of the experiment, as indicated by the icon.

#### Draft

An experiment which has been created but is not yet running. An experiment collects no data in draft mode and cannot
accept incoming assignment or event calls. An experiment in draft mode can still be edited (e.g. you can add more
buckets, change bucket allocation percentages, etc.). Most experiment parameters cannot be edited outside of draft mode.
The experiment remains in draft mode until it is manually started even if it is past the experiment start date.

#### Running

An experiment moves from draft to running when you press the start button (as long as the current date is past the start
date). A running experiment returns assignments when called, and collects event data (impressions and actions).  Note
that before you can start the experiment, the total allocation percentages for all the buckets must add up to exactly
100%.

#### Stopped

A running experiment can be stopped, during which time it continues to return existing assignments for UserIds that have
already been assigned, but it does not create any new assignments. It continues to collect event data (impressions and
actions) for UserIds that have already been assigned to a bucket. A paused experiment may be set back to the running
state.

#### Stopped (automatic)

A running experiment will be stopped automatically when the experiment End Date is reached. This is a TERMINAL STATE. A
stopped experiment cannot be re-started. It may only be archived.

#### Terminated

A stopped experiment can be terminated.  This causes the experiment to stop accepting new or returning existing
assignments.  This should only be done when you no longer have code that is using this experiment.  A terminated
experiment can only be deleted.

#### Deleted

Stopped or terminated experiments may be deleted. Deleted experiments can have their experiment name re-used. Deleted
experiments no longer appear in the experiment list UI.

#### Start Date

The date on which the experiment is allowed to start. Starting the experiment still requires pressing the Play button.
If you press Play before the start date, then the experiment will automatically start on that date. If you don’t press
Play until after the start date, then the experiment will start when you press Play.

#### End Date

The date on which the experiment is automatically stopped.  A running or paused experiment can be manually stopped by
pressing the Stop button at any time.

NOTE: In the future we plan to replace End Date with an optional parameter “Scheduled End Date” instead.

#### Actions

You can edit a Draft experiment by clicking on the name of the experiment.  There are also a set of action buttons which
allow you to change the state of the experiment. They consist of Start, Stop, Terminate and Delete.

### Creating an Experiment

Pressing the Create Experiment button brings up the Create Experiment dialog:


![Create Experiment](overview/createExperiment.png)


where you enter the Application Name, Experiment Name, Sampling Rate, Start Date, End Date, and a Description for your
experiment. Once you fill this in and press Create Experiment, the dialog expands and allows you to create buckets for
your new experiment.

![Edit Experiment](overview/editExperiment.png)

You can also go back and edit name, sampling rate, etc. This is the same dialog box as you see whenever you hit the Edit
button for an experiment in Draft mode. If you click on the +Add Bucket link, it brings up the Edit Bucket dialog where
you enter the bucket name, bucket allocation %, whether or not the bucket is the Control (e.g. the bucket to which other
buckets are compared), an optional description, and an optional bucket payload.

![Create Bucket](overview/createBucket.png)

A bucket payload is a piece of text that is returned to the caller with every assignment made to this bucket. It could
contain CSS, Javascript code, a URL so you can alter the look and feel for your experiment or redirect the user to a
different URL depending on the assignment. This is just a pass through. Whatever information you put in this field will
be returned to the caller each time an assignment is made to this bucket.

Note that the bucket allocation % for all buckets in your experiment must sum to 100%, and there cannot be more than one
Control bucket. You can edit or delete buckets by clicking on the appropriate buttons next to the buckets in the list.

![Buckets](overview/bucketsTab.png)



### Edit Experiment

Experiments can only be edited when they are in draft mode. Just click on the experiment name in the experiment list to
edit the experiment.

![Edit Experiment](overview/editExperiment2.png)



### Start Experiment

Clicking on the Play button starts the experiment if it is currently past the Start Date. If it is currently before the
Start Date, then it will set the experiment to automatically start on the Start Date. Once an experiment is started, you
can no longer access the Edit Experiment mode. Any parameters which can be changed are available via API or in the Track
& Manage Experiment UI.

NOTE: In the future we plan to replace Start Date with an optional parameter “Scheduled Start Date” instead.

### Stop Experiment

Pressing the Stop button stops the experiment, during which time it continues to return existing assignments for UserIds
that have already been assigned, but it does not create any new assignments. It continues to collect event data
(impressions and actions) for UserIds that have already been assigned to a bucket. A stopped experiment may be set back
to the running state.

### Terminate Experiment

Pressing the Terminate button terminates the experiment (this cannot be undone, and there is a dialog to warn you). A
running or stopped experiment can be terminated either by explicitly pressing the Terminate button, or calling the API,
or automatically stopped when the experiment End Date is reached. Terminated is a TERMINAL STATE. A terminated
experiment cannot be re-started. It may only be deleted.

### Delete Experiment

Pressing the Delete button deletes the experiment, which removes it from the UI. It can still be accessed via API by its
UUID. The experiment and its data are not actually deleted. Deleting an experiment is not reversible, and there is a
warning dialog in case you hit the button by accident.

NOTE: The current UI labels this as Delete Experiment. This may be renamed to Archive in an upcoming release.

## Experiment Tracking & Management

Once your experiment is running, you can access the tracking and management panel by clicking on the experiment name in
the list of experiments in the admin UI.

![Experiment Tracking](overview/detailsDialog.png)

### Concepts and Definitions
Please note that most of the terms in the tracking and management panel have Help descriptions available by clicking on
the question mark icon, .

#### Favorite Star

The yellow star can be clicked on to either make this experiment one of your "favorites" or not.  Your favorite
experiments are listed first on the Experiments list, making them easier to find.

#### Application and Experiment Name

The name of the application and experiment.

#### State

Whether this is a Running, Stopped or Terminated experiment.

#### Controls

Depending on the state of the experiment, you can use the controls to stop, re-start, terminate or delete this
experiment.

#### Status

Indicates whether or not there is a statistically significant winner among the buckets, which will be indicated by a
green checkmark in the left column next to the bucket name.

In the case that more than one bucket is in a statistical tie with one or more other buckets, but are statistically
each different from some other “losing” bucket(s), then the bucket(s) will be marked with the green checkmark to show
they are "winners".

#### Action Rate

The count of the unique users with an action divided by the count of unique users with an impression. E.g. this is
usually a click-through rate. If you have defined multiple different action types, the default UI shows only the
cumulative action rate (e.g. the numerator is the count of unique users with at least one action of any action type).
The individual action rates for each action type are available via the APIs.

#### Actions

If necessary, you can record more than one action.  The details of the action rates and counts for all the actions can
be found on the Actions tab.

#### Improvement

Indicates the improvement (or not) with 95% confidence interval compared to the control bucket. If there is no control
bucket specified, then comparisons are made vs. the first bucket in the list.

#### Rapid Experiment Max Users

As described in the Concepts and Definitions section above, this is a seldom-used feature that allows you to set a
maximum number of users to assign to the experiment.  The experiment will use whatever other settings are configured
for the experiment to determine users to assign to the experiment, but once this configured maximum number of users
has been reached, the experiment will be transitioned to the Stopped state, that is, existing assignments will be
returned, but no new users will be assigned to the experiment.

Once that has happened, if you need to increase the number of users, edit this setting to increase the number and then
start the experiment, again.

### Adjust Sample Rate
Allows you to change your sample rate so you can roll out a winning experience without a code release.

![Adjust Sample Rate](overview/changeSampling.png)



### Edit Buckets
You can click on the Edit Buckets link to bring up a dialog to modify the buckets.

NOTE: Editing buckets while an experiment is running or stopped is not advisable. This will make the analytics
unreliable and we cannot guarantee a good AB Test result at the end of the experiment. These changes cannot be undone
and you should understand the risks before you proceed.

![Edit Buckets](overview/editBuckets.png)

Close Bucket

If you need to stop adding new users to a bucket, but want the existing users to continue receiving their assignments to
that bucket, you should close the bucket.  When you close a bucket, we set the bucket allocation percentage of the
bucket to ZERO and redistribute the allocation percentage of that bucket proportionally among the other remaining
buckets. By doing this, we will make sure no new users are put into this bucket. We still retain existing users that are
part of this bucket and return assignments for existing users.

Empty Bucket

Empty a bucket (for when there are major experience issues with a bucket – returns all users to the null/“not in
experiment” assignment).  When you empty a bucket, we set the bucket allocation percentage of the bucket to ZERO and
redistribute the allocation percentage of that bucket proportionally among the other remaining buckets. By doing this,
we will make sure no new users are put into this bucket. We also make sure all users that are already assigned to this
bucket are cleared. They will be given new assignments the next time they come in.

### Changing Segmentation, Personalization or Description
There is a set of UI widgets used to edit the Segmentation rule, Personalization settings and Description of a non-Draft
experiment (e.g., Running, Stopped, etc.).  These widgets allow you to change aspects of the experiment individually and
save the changes immediately.

For example, if you edit a Running experiment, you will see the Description field displayed like this:

![Description](overview/editDescription.png)

To edit the description, simply click on the pencil icon in the upper right.  That will make the text area editable.

![Description](overview/editDescription2.png)

Once you have made your change, click on the green checkmark icon.  That will immediately save the new description.

If, instead, you decide that you want to cancel your changes, click on the red X icon.  Your changes will be discarded
and the previous value will be displayed.

This technique of putting part of the UI into editing mode and allowing the change to be saved or cancelled immediately
is also available for the Segmentation and Personalization tabs.

### Export Data (Download Assignment Data)
Allows you to export your raw data as a CSV file where each row is an impression or action event for a user.

NOTE: The file downloaded is currently actually a tab-delimited file, not a comma-separated values file.  You can import
the tab-delimited file into Excel by specifying the delimiter.

### Event Payloads
You can include Event payloads in your impression and action event calls. An Event payload is a small JSON payload with
your impressions or actions to track other details like query strings, prices paid, etc.

### Assignment Override
Allows you to specify the bucket assignment instead of having us provide it – useful for testing and if you have a
different system doing the bucket allocation, e.g. an existing AB testing implementation which you want to shadow with
our AB Testing service before migrating.
