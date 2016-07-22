/* global $:false */
/* global moment:false */
/*jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('ExperimentsCtrl', ['$scope', '$filter', '$http', '$timeout', 'ExperimentsFactory', '$modal', 'UtilitiesFactory', '$rootScope', 'StateFactory', 'DialogsFactory', 'AUTH_EVENTS', 'Session', 'PERMISSIONS', 'ConfigFactory', 'AuthzFactory', 'USER_ROLES', 'ApplicationsFactory', 'BucketsFactory', 'ExperimentStatisticsFactory', 'ApplicationStatisticsFactory',
        function ($scope, $filter, $http, $timeout, ExperimentsFactory, $modal, UtilitiesFactory, $rootScope, StateFactory, DialogsFactory, AUTH_EVENTS, Session, PERMISSIONS, ConfigFactory, AuthzFactory, USER_ROLES, ApplicationsFactory, BucketsFactory, ExperimentStatisticsFactory, ApplicationStatisticsFactory) {

             var today = moment().format('MM/DD/YYYY');

            // The data object is where values are stored that need to be data bound to the fields in the form.
            // I believe there was a scope problem and I found this solution on the Googles.  Basically, by
            // using the "data.xx" notation, you do a cleaner job of setting scope for the things that will be
            // bound from the form.  For the fields they are bound to, look at ExperimentTable.html .
            $scope.data = {
                lastSearchWasSimple: true,
                query: '',
                advStatus: 'notTerminated',
                advApplicationName: '',
                advExperimentName: '',
                advStartOrEndDate: 'startDate',
                adv1stDateSearchType: 'isAny',
                advTxtSearchDateOne: today,
                advTxtSearchDateTwo: today,
                showGrid: false,
                showAdvancedSearch: false,
                hideTerminated: true,
                enableCardView: false
            };

            // We save the $scope.data object above after a search so we have it when we come back to the list
            // from a Details dialog.  We don't want to save the pagedItems list, so putting it in a separate attribute.
            $scope.pagedData = {
                pagedItems: []
            };

            // sorting
            $scope.orderByField = 'applicationName';
            $scope.reverseSort = false;
            // pagination
            $scope.itemsPerPage = 10;
            $scope.groupedItems = [];
            $scope.filteredItems = [];
            $scope.currentPage = StateFactory.currentExperimentsPage;
            $scope.totalItems = $scope.filteredItems.length;
            $scope.hasAnyCreatePermissions = false;
            $scope.noExperiments = false;
            $scope.applicationsLoaded = false;

            $scope.experiments = [];
            $scope.applications = [];

            $scope.cardViewExperiments = [];
            $scope.cardViewTotalItems = 0;
            $scope.cardViewItemsPerPage = 8;

            $scope.cardViewData = {
                cardViewCurrentPage: StateFactory.currentCardViewPage
            };

            $scope.applicationsWithReadOrBetterAccess = [];
            $scope.allApplications = [];
            $scope.needDataForThese = [];
            $scope.disableShowAsGrid = false;
            $scope.initialGridsShown = 12;
            $scope.gridsShown = 0; // Tracks how many experiments are being shown in the grid view (lazy loading) out of the possible total from the filteredItems list.
            $scope.gridDataLoaded = 0; // Tracks for how many of the experiments shown in the grid view we have finished getting data (or got an error doing so)

            $scope.help = ConfigFactory.help;

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
                            }
                        }
                    }
                }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of applications could not be retrieved.');
                });
            };

            // *** Home page code

            $scope.moveExperiment = function(nextFave, tmpExperiments, experiments) {
                // Find this one in tmpExperiments
                var pos = tmpExperiments.map(function(e) { return e.applicationName + '|' + e.label; }).indexOf(nextFave);
                if (pos >= 0) {
                    // The item in the favorites cookie exists in the experiments list,
                    // remove the matching item from tmpExperiments and add it to $scope.experiments
                    experiments.push(tmpExperiments.splice(pos, 1)[0]);
                }
                return experiments;
            };

            $scope.actionRate = function(bucketLabel, buckets) {
                return UtilitiesFactory.actionRate(bucketLabel, buckets);
            };

            $scope.actionDiff = function(bucketLabel, buckets) {
                return UtilitiesFactory.actionDiff(bucketLabel, buckets);
            };

            /*
             The experiment is assumed to either have a control, marked in the buckets list, or we use the first bucket
             as the control (baseline).  That has already been marked in the buckets list in the experiment by saving
             controlBucketLabel. All concept of improvement is relative to this control/baseline bucket and it's action rate value.
             If the action rate of a bucket is greater than the action rate of the control/baseline, then it will have a
             class of "winning", and the control/baseline will have a class of "losing", as will any other buckets who
             have an action rate at or below the control/baseline.  If the control/baseline is
             the highest action rate, it will have a class of 'winning' and the other buckets will have a class of 'losing'.
             */
            $scope.improvementClass = function(bucketLabel, experiment) {
                return experiment.statistics.buckets[bucketLabel].improvementClass;
            };

            $scope.doneLoading = false; // Used so we don't keep calling this function once we've loaded everything.
            // This function is called by the WhenScrollEndsDirective when it detects we are near the bottom of scrolling
            // the grid view.  It adds more experiments from the filteredItems list to the gridItems list and they are
            // then displayed by Angular JS.  We will also call a method to start the process of loading the data for
            // the newly added experiments.
            $scope.loadMoreExperimentData = function() {
                if (!$scope.doneLoading) {
                    $scope.gridsShown = ($scope.filteredItems.length < $scope.gridsShown + 4 ? $scope.filteredItems.length : $scope.gridsShown + 4);
                    $scope.doneLoading = ($scope.gridsShown === $scope.filteredItems.length);
                    $scope.gridItems = $scope.filteredItems.slice(0,$scope.gridsShown);
                    $scope.startDataLoadForNextExperiment();
                }
            };

            // This method loads the extra data needed for each of the experiments in order to render
            // the Grid View (formerly, Home Page).  RUNNING, DRAFT, PAUSED, TERMINATED
            $scope.loadGridViewData = function (experiments) {
                if (experiments) {
                    $scope.needDataForThese = []; // This contains experiments we need to get supplementary data for.
                    experiments.forEach(function(experiment) {
                        experiment.dataRetrieved = false;
                        $scope.needDataForThese.push({
                            experiment: experiment,
                            // This flag lets us know we only need the data for a DRAFT experiment, which is a little less than for RUNNING, etc.
                            onlyBucketData: (experiment.state.toLowerCase() === 'draft')
                        });

                        UtilitiesFactory.getFormattedStartAndEndDates(experiment);
                        if (!experiment.isRapidExperiment) {
                            experiment.progressRollover = experiment.progressDaysLeft + ' days left in experiment';
                        }

                        var start = experiment.formattedStart;
                        var end = experiment.formattedEnd;

                        // Create experiment description tooltip
                        experiment.homePageTooltip = '';

                        var s = '<div style="font-weight:normal; font-size:15px;">' + experiment.label + '</div>';
                        s += '<div style="position:relative; padding-bottom:9px; font-size: 90%; color:rgb(92,92,92)">';
                        s += start.format('MMM DD YYYY') + ' - ' + end.format('MMM DD YYYY') + '</div>';
                        if (experiment.description && experiment.description.length > 0) {
                            s += '<div style="width:360px">' + experiment.description + '</div>';
                        }
                        experiment.homePageTooltip = s;

                        experiment.numUsers = 0;
                    });

                    if ($scope.data.showGrid) {
                        // Probably just refreshed when grid was showing, need to load data.
                        $scope.startDataLoadForNextExperiment();
                    }
                }

            };

            $scope.startDataLoadForNextExperiment = function() {
                // If there are any more experiments to get data for, get the next one.
                if ($scope.gridDataLoaded < $scope.gridsShown && $scope.needDataForThese.length > 0) {
                    // Start the process of getting extra data for the next experiment, unless it has the flag set
                    // that we have already gotten the data.
                    var foundExperimentInNeedOfLoading = false;
                    do {
                        var nextExperiment = $scope.needDataForThese.splice(0,1)[0];
                        $scope.gridDataLoaded += 1;
                        if (!nextExperiment.experiment.dataRetrieved) {
                            foundExperimentInNeedOfLoading = true;
                            $scope.loadBuckets(nextExperiment.experiment, !nextExperiment.onlyBucketData);
                        }
                    } while (!foundExperimentInNeedOfLoading && $scope.gridDataLoaded < $scope.gridsShown && $scope.needDataForThese.length > 0);
                }
                else {
                    // We are done loading the extra data for the grid view, so enable the Show grid checkbox.
                    $scope.disableShowAsGrid = false;
                }
            };

            $scope.loadApplicationStatistics = function(experiment) {
                ApplicationStatisticsFactory.query({
                    experimentId: experiment.id
                }).$promise.then(function (appInfo) {
                        if (appInfo && appInfo.totalUsers && appInfo.totalUsers.bucketAssignments) {
                            experiment.numUsers = appInfo.totalUsers.bucketAssignments;
                        }
                        if (experiment.isRapidExperiment) {
                            experiment.progressRollover = (experiment.userCap - experiment.progressDaysLeft) + ' user assignments still needed';
                        }
                        if (appInfo && appInfo.assignments && appInfo.assignments.length > 0) {
                            // Get bucket-level assignment counts.
                            UtilitiesFactory.transferMatchingValues(experiment.buckets,
                                appInfo.assignments, 'count', 'label', 'bucket');
                        }

                        $scope.startDataLoadForNextExperiment();
                    },
                    function(response) {
                        console.log('Error loading user count for ' + experiment.id);
                        $scope.startDataLoadForNextExperiment();
                });
            };

            $scope.loadBuckets = function (experiment, loadStatisticsFlag) {
                var loadStatisticsNext = (loadStatisticsFlag != undefined ? loadStatisticsFlag : true);
                BucketsFactory.query({
                    experimentId: experiment.id
                }).$promise.then(function (buckets) {
                        experiment.dataRetrieved = true;
                        if (buckets && buckets.length > 0) {
                            experiment.buckets = buckets;

                            // set baseline bucket (if no control bucket)
                            experiment.controlBucketLabel = buckets[0].label;
                            // get the label of the one control bucket (if any)
                            for (var i = 0; i < experiment.buckets.length; i++) {
                                if (experiment.buckets[i].isControl) {
                                    experiment.hasControlBucket = true;
                                    experiment.controlBucketLabel = experiment.buckets[i].label;
                                }

                                var s = '<div style="font-weight:normal; font-size:15px; padding-bottom:5px">' + experiment.buckets[i].label + '</div>';

                                if (experiment.buckets[i].description && experiment.buckets[i].description.length > 0) {
                                    s += '<div style="width:360px">' + experiment.buckets[i].description + '</div>';
                                }
                                experiment.buckets[i].homePageTooltip = s;
                            }

                            // set baseline bucket (if no control bucket)
                            if (!experiment.hasControlBucket) {
                                buckets[0].isBaseLine = true;
                            }
                        }
                        else {
                            experiment.buckets = [];
                        }

                        if (loadStatisticsNext) {
                            $scope.loadStatistics(experiment);
                        }
                        else {
                            $scope.startDataLoadForNextExperiment();
                        }
                    },
                    function(response) {
                        console.log('Error loading buckets for ' + experiment.id);
                        $scope.startDataLoadForNextExperiment();
                });
            };

            $scope.getBucket = function (bucketLabel, experiment) {
                return UtilitiesFactory.getBucket(bucketLabel, experiment);
            };

            $scope.loadStatistics = function (experiment) {
                ExperimentStatisticsFactory.query({experimentId: experiment.id}).$promise.
                    then(function (statistics) {
                        experiment.statistics = statistics;

                        $scope.loadApplicationStatistics(experiment);

                        UtilitiesFactory.determineBucketImprovementClass(experiment, experiment.controlBucketLabel);

                    }, function(response) {
                        console.log('Error retrieving experiment statistics for ' + experiment.id);
                        $scope.startDataLoadForNextExperiment();
                });
            };

            // *** END Home page code

            $scope.convertOrderByField = function() {
                switch ($scope.orderByField) {
                    case 'applicationName':
                        return 'application_name';
                    case 'label':
                        return 'experiment_name';
                    case 'creatorID':
                        return 'created_by';
                    case 'samplingPercent':
                        return 'sampling_percent';
                    case 'startTime':
                        return 'start_time';
                    case 'endTime':
                        return 'end_time';
                    case 'modificationTime':
                        return 'modification_time';
                    case 'state':
                        return 'state';
                    default:
                        return 'application_name';
                }
            };

            $scope.doLoadExperiments = function(pageSize, currentPage, afterLoadFunction) {
                var queryParams = {
                    per_page: pageSize,
                    page: currentPage,
                    sort: ($scope.reverseSort ? '-' : '') + $scope.convertOrderByField(),
                    filter: $scope.data.query
                };

                ExperimentsFactory.query(queryParams).$promise
                .then(afterLoadFunction,
                    function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of experiments could not be retrieved.');
                });
            };

            // load experiments from server
            $scope.loadExperiments = function () {
                $scope.doLoadExperiments($scope.itemsPerPage, $scope.currentPage, function (data) {
                    var experiments = data.experiments;
                    if (experiments) {
                        // Initialize all the experiments selected values to false so the checkboxes (when list used in selection dialog) will be unchecked.
                        for (var i = 0; i < experiments.length; i++) {
                            if (experiments[i]) {
                                experiments[i].selected = false;
                            } else {
                                delete experiments[i];
                            }
                        }
                        $scope.totalItems = data.totalEntries;
                    }
                    $scope.experiments = experiments;

                    $scope.experiments.forEach(function(item) {
                        if ($rootScope.applicationNames.indexOf(item.applicationName) < 0) {
                            // Only add if it's not already there.
                            $rootScope.applicationNames.push(item.applicationName);
                            if ($scope.hasPermission(item.applicationName, 'create')) {
                                $scope.hasAnyCreatePermissions = true;
                            }
                        }
                    });

                    // Get the list of applications for passing down to the create/edit experiment dialog.
                    // We also need the list of all applications they have any (specifically, read) access to, so
                    // we can use that in the advanced search menu of the experiments list.
                    $scope.applications = [];
                    Session.permissions.forEach(function(nextPermissions) {
                        if (UtilitiesFactory.hasPermission(nextPermissions.applicationName, PERMISSIONS.createPerm)) {
                            $scope.applications.push(nextPermissions.applicationName);
                        }
                        if (UtilitiesFactory.hasPermission(nextPermissions.applicationName, PERMISSIONS.readPerm)) {
                            $scope.applicationsWithReadOrBetterAccess.push(nextPermissions.applicationName);
                        }
                    });
                    if ($scope.applications.length > 1) {
                        // Sort them so they show up that way in the modal select menu.
                        $scope.applications.sort(function (a, b) {
                            return a.toLowerCase().localeCompare(b.toLowerCase());
                        });
                    }
                    if ($scope.applicationsWithReadOrBetterAccess.length > 1) {
                        // Sort them so they show up that way in the advanced search select menu.
                        $scope.applicationsWithReadOrBetterAccess.sort(function (a, b) {
                            return a.toLowerCase().localeCompare(b.toLowerCase());
                        });
                    }

                    $scope.applicationsLoaded = true;

                    $scope.loadAllApplications();

/*
                    if ($scope.experiments.length > 0) {
                        $scope.loadGridViewData($scope.experiments);
                    }
*/

                });
            };

            $scope.refreshSearch = function() {
                $scope.search();
                $scope.$digest(); // Force refresh of list
            };

            $scope.refreshAdvSearch = function() {
                $scope.advSearch();
                $scope.$digest(); // Force refresh of list
            };

            $scope.showMoreLessSearch = function(forceFlag) {
                var forceAdvanced = (forceFlag !== undefined ? forceFlag : false);

                if (forceAdvanced || !$scope.data.showAdvancedSearch) {
                    $scope.data.showAdvancedSearch = true;
                    setTimeout($scope.refreshAdvSearch, 400);
                }
                else {
                    $scope.data.showAdvancedSearch = false;
                    setTimeout($scope.refreshSearch, 400);
                }
                return false;
            };

            $scope.loadCardViewExperiments = function() {
                $scope.doLoadExperiments($scope.cardViewItemsPerPage, $scope.cardViewData.cardViewCurrentPage, function(data) {
                    var experiments = data.experiments;
                    if (experiments) {
                        // Initialize all the experiments selected values to false so the checkboxes (when list used in selection dialog) will be unchecked.
                        for (var i = 0; i < experiments.length; i++) {
                            if (experiments[i]) {
                                experiments[i].selected = false;
                            } else {
                                delete experiments[i];
                            }
                        }
                        $scope.cardViewTotalItems = data.totalEntries;
                    }
                    $scope.cardViewExperiments = experiments;

                    $scope.loadGridDataIfNecessary()
                });
            };

            // init controller
            if (Session && Session.switches) {
                $scope.data.enableCardView = Session.switches.ShowCardView;
            }
            $scope.loadExperiments($scope.orderByField);

            var tmpSearchSettings = localStorage.getItem('wasabiLastSearch');
            if (tmpSearchSettings) {
                $scope.data = JSON.parse(tmpSearchSettings);
            }
            if (!$scope.data.lastSearchWasSimple) {
                $scope.showMoreLessSearch(true);
            }

            if ($scope.data.enableCardView && $scope.data.showGrid) {
                $scope.loadCardViewExperiments();
            }

            UtilitiesFactory.hideHeading(false);
            UtilitiesFactory.selectTopLevelTab('Experiments');

            $rootScope.applicationNames = [];

            $scope.switchToGrid = function() {
                if ($scope.data.showGrid) {
                    // Switching back to list, do a filter in case a favorite has changed.
                    // So that it gets changed correctly in the localStorage that saves the search state,
                    // we need to actually toggle this one because it doesn't get updated until after this has
                    // executed.
                    $scope.data.showGrid = false;
                    localStorage.setItem('wasabiLastSearch', JSON.stringify($scope.data));
                    // TODO: DO we need this?
                    //$scope.applySearchSortFilters(false);
                }
                else {
                    // Record that we are showing the Card View in the localStorage
                    $scope.data.showGrid = true;
                    localStorage.setItem('wasabiLastSearch', JSON.stringify($scope.data));
                    // Switched to card view.  Since we're not pre-loading the data for the cards, we need to
                    // load (or check if we need to load) the data now.
                    $scope.loadCardViewExperiments();
                }
            };

            $scope.applySearchSortFilters = function(doSorting) {
                var faves = UtilitiesFactory.retrieveFavorites();
                $scope.experiments.forEach(function(item) {
                    item.isFavorite = (faves.indexOf(item.applicationName + '|' + item.label) >= 0);
                });

                if (doSorting) {
                    $scope.sortBy($scope.orderByField, $scope.reverseSort);
                } else {
                    if ($scope.data.showAdvancedSearch) {
                        $scope.advSearch($scope.currentPage);
                    }
                    else {
                        $scope.search($scope.currentPage);
                    }
                }
            };

            $scope.redoSearchAndSort = function() {
                $scope.applySearchSortFilters(true);
            };

            $scope.handleCardStarAnimation = function($item, tileWidth) {
                if ($item) {
                    $item.animate({opacity: 0}, 1000, 'swing', function() {
                        $scope.redoSearchAndSort();
                        $item.animate({opacity: 1.0}, 1000);
                        var experimentName = $item.find('.summaryHead h2').eq(0).text();
                        UtilitiesFactory.displayPageSuccessMessage('Favorite Changed', ($item.hasClass('favorite') ? 'Experiment ' + experimentName + ' has been made a favorite.' : 'Experiment ' + experimentName + ' is no longer a favorite.'));
                    });
                    return false;
                }
            };

            // This function is called after an experiment is made a favorite or not a favorite.
            $scope.handleListStarAnimation = function($item) {
                if ($item) {
                    // Fade out the row to show it is being moved.
                    $item.parent().animate({opacity: 0}, 1000, 'swing', function() {
                        // When that is complete, redo the search, so the item will move to it's new location.
                        $scope.redoSearchAndSort();
                        // Now fade the row in in its new location.  If this is on the same page, the user will see it.
                        $item.parent().animate({opacity: 1.0}, 1000);
                        var experimentName = $item.parent().find('td').eq(2).find('a').eq(0).text();
                        // Finally, show a green success box that tells the user the change was made.  This will
                        // disappear after a couple seconds.
                        UtilitiesFactory.displayPageSuccessMessage('Favorite Changed', ($item.hasClass('favorite') ? 'Experiment ' + experimentName + ' has been made a favorite.' : 'Experiment ' + experimentName + ' is no longer a favorite.'));
                    });
                }
            };

            $scope.deleteExperiment = function (experiment) {
                UtilitiesFactory.deleteExperiment(experiment, $scope.loadExperiments);
            };

            $scope.changeState = function (experiment, state) {
                UtilitiesFactory.changeState(experiment, state, $scope.loadExperiments);
            };

            $scope.stateImgUrl = function(state) {
                return UtilitiesFactory.stateImgUrl(state);
            };

            $scope.stateName = function(state) {
                return UtilitiesFactory.stateName(state);
            };

            $scope.capitalizeFirstLetter = function(string) {
                return UtilitiesFactory.capitalizeFirstLetter(string);
            };

            $scope.hasPermission = function(applicationName, permission) {
                return UtilitiesFactory.hasPermission(applicationName, permission);
            };

            $scope.sortBy = function (orderByField) {
                if ($scope.orderByField === orderByField) {
                    $scope.reverseSort = !$scope.reverseSort;
                }
                else {
                    $scope.reverseSort = true;
                }

                $scope.orderByField = orderByField;

                if ($scope.orderByField !== '') {
                    $scope.loadExperiments($scope.orderByField);
                }
            };

            var searchMatch = function (haystack, needle) {
                if (!needle) {
                    return true;
                }
                return haystack.toLowerCase().indexOf(needle.toLowerCase()) !== -1;
            };

            var bubbleFavoritesToTop = function() {
                // We want to pull the favorites to the top and have a secondary sort by the column we are
                // sorting by.
                var tmpArray = [],
                    indexesToRemove = [];
                for (var i = 0; i < $scope.filteredItems.length; i++) {
                    var item = $scope.filteredItems[i];
                    if (item.hasOwnProperty('isFavorite') && item.isFavorite) {
                        tmpArray.push($scope.filteredItems[i]);
                        indexesToRemove.push(i);
                    }
                }
                for (var k = indexesToRemove.length - 1; k >= 0 ; k--) {
                    $scope.filteredItems.splice(indexesToRemove[k], 1);
                }

                // Now add them to the beginning of the array.  They should already be in the correct order.
                for (var j = tmpArray.length - 1; j >= 0; j--) {
                    $scope.filteredItems.unshift(tmpArray[j]);
                }
            };

            $scope.loadGridDataIfNecessary = function() {
                if ($scope.data.showGrid) {
                    // Handle the list used for lazy loading of the grid.
                    $scope.gridsShown = ($scope.cardViewExperiments.length < $scope.initialGridsShown ? $scope.cardViewExperiments.length : $scope.initialGridsShown);
                    $scope.gridItems = $scope.cardViewExperiments.slice(0,$scope.gridsShown);
                    $scope.needDataForThese = []; // Reset list of experiments to get data for.
                    $scope.gridDataLoaded = 0;
                    $scope.cardViewExperiments.forEach(function(experiment) {
                        // We may have already retrieved the extra data for this experiment because this might
                        // be a filtering.
                        $scope.needDataForThese.push({
                            experiment: experiment,
                            onlyBucketData: (experiment.state.toLowerCase() === 'draft')
                        });
                    });
                    $scope.doneLoading = false;
                    $scope.startDataLoadForNextExperiment();
                }
            };

            $scope.clearSearch = function() {
                $scope.data.query = '';
                $scope.doSearch();
            };

            $scope.doSearch = function() {
                $scope.loadExperiments($scope.orderByField);

                // Handle the list used for lazy loading of the grid.
                $scope.loadGridDataIfNecessary();
                $scope.noExperiments = ((!$scope.experiments || $scope.experiments.length === 0) && $scope.totalItems === 0);

                UtilitiesFactory.doTrackingInit();
            };

            $scope.search = function () {
                $scope.data.lastSearchWasSimple = true;
                localStorage.setItem('wasabiLastSearch', JSON.stringify($scope.data));
                if ($.trim($scope.data.query).length > 0) {
                    if ($scope.searchTimer) {
                        $timeout.cancel($scope.searchTimer);
                    }
                    $scope.searchTimer = $timeout($scope.doSearch, 400);
                }
            };

            // init the filtered items
/*
            $scope.search = function (currentPage) {
                $scope.data.lastSearchWasSimple = true;
                localStorage.setItem('wasabiLastSearch', JSON.stringify($scope.data));
                if ($.trim($scope.data.query).length > 0) {
                    if ($scope.searchTimer) {
                        $timeout.cancel($scope.searchTimer);
                    }
                    $scope.searchTimer = $timeout($scope.doSearch, 400);
                }

                if (currentPage) {
                    $scope.currentPage = StateFactory.currentExperimentsPage = currentPage;
                } else {
                    $scope.currentPage = StateFactory.currentExperimentsPage = 1;
                }
                // now group by pages
                $scope.groupToPages();
                $scope.totalItems = $scope.filteredItems.length;
                $scope.noExperiments = ((!$scope.experiments || $scope.experiments.length === 0) && $scope.totalItems === 0);

                UtilitiesFactory.doTrackingInit();
            };
*/

            $scope.advSearch = function(currentPage) {
                if (!$scope.experiments || $scope.experiments.length === 0) {
                    return false;
                }
                // Save the advanced search settings
                $scope.data.lastSearchWasSimple = false;
                localStorage.setItem('wasabiLastSearch', JSON.stringify($scope.data));

                if ($scope.data.advApplicationName === null) {
                    $scope.data.advApplicationName = '';
                }
                // Filter out by the status
                $scope.filteredItems = $filter('filter')($scope.experiments, function(item) {
                    return $scope.advancedFilterList(item, $scope.data.advStatus) &&
                           searchMatch(item.applicationName.toString(), $scope.data.advApplicationName);
                });

                // Filter out by the experiment partial input field
                $scope.filteredItems = $filter('filter')($scope.filteredItems, function (item) {
                    return searchMatch(item.label.toString(), $scope.data.advExperimentName);
                });

                // Filter out by the dates
                $scope.filteredItems = $filter('filter')($scope.filteredItems, function (item) {
                    var filterDate = ($scope.data.advStartOrEndDate === 'startDate' ? moment(item.startTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']) : moment(item.endTime, ['YYYY-MM-DDTHH:mm:ssZ', 'ddd MMM DD YYYY HH:mm:ss ZZ']));
                    var testDate = moment($scope.data.advTxtSearchDateOne, 'M/D/YYYY');
                    var testDate2 = moment($scope.data.advTxtSearchDateTwo, 'M/D/YYYY');
                    switch ($scope.data.adv1stDateSearchType) {
                        case 'isBefore':
                            return filterDate.isBefore(testDate, 'day');
                        case 'isOn':
                            return filterDate.isSame(testDate, 'day');
                        case 'isAfter':
                            return filterDate.isAfter(testDate, 'day');
                        case 'isBetween':
                            // The add and subtract make this an inclusive test, e.g., if range is between 7/2 and 7/5, both 7/2 and 7/5 will be true.
                            return filterDate.isAfter(testDate.subtract(1, 'days'), 'day') && filterDate.isBefore(testDate2.add(1, 'days'), 'day');
                    }
                    // Handles isAny
                    return true;
                });

                // take care of the sorting order
                if ($scope.orderByField !== '') {
                    $scope.filteredItems = $filter('orderBy')($scope.filteredItems, $scope.orderByField, $scope.reverseSort);
                }
                bubbleFavoritesToTop();

                $scope.loadGridDataIfNecessary();

                if (currentPage) {
                    $scope.currentPage = StateFactory.currentExperimentsPage = currentPage;
                } else {
                    $scope.currentPage = StateFactory.currentExperimentsPage = 1;
                }
                // now group by pages
                $scope.groupToPages();
                $scope.totalItems = $scope.filteredItems.length;

                var searchParms = 'advStatus=' + $scope.data.advStatus +
                        '&advExperimentName=' + $scope.data.advExperimentName +
                        '&advStartOrEndDate=' + $scope.data.advStartOrEndDate +
                        '&adv1stDateSearchType=' + $scope.data.adv1stDateSearchType +
                        '&advTxtSearchDateOne=' + $scope.data.advTxtSearchDateOne +
                        '&advTxtSearchDateTwo=' + $scope.data.advTxtSearchDateTwo;
                UtilitiesFactory.trackEvent('advancedSearch',
                    {key: 'search_parms', value: searchParms});


                UtilitiesFactory.doTrackingInit();
            };

            // calculate page in place
            $scope.groupToPages = function () {
                var tmpArray = [];

                $scope.pagedData = {
                    pagedItems: []
                };
                if ($scope.filteredItems.length === 0) {
                    $scope.pagedData.pagedItems[0] = [];
                    return;
                }

                for (var i = 0; i < $scope.filteredItems.length; i++) {
                    if (i % $scope.itemsPerPage === 0) {
                        tmpArray[Math.floor(i / $scope.itemsPerPage)] = [ $scope.filteredItems[i] ];
                    } else {
                        tmpArray[Math.floor(i / $scope.itemsPerPage)].push($scope.filteredItems[i]);
                    }
                }
                $timeout(function() {
                    $scope.pagedData.pagedItems = tmpArray;
                    tmpArray = null;
                });
            };

            $scope.pageChanged = function() {
                StateFactory.currentExperimentsPage = $scope.currentPage;

                // The widget has updated the currentPage member.  By simply triggering the code to get the
                // logs list, we should update the page.
                $scope.loadExperiments($scope.orderByField);
            };

            $scope.cardViewPageChanged = function() {
                StateFactory.currentCardViewPage = $scope.cardViewData.cardViewCurrentPage;

                // The widget has updated the currentPage member.  By simply triggering the code to get the
                // logs list, we should update the page.
                $scope.loadCardViewExperiments();
            };

            $scope.doPageRangeStart = function (currentPage, totalItems, itemsPerPage) {
                try {
                    if (currentPage === 1) {
                        if (totalItems === 0) {
                            return 0;
                        } else {
                            return 1;
                        }
                    } else {
                        return (currentPage - 1) * itemsPerPage + 1;
                    }
                } catch (err) {
                    return 0;
                }
            };

            $scope.doPageRangeEnd = function (pageRangeStartFunc, totalItems, itemsPerPage) {
                try {
                    var start = 0 + pageRangeStartFunc();
                    var ret =  (totalItems >= (start + itemsPerPage) ? start + itemsPerPage - 1 : totalItems);
                    return ret;
                } catch (err) {
                    return 0;
                }
            };

            $scope.pageRangeStart = function() {
                return $scope.doPageRangeStart($scope.currentPage, $scope.totalItems, $scope.itemsPerPage);
            };
            $scope.pageRangeEnd = function() {
                return $scope.doPageRangeEnd($scope.pageRangeStart, $scope.totalItems, $scope.itemsPerPage);
            };
            $scope.cardViewPageRangeStart = function() {
                return $scope.doPageRangeStart($scope.cardViewData.cardViewCurrentPage, $scope.cardViewTotalItems, $scope.cardViewItemsPerPage);
            };
            $scope.cardViewPageRangeEnd = function() {
                return $scope.doPageRangeEnd($scope.cardViewPageRangeStart, $scope.cardViewTotalItems, $scope.cardViewItemsPerPage);
            };

            $scope.filterList = function(item) {
                if ($scope.data.hideTerminated) {
                    return (item.state.toLowerCase() !== 'terminated');
                }
                return true;
            };

            $scope.advancedFilterList = function(item, statusFilter) {
                switch (statusFilter) {
                    case 'notTerminated':
                        return (item.state.toLowerCase() !== 'terminated');
                    case 'terminated':
                    case 'running':
                    case 'paused':
                    case 'draft':
                        return (item.state.toLowerCase() === statusFilter);
                }
                // 'any' case
                return true;
            };

            $scope.hasDeletePermission = function(experiment) {
                return UtilitiesFactory.hasPermission(experiment.applicationName, PERMISSIONS.deletePerm);
            };

            $scope.hasUpdatePermission = function(experiment) {
                return UtilitiesFactory.hasPermission(experiment.applicationName, PERMISSIONS.updatePerm);
            };

            $scope.openExperimentModal = function (experiment) {
                var modalInstance = $modal.open({
                    templateUrl: 'views/ExperimentModal.html',
                    controller: 'ExperimentModalCtrl',
                    windowClass: 'xx-dialog',
                    backdrop: 'static',
                    resolve: {
                        experiment: function () {
                            if (experiment) {
                                // If we are editing an existing experiment, we have the object from the repeat
                                // of the list passed in.
                                return experiment;
                            } else {
                                return {
                                    // set time to 12 am (for TimePicker of new experiment)
                                    startTime: moment(0, 'HH').format('ddd MMM DD YYYY HH:mm:ss ZZ'),
                                    endTime: moment(0, 'HH').add(14, 'days').format('ddd MMM DD YYYY HH:mm:ss ZZ'),
                                    isPersonalizationEnabled: false
                                };
                            }
                        },
                        experiments: function () {
                            return $scope.experiments;
                        },
                        readOnly: function() {
                            return false;
                        },
                        openedFromModal: function() {
                            return false;
                        },
                        applications: function () {
                            var clone = $scope.applications.slice(0);
                            // Add ability for user to create a new application while creating an experiment.
                            clone.push(ConfigFactory.newApplicationNamePrompt);
                            return clone;
                        },
                        allApplications: function() {
                            return $scope.allApplications;
                        }
                    }
                });

                // This will cause the dialog to be closed and we get redirected to the Sign In page if
                // the login token has expired.
                UtilitiesFactory.failIfTokenExpired(modalInstance);
                // This handles closing the dialog if one of the child dialogs has encountered an expired token.
                $scope.$on(AUTH_EVENTS.notAuthenticated, function(/*event*/) {
                    modalInstance.close();
                });

                modalInstance.result.then(function (started) {
                    // Update the list of permissions with any newly created ones.
                    UtilitiesFactory.updatePermissionsAndAppList(function(applicationsList) {
                        $scope.applications = applicationsList;
                        $scope.loadExperiments();
                    });


                });
            };
        }]);
