'use strict';

describe('HttpInterceptor', function () {

    var HttpInterceptor, Session;
    beforeEach(inject(function(){
        var $injector = angular.injector(['ui.router', 'ngMock', 'wasabi', 'wasabi.services']);

        HttpInterceptor = function () {
            return $injector.get('HttpInterceptor');
        };
        
        Session = function () {
            return $injector.get('Session');
        };
    }));

    describe('Constructor', function () {

        it('exists and has correct properties', function () {
            var factory = new HttpInterceptor();
            var properties = ['request', 'response'];

            properties.forEach(function(d){
                expect(factory).to.have.property(d);
            });
        });
    });
    
    describe('#request', function () {

        it('exists and executes', function () {
            var factory = new HttpInterceptor();
            var cfg = {
                headers: {}
            };
            factory.request(cfg);
        });
    });
    
    describe('#response', function () {

        it('exists and executes', function () {
            var factory = new HttpInterceptor();
            var response = {
                config: {
                    data: {
                        password: '1'
                    }
                }
            };
            factory.response(response);
        });
    });
});