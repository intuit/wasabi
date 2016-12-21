#Wasabi AB Testing Service - Traffic Management Plugins

##Overview

The Wasabi AB Testing Service administrative UI has a plugin mechanism that allows addition
of user interface to control or investigate the Wasabi metadata.  These two plugins are
examples of using that plugin mechanism, but they may also be useful additions to your
Wasabi installation.

The Traffic Estimator plugin helps with the job of correctly setting the sampling percentages for
experiments that are related to each other through mutual exclusion and that will,
generally, be used through the "batch assignment" API
(e.g., /api/v1/assignments/applications/CTG/pages/page1/users/userID1) .  When you use
mutual exclusion, experiments that are of a higher priority (see the Priority tab)
will take traffic away from experiments that they are mutually exclusive to that are
of a lower priority.  In order to compensate for this, if you want one of those lower
priority experiments to have a specific "target sampling percentage" of the total traffic,
you need to increase the "experiment sampling percentage" (the sampling percentage configured
in the UI for that lower priority experiment).  The Traffic Estimator helps you
make such compensative calculations and even to set the sampling percentages with one button click.

Similarly, the Traffic Analyzer plugin helps you monitor the actual percentages of
user traffic assigned to each experiment related by mutual exclusion.  In other words,
once you have used the Traffic Estimator to set the correct sampling percentages into your
experiments, and have let them run for a day or two, you can then use the Traffic Analyzer
to automatically retrieve daily assignment results for the related experiments and
confirm that you are getting the desired target sampling percentages.

##Installation

Simply copy the plugins directory (which contains 3 other directories) from this directory
into the modules/ui/app directory.  

Next, look at the file modules/ui/app/scripts/plugins.js .  If it only has the following
in it:

```
var wasabiUIPlugins = [];
```

Then you can simply replace it with the file scripts/plugins.js in this directory.

NOTE: If the file already contains some other plugin definitions, you will need to
take the definition objects from the scripts/plugins.js file in this directory and
add them to the list in the wasabiUIPlugins array in the modules/ui/app/scripts/plugins.js file.

Now, simply restart your Wasabi server, which also serves the UI.  
If you are using the wasabi.sh script, that
would consist of:

```
./bin/wasabi.sh stop:wasabi start:wasabi
```

(NOTE: If you are running the Wasabi UI locally, e.g., using "grunt serve", simply stop
that command and run it again.)

To confirm successful installation of the plugins, login to the UI and click on the
Plugins tab.  You should see descriptions of the two plugins and buttons to launch them.

##Usage

When you have successfully installed the plugins and restarted the Wasabi server, you
can login and click on the Plugins tab to use the plugins.  You will see descriptions
of the two plugins and a button labeled Run next to each.  To use a plugin, simply
click on the Run button and a dialog will appear with the UI for the plugin.

To use the Traffic Management plugin, after you have created and potentially started
your mutually exclusive experiments, click on the Run button.  You will see a dialog
with a menu with all the applications for which you have permission.  After you
select the application in which you are interested, the menu below it will be populated
with the names of the experiments in that application.  Select one of the mutually
exclusive experiments.  The table below the menus will be populated with all experiments
related to that experiment through mutual exclusion in priority order.

Note that this doesn't just include experiments that to which the one you selected is directly
mutually exclusive.  Rather, it will include all the experiments that are mutually exclusive
the one you selected, but also any other experiments that are mutually exclusive to those
experiments.

For example, suppose you have experiments named A1, A2, A3 and A4.  Suppose that
A1 is mutually exclusive to A2 and A3.  However, suppose that A2 is also mutually exclusive
to A4.  If you select any one of those four experiments, all four of them will appear in
the list of the plugin UI.  That is because changes to one of the higher priority
experiments may have an effect on the traffic sent to the lower priority experiments, even
when those experiments are not directly mutually exclusive to the higher priority experiment.

After you have selected an experiment and the related experiments are displayed, you will
see the currently assigned experiment sampling percentages in the right column.  In the
next column to the left, you see the "target sampling percentage".  This is the effective
sampling percentage calculated from the experiment sampling percentages and the mutual
exclusion relationships and priorities.

For example, suppose you have two experiments, B1 and B2, and suppose they are mutually
exclusive to each other and that B1 is a higher priority than B2.  
Now suppose that you want 9% of the traffic to go to B1 and 8%
of the traffic to go to B2.  That is, if the page assignment for those experiments is
attempted for 10000 people, you want 900 people to be in experiment B1 and 800 people to
be assigned to B2.  Since they are the only experiments that are in a mutual exclusion
relationship to each other (e.g, there are no other experiments mutually exclusive with
either of them), you simply set the sampling percentage of B1 to 9% to get it 9% of
the traffic.  However, if you set the sampling percentage of B2 to 8%, you will NOT
get 8% of the traffic.  That is because, since they are mutually exclusive, a user
can't be assigned to both experiments.  So all the users assigned to B1 will NOT be
in the running to be assigned to B2.  That means that if you set the sampling percentage
of B2 to 8%, it will get 8% of the remaining 91% of the traffic, or 7.28%.  If you want
B2 to get 8% of the traffic, you need to increase its sampling percentage to 8.79% (which is
91, the remaining traffic after B1, divided by 8, the desired target percentage).  

In order to get this calculation from the Traffic Management plugin, you would first
set up those two experiments, say with a sampling percentage of 9 for B1 and 8 for B2.  
Then you would run the Traffic Estimator.  In the Target
Sampling % column, you would make sure the values are set to 9 for B1 and 8 for B2,
because these are your target, desired percentages of the total traffic.  Now you click
on the Calculate Sampling Percentages button.  You will see that the suggested
sampling percentage for B2 is changed to 8.79 (and the original 8 is crossed out).
If you now click on the Save New Sampling Percentages button, the new sampling percentage of
8.79% will be saved for B2.  If you now run those experiments and make batch assignments to
them (make sure you have assigned them to the same page), then after a large number of
assignments, you should see about 9% going to B1 and about 8% going to B2.

And that brings us to the Traffic Analyzer.  Suppose you have now let B1 and B2,
from the example above, run for a couple days and your app has been making batch
assignments to them.  In order to check if you are actually getting the target sampling
percentage of all assignments that you want, you can use the Traffic Analyzer.
You run the Traffic Analyzer by clicking on its Run button on the Plugins page.
Similar to the Traffic Management plugin, you select the application in which you
are interested (it should actually already be selected, as the plugins remember
the last application you chose).  This will cause the next menu to be populated with
the names of the experiments in that application.  When you select the name of one of
the related experiments in which you are interested, the table below will be populated
with data for those related experiments.  You can change the range of dates and click
on the Refresh button, if necessary.  The table will have columns for each of the
related experiments.  The first row will have the Priority of the experiments.  The second row
will have the target sampling percentage of each experiment (Note: this is calculated
from the experiment sampling percentages and the mutual exclusion relationships).
The third row is the experiment sampling percentages of each experiment (that is, the
sampling percentage configured with each experiment).  The rest of the rows are one
row for each date in the date range specified.  These are the actual percentage of
the total assignments (including null assignments, that is, users who didn't get assigned
to any experiment) for each of the experiments.  You can compare these to the target
percentages to confirm that you are getting the actual percentages of traffic that you
want for each experiment.

##Plugins

While useful on their own, if you happen to work with mutually exclusive experiments,
these plugins are also good examples of how to add functionality to the Wasabi UI.
If you are familiar with JavaScript and Angular JS, you should be able to
reverse engineer the plugins enough to either modify them to create whatever you
need or as an example to use when creating a new one.

We plan to add another item to the contrib folder that is an example plugin that
will go into more detail on the plugin mechanism, in case these examples aren't
clear enough.
