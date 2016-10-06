/*global $:false */
/*global moment:false */

'use strict';

// The next global is used to cancel the fade out, if necessary, like if we are displaying another
// page success message before the first one has faded out.
var globalPageSuccessMessageFadeOutTimer = null;

angular.module('wasabi.services').factory('UtilitiesFactory', ['Session', '$state', 'AuthFactory', '$rootScope', 'AUTH_EVENTS', 'PERMISSIONS', 'USER_ROLES', '$filter', 'AuthzFactory', 'BucketsFactory', 'DialogsFactory', 'ExperimentsFactory', 'WasabiFactory', '$modal', '$injector', 'FavoritesFactory', 'StateFactory',
    function (Session, $state, AuthFactory, $rootScope, AUTH_EVENTS, PERMISSIONS, USER_ROLES, $filter, AuthzFactory, BucketsFactory, DialogsFactory, ExperimentsFactory, WasabiFactory, $modal, $injector, FavoritesFactory, StateFactory) {
        return {
            // generate state image url
            stateImgUrl: function (state) {
                return (state ? 'images/status_' + this.stateName(state.toLowerCase()) + '.png' : '');
            },

            // This allows us to change the name of the state versus what it is called in the backend.
            stateName: function (state) {
                if (!state) return '';
                var stateLabel = state.toLowerCase();
                if (stateLabel === 'paused') {
                    stateLabel = 'stopped';
                }
                return stateLabel;
            },

            capitalizeFirstLetter: function (string) {
                return string.charAt(0).toUpperCase() + string.toLowerCase().slice(1);
            },

            arrayToString: function (string) {
                return string.charAt(0).toUpperCase() + string.toLowerCase().slice(1);
            },

            hideAdminTabs: function(flag) {
                var hideThem = (flag != undefined ? flag : true);
                if (!hideThem) {
                    this.hideTopLevelTab('Users', false);
                    this.hideTopLevelTab('Applications', false);
                    this.hideTopLevelTab('Logs', false);
                    this.hideTopLevelTab('Feedback', !Session.isSuperadmin);
                }
                else {
                    this.hideTopLevelTab('Users');
                    this.hideTopLevelTab('Applications');
                    this.hideTopLevelTab('Logs');
                    this.hideTopLevelTab('Feedback', true);
                }
            },

            selectTopLevelTab: function(label, perms) {
                if (!this.hasAdmin((this._isTesting === true ? perms : Session.permissions))) {
                    this.hideAdminTabs();
                    if (label === 'Users' || label === 'Applications') {
                        label = 'Experiments';
                    }
                }
                else {
                    this.hideAdminTabs(false);
                }
                if (label.toLowerCase() !== 'experiments') {
                    localStorage.removeItem('wasabiLastSearch'); // Clear the remembered search value
                    // Clear the remembered page value
                    StateFactory.currentExperimentsPage = 1;
                    StateFactory.currentCardViewPage = 1;
                }
                $('.main li').removeClass('sel');
                $('.main li a:contains(' + label + ')').parent().addClass('sel');
                $('#welcomeMsg').text('Welcome, ' + Session.userID).css('display', 'inline');
            },

            hideTopLevelTab: function(label, hideIt) {
                var hideTheTab = (hideIt !== undefined ? hideIt : true);
                if (hideTheTab) {
                    $('.main li a:contains(' + label + ')').parent().hide();
                }
                else {
                    $('.main li a:contains(' + label + ')').parent().show();
                }
            },

            deselectAllTabs: function() {
                $('.main li').removeClass('sel');
            },

            displayPageError: function(title, message, show) {
                var showError = (show === undefined || show);
                if (!showError) {
                    $('.pageError').hide();
                    return;
                }
                if ($('.pageError').length === 0) {
                    // Create the alert
                    $('body').append('<div class="pageError">' +
                            '        <h2>Alert</h2>' +
                            '        <span></span>' +
                            '        <a href="#" class="closePageError">Close</a><span class="icon"></span>' +
                            '    </div>');
                    $('.closePageError').on('click', function() {
                        $(this).parent().fadeOut(120);
                        return false;
                    });
                }
                $('.pageError h2').text(title);
                $('.pageError').find('span').eq(0).html(message);
                if ($('.pageError').is(':visible')) {
                    $('.pageError').fadeOut(120, function() {
                        $(this).fadeIn(250);
                    });
                }else {
                    $('.pageError').fadeIn();
                }
            },

            displayPageSuccessMessage: function(title, message, show) {
                var showSuccess = (show === undefined || show);
                if (!showSuccess) {
                    $('.pageSuccess').hide();
                    return;
                }
                if ($('.pageSuccess').length === 0) {
                    // Create the alert
                    $('body').append('<div class="pageSuccess">' +
                            '        <h2>Alert</h2>' +
                            '        <span></span>' +
                            '        <a href="#" class="closePageSuccess">Close</a><span class="icon"></span>' +
                            '    </div>');
                    $('.closePageSuccess').on('click', function() {
                        $(this).parent().fadeOut(120);
                        return false;
                    });
                }
                $('pageSuccess').stop(); // If it happens to be fading out, stop it.
                if (globalPageSuccessMessageFadeOutTimer != null) {
                    clearTimeout(globalPageSuccessMessageFadeOutTimer);
                }
                $('.pageSuccess h2').text(title);
                $('.pageSuccess').find('span').eq(0).html(message);
                if ($('.pageSuccess').is(':visible')) {
                    $('.pageSuccess').fadeOut(120, function() {
                        $(this).fadeIn(250);
                    });
                }else {
                    $('.pageSuccess').fadeIn();
                }

                // Fade the message out after 1.5 seconds.
                globalPageSuccessMessageFadeOutTimer = setTimeout(function() {
                    $('.pageSuccess').fadeOut(1500);
                    globalPageSuccessMessageFadeOutTimer = null;
                }, 6000);
            },

            hideHeading: function(flag) {
                if (flag) {
                    $('header').hide();
                }
                else {
                    $('header').show();
                }
            },

            // This allows us to show errors using an alert when the error is occurring when we don't have a form
            // in which we can highlight and display the error.
            handleGlobalError: function(response, defaultMessage) {
                // console.log(response);
                switch (response.status) {
                    case 400:
                        this.displayPageError('Error: ' + response.status, response.data.error.message);
                        break;
                    case 401:
                        // Detected that the authentication ticket has expired.
                        // Kill the session, and redirect to the Sign In page.
                        Session.destroy();
                        $state.go('signin');
                        break;
                    default:
                        var msg = ')';
                        if (response.data && response.data.error && response.data.error.message) {
                            msg = ', message: ' + response.data.error.message + ')';
                        }
                        this.displayPageError('Error', defaultMessage + ' (code: ' + response.status + msg);
                }
            },

            extractErrorFromResponse: function(response) {
                var errorCode = null;
                if (response.data && response.data.error && response.data.error.message && response.data.error.message.length > 0) {
                    errorCode = response.data.error.message;
                }
                switch (response.status) {
                    // We should have the error code in a JSON structure in the response.
                    case 400:
                        if (errorCode.indexOf('unique constraint violation') >= 0 /* Unique Constraint Violation */) {
                            return 'nonUnique';
                        }
                        // The "Invalid rule" happens when you have a bad rule, e.g.: name = "abc" & xyz
                        // The "Invalid condition format" happens when you do something like make the parameter name part of
                        // the rule "false", e.g., the entire rule is:  false = "abc"
                        else if (errorCode.indexOf('Invalid rule') >= 0 || errorCode.indexOf('Invalid condition format') >= 0) {
                            return 'invalidRule';
                        }
                        else {
                            return 'genericSubmitError';
                        }
                        break;
                    case 401:
                        // Detected that the authentication ticket has expired.  Notify the dialogs to close,
                        // kill the session, and redirect to the Sign In page.
                        $rootScope.$broadcast(AUTH_EVENTS.notAuthenticated);
                        Session.destroy();
                        $state.go('signin');
                        return 'unauthenticated';
                    default:
                        return 'genericSubmitError';
                }
            },

            failIfTokenExpired: function(modalInstance) {
                AuthFactory.verifyToken().$promise.then(function(/*result*/) {
                    // If it worked, we don't need to do anything.
                }, function(/*reason*/) {
                    // If it failed, assume the ticket has expired.  We need to
                    // close the modal dialog, if modalInstance was passed.
                    if (modalInstance) {
                        modalInstance.close();
                    }
                    // Broadcast that we have detected an expired ticket.  One use of this is it is
                    // listened to by a modal dialog that will close itself when it receives this.
                    $rootScope.$broadcast(AUTH_EVENTS.notAuthenticated);
                    // Finally, get rid of the session and show the Sign In page.
                    Session.destroy();
                    $state.go('signin');
                });
            },
            
            trackEvent: function(eventName, parm1, parm2, parm3, parm4) {
                // If you have implemented a contributeClickTracking plugin, this will call the trackEvent() function
                // on it, if it exists.
                if ($rootScope.plugins) {
                    var trackingPlugin = this.getTrackingPlugin($rootScope.plugins);
                    if (trackingPlugin && trackingPlugin.hasOwnProperty('trackEvent')) {
                        try {
                            trackingPlugin.trackEvent(eventName, parm1, parm2, parm3, parm4);
                        } catch(e) {
                            console.log('Problem with the tracking plugin trackEvent function');
                        }
                    }
                }
            },

            doTrackingInit: function() {
                // If you have implemented a contributeClickTracking plugin, this will call the reset() function on it, if it exists.
                if ($rootScope.plugins) {
                    var trackingPlugin = this.getTrackingPlugin($rootScope.plugins);
                    if (trackingPlugin && trackingPlugin.hasOwnProperty('reset')) {
                        try {
                            trackingPlugin.reset();
                        } catch(e) {
                            console.log('Problem with the tracking plugin reset function');
                        }
                    }
                }
            },

            getTrackingPlugin: function(plugins) {
                if (plugins) {
                    var filtered = plugins.filter(function(e, i, arr) {
                        return (e.hasOwnProperty('pluginType') && e.pluginType === 'contributeClickTracking');
                    });
                    if (filtered && filtered.length > 0) {
                        return tracking;
                    }
                }
                return null;
            },

            formatApplicationList: function(applications) {
                if (!applications || applications.length === 0) {
                    return '';
                }

                var displayString = '';
                applications.forEach(function(value) {
                    if (displayString.length !== 0) {
                        displayString += ',';
                    }
                    displayString += value.label;
                });
                return displayString;
            },

            filterAppsForUser: function(administeredApplications, usersApplications) {
                // Remove applications user has privileges for from administered applications.

                // First, clone the array of apps the logged in user can admin.
                var appsThatCanBeAdded = administeredApplications.map(function(currentValue) {
                    return $.extend({}, currentValue);
                });

                // Now go through the applications this user already has permissions for and remove them from
                // the apps that can be added.
                var tmpArray = null;
                usersApplications.forEach(function(value) {
                    tmpArray = $.map(appsThatCanBeAdded, function(val) {
                        if(val.label === value.label ) {
                            return null;
                        }
                        return val;
                    });
                    appsThatCanBeAdded = tmpArray;
                });
                return appsThatCanBeAdded;
            },

            hasPermission: function(applicationName, permission) {
                var hasPermission = false;

                if (Session && Session.isSuperadmin) {
                    return true;
                }

                if (Session && Session.permissions) {
                    // If we have just signed out, we can be in the state where we have no Session.
                    Session.permissions.forEach(function(nextPermissions) {
                        if (nextPermissions.applicationName === applicationName) {
                            hasPermission = (nextPermissions.permissions.indexOf(permission.toUpperCase()) >= 0);
                        }
                    });
                }
                return hasPermission;
            },

            hasAdmin: function(permissions) {
                var hasAdminPermission = false;
                if (permissions) {
                    permissions.forEach(function(item) {
                        if (item.hasOwnProperty('permissions')) {
                            item.permissions.forEach(function(nextPermission) {
                                if (nextPermission.toUpperCase() === PERMISSIONS.adminPerm) {
                                    hasAdminPermission = true;
                                }
                            });
                        }
                    });
                }
                return hasAdminPermission;
            },

            getAppsWithAnyPermissions: function() {
                var applicationNames = [];
                if (Session.permissions instanceof Array) {
                    Session.permissions.forEach(function(nextPermissions) {
                        applicationNames.push(nextPermissions.applicationName);
                    });
                    return applicationNames.sort(function (a, b) {
                        return a.toLowerCase().localeCompare(b.toLowerCase());
                    });
                } else {
                    return [];
                }
            },

            // Get the list of applications that the currently logged in user can administer by looking at their
            // list of permissions.
            getAdministeredApplications: function() {
                var appList = [];
                if (Session && Session.permissions) {
                    Session.permissions.forEach(function(nextPermissions) {
                        if (nextPermissions.permissions.indexOf(PERMISSIONS.adminPerm) >= 0) {
                            appList.push({
                                label: nextPermissions.applicationName
                            });
                        }
                    });
                }
                appList.sort(function (a, b) {
                    return a.label.toLowerCase().localeCompare(b.label.toLowerCase());
                });

                return appList;
            },

            userHasRole: function(user, privName) {
                var hasPrivs = false;
                if (user.applications) {
                    user.applications.forEach(function(app) {
                        if (privName === 'write') {
                            if (app.role === 'READWRITE' || app.role === 'ADMIN') {
                                hasPrivs = true;
                            }
                        }
                        if (privName === 'admin') {
                            if (app.role === 'ADMIN') {
                                hasPrivs = true;
                            }
                        }
                    });
                }
                return hasPrivs;
            },

            userHasRoleForApplication: function(user, appName, privName) {
                var hasPrivs = false;
                if (user.applications) {
                    user.applications.forEach(function(app) {
                        if (app.label === appName) {
                            if (privName === 'write') {
                                if (app.role === 'READWRITE' || app.role === 'ADMIN') {
                                    hasPrivs = true;
                                }
                            }
                            if (privName === 'admin') {
                                if (app.role === 'ADMIN') {
                                    hasPrivs = true;
                                }
                            }
                        }
                    });
                }
                return hasPrivs;
            },

            sortByHeadingValue: function(filteredItems, orderByField, reverseSort) {
                var orderBy = orderByField, that = this;
                if (orderByField === 'admin' || orderByField === 'write') {
                    // These are not simple properties, so the orderBy filter needs to have a function.
                    orderBy = function(nextUser) {
                        return !that.userHasRole(nextUser, orderByField /* which happens to be the role name */);
                    };
                }
                else if (orderByField === 'applications') {
                    orderBy = function(nextUser) {
                        return !that.formatApplicationList(nextUser.applications);
                    };
                }
                return $filter('orderBy')(filteredItems, orderBy, reverseSort);
            },

            actionRate: function (bucketLabel, buckets) {
                if (!buckets[bucketLabel] || isNaN(buckets[bucketLabel].jointActionRate.estimate)) {
                    return 0;
                } else {
                    var rate = buckets[bucketLabel].jointActionRate.estimate * 100;
                    return (Math.round(rate * 10) / 10);
                }
            },

            actionDiff: function (bucketLabel, buckets) {
                if (isNaN(buckets[bucketLabel].jointActionRate.estimate)) {
                    return 0;
                } else {
                    var diff = (((buckets[bucketLabel].jointActionRate.upperBound -
                        buckets[bucketLabel].jointActionRate.lowerBound) / 2) * 100);
                    return (Math.round(diff * 10) / 10);
                }
            },

            improvement: function (bucketLabel, experiment) {
                if (experiment.controlBucketLabel === bucketLabel) {
                    // for baseline bucket or control bucket, the UI shows 'N/A'
                    return undefined;
                } else {
                    for (var comparison in experiment.statistics.buckets[bucketLabel].bucketComparisons) {
                        if (experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].otherLabel === experiment.controlBucketLabel) {
                            if (isNaN(experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].jointActionComparison.actionRateDifference.estimate)) {
                                return 0;
                            } else {
                                var improvement = (experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].jointActionComparison.actionRateDifference.estimate * 100);
                                return (Math.round(improvement * 10) / 10);
                            }
                        }
                    }
                }
            },

            improvementDiff: function (bucketLabel, experiment) {
                if (experiment.controlBucketLabel === bucketLabel) {
                    // for baseline bucket or control bucket, the UI shows 'N/A'
                    return undefined;
                } else {
                    for (var comparison in experiment.statistics.buckets[bucketLabel].bucketComparisons) {
                        if (experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].otherLabel === experiment.controlBucketLabel) {
                            if (isNaN(experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].jointActionComparison.actionRateDifference.estimate)) {
                                return 0;
                            } else {
                                var improvementDiff = (experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].jointActionComparison.actionRateDifference.upperBound -
                                    experiment.statistics.buckets[bucketLabel].bucketComparisons[comparison].jointActionComparison.actionRateDifference.lowerBound) * 100 / 2;
                                return (Math.round(improvementDiff * 10) / 10);
                            }
                        }
                    }
                }
            },

            significance: function (bucketLabel, statistics) {
                if ($.inArray(bucketLabel, statistics.jointProgress.winnersSoFar) !== -1) {
                    return 'winner so far';
                } else if ($.inArray(bucketLabel, statistics.jointProgress.losersSoFar) !== -1) {
                    return 'loser so far';
                } else {
                    return 'undetermined';
                }
            },

            saveFavorite: function (experimentID, favoritesObj) {
                var that = this;
                FavoritesFactory.create({
                    'id': experimentID
                }).$promise.then(function (results) {
                    if (results && results.experimentIDs) {
                        favoritesObj.favorites = results.experimentIDs;
                    }
                    that.trackEvent('saveItemSuccess',
                        {key: 'dialog_name', value: 'createFavorite'},
                        {key: 'experiment_id', value: experimentID}
                    );
                },
                function(response) {
                    that.handleGlobalError(response, 'The favorite could not be created.');
                });
            },

            removeFavorite: function (experimentID, favoritesObj) {
                var that = this;
                FavoritesFactory.delete(
                    {
                        'id': experimentID
                    }
                ).$promise.then(function (results) {
                    if (results && results.experimentIDs) {
                        favoritesObj.favorites = results.experimentIDs;
                    }
                    that.trackEvent('saveItemSuccess',
                        {key: 'dialog_name', value: 'deleteFavorite'},
                        {key: 'experiment_id', value: experimentID}
                    );
                },
                function(response) {
                    that.handleGlobalError(response, 'The favorite could not be delete.');
                });
            },

            retrieveFavorites: function () {
                return FavoritesFactory.query();
            },

            getBucket: function (bucketLabel, experiment) {
                if (experiment && experiment.buckets) {
                    for (var i = 0; i < experiment.buckets.length; i++) {
                        var bucket = experiment.buckets[i];
                        if (bucket.label === bucketLabel) {
                            return bucket;
                        }
                    }
                }
                return null;
            },

            getFormattedStartAndEndDates: function(experiment) {
                var start = moment(experiment.startTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']);
                var end = moment(experiment.endTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']);
                experiment.formattedStart = start;
                experiment.formattedEnd = end;
            },

            rapidExperimentLabel: function(experiment) {
                return (!experiment.isRapidExperiment ? 'N/A' : experiment.userCap);
            },

            determineBucketImprovementClass: function(experiment, controlBucketLabel) {
                var that = this;
                // Set up so we know whether a given bucket is a winner, loser or not sure.
                var theBuckets = [];
                for (var bucketLabel in experiment.statistics.buckets) {
                    // As long as we're going through all the buckets in the statistics object, let's set the tooltip.
                    var matchingBucket = this.getBucket(bucketLabel, experiment);
                    if (matchingBucket) {
                        experiment.statistics.buckets[bucketLabel].homePageTooltip = matchingBucket.homePageTooltip;
                    }

                    // We need the buckets to be an array so we can sort it.
                    theBuckets.push(experiment.statistics.buckets[bucketLabel]);
                    // Find the matching bucket in the experiment bucket list, which has the count from
                    // the application statistics call on it, and put the count in the sorted bucket so
                    // it will be available for the list.
                    for (var i = 0; i < experiment.buckets.length; i++) {
                        if (experiment.buckets[i].label === bucketLabel) {
                            experiment.statistics.buckets[bucketLabel].count = experiment.buckets[i].count;
                        }
                    }

                }

                experiment.statistics.sortedBuckets = $filter('orderBy')(theBuckets, function(bucket) {
                    return that.actionRate(bucket.label, experiment.statistics.buckets);
                }, true);

                // We now have, in experiment.statistics.sortedBuckets, the buckets with the home page tooltips
                // set up, the count for each bucket added, and sorted by the actionRate.

                // Now we need to go through the sorted list of buckets and look for winnersSoFar and losersSoFar
                // so we can set the icons displayed in the bucket list. We use the jointProgress section of the
                // statistics results and look for bucket names in the winnersSoFar and losersSoFar.  If a bucket
                // is in winnersSoFar, we mark it with a trophy.  If it is in losersSoFar, we mark it as a loser.
                // Otherwise, we mark it as indeterminate.
                // NOTE: We won't do any of this until 7 days after the experiment start date.
                var numWinningBuckets = 0,
                    lastWinningBucketIndex = -1;

                experiment.statistics.sortedBucketsActiveOnly = []; // Will be used for Card View, only OPEN state buckets.
                if (experiment.statistics.sortedBuckets && experiment.statistics.sortedBuckets.length > 0) {
                    var foundAWinner = false;
                    for (var i = 0; i < experiment.statistics.sortedBuckets.length; i++) {
                        if (this.getBucket(experiment.statistics.sortedBuckets[i].label, experiment).state === 'OPEN') {
                            // Save in another array that is used to build the Card View, as we don't want closed or emptied
                            // buckets there.  Both arrays will point to the same objects, so all the stuff we do below
                            // will be there for the Card View, too.
                            experiment.statistics.sortedBucketsActiveOnly.push(experiment.statistics.sortedBuckets[i]);
                        }
                        if (!moment().subtract(7, 'days').isAfter(moment(experiment.startTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']))) {
                            // If the start time of the experiment is less than 7 days ago, don't check for winners or losers, yet.
                            experiment.statistics.buckets[experiment.statistics.sortedBuckets[i].label].toolTip = 'There is an insufficient number of users to identify a winning variation.';
                            continue;
                        }
                        var significance = this.significance(experiment.statistics.sortedBuckets[i].label, experiment.statistics);

                        if (significance === 'winner so far') {
                            foundAWinner = true;
                            // This bucket is a winner against at least one other bucket.
                            experiment.statistics.buckets[experiment.statistics.sortedBuckets[i].label].improvementClass = 'winner';
                            experiment.statistics.buckets[experiment.statistics.sortedBuckets[i].label].toolTip = 'This bucket has shown the best performance of all variations.  Consider switching to this experience.';
                            numWinningBuckets += 1;
                            if (numWinningBuckets > 1) {
                                // Multiple buckets are winners.  Different tooltip for both this one and the others.
                                experiment.statistics.buckets[experiment.statistics.sortedBuckets[i].label].toolTip =
                                    experiment.statistics.buckets[experiment.statistics.sortedBuckets[lastWinningBucketIndex].label].toolTip =
                                    'You have multiple buckets that performed best.  Consider a deeper analysis prior to switching to an experience.';
                            }
                            lastWinningBucketIndex = i;
                        }
                        else if (significance === 'loser so far') {
                            experiment.statistics.buckets[experiment.statistics.sortedBuckets[i].label].improvementClass = 'loser';
                            experiment.statistics.buckets[experiment.statistics.sortedBuckets[i].label].toolTip = 'This bucket has not shown the best performance of all variations.';
                        }
                        else {
                            experiment.statistics.buckets[experiment.statistics.sortedBuckets[i].label].improvementClass = 'indeterminate';
                            experiment.statistics.buckets[experiment.statistics.sortedBuckets[i].label].toolTip = 'This bucket\'s performance is not statistically distinguishable from other variations.';
                        }
                    }
                    if (!foundAWinner) {
                        // There was no winner.  Need to set improvement class so we can left shift the buckets.
                        for (var i = 0; i < experiment.statistics.sortedBuckets.length; i++) {
                            experiment.statistics.buckets[experiment.statistics.sortedBuckets[i].label].improvementClass = 'no-winner';
                        }
                    }
                }
            },

            filterNonExpiredExperiments: function(experiments) {
                return $filter('filter')(experiments, function (item) {
                    var expDate = moment(item.endTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']),
                        now = moment();
                    return (item.state === 'PAUSED' || item.state === 'RUNNING') || !expDate.isBefore(now);
                });
            },

            ruleValueValidationError: function(ruleType) {
                var tooltipText = 'Enter a valid number';
                switch (ruleType) {
                    case 'boolean':
                        tooltipText = 'Enter true or false';
                        break;
                    case 'date':
                        tooltipText = 'Enter YYYY-MM-DD';
                        break;
                    case 'string':
                        tooltipText = 'Enter a quoted string';
                        break;
                }
                return tooltipText;
            },

            validateRuleChunk: function(ruleValue, ruleType) {
                var regexObj = /[0-9]{4}-(0|1)[0-9]-[0-3][0-9]/,
                    isValid = false;

                switch (ruleType) {
                    case 'number':
                        isValid = $.isNumeric(ruleValue);
                        break;
                    case 'boolean':
                        isValid = (ruleValue === true || ruleValue === false ||
                                   (typeof ruleValue === 'string' && (ruleValue.toLowerCase() === 'true' || ruleValue.toLowerCase() === 'false')));
                        break;
                    case 'date':
                        isValid = regexObj.test(ruleValue);
                        break;
                    case 'string':
                        // Do a simple quoted string validation
                        if (ruleValue.length >= 3 &&
                            (ruleValue[0] === ruleValue[ruleValue.length-1]) &&
                            (ruleValue[0] === "'" || ruleValue[0] === '"')) {
                            // Validate that any of the same quotes within the string are escaped.
                            var foundBadQuote = false;
                            for (var i = 1; i < ruleValue.length - 1; i++) {
                                if (ruleValue[i] === ruleValue[0]) {
                                    // We have a case where the string is correctly quoted with either ' or ", but
                                    // there are the same quote within the string.
                                    foundBadQuote = true;
                                    break;
                                }
                            }
                            isValid = !foundBadQuote;
                        }
                        else if (ruleValue.length === 2 &&
                            (ruleValue === '""' || ruleValue === "''")) {
                            // We can allow empty string checks.
                            isValid = true;
                        }
                        break;
                }
                return { isValid: isValid, tooltipText: this.ruleValueValidationError(ruleType)};
            },

            firstPageEncoded: function(experiment) {
                if (experiment && experiment.pages && experiment.pages.length > 0) {
                    return encodeURIComponent(experiment.pages[0].name);
                }
                return '';
            },

            updatePermissionsAndAppList: function(updateApplicationListCallback) {
                var that = this,
                    applications = [];

                AuthzFactory.getPermissions({userId: Session.userID}).$promise.then(function(permissionsResult) {
                    Session.update({'permissions': permissionsResult.permissionsList});

                    applications = [];
                    Session.permissions.forEach(function(nextPermissions) {
                        if (that.hasPermission(nextPermissions.applicationName, PERMISSIONS.createPerm)) {
                            applications.push(nextPermissions.applicationName);
                        }
                    });
                    if (applications.length > 1) {
                        // Sort them so they show up that way in the modal select menu.
                        applications.sort(function (a, b) {
                            return a.toLowerCase().localeCompare(b.toLowerCase());
                        });
                    }

                    if (updateApplicationListCallback) {
                        updateApplicationListCallback(applications);
                    }

                    if (that.hasAdmin(permissionsResult.permissionsList)) {
                        Session.update({'userRole': USER_ROLES.admin});
                        that.hideAdminTabs(false);
                    }
                },
                function(/*reason*/) {
                    console.log('Problem getting authorization permissions.');
                });
            },

            balanceBuckets: function(buckets, experiment) {
                if (buckets && buckets.length > 0) {
                    // Using the number of buckets, allocate each an even percent of 100%.  If there is not an even
                    // distribution, e.g., for three buckets, go to 2 decimal places and add the necessary amount
                    // to even out to the last one.
                    var numBuckets = 0,
                        perBucketAllocation = 0,
                        notEven = false,
                        tempBucketAllocations = [],
                        lastValidBucketIndex = 0,
                        that = this;

                    // Determine the non-closed and non-emptied buckets total.
                    for (var j = 0; j < buckets.length; j++) {
                        if (buckets[j].state !== 'CLOSED' && buckets[j].state !== 'EMPTY') {
                            numBuckets++;
                        }
                    }
                    perBucketAllocation = parseFloat((1.00 / numBuckets).toFixed(4));
                    notEven = (1.0 - (perBucketAllocation * numBuckets)) !== 0.0;
                    for (var i = 0; i < buckets.length; i++) {
                        if (buckets[i].state !== 'CLOSED' && buckets[i].state !== 'EMPTY') {
                            tempBucketAllocations[i] = perBucketAllocation;
                            lastValidBucketIndex = i;
                        }
                        else {
                            tempBucketAllocations[i] = buckets[i].allocationPercent; // Should be zero
                        }
                    }
                    if (notEven) {
                        var delta = parseFloat((1.0 - (perBucketAllocation * numBuckets)).toFixed(4));
                        tempBucketAllocations[lastValidBucketIndex] += delta;
                    }

                    DialogsFactory.confirmDialog('Are you sure you want to change the bucket allocations so they are all the same?', 'Confirm Bucket Balancing',
                            function() {
                                var bucketAllocations = [];
                                for (var i = 0; i < tempBucketAllocations.length; i++) {
                                    bucketAllocations.push({
                                        'label': buckets[i].label,
                                        'allocationPercent': tempBucketAllocations[i]
                                    });
                                    buckets[i].allocationPercent = tempBucketAllocations[i];
                                }

                                if (bucketAllocations.length) {
                                    BucketsFactory.updateList({
                                        'experimentId': experiment.id,
                                        'buckets': bucketAllocations
                                    }).$promise.then(function (/*response*/) {
                                        that.trackEvent('saveItemSuccess',
                                            {key: 'dialog_name', value: 'balanceBucketAssignments'},
                                            {key: 'experiment_id', value: experiment.id});
                                    }, function(response) {
                                        // Handle error
                                        that.handleGlobalError(response);
                                    });
                                }
                            },
                            function() {/* Don't balance the buckets */});
                }
                return false;
            },

            totalBucketAllocation: function(localScope) {
                var total = 0;
                if (localScope.experiment.buckets && localScope.experiment.buckets.length > 0) {
                    for (var i = 0; i < localScope.experiment.buckets.length; i++) {
                        // We do some special manipulation here because floating arithmetic doesn't work
                        // at this precision.
                        total += Math.round(localScope.experiment.buckets[i].allocationPercent * 10000);
                    }
                }
                localScope.bucketTotalsValid = (total === 10000);
                return total / 10000;
            },

            // This function assumes you are trying to move the field with a name of valueLabelName
            // from objects in the source array to the target array.  The source and target objects from
            // which the value is to be moved from and to are matched using the sourceLabelName and
            // targetLabelName.  For example, if you want to move the "count" attribute from one array to the
            // other, where the objects can be matched by the "label" attribute in target and the "bucket"
            // attribute in the source, you'd call this function as:
            // transferMatchingCounts(target, source, 'count', 'label', 'bucket')
            // NOTE: One assumption is that the default value for the value attribute is 0.
            transferMatchingValues: function(target, source, valueLabelName, targetLabelName, sourceLabelName) {
                $.each(target, function(i, val) {
                    val[valueLabelName] = 0;
                    for (var j = 0; j < source.length; j++) {
                        // Find the matching assignment total for tue current bucket
                        if (val[targetLabelName] === source[j][sourceLabelName]) {
                            val[valueLabelName] = source[j][valueLabelName];
                            break;
                        }
                    }
                });
                return target;
            },

            getControlBucketLabel: function(buckets, experiment) {
                // get the label of the one control bucket (if any)
                for (var bucket in buckets) {
                    if (buckets[bucket].isControl) {
                        experiment.hasControlBucket = true;
                        experiment.controlBucketLabel = buckets[bucket].label;
                    }
                }

                // set baseline bucket (if no control bucket)
                if (!experiment.hasControlBucket) {
                    experiment.controlBucketLabel = buckets[0].label;
                    buckets[0].isBaseLine = true;
                }
            },

            convertValuesFromPre: function(htmlSource) {
                var ce = $('<pre />').html(htmlSource);
                $.browser = {};
                $.browser.msie = /msie/.test(navigator.userAgent.toLowerCase());
                $.browser.mozilla = /firefox/.test(navigator.userAgent.toLowerCase());
                $.browser.webkit = /webkit/.test(navigator.userAgent.toLowerCase()) || /chrome/.test(navigator.userAgent.toLowerCase());
                if($.browser.webkit)
                    ce.find("div").replaceWith(function() { return "\n" + this.innerHTML; });
                if($.browser.msie)
                    ce.find("p").replaceWith(function() { return this.innerHTML  +  "<br>"; });
                if($.browser.mozilla || $.browser.opera ||$.browser.msie )
                    ce.find("br").replaceWith("\n");

                return $.trim(ce.text());
            },

            changeState: function (experiment, state, afterUpdateFunction) {
                var stateChange = 'start',
                    that = this;
                switch (state.toLowerCase()) {
                    case 'paused':
                        stateChange = 'stop';
                        break;
                    case 'terminated':
                        stateChange = 'terminate';
                        break;
                }
                DialogsFactory.confirmDialog('Are you sure you want to ' + stateChange + ' the experiment ' + experiment.label + '?', 'Confirm State Change',
                        function() {
                            // Let the state change go through
                            ExperimentsFactory.update({id: experiment.id, state: state}).$promise.then(function () {
                                that.trackEvent('changeItemStateSuccess',
                                    {key: 'dialog_name', value: 'changeExperimentState'},
                                    {key: 'experiment_id', value: experiment.id},
                                    {key: 'item_id', value: state});

                                if (afterUpdateFunction && afterUpdateFunction === Object(afterUpdateFunction) &&
                                    typeof afterUpdateFunction !== 'function') {
                                    // afterUpdateFunction is actually an object where the properties should be the
                                    // name of a state.  We should call the function associated with that property
                                    // only after we make a change to that state.
                                    if (afterUpdateFunction.hasOwnProperty(state)) {
                                        afterUpdateFunction[state](experiment);
                                    }
                                }
                                else {
                                    // Otherwise, it is a function to be called for all state changes.
                                    afterUpdateFunction();
                                }
                            }, function(response) {
                                that.handleGlobalError(response, 'The state of your experiment could not be changed.');
                            });
                        },
                        function() {/* Don't do the state change */});
            },

            deleteExperiment: function (experiment, afterDeleteFunction) {
                var that = this;
                DialogsFactory.confirmDialog('Delete experiment ' + experiment.applicationName + ', ' + experiment.label + '?', 'Delete Experiment', function() {
                    ExperimentsFactory.delete({id: experiment.id}).$promise.then(function () {
                        that.trackEvent('deleteItemSuccess',
                            {key: 'dialog_name', value: 'deleteExperiment'},
                            {key: 'experiment_id', value: experiment.id});

                        afterDeleteFunction();
                    }, function(response) {
                        that.handleGlobalError(response, 'Your experiment could not be deleted.');
                    });
                });
            },

            checkBooleanSwitch: function(switchName, associatedExperimentName, defaultValue, trueBucket, completedCallback, errorCallback) {
                // Initialize switch to default value
                Session.restore(); // Get from session storage.
                var switches = Session.switches;
                switches[switchName] = defaultValue;
                Session.update({ switches: switches });
                WasabiFactory.getAssignment({expName: associatedExperimentName, userId: Session.userID}).
                    $promise.then(function(result) {
                        if (result && (result.assignment || result.assignment === null)) {
                            // The null check is in case this is a negative case, e.g., the user being in the null bucket
                            // is significant.
                            switches[switchName] = (result.assignment === trueBucket);
                            Session.update({ switches: switches});
                        }
                        if (completedCallback) {
                            // Do whatever we should do when we've finished setting the switch, in case that is dependent on
                            // us having done so.
                            completedCallback();
                        }
                    },
                    function(/*reason*/) {
                        console.log('Problem getting Wasabi switches.');
                        if (errorCallback) {
                            // Do whatever we should do on error.
                            errorCallback();
                        }
                    });
            },


            loadExternalFile: function(filename, tagName, contentType) {
                var fileref = document.createElement(tagName);
                if (tagName === 'link') {
                    fileref.setAttribute('rel', 'stylesheet');
                    fileref.setAttribute('href', filename);
                }
                else if (tagName === 'script') {
                    fileref.setAttribute('type', contentType);
                    fileref.setAttribute('src', filename);
                }
                document.getElementsByTagName('head')[0].appendChild(fileref);
            },

            openPluginModal: function (plugin, experiment) {
                var modalInstance = $modal.open({
                    templateUrl: plugin.templateUrl,
                    controller: plugin.ctrlName,
                    windowClass: 'xxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        experiment: function () {
                            return experiment;
                        }
                    }
                });

                modalInstance.result.then(function () {
                });

                return false;
            },

            openResultsModal: function (experiment, readOnly, afterResultsFunc) {
                var modalInstance = $modal.open({
                    templateUrl: 'views/ResultsModal.html',
                    controller: 'ResultsModalCtrl',
                    windowClass: 'xxx-dialog',
                    backdrop: 'static',
                    resolve: {
                        experiment: function () {
                            return experiment;
                        },
                        readOnly: function() {
                            return readOnly;
                        }
                    }
                });

                modalInstance.result.then(function () {
                    if (afterResultsFunc) {
                        afterResultsFunc();
                    }
                });
            }

        };
    }
]);
