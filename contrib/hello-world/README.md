#Wasabi AB Testing Service - Demo app

##Prerequisites
```shell
node
npm
```

##Configuration

If you are not running a copy of Wasabi locally on your system at localhost:8080, you will need
to search for "WASABI.setOptions" in the source (currently two locations) and then change the server
and possibly the protocol. You will also need to change the include of wasabi.js in the index.html. 

Also in that same set of code that calls WASABI.setOptions(), you will see a Wasabi application name and
experiment name referenced.  As you can see, the default code expects an experiment named "TestBuyButtonColor" in
an application named "MyStore" to be defined in the Wasabi server that you are hitting.  There is a script
named setup.sh that can be run to create and start that experiment.  Before running that script, be source
to edit it to make sure it points at the Wasabi server you are using (e.g., it might be localhost:8080,
localhost:8080, etc.) and make sure you put in your login name.

Then just run it like this:

```shell
./setup.sh
npm install
```

If you want, you can create the experiment in the Wasabi UI.  It is named "TestBuyButtonColor", is in the (new)
application "MyStore, and should have
3 buckets, "Control" (which should be marked as the control of the experiment), "GreenButton" and "BlueButton".  The
buckets should have Allocation Percentages that are balanced among the 3 buckets, e.g., 33.33%, 33.33% and 33.34%.  You
can make those Allocation Percentages easily using the "Balance bucket allocations..." link on the Edit experiment
dialog.
The experiment should also have 100% Sampling Percentage, an End Date far enough into the future that it won't expire while
you're using this app.  You should then Start the experiment.

If that experiment and application
don't exist, you will see an error on the JavaScript console, but the UI will happily continue and show you
the default button color.

##Running the Demo
```shell
% cd wasabi-hello-world
% node server
```

That will start a node server that will both serve the UI files and act as a REST API server.  
Point your browser to localhost:3000 to bring up the UI.

##What is Demoed

You should initially see a screen that says "Please Login".  You may enter any user name and password and
click on Submit.  You will then be "logged in" to the demo store application.

The interesting thing, however, is that when you were logged in, the app called Wasabi to test if you were in
the "TestBuyButtonColor" experiment.  If you were not already assigned to the experiment, you will have been
assigned to one of the three buckets, randomly.  If you were assigned to the "BlueButton" bucket, you should see
blue "Buy" buttons in the UI.  If you were assigned to the "GreenButton" bucket, you should see
green "Buy" buttons in the UI.  If you were assigned to the "Control" bucket (or if the experiment wasn't running
and so you weren't actually assigned to the experiment), you would see the default white button.

When you were shown the screen with the Buy buttons, the app also makes a call to Wasabi to record and "impression".
Then, if you click on one of the Buy buttons, the app will make a call to Wasabi to record an action.  This is
considered to be the "success activity" and so the more users who click on the Buy button, the higher the Action Rate
for that bucket.

You can login to the app as several different users, e.g., "user1", "user2", etc.  Over the course of several logins,
you should see blue, green and white Buy buttons.  If you then click on the Buy buttons, maybe only if they are one of the
colors, you should see the Action Rates diverging.

##What else can you do?

You can do things like using Segmentation Rules, Pages, Mutual Exclusion, or putting the experiment at different
places in the app.  You can also experiment with what happens when you stop the experiment and when you terminate it,
or when you empty or close a bucket.
