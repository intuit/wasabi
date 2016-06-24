# Integrating AB Testing in your product

With our service, integrating AB testing in your product is as simple as an API call. Really. It's that simple. Let's
look at the detailed steps involved.

## Step - 1

Create Experiment in the UI. Click on the "Create Experiment" button.

![Step 1](integration/step1.png)

## Step - 2

Enter the Experiment name (for example: tto_getdata_autoimport_test), select the Application name (for example: CTG,
SBG, CG, etc..), enter the Sampling percentage (What % of users do you want to send to this experiment?  This ranges
from 0.01% to 100%. 100% means all traffic will be sent to this experiment), Start date (when do you want this
experiment to start collecting data and assign users to buckets) and end date (When do you want this experiment to stop
collecting data and assigning users to buckets) and a brief description that describes what the experiment is about.
Note that you will probably not want to enter a value for the Rapid Experiment Max Users as that is a feature only a few
teams use. Read the help about the feature by clicking on the question mark icon if you are interested.

![Step 2](integration/step2.png)

## Step - 3

Click on the "Create Experiment" button.

## Step - 4

Once you finish step 3, you will now start creating buckets (recipes) for your experiment. Ideally, for AB Testing there
is a control bucket and other buckets that serve as alternatives to control. We'll go ahead and define those now in the
UI. Click on "Add Bucket" link in the UI under the "Buckets" tab.

![Step 4](integration/step4.png)

## Step - 5a

Enter the Bucket name, Allocation percentage (this is the percentage of people from the sampling percentage that you
entered in step 2 of creating the experiment. For example: When I enter 50% here, it means that of all users entering
the experiment 50% will see this version) and a brief description of what the bucket is. Also, make sure you check the
"Control" check box if this is the control (Typically bucket A or recipe A. You can come up with whatever name you
like).

![Step 5a](integration/step5a.png)

## Step - 5b

Follow the steps in 5a to create another bucket. We now have 2 buckets.

![Step 5b](integration/step5b.png)

You are now ready to start running the experiment. However, there are some advanced features that we will go into. If
you don't need to know these advanced features, feel free to jump right into the code section to see how you can
integrate AB testing in your code.

## Step - 6

Setup Mutual Exclusion rules by clicking on the Mutual Exclusion tab. Here you can select all other experiments running
in your current app which are mutually exclusive. Wasabi will look at the selection and makes sure that the same user is
not put into mutually exclusive buckets. Click the "Add experiments" link to start picking experiments.

![Step 6](integration/step6.png)

Be sure to read the discussion of Experiment Priorities below, as those only apply when you have chosen to make
experiments mutually exclusive with each other.

## Step - 7

You can give some targeting rules for these experiments. You can choose which segment of users will see this experiment.
If you want to target your experiment to a set of users that qualify certain criteria, you can define the criteria here.
For example, for my experiment I want to target users whose salary is greater than $80,000 and who live in California,
my segmentation rule would be: salary > 80000 & state ="CA".

![Step 7](integration/step7.png)

By default, you should see the "form view" for entering and editing a rule.  This allows you to enter and edit
segmentation rules without having to know the expression syntax.  So, for example, to enter the rule mentioned above,
you would first enter "salary" in the "Parameter name" field, then select "number" from the drop down.  When you do
that, the selections in the next drop down will be valid choices for number type expressions ("equals", "does not
equal", "is greater than", "is less than", "is greater than or equal to", or "is less than or equal to").  Select "is
greater than".  Finally, the value to test against, 80000, is entered in the final field.

Since we need another rule, "state = CA", we click on "Add rule...".  This adds another row of widgets, including one
that allows you to select "and" or "or" to the left of the second row.  We want to "and" the rule segments, so we leave
the default choice of "and" for the first drop down menu.  We enter "state" for the "Parameter name", select "string"
for the type, select "equals" for the comparison operator, and then enter "CA" for the comparison value.  Note that you
actually need to enter the quotes for the "CA" (the placeholder text gives you a hint for each type of value).

Your segmentation rule tab should now look like this:

![Step 7](integration/segRuleEdit2.png)

After you save the dialog, you can edit the experiment again (by clicking on the name of the experiment in the
Experiments list).  If you then go back to the Segmentation tab, you will see the "Test rule..." link is enabled.  By
clicking on that, you will get a dialog that is created from the rule and allows you to enter values and then test
whether the rule will be "passed" or "failed" given those values.  Here is an example of that dialog:

![Step 7](integration/segRuleTest.png)

You can also look at the rule syntax in text form by clicking on the "Switch to text view..." link.

Note that the call to assign a user to an experiment that has a segmentation rule is different from the call to assign a
user to an experiment that does not have one.  That is because in order for the rule to be run successfully, you need to
pass values for the parameters to the rule in the assignment call.  The values you pass should be for the user you are
assigning.  For examples of the syntax used when passing the rule values, see the API Calls tab for the experiment.

## Step - 8

(Entirely Optional) You can add a "Page" to your experiment. This like a tag that you use to identify a group of
experiments. A "Page" can be shared with more than one experiment. This is particularly helpful when you want to allow
assignment calls for a group of experiments. Without pages you would call assignments on each experiment manually. But
with pages, you would call the page assignment API and all experiments that this page refers to will get the assignment
call. In the screenshot below, I gave a page name as "checkout_page". The reason for that is this experiment is running
on my checkout page. There are other experiments running on this page, too, so I will need to add this page name to
them.

![Step 8](integration/step8.png)

See the API Calls tab for examples of the API used when you are assigning a user to multiple experiments using "pages"
in one call.

## Step - 9

Save the experiment by clicking on the "Save" button.

## Step - 10

You will find the experiment listed on the homepage. As you will notice, this is still in a draft state. You have to
manually click the "Play" button next to it to start the experiment. Go ahead and hit Play.

![Step 10](integration/step10.png)

## Step - 11

The status changes to the green Play icon indicating that the experiment is running. But the experiment won't perform
assignments and collect data until the start date.

![Step 11](integration/step11.png)

# Advanced topics

## Experiment priority

In setting up an experiment, there are eligibility rules associated with each experiment.  Some experiments have rules
which are fairly easy to satisfy (e.g. mobile users only), while others have much more stringent rules (e.g. mobile
users between the age of 25-39 in states with no income tax and who have previously used TurboTax).  Those experiments
with more stringent rules will oftentimes have an issue with getting enough users in the experiments for it to reach the
desired level of statistical confidence.  We want to allow the user a way to manage their experiments such that these
experiments with more stringent eligibility rules will still be able to acquire enough users to produce meaningful
results.

In order to support this use case, the AB Testing service allows you to specify a priority order that will be used with
mutually exclusive experiments to determine which should be checked, first, when deciding to assign a user to the
experiments.

To set up your priority order, you use the Priority UI.  In the Priority UI, you can change the priority in two ways:

Just type in the number you want in the priority box and the list will re-arrange according to the new priority.
Manually drag the row you want to the desired priority location and the priority numbers will be updated automatically.
Priority rankings are only relevant when used in conjunction with mutual exclusion and with the pages feature.  That is
because you must be trying to assign a user to multiple mutually exclusive experiments before the priority of the
experiments relative to each other is relevant.  By using these features, you can achieve efficient user assignment
management.

priority.png

Notice the "Edit Sampling %" button.  If you want, you can click on that button and the sampling percentage for all the
experiments in the list will become editable.  This is an easy to adjust the percentages of a group of related
experiments.

Also notice that if you click on one of the experiments, the other experiments in the list that are mutually exclusive
with that experiment will be highlighted.

Now that we have created the experiment in the UI, the next step is to integrate this in your code. Let's take a look at
how this works. Below is a very simple example on how to integrate AB Testing in your code.



# Code Samples

JavaScript

```javascript
    //Configuration
    var server = "abtesting-demo-e2e.a.intuit.com";
    var appName = "Demo_App";
    var expLabel = "BuyButton";
    var buckets = [ "BucketA", "BucketB" ];
    var colors = [ 'DarkSeaGreen', 'Peru' ];

    // Experiment related Information
    var userID;
    var experimentID;
    var bucket;
    ...

    userID = escape($('#userIDext').val());

    var assignmentCall = $.ajax({
        url: "http://"+ server
            + "/api/v1/assignments/applications/"
            + appName
            + "/experiments/"
            + expLabel
            + "/users/"
            + userID,
        dataType: 'json',
        timeout: 300,
        success: function(data) {
            response = JSON.parse(data);
            bucket = data.assignment;
        }
    });
```

Here is a very simple implementation of AB testing in your code. This app displays either a "Green" or "Orange" buy
button depending on the user. I created two buckets in my experiment with a sampling rate of 50% each. Each time a user
logs in, the app makes an API call to AB testing and provides the application name, experiment label and user ID of the
current user in the app. A user ID could be anything that you internally use to uniquely identify a user in your system.
Sometimes, it could be a visitor ID for marketing pages.

In the above code snippet, as soon as the app gets the user ID, it makes a simple ajax call to the assignments API
provided by the AB Testing service. AB Testing service will evaluate the request and return the bucket the user belongs
to. It could be one of the buckets that you created during your experiment setup or "null" indicating that this
particular user does not qualify for the experiment (if the Sampling Percentage of the experiment is less than 100%).

Best Practice: As you noticed I set a timeout of 300 milliseconds on the ajax call. The reason for this is to make sure
your users get an uninterrupted experience. We make sure we maintain at least 45 ms response time for all of our APIs,
but that doesn't include network latency, etc. It is your responsibility to prepare for the unexpected and provide a
seamless user experience, so you might want to experiment to find the right timeout for your situation. You obviously
don't want your users to see a spinning mouse on their browsers for an hour, do you?

Now that we have our bucket assignment from AB Testing service, the application then decides what experience to show to
the user depending on what bucket the user falls into.

Logging impressions and actions:

In order for the AB Testing service to determine which bucket is the winner, it needs to know how many users actually
saw the experience and if the users took any actions on that experience. Impressions are similar to page views and
actions are button clicks, hovers, right clicks, checkouts, or whatever metric you want to measure.

The following are the API calls that you would call from your code just like how you called the assignments API to tell
the AB Testing service about impressions and actions. This information helps the service determine a winner.  These are
just example calls using the "curl" command, but you can translate them to whatever language your application is using.


### Log an impression
```shell
curl -v -H 'Content-type: application/json' -d '{"events":[{"name":"IMPRESSION"}]}' http://abtesting-demo-e2e.a.intuit.com/api/v1/events/applications/$appName/experiments/$experimentName/users/$userId
```

### Log an action
```shell
curl -H 'Content-type: application/json' -d '{"events":[{"name":$actionName}]}' http://abtesting-demo-e2e.a.intuit.com/api/v1/events/applications/$appName/experiments/$experimentName/users/$userId
```
