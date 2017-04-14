'use strict';

angular.module('wasabi.controllers')
    .controller('SegmentationTestModalCtrl',
        ['$scope', '$filter', '$uibModalInstance', 'ApplicationsFactory', 'experiment', 'rules', 'UtilitiesFactory', '$uibModal', 'ConfigFactory', 'DialogsFactory',
            function ($scope, $filter, $uibModalInstance, ApplicationsFactory, experiment, rules, UtilitiesFactory, $uibModal, ConfigFactory, DialogsFactory) {

                $scope.experiment = experiment;
                $scope.rules = rules;

                $scope.help = ConfigFactory.help;

                $scope.ruleElements = [];

                if ($scope.rules && $scope.rules.length > 0) {
                    // Build the list of ruleElements that will be used to build the input form.
                    for (var i = 0; i < rules.length; i++) {
                        if (rules[i].subject.length === 0 ||
                            rules[i].value.length === 0) {
                            continue;
                        }
                        // If there are multiple rule elements with the same subject/name, only prompt for it once.
                        // NOTE: The value will only be provided in the profile below once, too.
                        var found = false;
                        for (var j = 0; j < $scope.ruleElements.length; j++) {
                            if ($scope.ruleElements[j].name === rules[i].subject) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            $scope.ruleElements.push({
                                name: rules[i].subject,
                                type: rules[i].type,
                                value: ''
                            });
                        }
                    }
                }

                $scope.doSegmentationTest = function () {
                    var profileValues = {'profile':{}}, val = null;
                    function doNothing() {
                        return false;
                    }
                    for (var i = 0; i < $scope.ruleElements.length; i++) {
                        var currentRuleElement = $scope.ruleElements[i];
                        if (currentRuleElement.name.length === 0) {
                            continue;
                        }
                        if (currentRuleElement.value.length === 0 &&
                            currentRuleElement.type !== 'string') {
                            // A test against the empty string is OK, but not the other types, just leave them off.
                            continue;
                        }
                        if (currentRuleElement.type === 'number') {
                            val = parseInt($scope.ruleElements[i].value);
                            if (isNaN(val)) {
                                DialogsFactory.alertDialog('The value for the profile parameter, ' + $scope.ruleElements[i].name + ', must be a number.', 'Invalid Number Value', doNothing);
                                return;
                            }
                            profileValues.profile[$scope.ruleElements[i].name] = val;
                        }
                        else if (currentRuleElement.type === 'boolean') {
                            val = $.trim($scope.ruleElements[i].value).toLowerCase();
                            if (val !== 'true' && val !== 'false') {
                                DialogsFactory.alertDialog('The value for the profile parameter, ' + $scope.ruleElements[i].name + ', must be true or false.', 'Invalid Boolean Value', doNothing);
                                return;
                            }
                            profileValues.profile[$scope.ruleElements[i].name] = ((typeof(val) === 'string' && val === 'true') || (typeof(val) === 'boolean' && val));
                        }
                        else {
                            profileValues.profile[$scope.ruleElements[i].name] = $.trim($scope.ruleElements[i].value);
                        }
                    }
                    profileValues.appName = $scope.experiment.applicationName;
                    profileValues.expName = $scope.experiment.label;
                    if ($scope.ruleElements.length) {
                        ApplicationsFactory.testRule(profileValues).$promise.then(function (response) {
                            UtilitiesFactory.trackEvent('saveItemSuccess',
                                {key: 'dialog_name', value: 'testSegmentationRule'},
                                {key: 'experiment_id', value: $scope.experiment.id});

                            if (response && response.result !== undefined) {
                                var message = response.result ?
                                    'The segmentation rule PASSES for these profile inputs.' :
                                    'The segmentation rule FAILS for these profile inputs.';
                                DialogsFactory.alertDialog(message, 'Rule Test', function() {/* nothing to do */});
                            }
                        }, function(response) {
                            // Handle error
                            UtilitiesFactory.handleGlobalError(response);
                        });
                    }
                    else {
                        $uibModalInstance.close();
                    }
                };

                $scope.cancel = function () {
                    // Changed this to call close() instead of dismiss().  We need to catch the Close or Cancel so
                    // we can re-load the buckets, in case the user changed the allocation percentages (since changes
                    // are data-bound to the model).
                    $uibModalInstance.close('cancel');
                };
            }]);
