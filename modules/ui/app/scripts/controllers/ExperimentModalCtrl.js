/* global moment:false */

'use strict';

angular.module('wasabi.controllers')
    .controller('ExperimentModalCtrl',
        ['$scope', '$uibModalInstance', '$uibModal', '$filter', 'ExperimentsFactory', 'experiments', 'experiment', 'UtilitiesFactory', '$rootScope', 'readOnly', 'applications', 'DialogsFactory', 'RuleEditFactory', 'ConfigFactory', 'Session', 'AuthzFactory', 'ApplicationsFactory', 'PERMISSIONS', '$cookies', 'openedFromModal', 'EmailFactory', 'favoritesObj',
            function ($scope, $uibModalInstance, $uibModal, $filter, ExperimentsFactory, experiments, experiment, UtilitiesFactory, $rootScope, readOnly, applications, DialogsFactory, RuleEditFactory, ConfigFactory, Session, AuthzFactory, ApplicationsFactory, PERMISSIONS, $cookies, openedFromModal, EmailFactory, favoritesObj) {

                UtilitiesFactory.trackEvent('loadedDialog',
                    {key: 'dialog_name', value: 'createOrEditExperiment'});

                $scope.data = {
                    disableSimple: false,
                    userCapValue: (experiment && experiment.isRapidExperiment ? experiment.userCap : ''),
                    apiLanguage: 'curl'
                };

                $scope.experiment = experiment;
                $scope.tags = [];
                $scope.allTags = [];
                $scope.experiment.applicationName2 = (applications.length === 1 ? '' : 'novalue');
                $scope.readOnly = (readOnly ? readOnly : false);
                $scope.simpleRuleEditing = $cookies.showAdvancedSegmentationEditor === undefined || $cookies.showAdvancedSegmentationEditor !== 'true';
                $scope.experimentFormSubmitted = $scope.readOnly;
                // needed to check uniqueness of name+label combination with directive
                $scope.experiments = experiments;
                $scope.applications = applications;
                $scope.allApplications = [];
                $scope.postSubmitError = null;
                $scope.modalInstance = $uibModalInstance;
                $scope.showApplicationName2 = false;
                $scope.newApplicationNamePrompt = ConfigFactory.newApplicationNamePrompt;
                $scope.currentUser = Session.userID;
                $scope.openedFromModal = openedFromModal;
                $scope.bucketTotalsValid = false;
                $scope.rulesChangedNotSaved = !($scope.experiment && $scope.experiment.rule && $scope.experiment.rule.length > 0);
                $scope.plugins = $rootScope.plugins;
                $scope.favoritesObj = favoritesObj;

                $scope.tabs = [
                    {active: true},
                    {active: false},
                    {active: false},
                    {active: false}
                ];

                $scope.types = RuleEditFactory.types;
                $scope.placeholders = RuleEditFactory.placeholders;
                $scope.stringTypeOperators = RuleEditFactory.stringTypeOperators;
                $scope.numberTypeOperators = RuleEditFactory.numberTypeOperators;
                $scope.booleanTypeOperators = RuleEditFactory.booleanTypeOperators;
                $scope.dateTypeOperators = RuleEditFactory.dateTypeOperators;
                $scope.operators = RuleEditFactory.operators();
                $scope.booleanOperators = RuleEditFactory.booleanOperators;

                // This will default with one empty rule, in case there is no rule.
                $scope.rules = [
                    {
                        booleanOperator: '',
                        type: 'string',
                        subject: '',
                        operator: 'equals',
                        value: '',
                        showDelete: true,
                        errorMessage: 'Enter a quoted string'
                    }
                ];

                $scope.help = ConfigFactory.help;
                $scope.labelStrings = ConfigFactory.labelStrings;

                if ($scope.applications.length === 1) {
                    $scope.experiment.applicationName = $scope.applications[0];
                }

                $scope.totalBucketAllocation = function() {
                    return UtilitiesFactory.totalBucketAllocation($scope);
                };

                $scope.toggleAdvanced = function() {
                    var results = RuleEditFactory.toggleAdvanced({
                        disableSimple: $scope.data.disableSimple,
                        experiment: $scope.experiment,
                        simpleRuleEditing: $scope.simpleRuleEditing,
                        rules: $scope.rules,
                        tabs: $scope.tabs
                    });
                    if (!results) {
                        return false;
                    }
                    else {
                        $scope.experiment = results.experiment;
                        $scope.simpleRuleEditing = results.simpleRuleEditing;
                    }
                };

                $scope.setSimpleDisabled = function() {
                    $scope.data.disableSimple = true;
                    $scope.rulesChangedNotSaved = true;
                };

                $scope.setAsFavorite = function() {
                    // Don't need to do anything.
                };

                $scope.typeSpecificPlaceholder = function(type) {
                    return $scope.placeholders[type];
                };

                $scope.changed = function() {
                    if ($scope.experiment.applicationName === ConfigFactory.newApplicationNamePrompt) {
                        $scope.experiment.applicationName2 = '';
                        $scope.showApplicationName2 = true;
                    }
                    else {
                        $scope.experiment.applicationName2 = 'novalue';
                        $scope.showApplicationName2 = false;
                        // Only need the list of tags in the application if one has been specified and it is not
                        // a new application, as new applications don't have associated tags.
                        $scope.loadAllTags();
                    }
                };

                $scope.stateImgUrl = function (state) {
                    return UtilitiesFactory.stateImgUrl(state);
                };

                $scope.stateName = function(state) {
                    return UtilitiesFactory.stateName(state);
                };

                // The signature of this function is consistent with the jQuery autocomplete extension's select
                // function.  It is passed the event and then ui.item.label is the selected label.
                $scope.selectApplication = function(event, ui) {
                    $scope.experiment.applicationName2 = ui.item.label;
                    $scope.$digest();
                    $scope.openAdminListModal($scope.experiment.applicationName2);
                    return false;
                };

                // This function is called by the autocomplete directive when the user hits enter.  We simply want to
                // have this be the selected application name, so do nothing.
                $scope.selectApplicationOnEnter = function(applicationName) {
                    $scope.experiment.applicationName2 = applicationName;
                    $scope.openAdminListModal(applicationName);
                    return false;
                };

                $scope.handleNewApplication = function() {
                    if ($scope.oldAppName === ConfigFactory.newApplicationNamePrompt) {
                        // Add the new application name to the array of applications.
                        $scope.applications.splice(1, 0, $scope.experiment.applicationName);
                        $scope.experiment.applicationName2 = 'novalue';
                        $scope.showApplicationName2 = false;
                    }
                };

                $scope.loadAllTags = function() {
                    UtilitiesFactory.loadAllTags($scope, true);
                };

                $scope.queryTags = function(query) {
                    return UtilitiesFactory.queryTags(query, $scope.allTags);
                };

                $scope.transferTags = function(fromExperiment) {
                    UtilitiesFactory.transferTags(fromExperiment, $scope);
                };

                $scope.init = function() {
                    $scope.processRule();
                    $scope.loadAllApplications();
                    $scope.loadAllTags();
                    $scope.transferTags(true);
                };

                $scope.loadExperiment = function () {
                    ExperimentsFactory.show({id: $scope.experiment.id}).$promise.then(function (experiment) {
                        // Need to just fill in the experiment object with the fields it doesn't have due to being
                        // provided by the Card View API.
                        for (var prop in experiment) {
                            if (experiment.hasOwnProperty(prop) && !$scope.experiment.hasOwnProperty(prop)) {
                                $scope.experiment[prop] = experiment[prop];
                            }
                        }
                        if ($scope.experiment.hypothesisIsCorrect === null) {
                            $scope.experiment.hypothesisIsCorrect = '';
                        }

                        $scope.init();
                    }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'Your experiment could not be retrieved.');
                    });
                };

                $scope.loadAllApplications = function () {
                    ApplicationsFactory.query().$promise.then(function (applications) {
                        if (applications) {
                            $scope.allApplications = [];
                            // Make a list of only the applications for which this user doesn't have access.
                            for (var i = 0; i < applications.length; i++) {
                                var hasAccessForApp = false;
                                for (var j = 0; j < $scope.applications.length; j++) {
                                    // Check if this application is one of the ones they already have access for.
                                    if (applications[i].applicationName === $scope.applications[j]) {
                                        hasAccessForApp = true;
                                        break;
                                    }
                                }
                                if (!hasAccessForApp) {
                                    $scope.allApplications.push(applications[i].applicationName);
                                    // Note: This will be picked up by the AutocompleteDirective attached to the New Application field.
                                }
                            }
                        }
                    }, function(response) {
                            UtilitiesFactory.handleGlobalError(response, 'The list of applications could not be retrieved.');
                        }
                    );
                };

                $scope.typeChanged = function(rule, subForm) {
                    RuleEditFactory.typeChanged(rule, subForm);
                    $scope.rulesChangedNotSaved = true;
                };

                $scope.ruleChanged = function() {
                    $scope.rulesChangedNotSaved = true;
                };

                // This is called by the UI to add a new, empty rule expression.
                $scope.addRule = function() {
                    RuleEditFactory.addRule($scope.rules);
                    $scope.rulesChangedNotSaved = true;
                };

                // This is called by the UI to remove a rule by deleting it from the $scope.rules array.
                $scope.removeRule = function(index) {
                    RuleEditFactory.removeRule(index, $scope.rules);
                    $scope.rulesChangedNotSaved = true;
                };

                $scope.convertRuleControlsToRuleString = function() {
                    return RuleEditFactory.convertRuleControlsToRuleString($scope.rules, $scope.tabs);
                };

                $scope.populateRuleControlsFromJSON = function(jsonRule) {
                    var params = {
                        simpleRuleEditing: $scope.simpleRuleEditing,
                        disableSimple: $scope.data.disableSimple
                    };
                    var results = RuleEditFactory.populateRuleControlsFromJSON(jsonRule, $scope.rules, params);
                    $scope.simpleRuleEditing = results.simpleRuleEditing;
                    $scope.data.disableSimple = results.disableSimple;
                };

                // If there is a JSON rule, clear the rule array and populate from the JSON.
                $scope.processRule = function() {
                    if ($scope.experiment.ruleJson && $scope.experiment.ruleJson.length > 0) {
                        $scope.rules = [];
                        $scope.populateRuleControlsFromJSON($scope.experiment.ruleJson);
                    }
                };

                $scope.rapidExperimentLabel = function() {
                    return UtilitiesFactory.rapidExperimentLabel($scope.experiment);
                };

                $scope.maxRapidUsersChanged = function() {
                    if ($scope.data.userCapValue && $scope.data.userCapValue > 0) {
                        $scope.experiment.isRapidExperiment = true;
                        $scope.experiment.userCap = $scope.data.userCapValue;
                    }
                    else {
                        $scope.experiment.isRapidExperiment = false;
                        $scope.experiment.userCap = 2147483647;
                    }
                };

                $scope.firstPageEncoded = function() {
                    return UtilitiesFactory.firstPageEncoded($scope.experiment);
                };

                if ($scope.experiment.id && $scope.experiment.id.length > 0 && !$scope.experiment.hasOwnProperty('samplingPercent')) {
                    // We know we are being called from the Card View, as that API doesn't include samplingPercent
                    $scope.loadExperiment();
                }
                else {
                    // We are being called with a complete experiment object being passed in, e.g., from the
                    // Experiments list, so we can just get the stuff we don't have, like all applications.
                    $scope.init();
                }

                $scope.doSaveOrCreateExperiment = function(experimentId, afterSaveFunc) {
                    var handleCreateSuccess = function(experiment, dialogName) {
                        $scope.experiment = experiment;

                        $scope.handleNewApplication();

                        UtilitiesFactory.trackEvent('saveItemSuccess',
                            {key: 'dialog_name', value: dialogName},
                            {key: 'application_name', value: experiment.applicationName},
                            {key: 'item_id', value: experiment.id},
                            {key: 'item_label', value: experiment.label});

                        // Tell the Pages tab that it can get the list of global pages, now, because it needed an application name.
                        $rootScope.$broadcast('experiment_created');
                        $scope.experiment.applicationName2 = 'novalue';
                    };
                    var handleCreateError = function(response) {
                        experiment.applicationName = $scope.oldAppName;
                        $scope.experimentFormSubmitted = true;
                        $scope.postSubmitError = UtilitiesFactory.extractErrorFromResponse(response);
                        if ($scope.postSubmitError === 'unauthenticated') {
                            $uibModalInstance.close();
                        }
                    };
                    var handleUpdateSuccess = function(dialogName) {
                        $scope.handleNewApplication();

                        UtilitiesFactory.trackEvent('saveItemSuccess',
                            {key: 'dialog_name', value: dialogName},
                            {key: 'application_name', value: $scope.experiment.applicationName},
                            {key: 'item_id', value: $scope.experiment.id},
                            {key: 'item_label', value: $scope.experiment.label});

                        UtilitiesFactory.displayPageError('', '', false); // Hide the error if it was showing due to an invalid rule.
                        $scope.experiment.applicationName2 = 'novalue';

                        if (afterSaveFunc) {
                            afterSaveFunc();
                        }
                        else {
                            $uibModalInstance.close();
                        }
                    };
                    var handleUpdateError = function(response) {
                        $scope.experimentFormSubmitted = true;
                        $scope.postSubmitError = UtilitiesFactory.extractErrorFromResponse(response);
                        if ($scope.postSubmitError === 'invalidRule') {
                            // Need to handle the special case of an error in the Segmentation Rule, which is
                            // reported within the "error" JSON returned in the response.
                            UtilitiesFactory.displayPageError('Invalid Segmentation Rule', 'Your segmentation rule has a syntactic error.');
                        }
                        else if ($scope.postSubmitError === 'unauthenticated') {
                            $uibModalInstance.close();
                        }
                    };

                    var continueWithCreation = function() {
                        if ($scope.simpleRuleEditing) {
                            // User was using the widgets to edit the rule, we need to convert them to the string to save.
                            $scope.experiment.rule = $scope.convertRuleControlsToRuleString();
                            if ($scope.experiment.rule === null) {
                                // Rule is invalid, as opposed to there being no rule.  This should already be reported
                                // in the UI due to the call to convertRuleControlsToRuleString().
                                return false;
                            }
                        }

                        $scope.oldAppName = experiment.applicationName;
                        experiment.applicationName = appName;

                        var startTime = moment($scope.experiment.startTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']).format('YYYY-MM-DDTHH:mm:ssZZ'),
                            endTime = moment($scope.experiment.endTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']).format('YYYY-MM-DDTHH:mm:ssZZ');

                        if (!experimentId) {
                            // create experiment
                            var newExperiment = {
                                applicationName: experiment.applicationName,
                                label: experiment.label,
                                samplingPercent: experiment.samplingPercent,
                                startTime: startTime,
                                endTime: endTime,
                                description: experiment.description,
                                isRapidExperiment: experiment.isRapidExperiment,
                                tags: experiment.tags
                            };
                            if (experiment.isRapidExperiment) {
                                newExperiment.userCap = experiment.userCap;
                            }
                            if (!creatingNewApplication) {
                                ExperimentsFactory.create(newExperiment).$promise.then(
                                        function (experiment) {
                                            handleCreateSuccess(experiment, 'createExperiment');
                                        },
                                        handleCreateError);
                            }
                            else {
                                ExperimentsFactory.createWithNewApplication(newExperiment).$promise.then(
                                    function (experiment) {
                                        handleCreateSuccess(experiment, 'createExperimentNewApplication');
                                    },
                                    handleCreateError);
                            }
                        } else {
                            // update experiment
                            var experimentUpdates = {
                                id: experimentId,
                                applicationName: $scope.experiment.applicationName,
                                label: $scope.experiment.label,
                                samplingPercent: $scope.experiment.samplingPercent,
                                startTime: startTime,
                                endTime: endTime,
                                description: $scope.experiment.description,
                                rule: $scope.experiment.rule,
                                isRapidExperiment: $scope.experiment.isRapidExperiment,
                                tags: $scope.experiment.tags
                            };
                            if ($scope.experiment.isRapidExperiment) {
                                experimentUpdates.userCap = $scope.experiment.userCap;
                            }
                            if (!creatingNewApplication) {
                                ExperimentsFactory.update(experimentUpdates).$promise.then(function () {
                                        handleUpdateSuccess('editExperiment');
                                    },
                                    handleUpdateError
                                );
                            }
                            else {
                                // Updating experiment, but creating a new application at the same time.
                                ExperimentsFactory.updateWithNewApplication(experimentUpdates).$promise.then(function () {
                                        handleUpdateSuccess('editExperimentNewApplication');
                                    },
                                    handleUpdateError
                                );
                            }
                        }
                    };
                    var preventUnauthorizedApplicationCreation = function(continueFunction) {
                        // We need a list of all the existing applications so we can prevent the user from trying to create
                        // one of the existing ones.
                        ApplicationsFactory.query().$promise.then(function (applications) {
                            if (applications) {
                                var unauthorizedApplications = [];
                                applications.forEach(function(nextApplication) {
                                    if (!UtilitiesFactory.hasPermission(nextApplication.applicationName, PERMISSIONS.createPerm)) {
                                        // User doesn't already have permission to create experiments in this application, so
                                        // add it to the list.
                                        unauthorizedApplications.push(nextApplication.applicationName);
                                    }
                                });

                                // We now have the list of applications for which the user is not authorized, check if
                                // they're trying to create one of those, that is, they are trying to create an experiment
                                // with a new application name which is one of those applications.
                                if (unauthorizedApplications && unauthorizedApplications.length > 0) {
                                    var notAuthorized = false;
                                    for (var i = 0; i < unauthorizedApplications.length; i++) {
                                        if (unauthorizedApplications[i].toLowerCase() === appName.toLowerCase()) {
                                            // User is trying to create an experiment in an existing application for which they
                                            // don't already have permissions, essentially trying to create an application that
                                            // already exists.  We need to prevent this as otherwise they would end up getting
                                            // admin rights!

                                            $scope.openAdminListModal(appName);
                                            notAuthorized = true;
                                            break;
                                        }
                                    }
                                    if (!notAuthorized) {
                                        // They can go ahead and create this application.
                                        continueFunction();
                                    }
                                    // Else, we will simply exit as we have displayed the error.
                                }
                                else {
                                    // There are no applications for which they are not authorized, let them do the create.
                                    continueFunction();
                                }
                            }
                            else {
                                // There are no applications AT ALL, let them do the create.
                                continueFunction();
                            }
                        }, function(response) {
                                UtilitiesFactory.handleGlobalError(response, 'The list of unauthorized applications could not be retrieved.');
                            }
                        );
                    };

                    var creatingNewApplication = (experiment.applicationName === ConfigFactory.newApplicationNamePrompt);
                    var appName = (creatingNewApplication ? experiment.applicationName2 : experiment.applicationName);
                    $scope.transferTags(false); // Transfer from the widget to the experiment.
                    if (creatingNewApplication) {
                        // We need to check for and prevent the user from creating an experiment in an existing
                        // application for which they don't have permission.
                        preventUnauthorizedApplicationCreation(continueWithCreation);
                    }
                    else {
                        // Don't need to do the validation because they are just creating an experiment in an
                        // application for which they are already authorized.
                        continueWithCreation();
                    }
                };

                $scope.validateBuckets = function(starting, experimentId, afterSaveFunc) {
                    if ($scope.experiment.buckets && $scope.experiment.buckets.length > 0) {
                        var totalAllocation = $scope.totalBucketAllocation();
                        if (totalAllocation !== 1.0) {
                            var msg = 'You will be able to start the experiment only if the total allocation percentage of all the buckets is 100%. Right now it is ' +
                                    (totalAllocation * 100).toFixed(2) + '%.',
                                title = 'Allocation Error';
                            if (starting) {
                                DialogsFactory.alertDialog(msg, title);
                            }
                            else {
                                DialogsFactory.confirmDialog(msg, title,
                                                function() {
                                                    // Let the save go through
                                                    $scope.doSaveOrCreateExperiment(experimentId);
                                                },
                                                function() {/* Don't do the save */});
                            }
                        }
                        else {
                            // OK to save
                            $scope.doSaveOrCreateExperiment(experimentId, afterSaveFunc);
                        }
                    }
                    else {
                        var msg2 = 'You need to have at least one bucket defined to be able to start the experiment.',
                            title2 = 'Bucket Error';
                        if (starting) {
                            DialogsFactory.alertDialog(msg2, title2);
                        }
                        else {
                            DialogsFactory.confirmDialog(msg2, title2,
                                    function() {
                                        // Let the save go through
                                        $scope.doSaveOrCreateExperiment(experimentId);
                                    },
                                    function() {/* Don't do the save */});
                        }
                    }
                };

                $scope.saveExperiment = function (experimentId, isFormInvalid) {
                    if (!isFormInvalid) {
                        // Submit as normal

                        if (!experimentId) {
                            // Creating experiment, don't need to validate buckets, yet.
                            $scope.doSaveOrCreateExperiment(experimentId);
                        }
                        else {
                            $scope.validateBuckets(false, experimentId);
                        }
                    } else {
                        $scope.convertRuleControlsToRuleString(); // May show error if part of the rule is what is invalid

                        $scope.experimentFormSubmitted = true;
                    }
                };

                $scope.changeState = function (experiment, state, afterChangeFunc) {
                    UtilitiesFactory.changeState(experiment, state, afterChangeFunc);
                };

                $scope.startExperiment = function(isFormInvalid) {
                    if (isFormInvalid) {
                        $scope.experimentFormSubmitted = true;
                        DialogsFactory.alertDialog('Please address the errors displayed before you can start your experiment.', 'Errors Preventing Start');
                        return;
                    }
                    $scope.validateBuckets(true, $scope.experiment.id, function() {
                        // Attempt to start the experiment.  If successful, let the user know.
                        $scope.changeState($scope.experiment, 'RUNNING', function() {
                            DialogsFactory.alertDialog('Your experiment has been started successfully!  PLEASE NOTE: The experiment may not be available for assignments for up to 5 minutes.', 'Start Successful');
                            $uibModalInstance.close(true);
                        });
                    });

                };

                $scope.cancel = function () {
                    $uibModalInstance.close();
                    //$uibModalInstance.dismiss('cancel');
                };

                $scope.openAdminListModal = function (applicationName) {
                    DialogsFactory.confirmDialog(
                        'You don\'t have any access for application ' + applicationName +
                        '.  Do you want to send an email to the admins of the application requesting access?',
                        'No Access For Application ' + applicationName,
                        function() {
                            // Send the email
                            // Get the URL of the UI, which means we need to remove "api/v1/"
                            var indexOfEnd = ConfigFactory.baseUrl().lastIndexOf('/', ConfigFactory.baseUrl().lastIndexOf('/', ConfigFactory.baseUrl().length - 1) - 1);
                            var uiBaseUrl = ConfigFactory.baseUrl().substring(0, indexOfEnd + 1) + '#/userAccess/';
                            var emailLinks = [
                                'For Read-only Access: ' + uiBaseUrl + Session.userID + '/' + applicationName + '/READONLY',
                                'For Read/Write Access: ' + uiBaseUrl + Session.userID + '/' + applicationName + '/READWRITE',
                                'For Admin Access: ' + uiBaseUrl + Session.userID + '/' + applicationName + '/ADMIN'
                            ];
                            EmailFactory.sendEmail({
                                'appName': applicationName,
                                'userName': Session.userID,
                                'emailLinks': emailLinks
                            }).$promise.then(function () {
                                UtilitiesFactory.trackEvent('saveItemSuccess',
                                    {key: 'dialog_name', value: 'experimentModalSendEmail'},
                                    {key: 'experiment_id', value: Session.userID});

                                var message = 'An access request email has been sent to the admins of application ' + applicationName + '.';
                                DialogsFactory.alertDialog(message, 'Email Sent', function() {/* nothing to do */});
                                $scope.experiment.applicationName2 = '';
                            }, function(response) {
                                UtilitiesFactory.handleGlobalError(response, 'Your email could not be sent.');
                            });
                        },
                        function() {
                            $scope.experiment.applicationName2 = '';
                        },
                        'Send Email',
                        'Cancel'
                    );
                };

                $scope.openSegmentationTestModal = function () {
                    var modalInstance = $uibModal.open({
                        templateUrl: 'views/SegmentationTestModal.html',
                        controller: 'SegmentationTestModalCtrl',
                        windowClass: 'xxx-dialog',
                        backdrop: 'static',
                        resolve: {
                            experiment: function () {
                                return $scope.experiment;
                            },
                            rules: function () {
                                return $scope.rules;
                            }
                        }
                    });

                    modalInstance.result.then(function () {
                    });
                };

                $scope.addTabs = function() {
                    if ($rootScope.plugins && $rootScope.plugins.length > 0) {

                    }
                };

                $scope.openPluginModal = function(plugin) {
                    UtilitiesFactory.openPluginModal(plugin, $scope.experiment);
                };

            }]);
