'use strict';

describe('ExperimentDetailsCtrl', function () {

    var scope, createController, state, modal;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, $stateParams){
        scope = $rootScope.$new();
        state = $stateParams;
        state['readonly'] = 'false';
        state['openedFromModal'] = 'false';

        createController = function () {
            return $controller('ExperimentDetailsCtrl', {
                '$scope': scope,
                '$stateParams': state,
                'simpleRuleEditing': false
            });
        };
    }));
    
    describe('Constructor', function () {

        it('should execute and not throw', function () {
            scope.application = {
                label: 'hello'
            };
            state.readonly = 'true';
            expect(createController).to.exist;
            expect(createController).to.not.throw();
        });
    });
    
    describe('loadExperiment', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.loadExperiment).to.exist;
            expect(scope.loadExperiment).to.not.throw();
        });
    });
    
    describe('loadBuckets', function () {

        it('should execute and not throw', function () {
            createController();
            state.experimentId = 1;
            expect(scope.loadBuckets).to.exist;
            expect(scope.loadBuckets).to.not.throw();
        });
    });
    
    describe('getBucket', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.getBucket).to.exist;
            expect(scope.getBucket).to.not.throw();
        });
    });
    
    describe('bucketState', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.bucketState).to.exist;
            expect(scope.bucketState).to.not.throw();
        });
        
        it('should return closed if state is CLOSED or EMPTY', function () {
            createController();
            expect(scope.bucketState({state: 'CLOSED'})).to.eql('closed');
            expect(scope.bucketState({state: 'EMPTY'})).to.eql('closed');
        });
    });
    
    describe('actionRate', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.actionRate).to.exist;
            var testFn = function () {
                var buckets = {
                    angularjs: {
                        label: 'angularjs',
                        jointActionRate: {
                            estimate: 0.5
                        }
                    }
                };
                scope.actionRate('angularjs', buckets);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('actionDiff', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.actionDiff).to.exist;
            var testFn = function () {
                var buckets = {
                    angularjs: {
                        label: 'angularjs',
                        jointActionRate: {
                            estimate: 0.5
                        }
                    }
                };
                scope.actionDiff('angularjs', buckets);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('actionRateTotal', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.actionRateTotal).to.exist;
            var testFn = function () {
                scope.experiment.statistics = {
                    jointActionRate: {
                        estimate: 1.0
                    }
                };
                scope.actionRateTotal();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should return 0 if jointActionRate.estimate is not a number NaN', function () {
            createController();
            var testFn = function () {
                scope.experiment.statistics = {
                    jointActionRate: {
                        estimate: 'true'
                    }
                };
                var result = scope.actionRateTotal();
                expect(result).to.eql(0);
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('actionDiffTotal', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.actionDiffTotal).to.exist;
            var testFn = function () {
                scope.experiment.statistics = {
                    jointActionRate: {
                        estimate: 1.0
                    }
                };
                scope.actionDiffTotal();
            };
            expect(testFn).to.not.throw();
        });
        
        it('should return 0 if jointActionRate.estimate is not a number NaN', function () {
            createController();
            expect(scope.actionDiffTotal).to.exist;
            var testFn = function () {
                scope.experiment.statistics = {
                    jointActionRate: {
                        estimate: 'true'
                    }
                };
                var result = scope.actionDiffTotal();
                expect(result).to.eql(0);
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('improvementClass', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.improvementClass).to.exist;
            var testFn = function () {
                var buckets = {
                    statistics: {
                        buckets: {
                            'angularjs': {
                                improvementClass: 'something'
                            }
                        }
                    }
                };
                scope.improvementClass('angularjs', buckets);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('improvement', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.improvement).to.exist;
            var testFn = function () {
                scope.experiment = {
                    statistics: {
                        buckets: {
                            'angularjs': {
                                improvementClass: 'something',
                                bucketComparisons: {
                                    'comp1': {
                                        otherLabel: 'angularjs',
                                        jointActionComparison: {
                                            actionRateDifference: {
                                                estimate: 1.0
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                };
                scope.improvement('angularjs');
            };
            expect(testFn).to.not.throw();
        });

        it('should return undefined if scope.controlBucketLabel is same as bucketLabel', function () {
            createController();
            expect(scope.improvement).to.exist;
            var testFn = function () {
                scope.experiment = {
                    statistics: {
                        buckets: {
                            'angularjs': {
                                improvementClass: 'something',
                                bucketComparisons: {
                                    'comp1': {
                                        otherLabel: 'angularjs',
                                        jointActionComparison: {
                                            actionRateDifference: {
                                                estimate: 'this is NaN'
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                };
                var oldLabel = scope.controlBucketLabel;
                scope.controlBucketLabel = 'angularjs';
                var result = scope.improvement('angularjs');
                scope.controlBucketLabel = oldLabel;
                expect(result).to.be.undefined;
            };
            expect(testFn).to.not.throw();
        });

/*
//Should be tested in UtilitiesFactory test.
        it('should return 0 if actionRateDifference.estimate is NaN', function () {
            createController();
            expect(scope.improvement).to.exist;
            var testFn = function () {
                scope.experiment = {
                    statistics: {
                        buckets: {
                            'angularjs': {
                                improvementClass: 'something',
                                bucketComparisons: {
                                    'comp1': {
                                        otherLabel: 'test',
                                        jointActionComparison: {
                                            actionRateDifference: {
                                                estimate: 'this is NaN'
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                };
                var oldLabel = scope.controlBucketLabel;
                scope.controlBucketLabel = 'test';
                expect(scope.improvement('angularjs')).to.eql(0);
                scope.controlBucketLabel = oldLabel;
            };
            expect(testFn).to.not.throw();
        });

        it('should return value if actionRateDifference.estimate is a number', function () {
            createController();
            expect(scope.improvement).to.exist;
            var testFn = function () {
                scope.experiment = {
                    statistics: {
                        buckets: {
                            'angularjs': {
                                improvementClass: 'something',
                                bucketComparisons: {
                                    'comp1': {
                                        otherLabel: 'test',
                                        jointActionComparison: {
                                            actionRateDifference: {
                                                estimate: 0.5
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                };
                var oldLabel = scope.controlBucketLabel;
                scope.controlBucketLabel = 'test';
                expect(scope.improvement('angularjs')).to.be.above(0);
                scope.controlBucketLabel = oldLabel;
            };
            expect(testFn).to.not.throw();
        });
*/
    });

    describe('improvementDiff', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.improvementDiff).to.exist;
            var testFn = function () {
                scope.experiment.statistics = {
                    jointActionRate: {
                        estimate: 1.0
                    }
                };
                scope.improvementDiff();
            };
            expect(testFn).to.not.throw();
        });
        
/*
Should be tested in UtilitiesFactory test
        it('should return zero if estimate is NaN', function () {
            createController();
            expect(scope.improvementDiff).to.exist;
            var testFn = function () {
                scope.experiment = {
                    statistics: {
                        buckets: {
                            'angularjs': {
                                improvementClass: 'something',
                                bucketComparisons: {
                                    'comp1': {
                                        otherLabel: 'test',
                                        jointActionComparison: {
                                            actionRateDifference: {
                                                estimate: 'this is NaN'
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                };
                var oldLabel = scope.controlBucketLabel;
                scope.controlBucketLabel = 'test';
                var result = scope.improvementDiff('angularjs');
                scope.controlBucketLabel = oldLabel;
                expect(result).to.eql(0);
            };
            expect(testFn).to.not.throw();
        });

        it('should return value if estimate is a Number', function () {
            createController();
            expect(scope.improvementDiff).to.exist;
            var testFn = function () {
                scope.experiment = {
                    statistics: {
                        buckets: {
                            'angularjs': {
                                improvementClass: 'something',
                                bucketComparisons: {
                                    'comp1': {
                                        otherLabel: 'test',
                                        jointActionComparison: {
                                            actionRateDifference: {
                                                estimate: 2,
                                                upperBound: 2,
                                                lowerBound: 1
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                };
                var oldLabel = scope.controlBucketLabel;
                scope.controlBucketLabel = 'test';
                var result = scope.improvementDiff('angularjs');
                scope.controlBucketLabel = oldLabel;
                expect(result).to.be.above(0);
            };
            expect(testFn).to.not.throw();
        });
*/
    });

    describe('significance', function () {

        it('should execute and not throw for loser so far', function () {
            createController();
            expect(scope.significance).to.exist;
            var testFn = function () {
                scope.experiment = {
                    statistics: {
                        jointProgress: {
                            winnersSoFar: [],
                            losersSoFar: ['angularjs']
                        }
                    }
                };
                expect(scope.significance('angularjs')).to.eql('loser so far');
            };
            expect(testFn).to.not.throw();
        });
        
        it('should execute and not throw for winner so far', function () {
            createController();
            expect(scope.significance).to.exist;
            var testFn = function () {
                scope.experiment = {
                    statistics: {
                        jointProgress: {
                            winnersSoFar: ['angularjs']
                        }
                    }
                };
                expect(scope.significance('angularjs')).to.eql('winner so far');
            };
            expect(testFn).to.not.throw();
        });
        
        it('should execute and not throw for undefined', function () {
            createController();
            expect(scope.significance).to.exist;
            var testFn = function () {
                scope.experiment = {
                    statistics: {
                        jointProgress: {
                            winnersSoFar: [],
                            losersSoFar: []
                        }
                    }
                };
                expect(scope.significance('angularjs')).to.eql('undetermined');
            };
            expect(testFn).to.not.throw();
        });
    });

/*
    describe('saveSegmentation', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.saveSegmentation).to.exist;
            var testFn = function () {
                scope.experiment.id = 1;
                scope.experiment.statistics = {
                    jointActionRate: {
                        estimate: 1.0
                    }
                };
                scope.simpleRuleEditing = false;
                scope.saveSegmentation(1);
            };
            expect(testFn).to.not.throw();
        });
    });
*/

    describe('openBucketModal', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.openBucketModal).to.exist;
            var testFn = function () {
                var bucketInfo = {
                    impressionCounts: {
                        eventCount: 50
                    },
                    jointActionCounts: {
                        eventCount: 50
                    }
                };
                scope.openBucketModal(1, 'angularjs', bucketInfo);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('openEditRunningBucketsModal', function () {

        it('should execute and not throw', function () {
            createController();
            expect(scope.openEditRunningBucketsModal).to.exist;
            var testFn = function () {
                scope.openEditRunningBucketsModal(scope.experiment);
            };
            expect(testFn).to.not.throw();
        });
    });
});