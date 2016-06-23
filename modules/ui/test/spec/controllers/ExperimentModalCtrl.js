'use strict';

describe('ExperimentModalCtrl', function () {

    var scope, createController, modal, ConfigFactory, httpBackend, ApplicationsFactory, EmailFactory;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, $httpBackend){
        scope = $rootScope.$new();
        
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };

        httpBackend = $httpBackend;

        var mockApplicationsFactory = {
            query: function() {
                return {
                    $promise: {
                        then: function(doneFunc) {
                            doneFunc([
                                {
                                    applicationName: 'app1'
                                },
                                {
                                    applicationName: 'app2'
                                }
                            ]);
                        }
                    }
                };
            }
        };

        var mockEmailFactory = {
            saveFavorite: sinon.stub(),
            removeFavorite: sinon.stub()
        };

        angular.mock.inject(function($injector){
            ConfigFactory = $injector.get('ConfigFactory');
            ApplicationsFactory = mockApplicationsFactory;
            EmailFactory = mockEmailFactory;
        });

        createController = function (experiments, experiment, readOnly, applications, allApplications) {
            return $controller('ExperimentModalCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'experiments': experiments,
                'experiment': experiment,
                'readOnly': readOnly,
                'applications': applications || 'applications',
                'allApplications': allApplications,
                'openedFromModal': false
                
            });
        };
    }));

    describe('Constructor', function () {

        it('should execute and add keys to scope', function () {
            createController([], {}, false, ['angularjs']);
            var keys = ['experiment', 'readOnly', 'experimentFormSubmitted', 'experiments', 'applications', 'modalInstance', 'stateImgUrl', 'doSaveOrCreateExperiment', 'saveExperiment', 'cancel'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.exist;
            });
        });
    });
    
    describe('#stateImgUrl', function () {

        it('should not throw', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai'
            };
            createController([scope.experiment], scope.experiment, true, ['angularjs'], []);
            var testFn = function () {
                scope.stateImgUrl('CLOSED');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#doSaveOrCreateExperiment', function () {

        it('should not throw on input', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai'
            };
            createController([scope.experiment], scope.experiment, true, ['angularjs'], []);
            var testFn = function () {
                scope.doSaveOrCreateExperiment(1);
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on null input', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai'
            };
            createController([scope.experiment], scope.experiment, true, ['angularjs'], []);
            var testFn = function () {
                scope.doSaveOrCreateExperiment();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#saveExperiment', function () {

        it('should not throw on input and empty buckets', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai',
                buckets: []
            };
            createController([scope.experiment], scope.experiment, true, ['angularjs'], []);
            var testFn = function () {
                scope.saveExperiment(1);
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on null input and non-empty buckets', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai',
                buckets: [
                    {
                        allocationPercent: 0.5
                    }
                ]
            };
            createController([scope.experiment], scope.experiment, true, ['angularjs'], []);
            var testFn = function () {
                scope.saveExperiment();
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw on null experiment ID and new application', function () {
            scope.experiment = {
                applicationName: 'New application...',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none'
            };

            createController([scope.experiment], scope.experiment, true, ['angularjs1','angularjs2'], []);
            var testFn = function () {
                scope.saveExperiment(null, false);
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw on input and non-empty buckets', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai',
                buckets: [
                    {
                        allocationPercent: 0.5
                    }
                ]
            };
            createController([scope.experiment], scope.experiment, true, ['angularjs'], []);
            var testFn = function () {
                scope.saveExperiment(1);
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on totalAllocation adds to 1.0', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai',
                buckets: [
                    {
                        allocationPercent: 0.5
                    },
                    {
                        allocationPercent: 0.5
                    }
                ]
            };
            createController([scope.experiment], scope.experiment, true, ['angularjs'], []);
            var testFn = function () {
                scope.saveExperiment(1);
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw on totalAllocation adds to 1.0', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai',
                buckets: [
                    {
                        allocationPercent: 0.5
                    },
                    {
                        allocationPercent: 0.5
                    }
                ]
            };
            createController([scope.experiment], scope.experiment, true, ['angularjs'], []);
            var testFn = function () {
                scope.saveExperiment(1, true);
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#cancel', function () {

        it('should not throw', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai'
            };
            createController([scope.experiment], scope.experiment, true, ['angularjs'], []);
            var testFn = function () {
                scope.cancel();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#changed', function () {

        it('should not throw', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai'
            };
            createController([scope.experiment], scope.experiment, true, ['angularjs'], []);
            var testFn = function () {
                scope.changed();
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw with new application', function () {
            scope.experiment = {
                applicationName: 'New application...',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai'
            };
            var controller = createController([scope.experiment], scope.experiment, true, ['angularjs', 'angularjs2'], []);
            expect(ConfigFactory.newApplicationNamePrompt).to.eql(scope.experiment.applicationName);
            var testFn = function () {
                scope.changed();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#stateName', function () {

        it('should not throw', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai'
            };
            var controller = createController([scope.experiment], scope.experiment, true, ['angularjs', 'angularjs2'], []);
            var testFn = function () {
                scope.stateName('CLOSED');
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#handleNewApplication', function () {

        it('should not throw', function () {
            scope.experiment = {
                applicationName: 'angularjs',
                label: 'angularjs',
                samplingPercent: 50,
                startTime: Date.now(),
                endTime: Date.now() + 50000,
                description: 'none',
                rule: 'ohai'
            };
            var controller = createController([scope.experiment], scope.experiment, true, ['angularjs', 'angularjs2'], []);
            scope.oldAppName = ConfigFactory.newApplicationNamePrompt;
            var testFn = function () {
                scope.handleNewApplication();
            };
            expect(testFn).to.not.throw();
        });
    });

});