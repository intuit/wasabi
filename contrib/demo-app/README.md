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
experiment name referenced.  As you can see, the default code expects an experiment named "TestBackground" in
an application named "PixLike" to be defined in the Wasabi server that you are hitting.  There is a script
named setup.sh that can be run to create and start that experiment.  Before running that script, if necessary,
edit it to make sure it points at the Wasabi server you are using (e.g., it might be localhost:8080 or
something else) and make sure you put in your login name.

Then just run it like this:

```shell
./setup.sh
npm install && npm start
```

In a different terminal window, if you plan on changing the Demo app, you can also run:

```shell
npm run webpack
```

That will run webpack so that it watches the source and processes any changed files.  You don't need to do this if you
just want to run the Demo app.

##Running the Demo

In case you have stopped the server, you can start it again as follows:

```shell
% cd contrib/demo-app
% npm start
```

That will start a node server that will both serve the UI files and act as a REST API server.  
Point your browser to localhost:3000 to bring up the UI.

##What is Demoed

You should initially see a screen that says "Please Login".  You may enter any user name and
click on Submit.  You will then be "logged in" to the demo application.

The interesting thing, however, is that when you were logged in, the app called Wasabi to test if you were in
the "TestBackground" experiment.  If you were not already assigned to the experiment, you will have been
assigned to one of the three buckets, randomly.  If you were assigned to the "ImageOne" bucket, you should see
a Cat background image in the UI.  If you were assigned to the "ImageTwo" bucket, you should see
a Dog background image in the UI.  If you were assigned to the "ImageThree" bucket (or if the experiment wasn't running
and so you weren't actually assigned to the experiment), you would see the default Fish background image.

When you were shown the screen with the buttons, the app also makes a call to Wasabi to record an "impression".
Then, if you click on the "like it" button (label "Great Pet!"), the app will make a call to Wasabi to record an action.  This is
considered to be the "success activity" and so the more users who click on this button, the higher the Action Rate
for that bucket.

You can login to the app (logging out first by clicking on the Logout link) as several different users, e.g.,
"user1", "user2", etc.  Over the course of several logins,
you should see the different background images.  If you then click on the "Great Pet!" button,
you should see the Action Rates diverging.
