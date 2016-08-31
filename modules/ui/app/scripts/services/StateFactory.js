'use strict';

angular.module('wasabi.services').factory('StateFactory', [
    function () {
        return {
            // This is the page counter for pagination of the Experiments list.  In order to make this persist
            // across changes at the page level, e.g., going to the Properties tab or the Details page, we
            // need it to live outside of the controller because the controller is reset when we navigate.
            currentExperimentsPage: 1,

            currentCardViewPage: 1,

            currentUsersPage: 0,

            currentUsersInApplicationPage: 0
        };
    }
]);
