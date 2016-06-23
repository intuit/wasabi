'use strict';

describe('PagesCtrl', function () {

    var scope, createController, modal,
        mockDialogsFactory = {
            alertDialog: function(content, title, doneFunc) {
                doneFunc();
            },
            confirmDialog: function(content, title, doneFunc) {
                doneFunc();
            }
        },mockApplicationsFactory = {
            getPages: function(options) {
                return {
                    $promise: {
                        then: function(doneFunc) {
                            doneFunc([{
                                name: 'myGlobalPage'
                            }]);
                        }
                    }
                };
            }
        },
        mockExperimentsFactory = {
            getPages: function(options) {
                return {
                    $promise: {
                        then: function(doneFunc) {
                            doneFunc([]);
                        }
                    }
                };
            },
            savePages: function(options) {
                return {
                    $promise: {
                        then: function() {}
                    }
                };
            },
            removePage: function(options) {
                return {
                    $promise: {
                        then: function() {}
                    }
                };
            }
        };
    
    beforeEach(function() {
        module('wasabi', 'ui.router', 'wasabi.controllers');
        module({
            DialogsFactory: mockDialogsFactory,
            ExperimentsFactory: mockExperimentsFactory,
            ApplicationsFactory: mockApplicationsFactory
        })
    });
    beforeEach(inject(function($rootScope, $controller){
        scope = $rootScope.$new();
        
        modal = {
            close: sinon.stub(),
            dismiss: sinon.stub()
        };
        
        scope.experiment = {
            id: 1
        };
        
        createController = function () {
            return $controller('PagesCtrl', {
                '$scope': scope,
                '$modalInstance': modal,
                'pages': []
            });
        };
    }));
    
    
/*
    describe('Constructor', function () {

        it('should exist and bind some keys to scope', function () {
            createController();
            var keys = ['pages', 'pagesData', 'loadPages', 'loadGlobalPages', 'initPages', 'doSavePages', 'updatePage', 'selectPage', 'validateAndAddPage', 'selectPageOnEnter', 'addPageClick', 'removePage', 'addPageToList'];
            
            keys.forEach(function(d){
                expect(scope[d]).to.not.be.undefined;
            });
        });
    });
    
    describe('#loadPages', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.loadPages();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#loadGlobalPages', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.loadGlobalPages();
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw when applicationName', function () {
            createController();
            scope.experiment.applicationName = 'CTG';
            var testFn = function () {
                scope.loadGlobalPages();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#initPages', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.pages = [
                    {
                        name: 'angularjs'
                    }
                ];
                scope.initPages();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#doSavePages', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.pages = [
                    {
                        name: 'angularjs'
                    }
                ];
                scope.doSavePages('angularjs');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#updatePage', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                var page = {
                    name: 'angularjs',
                    allowNewAssignment: false
                };
                scope.updatePage(page);
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#validateAndAddPage', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.pages = [
                    {
                        name: 'angularjs'
                    }
                ];
                scope.validateAndAddPage('angularjs');
            };
            expect(testFn).to.not.throw();
        });
        
        it('should not throw with no pages', function () {
            createController();
            var testFn = function () {
                scope.validateAndAddPage('angularjs');
            };
            expect(testFn).to.not.throw();
        });
        
        it('should return false if not matching syntax', function () {
            createController();
            var testFn = function () {
                expect(scope.validateAndAddPage('1angularjs')).to.be.undefined;
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#addPageClick', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.addPageClick();
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#removePage', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.removePage();
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw when page name not empty', function () {
            createController();
            scope.pages = [];
            var testFn = function () {
                scope.removePage('myPage');
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw when page name not empty and pages array not empty, but no match', function () {
            createController();
            scope.pages = [{
                name: 'mypage2'
            }];
            var testFn = function () {
                scope.removePage('myPage');
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw when page name not empty and pages array not empty, and find match', function () {
            createController();
            scope.pages = [{
                name: 'myPage'
            }];
            var testFn = function () {
                scope.removePage('myPage');
            };
            expect(testFn).to.not.throw();
        });
    });
    
    describe('#addPageToList', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                scope.addPageToList();
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#selectPage', function () {

        it('should not throw', function () {
            createController();
            var testFn = function () {
                var event = {},
                    ui = {
                        item: {
                            label: 'test'
                        }
                    }
                scope.selectPage(event, ui);
            };
            expect(testFn).to.not.throw();
        });
    });

    describe('#selectPageOnEnter', function () {

        it('should not throw, addingPage true', function () {
            createController();
            scope.addingPage = true;
            var testFn = function () {
                scope.selectPageOnEnter();
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw, addingPage false', function () {
            createController();
            scope.addingPage = false;
            var testFn = function () {
                scope.selectPageOnEnter('myPage');
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw, addingPage false, new page', function () {
            createController();
            scope.addingPage = false;
            scope.pagesData.groupPages.push('bogus');
            var testFn = function () {
                scope.selectPageOnEnter('myPage');
            };
            expect(testFn).to.not.throw();
        });

        it('should not throw, addingPage false, existing page', function () {
            createController();
            scope.addingPage = false;
            scope.pagesData.groupPages.push('myPage');
            var testFn = function () {
                scope.selectPageOnEnter('myPage');
            };
            expect(testFn).to.not.throw();
        });
    });
*/

});