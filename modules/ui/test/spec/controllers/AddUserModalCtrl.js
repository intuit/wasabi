'use strict';

describe('AddUserModalCtrl', function () {

    var scope, createController,
        modal,
        application,
        user;
    
    beforeEach(module('wasabi', 'ui.bootstrap', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        application = 'angularjs';
        user = {};
        createController = function (isEditingPermissions, _application, _user) {
            return $controller('AddUserModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'application': _application || application,
                'user': _user || user,
                'isEditingPermissions': isEditingPermissions || false
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and add known keys to scope', function () {
            createController();
            var keys = ['user', 'applications', 'application', 'userValidated', 'validating', 'failedValidation', 'userHasPrivilegesForAllApps', 'isEditingPermissions', 'addUserFormSubmitted', 'verifyFormNoValueError', 'stateImgUrl', 'capitalizeFirstLetter', 'getUsersPrivilegesForApplication', 'updateApplicationRoles', 'verifyUser', 'saveAddUser', 'cancel'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
        
        it('should exist and add known keys to scope', function () {
            scope.user = {
                applications: [
                    {
                        label: 'angularjs'
                    }
                ]
            };
            scope.application = {
                label:'angularjs'
            };
            createController(true, application, scope.user);
            var keys = ['user', 'applications', 'application', 'userValidated', 'validating', 'failedValidation', 'userHasPrivilegesForAllApps', 'isEditingPermissions', 'addUserFormSubmitted', 'verifyFormNoValueError', 'stateImgUrl', 'capitalizeFirstLetter', 'getUsersPrivilegesForApplication', 'updateApplicationRoles', 'verifyUser', 'saveAddUser', 'cancel'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
    });
    
    describe('#stateImgUrl', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.stateImgUrl).to.exist;
            var testFn = function () {
                return scope.stateImgUrl('CLOSED');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#capitalizeFirstLetter', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.capitalizeFirstLetter).to.exist;
            var testFn = function () {
                return scope.capitalizeFirstLetter('angularjs');
            };
            expect(testFn).to.not.throw();
            expect(testFn()).to.eql('Angularjs');
        });
    });
    
    describe('#getUsersPrivilegesForApplication', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.getUsersPrivilegesForApplication).to.exist;
            var testFn = function () {
                return scope.getUsersPrivilegesForApplication('angularjs');
            };
            expect(testFn).to.not.throw();
        });
        
        it('should return', function () {
            createController();
            var testFn = function () {
                scope.application = {
                    label: 'angularjs'
                };
                scope.applications = [
                    {
                        label: 'angularjs'
                    }
                ];
                return scope.getUsersPrivilegesForApplication();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#verifyUser', function () {

        it('should exist and not throw on empty string userEmail input', function () {
            createController();
            expect(scope.verifyUser).to.exist;
            var testFn = function () {
                scope.data.userEmail = '';
                return scope.verifyUser();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should exist and not throw on valid userEmail', function () {
            createController();
            expect(scope.verifyUser).to.exist;
            var testFn = function () {
                scope.data.userEmail = 'wasabi_admin@example.com';
                return scope.verifyUser();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should exist and not throw on test userEmail', function () {
            createController();
            expect(scope.verifyUser).to.exist;
            var testFn = function () {
                scope.data.userEmail = 'wasabi_admin';
                return scope.verifyUser();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should exist and not throw on non-test userEmail, invalid', function () {
            createController();
            expect(scope.verifyUser).to.exist;
            var testFn = function () {
                scope.data.userEmail = 'admin';
                return scope.verifyUser();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#saveAddUser', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.saveAddUser).to.exist;
            var testFn = function () {
                return scope.saveAddUser();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#cancel', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.cancel).to.exist;
            var testFn = function () {
                return scope.cancel();
            };
            expect(testFn).to.not.throw();
        });
    });
});