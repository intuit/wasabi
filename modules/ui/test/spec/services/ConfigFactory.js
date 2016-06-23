'use strict';

describe('ConfigFactory', function () {

    var ConfigFactory;
    beforeEach(inject(function(){
        var $injector = angular.injector(['wasabi.services', 'config']);

        ConfigFactory = function () {
            return $injector.get('ConfigFactory');
        };
    }));

    describe('Constructor', function () {

        it('exists and has correct properties', function () {
            var factory = new ConfigFactory();
            var properties = ['baseUrl', 'loginTimeoutWarningTime', 'loginTimeoutTime'];

            properties.forEach(function(d){
                expect(factory).to.have.property(d);
            });
        });
    });
});