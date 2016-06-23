'use strict';

describe('EditApplicationOnPageCtrl', function () {

    var scope, $location, createController;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        createController = function () {
            return $controller('EditApplicationOnPageCtrl', {
                '$scope': scope
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should execute and not throw', function () {
            scope.application = {
                label: 'hello'
            };
            expect(createController).to.exist;
            expect(createController).to.not.throw();
        });
    });
    
    describe('#userHasRole', function () {

        it('should execute and not throw', function () {
            scope.application = {
                label: 'hello'
            };
            createController();
            expect(scope.userHasRole).to.exist;
            expect(scope.userHasRole).to.be.a('function');
            var testFn = function () {
                scope.userHasRole('angular', 'helloworld');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#search', function () {

        it('should execute and not throw', function () {
            scope.application = {
                label: 'hello'
            };
            createController();
            expect(scope.search).to.exist;
            expect(scope.search).to.be.a('function');
            var testFn = function () {
                var page = 3;
                scope.search(page);
                expect(scope.currentPage).to.eql(page);
            };
            expect(testFn).to.not.throw();
        });
        
        it('should execute with null arguments and not throw', function () {
            scope.application = {
                label: 'hello'
            };
            createController();
            expect(scope.search).to.exist;
            expect(scope.search).to.be.a('function');
            var testFn = function () {
                scope.search();
                
            };
            expect(testFn).to.not.throw();
        });
        
        it('should loop over non-empty scope.users', function () {
            scope.application = {
                label: 'hello'
            };
            createController();
            expect(scope.search).to.exist;
            expect(scope.search).to.be.a('function');
            var testFn = function () {
                scope.search('angular');
            };
            scope.users = [
                {
                    name: 'angular'
                }
            ];
            expect(testFn).to.not.throw();
        });
        
        it('should loop over empty scope.users', function () {
            scope.application = {
                label: 'hello'
            };
            createController();
            expect(scope.search).to.exist;
            expect(scope.search).to.be.a('function');
            var testFn = function () {
                scope.search('angular');
            };
            scope.users = [
                {}
            ];
            expect(testFn).to.not.throw();
        });
    });

    describe('#pageChanged', function () {

        it('should execute and not throw', function () {
            scope.application = {
                label: 'hello'
            };
            scope.currentPage = 2;
            createController();
            expect(scope.pageChanged).to.exist;
            expect(scope.pageChanged).to.be.a('function');
            var testFn = function () {
                scope.pageChanged();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#pageRangeStart', function () {

        it('should execute and not throw', function () {
            scope.application = {
                label: 'hello'
            };
            scope.currentPage = 2;
            createController();
            expect(scope.pageRangeStart).to.exist;
            expect(scope.pageRangeStart).to.be.a('function');
            var testFn = function () {
                scope.pageRangeStart();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should return 0 on first page and no paged items', function () {
            scope.application = {
                label: 'hello'
            };
            createController();
            scope.currentPage = 1;
            scope.pagedItems = [[]];
            expect(scope.pageRangeStart).to.exist;
            expect(scope.pageRangeStart).to.be.a('function');
            var result = scope.pageRangeStart();
            expect(result).to.eql(0);
        });
        
        it('should return 1 on first page and non-zero paged items', function () {
            scope.application = {
                label: 'hello'
            };
            createController();
            scope.currentPage = 1;
            scope.pagedItems = [[1]];
            expect(scope.pageRangeStart).to.exist;
            expect(scope.pageRangeStart).to.be.a('function');
            var result = scope.pageRangeStart();
            expect(result).to.eql(1);
        });
        
        it('should return 0 on TypeError', function () {
            scope.application = {
                label: 'hello'
            };
            createController();
            scope.currentPage = 1;
            scope.pagedItems = [undefined];
            expect(scope.pageRangeStart).to.exist;
            expect(scope.pageRangeStart).to.be.a('function');
            var result = scope.pageRangeStart();
            expect(result).to.eql(0);
        });
    });

    describe('#pageRangeEnd', function () {

        it('should execute and not throw', function () {
            scope.application = {
                label: 'hello'
            };
            scope.currentPage = 'hello';
            createController();
            expect(scope.pageRangeEnd).to.exist;
            expect(scope.pageRangeEnd).to.be.a('function');
            var testFn = function () {
                scope.pageRangeEnd();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#stateImgUrl', function () {

        it('should execute and not throw', function () {
            scope.application = {
                label: 'hello'
            };
            scope.currentPage = 'hello';
            createController();
            expect(scope.stateImgUrl).to.exist;
            expect(scope.stateImgUrl).to.be.a('function');
            var testFn = function () {
                scope.stateImgUrl('hello');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#removeRoles', function () {

        it('should execute and not throw', function () {
            var user = {
                userID: 1
            };
            scope.application = {
                label: 'hello'
            };
            scope.currentPage = 'hello';
            createController();
            expect(scope.removeRoles).to.exist;
            expect(scope.removeRoles).to.be.a('function');
            var testFn = function () {
                scope.removeRoles(user);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#openUserModal', function () {

        it('should execute and not throw', function () {
            scope.application = {
                label: 'hello'
            };
            scope.currentPage = 'hello';
            createController();
            expect(scope.openUserModal).to.exist;
            expect(scope.openUserModal).to.be.a('function');
            var testFn = function () {
                scope.openUserModal();
            };
            expect(testFn).to.not.throw();
        });
    });
});