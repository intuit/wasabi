'use strict';

angular.module('wasabi.services').factory('ConfigFactory', ['apiHostBaseUrlValue', 'authnType', 'noAuthRedirect', 'ssoLogoutRedirect', 'apiAuthInfo', 'downloadBaseUrlValue',
function(apiHostBaseUrlValue, authnType, noAuthRedirect, ssoLogoutRedirect, apiAuthInfo, downloadBaseUrlValue) {
    return {
        baseUrl: function() {
            if (apiHostBaseUrlValue === 'DEFAULT') {
                // We want to pull the URL to use as the backend URL from where the UI was served.
                return window.location.protocol + '//' + window.location.host + '/api/v1';
            }
            return apiHostBaseUrlValue;
        },

        downloadBaseUrl: function() {
            if (downloadBaseUrlValue === 'DEFAULT') {
                // We want to pull the URL to use as the backend URL from where the UI was served.
                return window.location.protocol + '//' + window.location.host + '/api/v1';
            }
            return downloadBaseUrlValue;
        },

        authnType: function() {
            return authnType;
        },

        noAuthRedirect: function() {
            return noAuthRedirect;
        },

        ssoLogoutRedirect: function() {
            return ssoLogoutRedirect;
        },

        apiAuthInfo: function() {
            return apiAuthInfo;
        },

        'loginTimeoutWarningTime': 55 /* minutes */ * 60 /* seconds/minute */ * 1000 /* milliseconds/ second */,
        'loginTimeoutTime': 60 /* minutes */ * 60 /* seconds/minute */ * 1000 /* milliseconds/ second */,
        //'loginTimeoutWarningTime': 10 /* seconds/minute */ * 1000 /* milliseconds/ second */,
        //'loginTimeoutTime': 30 /* seconds/minute */ * 1000 /* milliseconds/ second */

        'labelStrings': {
            'switchToTextView': 'Switch to text view...',
            'switchToFormView': 'Switch to form view...',
            'rememberSegmentationViewChoice': 'Always show the text view'
        },

        'newApplicationNamePrompt': 'New application...',

        // HELP
        'help': {
            'closeBucket': '<h1>What is Close Bucket?</h1><div>When you close a bucket, we set the bucket allocation percentage of the bucket to ZERO and redistribute the allocation percentage of that bucket proportionally among the other remaining buckets. By doing this, we will make sure no new users are put into this bucket. We still retain existing users that are part of this bucket and return assignments for existing users.</div>',
            'emptyBucket': '<h1>What is Empty Bucket?</h1><div>When you empty a bucket, we set the bucket allocation percentage of the bucket to ZERO and redistribute the allocation percentage of that bucket proportionally among the other remaining buckets. By doing this, we will make sure no new users are put into this bucket. We also make sure all users that are already assigned to this bucket are cleared.  They will be given new assignments the next time they come in.</div>',
            'controlBucket': '<h1>What is a control group?</h1><div>A control group is the bucket that you want your other buckets compared to. Typically, this is your existing experience. If you don\'t have an existing experience, just pick one as a control group. If you don\'t pick any bucket as a control group, we will pick the first bucket you add as the control group.</div>',
            'bucketPayload': '<h1>What is a bucket payload?</h1><div>A bucket payload is a piece of text that is returned to the caller with every assignment made to this bucket. It could contain CSS, Javascript code, a URL so you can alter the look and feel for your experiment or redirect the user to a different URL depending on the assignment. This is just a pass through. Whatever information you put in this field will be returned to the caller each time an assignment is made to this bucket.</div>',
            'trophy': '<h1>What does the trophy mean?</h1><div>Trophy next to a bucket means that the bucket is the A/B Test winner.</div>',
            'totalImpressions': 'Count of the number of times users were shown a treatment. Logged via the Events API as impressions.',
            'totalActions': 'Count of the number of times users took a particular action that you are recording, e.g. clicking on a button, making a purchase, etc. Logged via the Events API as actions.',
            'assignedUsers': 'The total number of users who have been assigned to this experiment.',
            'uniqueImpressions': 'Count of the number of unique users who were presented a particular treatment. This number is the denominator for calculating action rates and may be lower than Impression Counts if some users saw the treatment more than once.',
            'uniqueActions': 'Count of the number of unique users who were took a particular action. This number is the numerator for calculating action rates. This number may be lower than Action Counts if some users took the same action more than once.',
            'samplingPercentage': 'The percentage of all users who visit your application that will be assigned to this experiment.',
            'bucketActions': '<h1>What are actions?</h1><div>Of all the users who see an impression of a particular user experience, some subset of those may take one or more actions that you are interested in logging and measuring as metrics for the success of that particular treatment. Each different kind of action gives rise to an action rate (roughly actions divided by impressions, usually restricting to unique users). Measuring these action rates and determining when they differ in a statistically significant way is the object of AB testing.</div>',
            'bucketImpressions': '<h1>What are impressions?</h1><div>Once a user is assigned to a bucket and has been shown the first part of the corresponding user experience or treatment, you record an impression for that user. They become part of the denominator for calculating statistics. This is logically separate from assignment, because in some situations you may assign a user to a bucket before they get to the part of your product where they see the impression of the user experience.</div>',
            'bucketActionRate': '<h1>What is the action rate?</h1><div>The count of the unique users with an action divided by the count of unique users with an impression. E.g. this is usually a click-through rate. If you have defined multiple different action types, the default UI shows only the cumulative action rate (e.g. the numerator is the count of unique users with at least one action of any action type). The individual action rates for each action type are available via the APIs.</div>',
            'bucketImprovement': '<h1>What is improvement?</h1><div>Indicates the improvement (or not) with 95% confidence interval compared to the control bucket. If there is no control bucket specified, then comparisons are made vs. the first bucket in the list.</div>',
            'performanceGraph': '<h1>What is the performance graph?</h1><div>The graph shows the cumulative action rate for each bucket over time. This helps you visualize the relative performance of buckets.</div>',
            'experimentName': '<h1>Experiment Name</h1><div>Unique experiment identifier within the context of an application. Must be unique together with &quot;Application Name.&quot; No spaces allowed.</div>',
            'applicationName': '<h1>Application Name</h1><div>Globally unique application identifier. Must be unique together with &quot;Experiment Name&quot;. No spaces allowed.</div>',
            'samplingRate': '<h1>What is Sampling Percentage?</h1><div>This is the percentage of users for whom an assignment is requested who will be placed into one of the experiment buckets. In most experiments, the sampling % is relatively small, and only a fraction of the user traffic is put into one of the buckets or experiment variations. The rest of the users receive the default experience. Users who are not assigned to a bucket will be given the assignment value “NULL” from the AB Testing service assignment API.</div>',
            'buckets': '<h1>What are buckets?</h1><div>Users going through an experiment are randomly assigned to a bucket (bucket A, bucket B, etc.) which determines which user experience they will be offered. This is often called by other, equivalent names such as variation, treatment, or recipe. For example: Let\'s say you want to test how a &quot;Blue&quot; button performs compared to a &quot;Green&quot; button. &quot;Blue_button&quot; and &quot;Green_button&quot; will be your buckets.<br/><br/>' +
                    'When you first start your test, and for the first seven days, you will see an icon next to each bucket that indicates the results are inconclusive for that bucket.  That is because you must gather enough data before any predictions can be accurately made regarding which bucket is performing better.<br/><br/>' +
                    'However, after seven days, if you are recording impressions and actions in your experiment, you may see other icons next to the buckets.  If you see a trophy icon, that bucket is performing statistically better than other buckets.  If you see a red X icon next to a bucket, that bucket is performing statistically worse than other buckets.</div>',
            'mutualExclusion': '<h1>What is Mutual Exclusion?</h1><div>Mutually exclusive experiments mean that a user can be assigned to only one of the experiments that are mutually exclusive to each other. For example: If experiment A is mutually exclusive to experiment B, then a user can be part of either A or B but not both.  Setup Mutual Exclusion rules by clicking on the Mutual Exclusion tab. Here you can select the other experiments running in your application which are mutually exclusive. The AB Testing Service will ensure that the same user is not put into mutually exclusive experiments. Click the &quot;Add Experiment&quot; link to start picking experiments.</div>',
            'segmentationRule': '<h1>What is Segmentation?</h1><div>You can give some targeting rules for these experiments. You can choose which segment of users will see this experiment. If you want to target your experiment to a set of users that meet certain criteria, you can define the criteria here. For example, for my experiment I want to target users whose salary is greater than $80,000 and who live in California, my segmentation rule would be: salary > 80000 & state = &quot;CA&quot;.<br/><br/>Rules are constructed from rule segments, e.g., variable-name operator value .  These are represented in the UI by a set of controls that allow you to specify the variable name, the variable type (string, number or boolean), the relational operator (e.g., less than), and the value to compare to.  You may combine multiple rule segments with an "and" or an "or".<br/><br/>When you call the server from your application to assign a user to the test, you need to pass the values that will be used in the rule, for example, in my rule above, you would need to pass a value for "salary" and one for "state".<br/><br/>' +
                    'You can also choose to specify the rule as a string, which will be similar to the example above.<br/><br/>' +
                    'The valid types and operators are:<br/>' +
                    '<ul><li><span>String</span>' +
                    '<ul><li><span>Equals (=)</span></li>' +
                    '<li><span>Does Not Equal (!=)</span></li>' +
                    '<li><span>Equals, Same Case (^=)</span></li>' +
                    '<li><span>Matches Using Regular Expresion (=~)</span></li>' +
                    '<li><span>Does Not Match (!~)</span></li></ul></li>' +
                    '<li><span>Number</span>' +
                    '<ul><li><span>Equals (=)</span></li>' +
                    '<li><span>Does Not Equal (!=)</span></li>' +
                    '<li><span>Is Less Than (&lt;)</span></li>' +
                    '<li><span>Is Greater Than (&gt;)</span></li>' +
                    '<li><span>Is Less Than Or Equal To (&lt;=)</span></li>' +
                    '<li><span>Is Greater Than Or Equal To (&gt;=)</span></li></ul></li>' +
                    '<li><span>Boolean</span><ul><li><span>Equals (=)</span></li></ul></li></ul></div>',
            'pages': '<h1>Pages</h1><div>The pages feature enables you to make batch assignment calls. What this means is that you can request assignments for your users for more than one experiment at a time.<br/><br/><span style="font-weight: bold;">For example:</span> Let\'s say you have two experiments <span style="font-weight: bold;">exp1</span> and <span style="font-weight: bold;">exp2</span>. Now suppose they have the same Page mentioned on their Pages tab, say <span style="font-weight: bold;">page1</span>.  If you now make a batch assignment call mentioning page1, the system will attempt to assign the user to both exp1 and exp2.<br/><br/> ' +
                'In order to use the Pages feature with batch assignments:' +
                '<br/><br/><ol><li>On the Pages tab for each experiment you want to batch together, type a page name in the &quot;Add Page&quot; text box. If you already have a page with the same name in that application, you may select it from the list of pages that appears as you type.  If not, we will create it.</li> ' +
                '<li>If the &quot;Allow Assignment&quot; checkbox is checked (it is checked by default), that experiment will be included in the batch call.  If you don\'t want to include the experiment in the batch call, then uncheck the checkbox.</li>' +
                '<li>Now that you have grouped your experiments with Pages, you need to call the batch assignment API to request assignments.  The service will automatically find all the experiments on the page you mentioned in the API call and attempt to assign users to those experiments.</li>' +
                '<li>The batch API call is of the form:  /v1/assignments/applications<br/>/{applicationName}/pages/{pageName}<br/>/users/{userID}</li></ol>' +
                '<br/>Note: You may still explicitly request an assignment for an individual experiment even if it has a Page associated with it.  In order to do so, you use this API:<br/>/v1/assignments/applications<br/>/{applicationName}/experiments<br/>/{experimentLabel}/users/{userID}</div>',
            'status': '<h1>What do the statuses mean?</h1><br/><ul><li><span style="font-weight: bold;">Draft:</span> An experiment which has been created but is not yet running. An experiment collects no data in draft mode and cannot accept incoming assignment or event calls. An experiment in draft mode can still be edited (e.g. you can add more buckets, change bucket allocation percentages, etc.). Most experiment parameters cannot be edited outside of draft mode. The experiment remains in draft mode until it is manually started even if it is past the experiment start date.<br/>&nbsp;</li><li><span style="font-weight: bold;">Running:</span> An experiment moves from draft to running when you press the start button (as long as the current date is past the start date). A running experiment returns assignments when called, and collects event data (impressions and actions).<br/>&nbsp;</li><li><span style="font-weight: bold;">Stopped:</span> A running experiment can be stopped, during which time it continues to return existing assignments for UserIds that have already been assigned, but it does not create any new assignments. It continues to collect event data (impressions and actions) for UserIds that have already been assigned to a bucket. A stopped experiment may be set back to the running state.<br/>&nbsp;</li><li><span style="font-weight: bold;">Terminated:</span> A running or stopped experiment can be terminated either by explicitly pressing the Terminate button, or calling the API, or automatically terminated when the experiment End Date is reached. A terminated experiment cannot be re-started. It may only be archived.<br/>&nbsp;</li><li><span style="font-weight: bold;">Delete:</span> Terminated experiments may be deleted. We archive the experiment when you delete. Archived experiments can have their experiment name re-used. Archived experiments no longer appear in the experiment list UI.</li></ul>',
            'priority': '<h1>What is priority?</h1><div>In setting up an experiment, there are eligibility rules associated with each experiment.  Some experiments have rules which are fairly easy to satisfy (e.g. mobile users only), while others have much more stringent rules (e.g. mobile users between the age of 25-39 in states with no income tax and who have previously used snaptax).  Those experiments with more stringent rules will oftentimes have an issue with getting enough users in the experiments for it to reach the desired level of statistical confidence.  Priority is how we allow the user a way to manage their experiments such that these experiments with more stringent eligibility rules will still be able to acquire enough users to produce meaningful results.<br/><br/>Priority will be considered only during a batch assignment call (Pages API).<br/><br/>In the priority UI, you can change the priority in two ways:<br/>&nbsp;<ol><li>Just type in the number you want in the priority box and the list will re-arrange according to the new priority.<br/>&nbsp;</li><li>Manually drag the row you want to the desired priority location and the priority numbers will be updated automatically.</li></ol><br/>Priority rankings should be used in conjunction with mutual exclusion and Pages, this allows for efficient user assignment management.</div>',
            'rapidExperimentMaxUsers': '<h1>What is a Rapid Experiment?</h1><div>Usually, for an A/B test, you want to have users randomly assigned to buckets and run the test for a period of time to try to get statistically significant results.<br/><br/>However, sometimes you just want to try out a test on a few people.  Rather than picking a certain sampling percentage and trying to monitor the experiment until you have a sufficiently large number of assignments, you can make the experiment a "rapid experiment".  Specify the number of users you want in the experiment and when that number of assignments have been made, the experiment will Stop (meaning users will stop being assigned to the experiment, but users who have been assigned will still receive those assignments).<br/><br/>For a stopped experiment, you can increase the maximum number of users and restart the experiment.  New users will then start being assigned to the experiment, again, until you\'ve reached the newly entered number of users.<br/><br/>If the experiment is a rapid experiment, the target number of users is displayed.  If it is not a rapid experiment, "N/A" (not applicable) is displayed.</div>',
            'actionAnalysis': '<h1>What does the Actions table show?</h1><div>You can use impressions and actions to track the behavior of the users in your test.  Each time you show an experience to your users, related to a bucket, you can record an impression (see the API Calls tab for an example).  When a user performs an activity or reaches an end state that is significant you can record an action which defines "success".  This is also known as a conversion event.  The ratio of the number of users who see a given impression and then perform the associated action is the action rate, which is a measure of the success of that experience.<br/><br/>' +
                    'The simplest way to use actions and impressions are to have one impression and one action and to record them for both the test experience(s) and for the control experience.  If you do that, then the overall bucket action rate is a good measure of the success of a given experience.<br/><br/>' +
                    'However, if it is interesting to your test, you can record multiple actions for each bucket while the user is interacting with a given experience.  The event name that you pass to the action API call is completely defined by you.  If you record actions with the same names for each of your test and control buckets, then you perform a comparison of each of the actions for each of the test experiences as compared to your control experience.  This comparison is expressed as the improvement of the action rate for a given action and bucket versus the same action and the control bucket.<br/><br/>' +
                    'The performance of each action for each bucket, as compared to the same action for the control bucket, is shown in the table.  If the improvement of one of your buckets is statistically significant, as compared to the performance of the same action for the control bucket, the action rate and improvement for that action will be highlighted in green.  If the control bucket is performing better for that action, then the test buckets will be highlighted in red.<br/><br/>' +
                    'One thing to note about actions and this table are that actions only appear in the table once they have been recorded by a user.  That is, until your code has actually called the API to record a given action for a user in a given bucket, nothing will show in the row for that action and for that bucket.  And if that action has not been recorded for any bucket, there will not even be a row for that action.  Don\'t be surprised if some of your actions are missing from the table immediately after you start your experiment.</div>',
            'pageManagement': '<h1>What does the Page Management tool do?</h1><div>The "pages" feature of Wasabi allows you to assign a user to a group of experiments in one ' +
                    'call.  The Page Management tool allows you to easily determine which experiments are associated with a given page and to easily add or remove ' +
                    'experiments from a page.<br/><br/>A page does not exist without being associated with at least one experiment, so the first step toward managing a page ' +
                    ' is to create the page by associating it with the first experiment.<br/><br/>' +
                    'The next step is to select which application\'s pages you will be managing, since pages are managed within the context of an application. ' +
                    'So the next step is to select the application whose pages you want to manage. If you are authorized to administer more than one application within Wasabi, ' +
                    'you must select the application in which you are interested from a menu of all those applications.  If you are only authorized to administer one application, ' +
                    'you will see that application displayed at the top of the Page Management screen.</div>',
            'pageManagementSelectPage': '<h1>Select the page to manage</h1><div>Once you have selected the application in which you are interested, the Step 1 list will populate ' +
                    'with a list of the pages associated with any experiments within that application.  You can manage the experiments associated with that page by ' +
                    'selecting the page in the list.  This will populate the Step 2 list.</div>',
            'pageManagementManagePage': '<h1>Manage your page</h1><div>Once you have selected the page that you wish to manage in Step 1, the Step 2 list will populate with ' +
                    'the names of the experiments that are currently associated with that page.  These are the experiments that a user will be assigned to if you call the ' +
                    'pages API (sometimes called the "batch API") for that page.  You can add experiments to the page or remove them from the page:<br/><br/>' +
                    'In order to add an experiment to a page:<br/><br/> ' +
                    '<ol><li>Click on the "plus" button below the Step 2 list</li>' +
                    '<li>You will be presented with a dialog containing the list of experiments in this application that are not currently associated with this page</li>' +
                    '<li>Select the experiment(s) that you want to add to the page and click Add</li></ol><br/>' +
                    'In order to remove an experiment from a page:<br/><br/> ' +
                    '<ol><li>Click on the experiment(s) that you want to remove from the page</li>' +
                    '<li>Click on the "minus" button below the Step 2 list</li>' +
                    '<li>You will be prompted to confirm that you want to remove those experiments from the page</li>' +
                    '<li>Click on Remove Experiments to remove them from the page</li></ol>' +
                    '</div>'
        }
    };
}]);

