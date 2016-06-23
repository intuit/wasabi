'use strict';

chai.Assertion.addProperty('lowercase', function () {
    var obj = this._obj;
    new chai.Assertion(obj).to.be.a('string');

    this.assert(obj === obj.toLowerCase(), 'expected #{this} to be all lowercase', 'expected #{this} to not be all lowercase');
});


describe('UtilitiesFactory', function () {

    var UtilitiesFactory, Session;
    beforeEach(inject(function(){
        var $injector = angular.injector(['ui.router', 'ngMock', 'wasabi', 'wasabi.services']);

        UtilitiesFactory = function () {
            return $injector.get('UtilitiesFactory');
        };
        
        Session = function () {
            return $injector.get('Session');
        };
    }));

    describe('Constructor', function () {

        it('exists and has correct properties', function () {
            var factory = new UtilitiesFactory();
            var properties = ['stateImgUrl', 'stateName', 'capitalizeFirstLetter', 'arrayToString', 'selectTopLevelTab', 'hideTopLevelTab', 'displayPageError', 'hideHeading', 'handleGlobalError', 'extractErrorFromResponse', 'failIfTokenExpired', 'trackEvent', 'doTrackingInit', 'formatApplicationList', 'filterAppsForUser', 'hasPermission', 'hasAdmin', 'getAppsWithAnyPermissions', 'getAdministeredApplications', 'userHasRole', 'userHasRoleForApplication', 'sortByHeadingValue', 'actionRate', 'actionDiff', 'significance', 'saveFavorite', 'removeFavorite', 'retrieveFavorites', 'getBucket', 'getFormattedStartAndEndDates', 'determineBucketImprovementClass'];

            properties.forEach(function(d){
                expect(factory).to.have.property(d).that.is.a('function');
            });
        });
    });

    describe('#stateImgUrl', function () {

        it('returns a .png path given valid input', function () {
            var factory = new UtilitiesFactory();
            var result = factory.stateImgUrl('test');
            expect(result).to.be.a('string');
            expect(result).to.include('.png');
        });

        it('returns lower case .png path given valid input', function () {
            var factory = new UtilitiesFactory();
            var result = factory.stateImgUrl('TEST');
            expect(result).to.be.lowercase;
        });
    });

    describe('#stateName', function () {

        it('returns "stopped" given input "paused"', function () {
            var factory = new UtilitiesFactory();
            var result = factory.stateName('paused');
            expect(result).to.eql('stopped');
        });

        it('returns input given non-"paused" input', function () {
            var factory = new UtilitiesFactory();
            var result = factory.stateName('hello');
            var result2 = factory.stateName('HelLO');
            expect(result).to.eql('hello');
            expect(result2).to.eql('hello');
        });
    });

    describe('#capitalizeFirstLetter', function () {

        it('returns input with only first letter capitalized', function () {
            var factory = new UtilitiesFactory();
            var cases = [
                ['hello', 'Hello'],
                ['hello world', 'Hello world'],
                ['1day', '1day']
            ];

            cases.forEach(function(d){
                expect(factory.capitalizeFirstLetter(d[0])).to.eql(d[1]);
            });
        });
    });

    // describe('#arrayToString', function () {

    //     it('')
    // });

    describe('#selectTopLevelTab', function () {

        it('performs correctly without permissions', function () {
            var factory = new UtilitiesFactory();
            sinon.spy(factory, 'hasAdmin');
            factory.selectTopLevelTab('Users');
            expect(factory.hasAdmin.calledOnce).to.eql(true);
        });
        
        it('performs correctly while bypassing permissions with admin', function () {
            var factory = new UtilitiesFactory();
            factory._isTesting = true;
            sinon.spy(factory, 'hideTopLevelTab');
            factory.selectTopLevelTab('Users', ['ADMIN']);
            expect(factory.hideTopLevelTab.called).to.eql(true);
        });
        
        it('performs correctly while bypassing permissions without admin', function () {
            var factory = new UtilitiesFactory();
            factory._isTesting = true;
            sinon.spy(factory, 'hideTopLevelTab');
            factory.selectTopLevelTab('Users', []);
            expect(factory.hideTopLevelTab.called).to.eql(true);
        });
    });

    describe('#hideTopLevelTab', function () {

        it('does not throw with hideIt=true', function () {
            var factory = new UtilitiesFactory();
            sinon.spy(factory, 'hasAdmin');
            var testFn = function () { factory.hideTopLevelTab('Users', true); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('does not throw with hideIt=false', function () {
            var factory = new UtilitiesFactory();
            sinon.spy(factory, 'hasAdmin');
            var testFn = function () { factory.hideTopLevelTab('Users', false); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('does not throw with hideIt=undefined', function () {
            var factory = new UtilitiesFactory();
            sinon.spy(factory, 'hasAdmin');
            var testFn = function () { factory.hideTopLevelTab('Users'); };
            expect(testFn).to.not.throw(Error);
        });
    });


    describe('#displayPageError', function () {

        it('does not throw with show=true', function () {
            var factory = new UtilitiesFactory();
            var testFn = function () { factory.displayPageError('Users', 'hello', true); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('does not throw with show=false', function () {
            var factory = new UtilitiesFactory();
            var testFn = function () { factory.displayPageError('Users', 'hello', false); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('closePageError:click does not throw', function () {
            var factory = new UtilitiesFactory();
            var testFn = function () { factory.displayPageError('Users', 'hello', true); };
            expect(testFn).to.not.throw(Error);
            expect(function(){ $('.closePageError').click(); }).to.not.throw();
        });
    });
    
    describe('#handleGlobalError', function () {

        it('does not throw with status=200', function () {
            var factory = new UtilitiesFactory();
            var response = {
                status: 200,
                data: 'ohai'
            };
            var _defaultMessage = 'hello';
            var testFn = function () { factory.handleGlobalError(response, _defaultMessage); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('does not throw with status=400', function () {
            var factory = new UtilitiesFactory();
            var response = {
                status: 400,
                data: {
                        error:
                            {
                                message: 'hello'
                            }
                }
            };
            var _defaultMessage = 'hello';
            var testFn = function () { factory.handleGlobalError(response, _defaultMessage); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('does not throw with status=401', function () {
            var factory = new UtilitiesFactory();
            var response = {
                status: 401,
                data: {
                    errors: {
                        error: [
                            {
                                detail: 'hello'
                            }
                        ]
                    }
                }
            };
            var _defaultMessage = 'hello';
            var testFn = function () { factory.handleGlobalError(response, _defaultMessage); };
            expect(testFn).to.not.throw(Error);
        });
    });


    describe('#extractErrorFromResponse', function () {

        it('does not throw with status=400 with non unique constraint violation', function () {
            var factory = new UtilitiesFactory();
            var response = {
                status: 400,
                data: {
                        error:
                            {
                                message: 'hello'
                            }
                }
            };
            var testFn = function () { factory.extractErrorFromResponse(response); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('does not throw with status=400 with unique constraint violation', function () {
            var factory = new UtilitiesFactory();
            var response = {
                status: 400,
                data: {
                        error:
                            {
                                message: 'unique constraint violation'
                            }
                }
            };
            var testFn = function () { factory.extractErrorFromResponse(response); };
            expect(testFn).to.not.throw(Error);
            expect(factory.extractErrorFromResponse(response)).to.eql('nonUnique');
        });

        it('does not throw with status=401', function () {
            var factory = new UtilitiesFactory();
            var response = {
                status: 401,
                data: {
                    errors: {
                        error: [
                            {
                                detail: 'hello'
                            }
                        ]
                    }
                }
            };
            var testFn = function () { factory.extractErrorFromResponse(response); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('does not throw with status=200', function () {
            var factory = new UtilitiesFactory();
            var response = {
                status: 200,
                data: {
                    errors: {
                        error: [
                            {
                                detail: 'hello'
                            }
                        ]
                    }
                }
            };
            var testFn = function () { factory.extractErrorFromResponse(response); };
            expect(testFn).to.not.throw(Error);
        });
    });


    describe('#failIfTokenExpired', function () {

        it('does not throw with modal test', function () {
            var factory = new UtilitiesFactory();
            var modal = {
                close: sinon.spy()
            };
            var testFn = function () { factory.failIfTokenExpired(modal); };
            expect(testFn).to.not.throw(Error);
            expect(modal.close.calledOnce).to.exist;
        });
    });
    
    describe('#trackEvent', function () {

        it('does not throw', function () {
            var factory = new UtilitiesFactory();
            var parm = {
                key: 'hello',
                value: 'angularjs'
            };
            var testFn = function () { factory.trackEvent('karma test', parm, parm, parm, parm); };
            expect(testFn).to.not.throw(Error);
        });
    });
    
    describe('#formatApplicationList', function () {

        it('does not throw', function () {
            var factory = new UtilitiesFactory();
            var apps = [
                {
                    label: 'Angularjs'
                },
                {
                    label: 'Angularjs2'
                }
            ];
            var testFn = function () { factory.formatApplicationList(apps); };
            expect(testFn).to.not.throw(Error);
        });
    });
    
    describe('#filterAppsForUser', function () {

        it('does not throw', function () {
            var factory = new UtilitiesFactory();
            var apps = [
                {
                    label: 'Angularjs'
                },
                {
                    label: 'Angularjs2'
                }
            ];
            var testFn = function () { factory.filterAppsForUser(apps, apps); };
            expect(testFn).to.not.throw(Error);
        });
    });
    
    describe('#hasPermission', function () {

        // var SessionFactory = new Session();
        // var sessionInfo = {
        //     userID: 'angular',
        //     accessToken: '12345',
        //     tokenType: 'random',
        //     userRole: 'ADMIN',
        //     isSuperadmin: true,
        //     permissions: ['ADMIN']
        // };
        // var session = SessionFactory.create(sessionInfo);
        
        it('does not throw', function () {
            var factory = new UtilitiesFactory();
            var testFn = function () { factory.hasPermission('test', 'ADMIN'); };
            expect(testFn).to.not.throw(Error);
            // TODO(van): this currently does not work properly
        });
    });
    
    describe('#hasAdmin', function () {
        
        it('does not throw', function () {
            var factory = new UtilitiesFactory();
            var perms = [
                {
                    permissions: ['ADMIN']
                }
            ];
            var testFn = function () { factory.hasAdmin(perms); };
            expect(testFn).to.not.throw(Error);
            // TODO(van): this currently does not work properly
        });
    });
    
    describe('#getAppsWithAnyPermissions', function () {
        
        it('does not throw', function () {
            var factory = new UtilitiesFactory();
            var testFn = function () { factory.getAppsWithAnyPermissions(); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('should return empty array with no apps/session', function () {
            var factory = new UtilitiesFactory();
            var testFn = function () { factory.getAppsWithAnyPermissions(); };
            expect(testFn).to.not.throw(Error);
            expect(factory.getAppsWithAnyPermissions()).to.eql([]);
        });
    });
    
    
    describe('#userHasRole', function () {
        
        it('should return true with READWRITE role and privName=write', function () {
            var factory = new UtilitiesFactory();
            var user = {
                applications: [
                    {
                        role: 'READWRITE'
                    }
                ]
            };
            var privName = 'write';
            var testFn = function () { factory.userHasRole(user, privName); };
            expect(testFn).to.not.throw(Error);
            expect(factory.userHasRole(user, privName)).to.be.true;
        });
        
        it('should return true with ADMIN role and privName=write', function () {
            var factory = new UtilitiesFactory();
            var user = {
                applications: [
                    {
                        role: 'ADMIN'
                    }
                ]
            };
            var privName = 'write';
            var testFn = function () { factory.userHasRole(user, privName); };
            expect(testFn).to.not.throw(Error);
            expect(factory.userHasRole(user, privName)).to.be.true;
        });
        
        it('should return true with ADMIN role and privName=write', function () {
            var factory = new UtilitiesFactory();
            var user = {
                applications: [
                    {
                        role: 'ADMIN'
                    }
                ]
            };
            var privName = 'admin';
            var testFn = function () { factory.userHasRole(user, privName); };
            expect(testFn).to.not.throw(Error);
            expect(factory.userHasRole(user, privName)).to.be.true;
        });
    });

    describe('#userHasRoleForApplication', function () {
        
        it('should return true with ADMIN role and privName=admin', function () {
            var factory = new UtilitiesFactory();
            var appName = 'hello';
            var user = {
                applications: [
                    {
                        label: appName,
                        role: 'ADMIN'
                    }
                ]
            };
            var privName = 'admin';
            var testFn = function () { factory.userHasRoleForApplication(user, appName, privName); };
            expect(testFn).to.not.throw(Error);
            expect(factory.userHasRoleForApplication(user, appName, privName)).to.be.true;
        });
        
        it('should return true with ADMIN role and privName=write', function () {
            var factory = new UtilitiesFactory();
            var appName = 'hello';
            var user = {
                applications: [
                    {
                        label: appName,
                        role: 'ADMIN'
                    }
                ]
            };
            var privName = 'write';
            var testFn = function () { factory.userHasRoleForApplication(user, appName, privName); };
            expect(testFn).to.not.throw(Error);
            expect(factory.userHasRoleForApplication(user, appName, privName)).to.be.true;
        });
    });

    describe('#sortByHeadingValue', function () {
        
        it('should sort by admin', function () {
            var factory = new UtilitiesFactory();
            var items = [
                {
                    label: 'angularjs',
                    application: 'test',
                    role: 'ADMIN'
                }
            ];
            var orderBy = 'admin';
            var reverse = false;
            var testFn = function () { factory.sortByHeadingValue(items, orderBy, reverse); };
            expect(testFn).to.not.throw(Error);
            expect(factory.sortByHeadingValue(items, orderBy, reverse).length).to.eql(items.length);
        });
        
        it('should sort by applications', function () {
            var factory = new UtilitiesFactory();
            var items = [
                {
                    label: 'angularjs',
                    application: 'test',
                    role: 'ADMIN'
                }
            ];
            var orderBy = 'applications';
            var reverse = false;
            var testFn = function () { factory.sortByHeadingValue(items, orderBy, reverse); };
            expect(testFn).to.not.throw(Error);
            expect(factory.sortByHeadingValue(items, orderBy, reverse).length).to.eql(items.length);
        });
    });

    describe('#actionRate', function () {
        
        it('should respond properly with an actual Number', function () {
            var factory = new UtilitiesFactory();
            var label = 'test';
            var _input = 0.2;
            var _output = 20;
            var buckets = {
                'test': {
                    jointActionRate: {
                        estimate: _input
                    }
                }
            };
            var testFn = function () { factory.actionRate(label, buckets); };
            expect(testFn).to.not.throw(Error);
            expect(factory.actionRate(label, buckets)).to.eql(_output);
        });
        
        it('should respond properly with NaNs', function () {
            var factory = new UtilitiesFactory();
            var label = 'test';
            var buckets = {
                'test': {
                    jointActionRate: {
                        estimate: 'test'
                    }
                }
            };
            var testFn = function () { factory.actionRate(label, buckets); };
            expect(testFn).to.not.throw(Error);
            expect(factory.actionRate(label, buckets)).to.eql(0);
        });
    });

    describe('#actionDiff', function () {
        
        it('should respond properly with an actual Number', function () {
            var factory = new UtilitiesFactory();
            var label = 'test';
            var _input = 0.2;
            var _output = 20;
            var buckets = {
                'test': {
                    jointActionRate: {
                        estimate: _input
                    }
                }
            };
            var testFn = function () { factory.actionDiff(label, buckets); };
            expect(testFn).to.not.throw(Error);
            expect(factory.actionRate(label, buckets)).to.eql(_output);
        });
        
        it('should respond properly with NaNs', function () {
            var factory = new UtilitiesFactory();
            var label = 'test';
            var buckets = {
                'test': {
                    jointActionRate: {
                        estimate: 'test'
                    }
                }
            };
            var testFn = function () { factory.actionDiff(label, buckets); };
            expect(testFn).to.not.throw(Error);
            expect(factory.actionDiff(label, buckets)).to.eql(0);
        });
    });

    describe('#actionRate', function () {
        
        it('should respond properly with an actual Number', function () {
            var factory = new UtilitiesFactory();
            var label = 'test';
            var _input = 0.2;
            var _output = 20;
            var buckets = {
                'test': {
                    jointActionRate: {
                        estimate: _input
                    }
                }
            };
            var testFn = function () { factory.actionRate(label, buckets); };
            expect(testFn).to.not.throw(Error);
            expect(factory.actionRate(label, buckets)).to.eql(_output);
        });
        
        it('should respond properly with NaNs', function () {
            var factory = new UtilitiesFactory();
            var label = 'test';
            var buckets = {
                'test': {
                    jointActionRate: {
                        estimate: 'test'
                    }
                }
            };
            var testFn = function () { factory.actionRate(label, buckets); };
            expect(testFn).to.not.throw(Error);
            expect(factory.actionRate(label, buckets)).to.eql(0);
        });
    });

    describe('#saveFavorite', function () {
        
        it('should not throw when passed in simple inputs', function () {
            var factory = new UtilitiesFactory();
            var app = 'test';
            var experiment = 'angular';
            var testFn = function () { factory.saveFavorite(app, experiment); };
            expect(testFn).to.not.throw(Error);
        });
    });
    
    describe('#removeFavorite', function () {
        
        it('should not throw when passed in simple inputs', function () {
            var factory = new UtilitiesFactory();
            var app = 'test';
            var experiment = 'angular';
            var testFn = function () { factory.removeFavorite(app, experiment); };
            expect(testFn).to.not.throw(Error);
        });
    });
    
    describe('#retrieveFavorites', function () {
        
        it('should not throw when passed in simple inputs', function () {
            var factory = new UtilitiesFactory();
            var testFn = function () { factory.retrieveFavorites(); };
            expect(testFn).to.not.throw(Error);
        });
    });
    
    describe('#getBucket', function () {
        
        it('should not throw when bucket label is found', function () {
            var factory = new UtilitiesFactory();
            var label = 'test';
            var experiment = {
                buckets: [
                    {
                        label: label
                    }
                ]
            };
            var testFn = function () { factory.getBucket(label, experiment); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('should not throw when bucket label is not found', function () {
            var factory = new UtilitiesFactory();
            var label = 'test';
            var experiment = {
                buckets: [
                    {
                        label: 'label'
                    }
                ]
            };
            var testFn = function () { factory.getBucket(label, experiment); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('should not throw when `experiments` is not found', function () {
            var factory = new UtilitiesFactory();
            var label = 'test';
            var testFn = function () { factory.getBucket(label); };
            expect(testFn).to.not.throw(Error);
        });
    });

    describe('#getFormattedStartAndEndDates', function () {
        
        it('should not throw when passed in simple inputs', function () {
            var factory = new UtilitiesFactory();
            var ts = Date.now();
            var experiment = {
                startTime: new Date(ts - 10000000),
                endTime: new Date(ts - 10000)
            };
            var testFn = function () { factory.getFormattedStartAndEndDates(experiment); };
            expect(testFn).to.not.throw(Error);
        });
    });
    
    describe('#determineBucketImprovementClass', function () {

        var testBucket = {
            label: 'test',
            homePageTooltip: 'test',
            jointActionRate: {
                estimate: 1
            }
        };
        var ctrlBucket = {
            label: 'control',
            homePageTooltip: 'control',
            jointActionRate: {
                estimate: 0.5
            }
        };
        
        it('should not throw when passed in simple inputs', function () {
            var factory = new UtilitiesFactory();
            
            var experiment = {
                progress: 100,
                statistics: {
                    buckets: {
                        'test': testBucket,
                        'control': ctrlBucket
                    },
                    jointProgress: {
                        winnersSoFar: ['test'],
                        losersSoFar: ['control']
                    },
                },
                buckets: [
                    testBucket, ctrlBucket
                ]
            };
            var label = 'control';
            var testFn = function () { factory.determineBucketImprovementClass(experiment, label); };
            expect(testFn).to.not.throw(Error);
        });
        
        it('should not throw when no winners so far', function () {
            var factory = new UtilitiesFactory();
            
            var experiment = {
                progress: 100,
                statistics: {
                    buckets: {
                        'test': testBucket,
                        'control': ctrlBucket
                    },
                    jointProgress: {
                        losersSoFar: ['control']
                    },
                },
                buckets: [
                    testBucket, ctrlBucket
                ]
            };
            var label = 'control';
            var testFn = function () { factory.determineBucketImprovementClass(experiment, label); };
            expect(testFn).to.not.throw(Error);
        });
    });
});