'use strict';

describe('ExperimentsCtrl', function () {

    var scope, $location, createController, $httpBackend;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        angular.mock.inject(function($injector){
            $httpBackend = $injector.get('$httpBackend');
        });
        
        createController = function () {
            return $controller('ExperimentsCtrl', {
                '$scope': scope
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should not throw and should generate known scope keys', function () {
            createController();
            var keys = ['data', 'orderByField', 'reverseSort', 'itemsPerPage', 'pagedData', 'groupedItems', 'filteredItems', 'currentPage', 'totalItems', 'hasAnyCreatePermissions', 'experiment', 'applications', 'loadExperiments', 'applySearchSortFilters', 'deleteExperiment', 'changeState', 'stateImgUrl', 'stateName', 'capitalizeFirstLetter', 'hasPermission', 'sortBy', 'search', 'advSearch', 'pageChanged', 'pageRangeStart', 'pageRangeEnd', 'filterList', 'advancedFilterList', 'refreshSearch', 'refreshAdvSearch', 'showMoreLessSearch', 'hasDeletePermission', 'hasUpdatePermission', 'openExperimentModal'];
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
    });
    
    describe('#loadExperiments', function () {

        it('should not throw ', function () {
            createController();
            var orderbyField = 'id';
            var testFn = function () {
                scope.loadExperiments(orderbyField);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#search', function () {

        it('should not throw on defined scope.data.query', function () {
            createController();
            var testFn = function () {
                scope.search('angularjs');
            };
            scope.data.query = '2';
            scope.experiments = [
                {
                    label: 'angularjs',
                    id: 1,
                    percent: '2',
                    state: 'DRAFT'
                }
            ];
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on empty string scope.data.query', function () {
            createController();
            var testFn = function () {
                scope.search('angularjs');
            };
            scope.data.query = '';
            scope.experiments = [
                {
                    label: 'angularjs',
                    id: 1,
                    state: 'DRAFT'
                }
            ];
            expect(testFn).to.not.throw();
        });
    });

    describe('#advSearch', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.advSearch('angularjs');
            };
            scope.data.query = '2';
            scope.data.advStatus = 'ohai';
            scope.experiments = [
                {
                    label: 'angularjs',
                    applicationName: 'angularjs',
                    id: 1,
                    state: 'CLOSED',
                    percent: '2'
                }
            ];
            expect(testFn).to.not.throw();
        });
        
        it('should not throw with null input', function () {
            createController();
            var testFn = function () {
                scope.advSearch();
            };
            scope.data.query = '2';
            scope.data.advStatus = 'ohai';
            scope.experiments = [
                {
                    label: 'angularjs',
                    applicationName: 'angularjs',
                    id: 1,
                    state: 'CLOSED',
                    percent: '2'
                }
            ];
            expect(testFn).to.not.throw();
        });
        
        it('should set advApplicationName if null', function () {
            createController();
            var testFn = function () {
                scope.advSearch('angularjs');
            };
            scope.data.query = '2';
            scope.data.advStatus = 'ohai';
            scope.data.advApplicationName = null;
            scope.experiments = [
                {
                    label: 'angularjs',
                    applicationName: 'angularjs',
                    id: 1,
                    state: 'CLOSED',
                    percent: '2'
                }
            ];
            expect(testFn).to.not.throw();
            expect(scope.data.advApplicationName).to.eql('');
        });
        
        it('should handle adv1stDateSearchType types', function () {
            createController();
            
            var _types = ['isBefore', 'isOn', 'isAfter', 'isBetween'];
            _types.forEach(function(d){
                
                scope.data.adv1stDateSearchType = d;
                var testFn = function () {
                    scope.advSearch('angularjs');
                };
                scope.data.query = '2';
                scope.data.advStatus = 'ohai';
                scope.data.advApplicationName = null;
                scope.experiments = [
                    {
                        label: 'angularjs',
                        applicationName: 'angularjs',
                        id: 1,
                        state: 'CLOSED',
                        percent: '2'
                    }
                ];
                expect(testFn).to.not.throw();
                expect(scope.data.advApplicationName).to.eql('');
            });
        });
    });

    describe('#pageChanged', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.pageChanged();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#pageRangeStart', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.pageRangeStart();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#pageRangeEnd', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.pageRangeEnd();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#deleteExperiment', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                var exp = {
                    applicationName: 'angularjs',
                    label: 'angularjs',
                    id: 1
                };
                scope.deleteExperiment(exp);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#stateImgUrl', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.stateImgUrl('CLOSED');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#stateName', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.stateName('CLOSED');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#capitalizeFirstLetter', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.capitalizeFirstLetter('angularjs');
            };
            expect(testFn).to.not.throw();
            expect(scope.capitalizeFirstLetter('angularjs')).to.eql('Angularjs');
        });
    });
    
    describe('#hasPermission', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.hasPermission('angluarjs', 'ADMIN');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#sortBy', function () {

        it('should not throw', function () {
            createController();
            scope.data.query = '2';
            scope.data.advStatus = 'ohai';
            scope.data.advApplicationName = null;
            scope.experiments = [
                {
                    label: 'angularjs',
                    applicationName: 'angularjs',
                    id: 1,
                    state: 'CLOSED',
                    percent: '2'
                }
            ];
            var testFn = function () {
                scope.sortBy('label', false);
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on matching orderByField', function () {
            createController();
            scope.data.query = '2';
            scope.data.advStatus = 'ohai';
            scope.data.advApplicationName = null;
            scope.experiments = [
                {
                    label: 'angularjs',
                    applicationName: 'angularjs',
                    id: 1,
                    state: 'CLOSED',
                    percent: '2'
                }
            ];
            scope.orderByField = 'label';
            var testFn = function () {
                scope.sortBy(scope.orderByField, false);
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on reverseSort', function () {
            createController();
            scope.data.query = '2';
            scope.data.advStatus = 'ohai';
            scope.data.advApplicationName = null;
            scope.experiments = [
                {
                    label: 'angularjs',
                    applicationName: 'angularjs',
                    id: 1,
                    state: 'CLOSED',
                    percent: '2'
                }
            ];
            scope.orderByField = 'label';
            var testFn = function () {
                scope.sortBy(scope.orderByField, true);
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#hasDeletePermission', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.hasDeletePermission('angluarjs');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#hasUpdatePermission', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.hasDeletePermission('angluarjs');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#openExperimentModal', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.openExperimentModal('angluarjs');
            };
            expect(testFn).to.not.throw();
        });
    });
});