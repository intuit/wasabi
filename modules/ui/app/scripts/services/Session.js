'use strict';

angular.module('wasabi.services').factory('Session', [function () {
    this.create = function (sessionInfo) {
        this.userID = sessionInfo.userID;
        this.accessToken = sessionInfo.accessToken;
        this.tokenType = sessionInfo.tokenType;
        this.userRole = sessionInfo.userRole;
        this.isSuperadmin = sessionInfo.isSuperadmin;
        this.permissions = sessionInfo.permissions;
        if (sessionInfo.switches) {
            this.switches = sessionInfo.switches;
        }
        else {
            this.switches = sessionInfo.switches = {};
        }

        /*
         * persist session (sessionStorage lives until you close browser tab)
         */
        sessionStorage.setItem('session', JSON.stringify(sessionInfo));
    };
    this.update = function (sessionInfo) {
        var session = JSON.parse(sessionStorage.getItem('session'));
        if(session) {
            for (var nextField in sessionInfo) {
                if (sessionInfo.hasOwnProperty(nextField)) {
                    this[nextField] = session[nextField] = sessionInfo[nextField];
                }
            }
            sessionStorage.setItem('session', JSON.stringify(session));
        }
    };
    this.destroy = function () {
        this.userID = null;
        this.accessToken = null;
        this.tokenType = null;
        this.userRole = null;
        this.permissions = null;
        this.isSuperadmin = false;
        this.switches = {};

        sessionStorage.removeItem('session');
    };

    this.restore = function() {
        var session = JSON.parse(sessionStorage.getItem('session'));
        if(session) {
            this.userID = session.userID;
            this.accessToken = session.accessToken;
            this.tokenType = session.tokenType;
            this.userRole = session.userRole;
            this.permissions = session.permissions;
            this.isSuperadmin = session.isSuperadmin;
            this.switches = session.switches;
        }
    };
    return this;
}]);