'use strict';

describe('AddApplicationModalCtrl', function () {

    var scope, createController,
        modal,
        application,
        applications,
        administeredApplications,
        appsThatCanBeAdded,
        user;
    
    beforeEach(module('wasabi', 'ui.bootstrap', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        application = 'angularjs';
        applications = [];
        administeredApplications = [];
        user = {};
        appsThatCanBeAdded = [];
        createController = function (isEditingPermissions) {
            return $controller('AddApplicationModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'application': application,
                'applications': applications,
                'administeredApplications': administeredApplications,
                'appsThatCanBeAdded': appsThatCanBeAdded,
                'user': user,
                'isEditingPermissions': isEditingPermissions || false
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should exist and add known keys to scope', function () {
            createController();
            var keys = ['user', 'application', 'applications', 'administeredApplications', 'appsThatCanBeAdded', 'addApplicationFormSubmitted', 'orderByField', 'reverseSort', 'filteredItems', 'isEditingPermissions', 'stateImgUrl', 'capitalizeFirstLetter', 'search', 'saveAddApplication', 'cancel'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
        
        it('should set keys if isEditingPermissions is true', function () {
            createController(true);
            
            expect(scope.data.addWritePrivileges).to.exist;
            expect(scope.data.addWritePrivileges).to.eql(false);
            expect(scope.data.addAdminPrivileges).to.exist;
            expect(scope.data.addAdminPrivileges).to.eql(false);
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
            expect(scope.stateImgUrl).to.exist;
            var testFn = function () {
                return scope.capitalizeFirstLetter('angularjs');
            };
            expect(testFn).to.not.throw();
            expect(testFn()).to.eql('Angularjs');
        });
    });
    
    describe('#search', function () {

        it('should exist and not throw', function () {
            createController();
            expect(scope.stateImgUrl).to.exist;
            var testFn = function () {
                scope.appsThatCanBeAdded = [
                    {
                        'value': 1
                    }
                ];
                return scope.search();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should reach the return false case with empty objects', function () {
            createController();
            expect(scope.stateImgUrl).to.exist;
            var testFn = function () {
                scope.appsThatCanBeAdded = [
                    {}
                ];
                return scope.search();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#saveAddApplication', function () {

        it('should exist and not throw with isEditingPermissions=true', function () {
            createController(true);
            expect(scope.saveAddApplication).to.exist;
            var testFn = function () {
                return scope.saveAddApplication();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should exist and not throw with isEditingPermissions=false', function () {
            createController(false);
            expect(scope.saveAddApplication).to.exist;
            var testFn = function () {
                scope.appsThatCanBeAdded = [
                    {
                        label: 'angularjs',
                        selected: true
                    }
                ];
                return scope.saveAddApplication();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should exist and not throw with isEditingPermissions=false with no appsThatCanBeAdded', function () {
            createController(false);
            expect(scope.saveAddApplication).to.exist;
            var testFn = function () {
                return scope.saveAddApplication();
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