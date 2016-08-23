"use strict";

(function() {
    angular.module('storeApp')
    .controller('LoginController', ['$http', '$window', function LoginController($http, $window) {
        var login = this;
        login.name = '';
        login.password = '';
        login.errorMessage = '';

        var session = sessionStorage.getItem('session');
        if (session) {
            session = JSON.parse(session);
            if (session.login.loggedIn) {
                session.login.loggedIn = false;
                sessionStorage.setItem('session', JSON.stringify(session));
            }
        }

        login.submit = function() {
            if (validate()) {
                console.log('Login submitted');
                // TODO: We would usually actually do a login, but for this test app, we let them in.
                // var data = JSON.stringify({name: login.name, cost: login.cost});
                // $http.post('http://localhost:3000/login', data).success(function(data) {
                // console.log(data);
                // });
                var sessionObj = {
                    'login': {
                        'name': login.name,
                        'loggedIn': true
                    }
                };

                // Set up properties that will be the same on all Wasabi calls.
                WASABI.setOptions({
                    'applicationName': 'MyStore',
                    'experimentName': 'TestBuyButtonColor',
                    'protocol': 'http',
                    'host': 'localhost:8080'
                });

                // Check Wasabi to see if this user should be in the test and which bucket.
                WASABI.getAssignment({
                    'userID': login.name
                }).then(
                    function(response) {
                        console.log('getAssignment: success');
                        console.log(JSON.stringify(response));
                        // This object will include the assignment made and the status, which might tell you the experiment
                        // has not been started, etc.
                        // Note that if the experiment doesn't exist or hasn't been started, response.assignment is undefined, which is OK.
                        sessionObj.switches = {};
                        if (response.assignment === 'BlueButton') {
                            sessionObj.switches = {
                                'buyButtonColor': '#7474EA'
                            };
                        }
                        else if (response.assignment === 'GreenButton') {
                            sessionObj.switches = {
                                'buyButtonColor': 'green'
                            };
                        }
                        // else the user got the Control bucket, and we don't do anything.

                        sessionStorage.setItem('session', JSON.stringify(sessionObj));
                        $window.location.href = '/';
                    },
                    function(error) {
                        console.log('getAssignment: error');
                        sessionStorage.setItem('session', JSON.stringify(sessionObj));
                        $window.location.href = '/';
                    }
                );
            }
            else {
                console.log('invalid');
            }
        };

        function validate() {
            if (typeof WASABI == 'undefined') {
                console.log('The wasabi.js library was not loaded.');
                login.errorMessage = 'The required version of the Wasabi server is missing!';
                return false;
            }

            if (login.name == '' || !login.name) {
                login.errorMessage = 'name must be present';
                return false;
            }
            if (login.password == '' || !login.password) {
                login.errorMessage = 'password must be present';
                return false;
            }
            login.errorMessage = '';
            return true;
        }
    }]);

})();
