/*global $:false */
/* jshint devel:true */

'use strict';

angular.module('wasabi.controllers').
    controller('PagesCtrl', ['$scope', '$filter', '$http', 'ExperimentsFactory', '$uibModal', 'ApplicationsFactory', 'DialogsFactory', 'UtilitiesFactory', '$rootScope', 'ConfigFactory',
        function ($scope, $filter, $http, ExperimentsFactory, $uibModal, ApplicationsFactory, DialogsFactory, UtilitiesFactory, $rootScope, ConfigFactory) {
            $scope.pages = [];
            $scope.pagesData = {
                groupPages: [],
                pageName: '',
                setPageNameFocus: 0
            };

            $scope.loadPages = function () {
                if($scope.experiment.id) {
                    ExperimentsFactory.getPages({
                        id: $scope.experiment.id
                    }).$promise.then(function (pages) {
                            $scope.pages = pages;
                            $scope.experiment.pages = pages; // So we have access to the list when creating API call examples.
                            $scope.initPages();
                        }, function(response) {
                        UtilitiesFactory.handleGlobalError(response, 'The list of pages for your experiment could not be loaded.');
                        $scope.modalInstance.close();
                    });
                }
            };

            $scope.loadGlobalPages = function() {
                var unbindHandler = null;
                if (!$scope.experiment || !$scope.experiment.applicationName || $scope.experiment.applicationName === ConfigFactory.newApplicationNamePrompt) {
                    // This should mean that we are on the Create dialog, so the applicationName hasn't been specified, yet.
                    // Wait until the experiment is created at which time this message will be broadcast.
                    unbindHandler = $rootScope.$on('experiment_created', function() {
                        $scope.loadGlobalPages();
                        unbindHandler();
                    });
                    return;
                }
                ApplicationsFactory.getPages({appName: $scope.experiment.applicationName}).$promise.then(function (pages) {
                    pages.forEach(function(item) {
                        $scope.pagesData.groupPages.push(item.name);
                    });
                    $scope.loadPages();
                });
            };

            // Initially, remove all the items currently in our list from the global list kept locally.
            $scope.initPages = function() {
                if ($scope.pages && $scope.pages.length > 0) {
                    $scope.pages.forEach(function(element) {
                        removePageFromGlobal(element.name);
                    });
                }
            };

            // init controller
            $scope.loadGlobalPages();

            // This just removes the page from the local list of global pages used for autocomplete.  That way,
            // after you have selected one of those global pages to associate with this experiment, the page
            // won't be presented as a choice on autocomplete.
            var removePageFromGlobal = function(page) {
                var pageLoc = $.inArray(page, $scope.pagesData.groupPages);
                if (pageLoc > -1) {
                    $scope.pagesData.groupPages.splice( pageLoc, 1 );
                }
            };

            var clearPageInput = function() {
                setTimeout(function() {
                    $scope.pagesData.pageName = '';
                    $scope.pagesData.setPageNameFocus += 1;
                }, 30);
            };

            // Save the current state of the pages associated with this experiment to the back end.
            $scope.doSavePages = function(pageName, onErrorCleanupFunction) {
                var tmpPages = $scope.pages.slice(0);
                // Remove $$hashTag and any other funky fields before sending to API.
                tmpPages.forEach(function(item) {
                    for (var property in item) {
                        if (item.hasOwnProperty(property)) {
                            if (property !== 'name' && property !== 'allowNewAssignment') {
                                delete item[property];
                            }
                        }
                    }
                });
                ExperimentsFactory.savePages({
                    id: $scope.experiment.id,
                    pages: $scope.pages
                }).$promise.then(function () {
                    UtilitiesFactory.trackEvent('saveItemSuccess',
                        {key: 'dialog_name', value: 'addPage'},
                        {key: 'experiment_id', value: $scope.experiment.id});

                    UtilitiesFactory.displaySuccessWithCacheWarning('Page Changes Saved', 'Your page changes have been saved.');
                    $scope.loadPages();
                }, function(response) {
                    if (onErrorCleanupFunction) {
                        onErrorCleanupFunction.call(this, pageName);
                    }
                    UtilitiesFactory.handleGlobalError(response, 'Your pages could not be saved.');
                });
            };

            // Call the API to associate a page with this experiment.  Note that this starts with
            // the allowNewAssignment flag set to false.  It can then be updated by the user with the checkboxes.
            var associatePage = function(pageName) {
                $scope.pages.push({name: pageName, allowNewAssignment: true});
                if (pageName && pageName.length) {
                    $scope.doSavePages(
                        pageName,
                        // If the association fails, we want to remove the page we added a couple lines up.
                        function(pageName) {
                            $scope.pages.forEach(function(item, index) {
                                if (item.name.toLowerCase() === pageName.toLowerCase()) {
                                    $scope.pages.splice( index, 1);
                                }
                            });

                        }
                    );
                }
            };

            // Remove the association of this page with this experiment.  We do this by finding the page
            // in the local list so we can remove it from that (although, since we do a loadPages() when the actual
            // remove is successful, we might not need to do that).
            var removePageFromExperiment = function(pageName) {
                if (pageName && pageName.length) {
                    $scope.pages.forEach(function(item, index) {
                        if (item.name.toLowerCase() === pageName.toLowerCase()) {
                            $scope.pages.splice( index, 1);
                            ExperimentsFactory.removePage({
                                id: $scope.experiment.id,
                                pageName: item.name
                            }).$promise.then(function () {
                                UtilitiesFactory.trackEvent('deleteItemSuccess',
                                    {key: 'dialog_name', value: 'deletePage'},
                                    {key: 'experiment_id', value: $scope.experiment.id},
                                    {key: 'item_id', value: item.name});

                                UtilitiesFactory.displaySuccessWithCacheWarning('Page Association Deleted', 'Your page change has been saved.');
                                $scope.loadPages();
                            }, function(response) {
                                $scope.loadPages(); // To put the page back that we removed above.
                                UtilitiesFactory.handleGlobalError(response, 'Your page could not be removed.');
                            });
                        }
                    });
                }
            };

            $scope.updatePage = function(page) {
                // Just save the current state of the pages list.
                $scope.doSavePages(
                    page.name,
                    // If the update fails, we want to change the allowNewAssignment back to its previous value.
                    function(pageName) {
                        $scope.pages.forEach(function(item) {
                            if (item.name.toLowerCase() === pageName.toLowerCase()) {
                                item.allowNewAssignment = !item.allowNewAssignment;
                            }
                        });
                    });
            };

            // The signature of this function is consistent with the jQuery autocomplete extension's select
            // function.  It is passed the event and then ui.item.label is the selected label.
            $scope.selectPage = function(event, ui) {
                $scope.addingPage = true;
                //console.log('called selectPage');
                var page = ui.item.label;

                $scope.addPageToList(page);
                $scope.$digest();
            };

            $scope.validateAndAddPage = function(pageName) {
                // Is not in the global list, but is it already in the experiment?
                var dontAdd = false;
                $scope.pages.forEach(function(item) {
                    if (item.name.toLowerCase() === pageName.toLowerCase()) {
                        // Don't need to add it.
                        DialogsFactory.alertDialog('The page ' + pageName + ' has already been added.', 'Page Already Added', function() {$scope.pagesData.setPageNameFocus += 1;});
                        dontAdd = true;
                    }
                });
                if (dontAdd) {
                    return;
                }

                // Is not in the list of pages associated with this experiment, so ask to add.
                DialogsFactory.confirmDialog('Are you sure you want to create a new page named "' + pageName + '" for this application?', 'New Page', function() {$scope.addPageToList(pageName);});
            };

            // This function is called by the autocomplete directive when the user hits enter.  We have to make
            // sure the page isn't on the experiment already and if it isn't in the global list, add it.
            $scope.selectPageOnEnter = function(page) {
                function addPage() {
                    $scope.addPageToList(page);
                }

                if ($scope.addingPage) {
                    // This is necessary because if you start typing something, get some autocompletes, and then
                    // navigate down to one and hit enter, you will both call the selectPage() function from the
                    // autocomplete.select function and also call this function, because you hit enter in the
                    // text field.  By setting this flag, we prevent doing both.
                    $scope.addingPage = false;
                    return false;
                }
                // add page on enter key
                if ($.inArray(page, $scope.pagesData.groupPages) < 0) {
                    // Not in the global list of pages, is it already in the list for this experiment?
                    $scope.validateAndAddPage(page);
                }
                else if ($.inArray(page, $scope.pagesData.groupPages) >= 0) {
                    addPage();
                    $scope.$digest();
                }
                return false;
            };

            // This function is called when the Add button is clicked.
            $scope.addPageClick = function() {
                var page = $.trim($scope.pagesData.pageName);

                if (!page.length) {
                    DialogsFactory.alertDialog('Enter a page identifier in the adjoining field.', 'Page Is Required', function() {$scope.pagesData.setPageNameFocus += 1;});
                }else if ($.inArray(page, $scope.pagesData.groupPages) > -1) {
                    // Is in global list, must not be on this experiment, yet.
                    $scope.addPageToList(page);
                }else {
                    $scope.validateAndAddPage(page);
                }
                return false;
            };

            $scope.removePage = function(pageName) {
                DialogsFactory.confirmDialog('Are you sure that you want to remove the page, "' + pageName + '"?', 'Remove Page', function() {removePageFromExperiment(pageName);});
            };

            $scope.addPageToList = function(page) {
                associatePage(page);
                clearPageInput();
            };
        }]);
