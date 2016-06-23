'use strict';

describe('UsersCtrl', function () {

    var scope, $location, createController,
        mockDialogsFactory = {
            alertDialog: function(content, title, doneFunc) {
                doneFunc();
            },
            confirmDialog: function(content, title, doneFunc) {
                doneFunc();
            }
        },
        mockAuthzFactory = {
            getUsersForApplication: function(options) {
                return {
                    $promise: {
                        then: function(doneFunc) {
                            doneFunc();
                        }
                    }
                };
            },
            getUsersRoles: function(options) {
                return {
                    $promise: {
                        then: function(doneFunc) {
                            doneFunc([]);
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
        };
    
    beforeEach(function() {
        module('wasabi', 'ui.router', 'wasabi.controllers');
        module({
            DialogsFactory: mockDialogsFactory,
            AuthzFactory: mockAuthzFactory
        });
    });
    beforeEach(inject(function($rootScope, $controller, _$location_, UtilitiesFactory){
        $location = _$location_;
        scope = $rootScope.$new();
        
        createController = function () {
            return $controller('UsersCtrl', {
                '$scope': scope
            });
        };
    }));
    
    it('should exist and set scope properties/methods', function () {
        var controller = createController();
        
        var props = ['data', 'orderByField', 'reverseSort', 'itemsPerPage', 'pagedItems', 'groupedItems', 'filteredItems', 'totalItems', 'currentPage', 'currentUser', 'users', 'administeredApplications', 'appNames'];
        var methods = ['loadUsers', 'loadAdministeredApplications', 'applySearchSortFilters', 'removeUser', 'capitalizeFirstLetter', 'sortBy', 'search', 'groupToPages', 'range', 'pageChanged', 'pageRangeStart', 'pageRangeEnd', 'refreshSearch', 'userHasRole', 'openUserModal'];
        
        props.forEach(function(d){
            expect(scope).to.have.property(d);
            expect(scope[d]).to.not.be.a('function');
        });
        
        methods.forEach(function(d){
            expect(scope).to.have.property(d);
            expect(scope[d]).to.be.a('function');
        });
        controller = null;
    });
    
    describe('#applySearchSortFilters', function () {

        it('should return undefined with doSorting=true but no data', function () {
           var controller = createController();
           var result = scope.applySearchSortFilters(true);
           expect(result).to.be.undefined;
           controller = null;
        });
        
        it('should return undefined with doSorting=false but no data', function () {
           var controller = createController();
           var result = scope.applySearchSortFilters(false);
           expect(result).to.be.undefined;
           controller = null;
        });
    });
    
    
    describe('#loadUsers', function () {

        it('should not throw', function () {
           var controller = createController();
            var testFn = function () {
                scope.loadUsers();
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw with non-empty list of administered applications', function () {
           var controller = createController();
            var testFn = function () {
                scope.administeredApplications = [{
                    label: 'bogusAppName'
                }];
                scope.loadUsers();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#removeUser', function () {

        it('should throw error on null input', function () {
           var controller = createController();
           expect(scope.removeUser).to.throw();
           controller = null;
        });

        it('should not throw with user with no applications', function () {
            var controller = createController();
            var u = {
                userID: '1',
                applications: []
            };
            var testFn = function () {
                scope.removeUser(u);
            };
            expect(testFn).to.not.throw();
            controller = null;
        });

        it('should not throw with user with applications', function () {
            var controller = createController();
            var u = {
                userID: '1',
                applications: [{
                    label: 'anotherAppName'
                }]
            };
            var testFn = function () {
                scope.removeUser(u);
            };
            expect(testFn).to.not.throw();
            controller = null;
        });
    });

    describe('#search', function () {

        it('should not throw on defined scope.data.query', function () {
            var controller = createController();
            var testFn = function () {
                scope.search();
            };
            scope.data.query = 'angularjs';
            scope.users = [
                {
                    userID: 'angularjs',
                    id: 1,
                    percent: '2'
                }
            ];
            expect(testFn).to.not.throw();
            controller = null;
        });

    });

    describe('#pageRangeStart', function () {

        it('should not throw but return 0 when no items', function () {
            var controller = createController();
            var result = -1;
            var testFn = function () {
                scope.currentPage = 1;
                scope.pagedItems = [];
                result = scope.pageRangeStart();
            };
            expect(testFn).to.not.throw();
            expect(result).to.eql(0);
            controller = null;
        });

        it('should not throw but return 1 when items', function () {
            var controller = createController();
            var result = -1;
            var testFn = function () {
                scope.currentPage = 1;
                scope.pagedItems = [[]];
                result = scope.pageRangeStart();
            };
            expect(testFn).to.not.throw();
            expect(result).to.eql(0);
            controller = null;
        });

        it('should not throw but return 1 when items', function () {
            var controller = createController();
            var result = -1;
            var testFn = function () {
                scope.currentPage = 1;
                scope.pagedItems = [[{
                    name: 'bogusUser'
                }]];
                result = scope.pageRangeStart();
            };
            expect(testFn).to.not.throw();
            expect(result).to.eql(1);
            controller = null;
        });

        it('second page', function () {
            var controller = createController();
            var result = -1;
            var testFn = function () {
                scope.currentPage = 2;
                scope.pagedItems = [[
                    {
                        name: '1'
                    },
                    {
                        name: '2'
                    },
                    {
                        name: '3'
                    },
                    {
                        name: '4'
                    },
                    {
                        name: '5'
                    },
                    {
                        name: '6'
                    },
                    {
                        name: '7'
                    },
                    {
                        name: '8'
                    },
                    {
                        name: '9'
                    },
                    {
                        name: '10'
                    }
                ],
                [
                    {
                        name: '11'
                    }
                ]];
                result = scope.pageRangeStart();
            };
            expect(testFn).to.not.throw();
            expect(result).to.eql(11);
            controller = null;
        });
    });

    describe('#pageRangeEnd', function () {

        it('should not throw but return 0 when no items', function () {
            var controller = createController();
            var result = -1;
            var testFn = function () {
                scope.currentPage = 1;
                scope.pagedItems = [];
                result = scope.pageRangeEnd();
            };
            expect(testFn).to.not.throw();
            expect(result).to.eql(0);
            controller = null;
        });

        it('should not throw but return 1 when items', function () {
            var controller = createController();
            var result = -1;
            var testFn = function () {
                scope.currentPage = 1;
                scope.pagedItems = [[]];
                result = scope.pageRangeEnd();
            };
            expect(testFn).to.not.throw();
            expect(result).to.eql(-1);
            controller = null;
        });

        it('should not throw but return 1 when items', function () {
            var controller = createController();
            var result = -1;
            var testFn = function () {
                scope.currentPage = 1;
                scope.pagedItems = [[{
                    name: 'bogusUser'
                }]];
                result = scope.pageRangeEnd();
            };
            expect(testFn).to.not.throw();
            expect(result).to.eql(1);
            controller = null;
        });

        it('second page', function () {
            var controller = createController();
            var result = -1;
            var testFn = function () {
                scope.currentPage = 2;
                scope.pagedItems = [[
                    {
                        name: '1'
                    },
                    {
                        name: '2'
                    },
                    {
                        name: '3'
                    },
                    {
                        name: '4'
                    },
                    {
                        name: '5'
                    },
                    {
                        name: '6'
                    },
                    {
                        name: '7'
                    },
                    {
                        name: '8'
                    },
                    {
                        name: '9'
                    },
                    {
                        name: '10'
                    }
                ],
                [
                    {
                        name: '11'
                    },
                    {
                        name: '12'
                    }
                ]];
                result = scope.pageRangeEnd();
            };
            expect(testFn).to.not.throw();
            expect(result).to.eql(12);
            controller = null;
        });
    });

});

