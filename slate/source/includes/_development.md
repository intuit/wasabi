# Developing Wasabi


## Extending Server-side Modules

Wasabi uses [Guice](https://github.com/google/guice) heavily for dependency injection. In order to extend the
functionality, you will have to implement the appropriate interfaces below, and optionally, extend the corresponding
module class to bind objects to your implementation.


### Authentication

- Interface: `com.intuit.idea.abntest.authentication.Authentication`

By implementing this interface, you can include your own authentication scheme. Be sure to update the
`<authentication.class.name>` property in the root pom.xml file with your appropriate class.

The default implementation for the Authentication interface is
`com.intuit.idea.abntest.authentication.impl.DefaultAuthentication`. It reads user credentials from the
`userDirectory.properties` file.


### Authorization

- Interface: `com.intuit.idea.abntest.authorization.Authorization`

Implement this interface to include your own authorization scheme. Be sure to update the `<authorization.class.name>`
property in the root pom.xml file with your appropriate class.

If you extended the `com.intuit.idea.abntest.authentication.AuthenticationModule` Guice module with your own
implementation, then you must extend the `com.intuit.idea.abntest.authorization.AuthorizationModule` Guice module as
well in order to install your appropriate authenticaton module.

By implementing this interface, you can include your own authorization scheme. Be sure to update the
`<authorization.class.name>` property in the root pom.xml file with your appropriate class.


### Real-Time Data Exportation


Wasabi allows for the exportation of data in real-time as events occur. Two types of data types can are exported:
assignment data, and impression/action data.


#### Assignment data

Whenever an assignment data object is created/modified, you can export that event in the following way:

<div></div>
* Create a class that implements the AssignmentIngestionExecutor interface, and will execute when an
AssignmentEnvelopePayload object is received:

```java
package com.mypackage;

import com.intuit.idea.abntest.assignment.AssignmentIngestionExecutor;
import com.intuit.idea.abntest.assignmentobjects.AssignmentEnvelopePayload;

/**
 * MyIngestionExecutor
 */
public class MyIngestionExecutor implements AssignmentIngestionExecutor {

    public static final String NAME = "MYINGESTOR";

    // Override the methods below appropriately

    @Override
    public void execute(AssignmentEnvelopePayload assignmentEnvelopePayload) {
    }

    @Override
    public int queueLength() {
        return 0;
    }

    @Override
    public String name() {
        return null;
    }
}
```

<div></div>
* Extend the AssignmentsModule class to create a thread pool and bind your IngestmentExecutor class:

```java
package com.mypackage;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static com.google.inject.name.Names.named;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by tislam1 on 6/21/16.
 */
public class MyAssignmentsModule extends AssignmentsModule {

    @Override
    protected void configure() {
        super.configure();

        // bind the threadpool executor to your ingestor's threadpool
        bindMyIngestionThreadPool();

        // add your IngestionExecutor's class to the mapBinder
        mapBinder.addBinding(MyIngestionExecutor.NAME).to(MyIngestionExecutor.class);
    }

    private void bindMyIngestionThreadPool() {
        // create an in-memory queue
        LinkedBlockingQueue<Runnable> myQueue = new LinkedBlockingQueue<>();
        // set your threadpool size
        int myThreadPoolSize = 5;
        ThreadPoolExecutor myThreadPoolExecutor = new ThreadPoolExecutor(myThreadPoolSize,
                myThreadPoolSize, 0L, MILLISECONDS, myQueue, new ThreadFactoryBuilder()
                .setNameFormat("MyIngestion-%d")
                .setDaemon(true)
                .build());
        bind(ThreadPoolExecutor.class).annotatedWith(named("my.assignmentThreadPoolExecutor")).toInstance(myThreadPoolExecutor);
    }
}
```

<div></div>
* Modify the EventsModule class's configure() method to install the MyAssignmentsModule class.




### Impressions/Actions data

- Interface:



## Extending the UI Plugin Mechanism

Overview
Code/Template Plugin Mechanism
Configuration Mechanism
Branding Mechanism
Appendix

### Overview

In order to create a version of the Wasabi UI that we could Open Source, we removed several features that we felt either didn’t make sense to be open sourced or couldn’t be.  However, when we create a version of Wasabi that is for Intuit use, we want to put those features back (or most of them).  In order to handle that in as general a manner as possible, we created several mechanisms that would have no effect on the UI unless activated by some configuration.  The generic open source version would then not have the features or branding, but by putting some configuration in place, the Intuit version would.  That also makes the plugin capability a feature that could be used by other adopters of our open source version.

This document describes the plugin mechanisms and the changes that would need to be made to put the Intuit features back in.

### Code/Template Plugin Mechanism

#### Motivation

One of the features we took out and wanted to put back in was a feature in the UI that allowed the user to configure the Personalization feature.  This is basically a reference to a service that Wasabi will turn around and call to make decisions about bucketing, that is, rather than using our usual rules of using the bucket allocation percentages to determine whether a user is in one bucket or another, the service can use whatever criteria it wants to make that decision.

The UI allowed the user to turn Personalization on or off (“make this a personalization experiment”) and if on, to configure the name of the model (the service) and a version number of the model.  These features in the UI were presented in one of the tabs on the Draft experiment dialog and on the Running experiment dialog (different implementations).

#### Implementation

In order to support putting that feature back in, we added a tab called Plugins in the Draft and Running dialogs.  We then added the ability to specify, through configuration, a template for the UI of each of the features to be added as well as the controller code for the feature.  When a feature is configured for the Draft dialog, for example, a button is created in the Plugins tab.  If the user clicks on that button, a dialog appears and shows the template, using the configured controller.

#### Use

```javascript
var wasabiUIPlugins = [
	{
    	"pluginType": "contributeDraftTab",
    	"displayName": "Personalization",
    	"ctrlName": "PersonalizationCtrl",
    	"ctrl": "plugins/personalization/ctrl.js",
    	"templateUrl": "plugins/personalization/template.html"
	},
	{
    	"pluginType": "contributeDetailsTab",
    	"displayName": "Personalization",
    	"ctrlName": "PersonalizationDetailsCtrl",
    	"ctrl": "plugins/personalization/detailsCtrl.js",
    	"templateUrl": "plugins/personalization/detailsTemplate.html"
	}
];
```

In order to contribute a plugin to one of the dialogs (Draft or Running), you need to add some configuration to the file modules/ui/app/scripts/plugins.js . The configuration looks like this:

<div></div>

This file exists in vanilla open source Wasabi, but it simply contains an empty array.

The configuration above defines two plugins.  The first contributes a plugin to the dialog for editing a Draft experiment.  The second contributes a plugin to the dialog for editing Running experiments.  In both cases, the Plugins tab will have a button with the label “Personalization” (the “displayName”).  The last piece of configuration defines the name of the controller created by the code referenced by the “ctrl” parameter.  Of course, in order for this configuration to work, the files must exist beneath the modules/ui/app/scripts/plugins directory.

Note that we have also used this mechanism to implement the plugging in of click tracking in the Wasabi UI itself.  We needed to remove that because we use our WebSDK to Trinity and that isn’t externally visible.  This consists of creating an object that has 3 methods, init(), reset() and trackEvent(eventName, parm1, parm2, parm3, parm4).  In conjunction with the WebSDK, we also added a plugin configuration that is mainly there just to load the WebSDK library.  Here is the configuration:

```javascript
	{
    	"pluginType": "contributeClickTrackingLib",
    	"ctrl": "https://dwum8argi892z.cloudfront.net/js/cdc_lib_min_latest.js"
	},
	{
    	"pluginType": "contributeClickTracking",
    	"displayName": "",
    	"ctrlName": "TrackingFactory",
    	"ctrl": "plugins/tracking/factory.js",
    	"templateUrl": ""
	}
```

By simply configuring a plugin of a type that isn’t used anywhere, but registering the WebSDK library as the “ctrl”, the file will be loaded when the plugins are loaded.  Then the “contributeClickTracking” will be loaded, which will create an object that is called by the generic code in UtilitiesFactory and app.js.

If you look at the code, notice that there needed to be some code to handle the fact that, while we start the loading of the WebSDK (the cdc_lib_min_latest.js file above) first, it may not have loaded by the time the TrackingFactory code is called, so we need to handle that with some setTimeouts.

See the appendix for an example of the code and templates.

#### Technical Details

In order to implement this, we needed to load the controller code dynamically from the configuration at the time the app is loaded into the browser.  This is done by the code that creates the Angular JS app for the UI in modules/ui/app/scripts/app.js .  The plugins.js file is referenced by index.html and so that JavaScript object (an array of objects) will be loaded.  But then we iterate over the array and save the configuration in a global array (in $rootScope) and then use the UtilitiesFactory.loadExternalFile() method to cause the browser to pull the controller code in and load the object.

Now, when each of the dialogs is created, the $rootScope.plugins array is available to the controller and is used to create whatever buttons are necessary on the Plugins tab.  When the user clicks on the button, the openPluginModal() method is called.  That method displays a modal dialog using the configured template and controller code, hence presenting the plugin and allowing it to implement its own UI.

### Configuration Mechanism

#### Motivation

Several of the things we needed configurable didn’t need to be as complex as a plugin.  One of them was an error message that displayed our support email as part of the message.  Obviously, we didn’t want our support email in the open source version, but we wanted to be able to put it back for the Intuit version.

#### Implementation

Since this involved a simple, configurable piece of text, we implemented it by pulling in a grunt plugin called grunt-ng-constant (or ngconstant).  This plugin allows you to configure values that will be placed in a generated Angular JS module.  That file is included by index.html, causing the module to be instantiated, and then the module just has to be injected into the other parts of your Angular JS app.

We configured ngconstant to run in the grunt build and generate a file named modules/ui/app/scripts/config.js from an input file at modules/ui/constants.json .  The config.js file contains the configured values, including “supportEmail”, in a module definition.  That value is injected into the ExperimentDetailsCtrl and used in an error message, if it exists.

At the same time, we changed the configuration of the URL of the backend server to be also defined using the ngconstant mechanism.

#### Use

```javascript
{
  	"supportEmail": "",
  	"apiHostBaseUrlValue": "http://localhost:8080/api/v1"
}
```
In order to configure a support email for Wasabi, you simply edit the modules/ui/constants.json file which looks like this by default:

<div></div>

```javascript
{
  "supportEmail": "abtesting.support@intuit.com",
  "apiHostBaseUrlValue": "http://localhost:8080/api/v1"
}
```
This will cause no support email to be displayed in the error message.  If, instead, it looked like this:



Then the support email will be displayed in that error message.

#### Technical Details

One thing of note, in order to make it possible to be able to customize the backend server URL after the build (e.g.,
  during deployment), we needed to make sure the config.js file was not minimized into the wasabi.js file during the
  grunt build.  We also needed to make sure it was loaded by the index.html .

In order to keep the config.js file from being included in the combined and minified file, we make sure we call the
ngconstant grunt task after the task that creates that minified file and we also make sure the file is generated into t
he dist directory.

In order to put the include of config.js into the index.html, we pulled in another grunt plugin, file_append, and cause
it to add a `<script>` tag to pull in the config.js file at the very end of the build.

### Branding Mechanism

#### Motivation

Another area where we needed to pull something out of the open source version of the UI, but want to be able to put it back, is with the Intuit and IDEA branding on the login page and the header area of all the other pages.  This involves both some images and some CSS (to adjust the position of the login form, for example).

#### Implementation

Since we needed to override some CSS to make the positioning work, and to pull in the images, we added a file that will be loaded in the open source version by index.html, but which is empty.  By putting the necessary image files in the right place and then putting the necessary CSS to adjust the login form and the header and to invoke those images in the file modules/ui/app/styles/branding.scss , you can customize those parts of the UI.

#### Use
```css
header {
  .brandBanner {
	   .brandContent {
  	    background: #373737 url(/images/idea_horizontal_reversed_46.png) no-repeat right center !important;
     }
  }
}

.signinBox {
	div.signinLogo {
    	margin-left: 0 !important;
    	position: relative !important;
    	padding-top: 5px !important;
    	padding-left: 175px !important;
    	background: white url(/images/intuit_signin.png) no-repeat -10px 1px !important;
	}
}
```
In the case of our Intuit/IDEA branding, you need to put CSS like this in the branding.scss file (note: it is actually SCSS, which is processed by compass into a CSS file at build time):

<div></div>
Notice the copious use of “!important”.  That is a CSS feature that causes those settings to override any equivalent setting that had already been specified in a previous CSS file.  That is necessary because the branding.scss file content is pulled in by index.html after the normal CSS file, so we need to override any existing settings to make sure our customizations are used.  Also notice the references to the two image files.  They must be put in modules/ui/app/images .



### Appendix

Note: you may need to be careful about copying and pasting this code, as newlines may be added in odd places.

> plugins/personalization/ctrl.js

```javascript
'use strict';

angular.module('wasabi.controllers').
	controllerProvider.register('PersonalizationCtrl', ['$scope', 'experiment', 'UtilitiesFactory', '$modalInstance',
    	function ($scope, experiment, UtilitiesFactory, $modalInstance) {
        	$scope.experiment = experiment;
        	$scope.personalizationFormSubmitted = false;

        	$scope.experiment.isPersonalizationEnabled = (experiment && experiment.hasOwnProperty('isPersonalizationEnabled') ? experiment.isPersonalizationEnabled : false);

        	$scope.savePersonalization = function(isFormInvalid, theForm) {
            	if (!isFormInvalid) {
                	$modalInstance.close();
            	}
            	else {
                	$scope.personalizationFormSubmitted = true;
            	}
        	};

        	$scope.cancel = function() {
            	$modalInstance.close();
        	};
    	}
]);
```

> plugins/personalization/template.html

```html
<div id="personalizationModal" class="modalDialog" style="width: 700px; left: 0;">
	<h1>Personalization</h1>
	<form name="personalizationForm" novalidate ng-submit="savePersonalization(personalizationForm.$invalid, personalizationForm);">
    	<div class="dialogContent">
         	<div>
            	<ul class="formLayout" ng-show="!readOnly">
                	<li>
                    	<div style="width: 100%;">
                        	<input id="chkPersonalization" name="chkPersonalization" type="checkbox" ng-model="experiment.isPersonalizationEnabled" ensure-personalization-model ng-checked="{{experiment.isPersonalizationEnabled}}"/>&nbsp;&nbsp;<label
                        	for="chkPersonalization" class="checkboxLabel" name="isPersonalizationEnabled">This is a personalization experiment</label>

                    	</div>
                	</li>
                	<li class="layout8020" ng-show="experiment.isPersonalizationEnabled">
                    	<div style="width: 320px;">
                        	<label>Model Name</label>
                        	<input id="modelName" name="modelName" ng-model="experiment.modelName" class="form-control text" ng-pattern="/^[_\-\$A-Za-z][_\-\$A-Za-z0-9]*$/" ng-maxlength="64"/>&nbsp;&nbsp;
                        	<!-- Validation error -->
                        	<div class="error"
                             	ng-show="personalizationForm.chkPersonalization.$modelValue == true &&
                             	((personalizationForm.modelName.$dirty && personalizationForm.modelName.$invalid &&
                                             	!personalizationForm.modelName.$focused) ||
                                      	personalizationFormSubmitted)">
                            	<small class="fieldError"
                                   	ng-show="personalizationForm.chkPersonalization.$error.ensurePersonalizationModel && !personalizationForm.modelName.$error.pattern">
                                	Model name required.
                            	</small>
                        	</div>
                        	<div class="error"
                             	ng-show="(personalizationForm.modelName.$dirty && personalizationForm.modelName.$invalid &&
                                      	!personalizationForm.modelName.$focused) ||
                                      	(personalizationFormSubmitted &&
                                       	(personalizationForm.modelName.$error.required || personalizationForm.modelName.$error.pattern || personalizationForm.modelName.$error.maxlength))">
                            	<small class="fieldError"
                                   	ng-show="personalizationForm.modelName.$error.pattern">
                                	Invalid model name.
                            	</small>
                            	<small class="fieldError"
                                   	ng-show="personalizationForm.modelName.$error.maxlength">
                                	Model name cannot be longer than 64 characters.
                            	</small>
                        	</div>
                    	</div>
                    	<div>
                        	<label>Model Version</label>
                        	<input id="modelVersion" name="modelVersion" ng-model="experiment.modelVersion" class="form-control text" ng-pattern="/^[_\-\$A-Za-z0-9][_\.\-\$A-Za-z0-9]*$/" ng-maxlength="64" />
                        	<div class="error"
                             	ng-show="(personalizationForm.modelVersion.$dirty && personalizationForm.modelVersion.$invalid &&
                                      	!personalizationForm.modelVersion.$focused) || personalizationFormSubmitted">
                            	<small class="fieldError"
                                   	ng-show="personalizationForm.modelVersion.$error.pattern">
                                	Invalid model version.
                            	</small>
                            	<small class="fieldError"
                                   	ng-show="personalizationForm.modelVersion.$error.maxlength">
                                	Model version cannot be longer than 64 characters.
                            	</small>
                        	</div>
                    	</div>
                	</li>
            	</ul>
            	<div ng-show="readOnly">
                	<label ng-show="experiment.isPersonalizationEnabled">Personalization is enabled for this experiment</label>
                	<label ng-show="!experiment.isPersonalizationEnabled">Personalization is <b>not</b> enabled for this experiment</label>
            	</div>
        	</div>
        	<div class="buttonBar">
            	<button id="btnSavePersonalization" class="blue cancel">Save</button>
            	<button id="btnSavePersonalizationCancel" class="cancel" onclick="return false;" ng-click="cancel();">Cancel</button>
        	</div>
    	</div>
	</form>
</div>
```

> plugins/personalization/detailsCtrl.js

```javascript
'use strict';

angular.module('wasabi.controllers').
	controllerProvider.register('PersonalizationDetailsCtrl', ['$scope', 'experiment', 'UtilitiesFactory', '$modalInstance', 'ExperimentsFactory',
    	function ($scope, experiment, UtilitiesFactory, $modalInstance, ExperimentsFactory) {
        	$scope.experiment = experiment;
        	$scope.personalizationFormSubmitted = false;

        	$scope.data = {
            	disablePersonalizationFields: true
        	};

        	$scope.experiment.isPersonalizationEnabled = (experiment && experiment.hasOwnProperty('isPersonalizationEnabled') ? experiment.isPersonalizationEnabled : false);

        	$scope.savePersonalizationValues = function() {
            	$scope.stringifiedPersonalization = JSON.stringify({
                	isPersonalizationEnabled: ($scope.experiment.isPersonalizationEnabled ? $scope.experiment.isPersonalizationEnabled : false),
                	modelName: ($scope.experiment.modelName ? $scope.experiment.modelName : ''),
                	modelVersion: ($scope.experiment.modelVersion ? $scope.experiment.modelVersion : '')
            	});
        	};

        	$scope.restorePersonalizationValues = function(values) {
            	return JSON.parse(values);
        	};

        	$scope.editPersonalization = function() {
            	$scope.data.disablePersonalizationFields = false;
            	$scope.$apply(); // Needed to poke Angular to update the fields based on that variable.
            	$scope.savePersonalizationValues();
            	return $scope.stringifiedPersonalization;
        	};

        	$scope.cancelPersonalization = function(tempValue) {
            	var tmp = $scope.restorePersonalizationValues(tempValue);
            	$scope.experiment.isPersonalizationEnabled = tmp.isPersonalizationEnabled;
            	$scope.experiment.modelName = tmp.modelName;
            	$scope.experiment.modelVersion = tmp.modelVersion;
            	$scope.data.disablePersonalizationFields = true;
            	$scope.experimentFormSubmitted = false;
            	$scope.$apply();
        	};

        	// This function handles saving the personalization metadata when the checkbox is checked or unchecked.
        	// If it is being checked, we need to validate that there is a model name provided.  In either case,
        	// we need to set the value of data.disableModelFields so that the fields will be enabled or disabled
        	// correctly, based on the state of the isPersonalizationEnabled flag.
        	$scope.savePersonalization = function() {
            	var experiment = $scope.experiment;
            	$scope.personalizationFormSubmitted = true;
            	$scope.$apply();
            	if (experiment.isPersonalizationEnabled &&
                	(!experiment.modelName || $.trim(experiment.modelName).length === 0 ||
                 	($('#modelVersion').closest('div').find('.fieldError').length !==
                  	$('#modelVersion').closest('div').find('.ng-hide').length))) {
                	// Need modelName, error already displayed,
                	// or the second test is a kludge because we don't have access to the $error from the form or
                	// fields here, so we just check if there are any unhidden error messages for the model version.

                	// Handle the problem that the dynamic edit widgets (the pencil, etc., buttons) collapse
                	// when you do a save...even if there is an error.  In the error case, we want them to show.
                	$('#personalizationToolbar').data('dynamicEdit').displayWidgets($('#personalizationToolbar .dynamicEdit'), false);
                	return;
            	}
            	var updates = {
                    	id: experiment.id,
                    	isPersonalizationEnabled: experiment.isPersonalizationEnabled,
                    	modelName: experiment.modelName
                	};
            	if (experiment.modelVersion && $.trim(experiment.modelVersion).length > 0) {
                	updates.modelVersion = $.trim(experiment.modelVersion);
            	}
            	else {
                	updates.modelVersion = '';
            	}
            	ExperimentsFactory.update(updates).$promise.then(function () {
                    	$scope.data.disablePersonalizationFields = true;
                    	UtilitiesFactory.trackEvent('saveItemSuccess',
                        	{key: 'dialog_name', value: 'savePersonalizationFromDetails'},
                        	{key: 'application_name', value: experiment.applicationName},
                        	{key: 'item_id', value: experiment.id},
                        	{key: 'item_label', value: experiment.label});
                	},
                	function(response) {
                    	$scope.data.disablePersonalizationFields = true;
                    	UtilitiesFactory.handleGlobalError(response);
                	}
            	);
        	};

        	$scope.cancel = function() {
            	$modalInstance.close();
        	};
    	}
]);
```

> plugins/personalization/detailsTemplate.html

```html
<div id="personalizationDetailsModal" class="modalDialog" style="width: 700px; left: 0;">
	<h1>Personalization</h1>
	<form name="personalizationForm">
    	<div class="dialogContent">
         	<div>
            	<div id="personalizationToolbar" ng-show="!readOnly" dynamic-edit input-tag="stringifiedPersonalization" select-function="savePersonalization" edit-function="editPersonalization" cancel-function="cancelPersonalization" ng-model="experiment.modelName" class="dynamicToolbar" style="top: 43px; left: 300px;"></div>
            	<ul class="formLayout oneCol" ng-show="!readOnly">
                	<li>
                    	<div style="width: 100%;">
                        	<input id="chkPersonalization" name="chkPersonalization" type="checkbox" ng-model="experiment.isPersonalizationEnabled" ensure-personalization-model ng-checked="{{experiment.isPersonalizationEnabled}}" ng-disabled="data.disablePersonalizationFields" ng-class="{disabled: data.disablePersonalizationFields}"/>&nbsp;&nbsp;<label
                        	for="chkPersonalization" class="checkboxLabel" name="isPersonalizationEnabled" ng-class="{disabled: data.disablePersonalizationFields}">This is a personalization experiment</label>

                    	</div>
                	</li>
                	<li class="layout8020" ng-show="experiment.isPersonalizationEnabled">
                    	<div style="width: 320px;">
                        	<label ng-class="{disabled: data.disablePersonalizationFields}">Model Name</label>
                        	<input id="modelName" name="modelName" ng-model="experiment.modelName" class="form-control text" ng-pattern="/^[_\-\$A-Za-z][_\-\$A-Za-z0-9]*$/" ng-maxlength="64" ng-disabled="data.disablePersonalizationFields" ng-class="{disabled: data.disablePersonalizationFields}" />&nbsp;&nbsp;
                        	<!-- Validation error -->
                        	<div class="error"
                             	ng-show="personalizationForm.chkPersonalization.$modelValue == true &&
                             	((personalizationForm.modelName.$dirty && personalizationForm.modelName.$invalid &&
                                             	!personalizationForm.modelName.$focused) ||
                                      	personalizationFormSubmitted)">
                            	<small class="fieldError"
                                   	ng-show="personalizationForm.chkPersonalization.$error.ensurePersonalizationModel && !personalizationForm.modelName.$error.pattern">
                                	Model name required.
                            	</small>
                        	</div>
                        	<div class="error"
                             	ng-show="(personalizationForm.modelName.$dirty && personalizationForm.modelName.$invalid &&
                                      	!personalizationForm.modelName.$focused) ||
                                      	(personalizationFormSubmitted &&
                                       	(personalizationForm.modelName.$error.required || personalizationForm.modelName.$error.pattern || personalizationForm.modelName.$error.maxlength))">
                            	<small class="fieldError"
                                   	ng-show="personalizationForm.modelName.$error.pattern">
                                	Invalid model name.
                            	</small>
                            	<small class="fieldError"
                                   	ng-show="personalizationForm.modelName.$error.maxlength">
                                	Model name cannot be longer than 64 characters.
                            	</small>
                        	</div>
                    	</div>
                    	<div>
                        	<label ng-class="{disabled: data.disablePersonalizationFields}">Model Version</label>
                        	<input id="modelVersion" name="modelVersion" ng-model="experiment.modelVersion" class="form-control text" ng-pattern="/^[_\-\$A-Za-z0-9][_\.\-\$A-Za-z0-9]*$/" ng-maxlength="64" ng-disabled="data.disablePersonalizationFields" ng-class="{disabled: data.disablePersonalizationFields}"  />
                        	<div class="error"
                             	ng-show="(personalizationForm.modelVersion.$dirty && personalizationForm.modelVersion.$invalid &&
                                      	!personalizationForm.modelVersion.$focused) || personalizationFormSubmitted">
                            	<small class="fieldError"
                                   	ng-show="personalizationForm.modelVersion.$error.pattern">
                                	Invalid model version.
                            	</small>
                            	<small class="fieldError"
                                   	ng-show="personalizationForm.modelVersion.$error.maxlength">
                                	Model version cannot be longer than 64 characters.
                            	</small>
                        	</div>
                    	</div>
                	</li>
            	</ul>
            	<div class="buttonBar">
                	<button id="btnSavePersonalizationCancel" class="blue cancel" onclick="return false;" ng-click="cancel();">Close</button>
            	</div>
        	</div>
    	</div>
	</form>
	<div ng-show="readOnly">
    	<label ng-show="experiment.isPersonalizationEnabled">Personalization is enabled for this experiment</label>
    	<label ng-show="!experiment.isPersonalizationEnabled">Personalization is <b>not</b> enabled for this experiment</label>
	</div>

</div>
```

>plugins/tracking/factory.js

```javascript
'use strict';

var globalWebSDKTrackingParams = {},
	tracking = {
    	init: function() {
        	var that = this;
        	if (typeof(intuit) !== 'undefined') {
            	tracking.doInit();
        	}
        	else {
            	setTimeout(that.init, 1000);
        	}
    	},

    	doInit: function() {
        	// Initialize Web SDK tracking
        	window.webAnalyticsSDKTracker = new intuit.web.analytics();
        	window.webAnalyticsSDKTracker.init({
            	'cec_version': '1',
            	'app_name': 'Wasabi',
            	'offering_id': 'Wasabi',

            	'app_log_enable': false,
            	'app_force_crossdomain_tracking': true,
            	'app_force_crossdomain_tracking_timeout': 250,

            	'providers': {
                	'iac':{
                    	'enable': true,
                    	'clickstream_format_version': '1',
                    	'server_endpoint': 'http://trinity-prfqdc.intuit.com/trinity/v1/idea-wasabicui-clickstream',
                    	'server_endpoint_secure': 'https://trinity-prfqdc.intuit.com/trinity/v1/idea-wasabicui-clickstream'
                	},

                	'siteCatalyst' :{
                    	'enable': false,
                    	'server_endpoint': 'http://ci.intuit.com/b/ss/',
                    	'server_endpoint_secure': 'https://sci.intuit.com/b/ss/'
                	}
            	},
            	'dom_events' : [
                	{
                    	'css_selector': 'input[type=button]',
                    	'event': 'click',
                    	'standard': true
                	},
                	{
                    	'css_selector': 'button',
                    	'event': 'click',
                    	'standard': true
                	},
                	{
                    	'css_selector': 'a',
                    	'event': 'click',
                    	'standard': true
                	},
                	{
                    	'event': 'load',
                    	'standard': true
                	},
                	{
                    	'css_selector': 'button#btnSearch',  //Group of DOM elements to be tracked (Required for any DOM event except load)
                    	'event': 'click',         	//DOM event (Required) supported types: click, change, focus, blur
                    	'capture' : {
                        	'iac': [
                            	{
                                	'sampling': true,
                                	'inherit_from': ['page_parent_iac'],
                                	'properties' : {
                                    	'event.event_name': 'click',
                                    	'event.event_category': 'dom',
                                    	'event.properties.pagination_item': 'page'
                                	}
                            	}
                        	]
                    	}
                	}
            	],
            	'custom_events': {
                	'saveItemSuccess': [ // Tracked when the save of an item, like an experiment, was successful.
                    	{
                        	'capture': {
                            	'iac': [
                                	{
                                    	'properties': {
                                        	'event.event_name': 'saveItemSuccess',
                                        	'event.dialog_name': '#DIALOG_NAME#',
                                        	'event.application_name': '#APPLICATION_NAME#',
                                        	'event.experiment_id': '#EXPERIMENT_ID#',
                                        	'event.item_id': '#ITEM_ID#',
                                        	'event.item_label': '#ITEM_LABEL#'
                                    	}
                                	}
                            	]
                        	}
                    	}
                	],
                	'updateItemSuccess': [ // Tracked when an item is updated.
                    	{
                        	'capture': {
                            	'iac': [
                                	{
                                    	'properties': {
                                        	'event.event_name': 'updateItemSuccess',
                                        	'event.dialog_name': '#DIALOG_NAME#',
                                        	'event.experiment_id': '#EXPERIMENT_ID#',
                                        	'event.item_value': '#ITEM_VALUE#'
                                    	}
                                	}
                            	]
                        	}
                    	}
                	],
                	'deleteItemSuccess': [ // Tracked when an item is deleted.
                    	{
                        	'capture': {
                            	'iac': [
                                	{
                                    	'properties': {
                                        	'event.event_name': 'deleteItemSuccess',
                                        	'event.dialog_name': '#DIALOG_NAME#',
                                        	'event.experiment_id': '#EXPERIMENT_ID#',
                                        	'event.item_id': '#ITEM_ID#'
                                    	}
                                	}
                            	]
                        	}
                    	}
                	],
                	'changeItemStateSuccess': [ // Tracked when an item's state is changed.
                    	{
                        	'capture': {
                            	'iac': [
                                	{
                                    	'properties': {
                                        	'event.event_name': 'changeStateSuccess',
                                        	'event.dialog_name': '#DIALOG_NAME#',
                                        	'event.experiment_id': '#EXPERIMENT_ID#',
                                        	'event.item_id': '#ITEM_ID#'
                                    	}
                                	}
                            	]
                        	}
                    	}
                	],
                	'closeBucketSuccess': [ // Tracked when a bucket is closed.
                    	{
                        	'capture': {
                            	'iac': [
                                	{
                                    	'properties': {
                                        	'event.event_name': 'closeBucketSuccess',
                                        	'event.dialog_name': '#DIALOG_NAME#',
                                        	'event.experiment_id': '#EXPERIMENT_ID#',
                                        	'event.item_label': '#ITEM_LABEL#'
                                    	}
                                	}
                            	]
                        	}
                    	}
                	],
                	'loadedDialog': [ // Tracked when a dialog is loaded.
                    	{
                        	'capture': {
                            	'iac': [
                                	{
                                    	'properties': {
                                        	'event.event_name': 'loadedDialog',
                                        	'event.dialog_name': '#DIALOG_NAME#',
                                        	'event.application_name': '#APPLICATION_NAME#'
                                    	}
                                	}
                            	]
                        	}
                    	}
                	],
                	'canceledDialog': [ // Tracked when a dialog is closed without save.
                    	{
                        	'capture': {
                            	'iac': [
                                	{
                                    	'properties': {
                                        	'event.event_name': 'canceledDialog',
                                        	'event.dialog_name': '#DIALOG_NAME#'
                                    	}
                                	}
                            	]
                        	}
                    	}
                	],
                	'advancedSearch': [ // Tracked when an advanced search is done (Go button clicked).
                    	{
                        	'capture': {
                            	'iac': [
                                	{
                                    	'properties': {
                                        	'event.event_name': 'advancedSearch',
                                        	'event.search_parms': '#SEARCH_PARMS#'
                                    	}
                                	}
                            	]
                        	}
                    	}
                	],
                	'saveRolesSuccess': [ // Tracked when the save of an item, like an experiment, was successful.
                    	{
                        	'capture': {
                            	'iac': [
                                	{
                                    	'properties': {
                                        	'event.event_name': 'saveRolesSuccess',
                                        	'event.dialog_name': '#DIALOG_NAME#',
                                        	'event.application_name': '#APPLICATION_NAME#',
                                        	'event.item_id': '#ITEM_ID#',
                                        	'event.item_role': '#ITEM_ROLE#'
                                    	}
                                	}
                            	]
                        	}
                    	}
                	],
                	'deleteRolesSuccess': [ // Tracked when an item is deleted.
                    	{
                        	'capture': {
                            	'iac': [
                                	{
                                    	'properties': {
                                        	'event.event_name': 'deleteRolesSuccess',
                                        	'event.dialog_name': '#DIALOG_NAME#',
                                        	'event.application_name': '#APPLICATION_NAME#',
                                        	'event.item_id': '#ITEM_ID#',
                                        	'event.item_role': '#ITEM_ROLE#'
                                    	}
                                	}
                            	]
                        	}
                    	}
                	]
            	},
            	'inheritances' : {},
            	'parameters' : { // These tie the data saved when tracking to a global var, necessary because the tracking is asynch so you can only point the Web SDK at something that it can pull the value from.
                	'SCREEN_ID': {
                    	'values': [
                        	{'js_var': 'screen_id'}
                    	]
                	},
                	'DIALOG_NAME': {
                    	'values': [
                        	{'js_var': 'globalWebSDKTrackingParams.dialog_name'}
                    	]
                	},
                	'APPLICATION_NAME': {
                    	'values': [
                        	{'js_var': 'globalWebSDKTrackingParams.application_name'}
                    	]
                	},
                	'EXPERIMENT_ID': {
                    	'values': [
                        	{'js_var': 'globalWebSDKTrackingParams.experiment_id'}
                    	]
                	},
                	'ITEM_ID': {
                    	'values': [
                        	{'js_var': 'globalWebSDKTrackingParams.item_id'}
                    	]
                	},
                	'ITEM_LABEL': {
                    	'values': [
                        	{'js_var': 'globalWebSDKTrackingParams.item_label'}
                    	]
                	},
                	'ITEM_VALUE': {
                    	'values': [
                        	{'js_var': 'globalWebSDKTrackingParams.item_value'}
                    	]
                	},
                	'ITEM_ROLE': {
                    	'values': [
                        	{'js_var': 'globalWebSDKTrackingParams.item_role'}
                    	]
                	},
                	'RESPONSE_STATUS': {
                    	'values': [
                        	{'js_var': 'globalWebSDKTrackingParams.response_status'}
                    	]
                	},
                	'RESPONSE_DATA': {
                    	'values': [
                        	{'js_var': 'globalWebSDKTrackingParams.response_data'}
                    	]
                	},
                	'SEARCH_PARMS': {
                    	'values': [
                        	{'js_var': 'globalWebSDKTrackingParams.search_parms'}
                    	]
                	},
                	'PAGINATION_ITEM': {
                    	'values': [
                        	{
                            	'element': {  //Get value from a DOM element
                                	'element_id': 'self', //Any element ID or "self" (element that triggers the DOM event).  "self" can be used in DOM_events's properties only.
                                	'element_property': 'innerHtml' //Get value from the DOM element's property
                            	}
                        	}
                    	]
                	}
            	}
        	});
    	},

    	reset: function() {
        	function doInitNow() {
            	if (window.webAnalyticsSDKTracker) {
                	window.webAnalyticsSDKTracker.resetListeners();
            	}
        	}
        	setTimeout(doInitNow, 100);
    	},

    	trackEvent: function(eventName, parm1, parm2, parm3, parm4) {
        	if (parm1) {
            	globalWebSDKTrackingParams[parm1.key] = parm1.value;
        	}
        	if (parm2) {
            	globalWebSDKTrackingParams[parm2.key] = parm2.value;
        	}
        	if (parm3) {
            	globalWebSDKTrackingParams[parm3.key] = parm3.value;
        	}
        	if (parm4) {
            	globalWebSDKTrackingParams[parm4.key] = parm4.value;
        	}
        	webAnalyticsSDKTracker.triggerEvent(eventName);
    	}
	};

tracking.init();
```
