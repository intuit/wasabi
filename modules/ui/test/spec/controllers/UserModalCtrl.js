'use strict';

describe('UserModalCtrl', function () {

    var scope, modal, utilFactory, $location, createController, $httpBackend,
        mockAuthzFactory = {
            getUserRoles: function(options) {
                return {
                    $promise: {
                        then: function(doneFunc) {
                            var pages = [{
                                applicationName: 'bogusAppName',
                                role: 'admin'
                            }];
                            if (!options) {
                                doneFunc();
                            }
                            else {
                                doneFunc(pages);
                            }
                        }
                    }
                };
            },
            deleteRoleForApplication: function(options) {
                return {
                    $promise: {
                        then: function(doneFunc) {
                            doneFunc();
                        }
                    }
                };
            }
        },
        mockAuthFactory = {
            checkValidUser: function(options) {
                return {
                    $promise: {
                        then: function(doneFunc) {
                            var results = {
                                userName: 'jdoe',
                                firstName: 'John',
                                lastName: 'Doe',
                                email: 'john_doe@example.com'
                            };
                            if (!options) {
                                doneFunc();
                            }
                            else {
                                doneFunc(results);
                            }
                        }
                    }
                };
            }
        },
        mockDialogsFactory = {
            alertDialog: function(content, title, doneFunc) {
                doneFunc();
            },
            confirmDialog: function(content, title, doneFunc) {
                doneFunc();
            }
        };
    
    beforeEach(function() {
        module('wasabi', 'ui.router', 'wasabi.controllers');
        module({
            AuthzFactory: mockAuthzFactory,
            AuthFactory: mockAuthFactory,
            DialogsFactory: mockDialogsFactory
        })
    });
    beforeEach(inject(function($rootScope, $controller, _$location_, $httpBackend){
        $location = _$location_;
        scope = $rootScope.$new();

        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };

        utilFactory = {
            failIfTokenExpired: sinon.stub(),
            trackEvent: sinon.stub(),
            handleGlobalError: sinon.stub(),
            filterAppsForUser: sinon.stub().returns([]),
            stateImgUrl: sinon.stub()
        };

        createController = function () {
            return $controller('UserModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'user': {},
                'administeredApplications': [],
                'UtilitiesFactory': utilFactory
            });
        };
    }));

});