'use strict';

describe('StateFactory', function () {

    var StateFactory;
    beforeEach(inject(function(){
        var $injector = angular.injector(['wasabi.services']);

        StateFactory = function () {
            return $injector.get('StateFactory');
        };
    }));

    describe('Constructor', function () {

        it('exists and has correct properties', function () {
            var factory = new StateFactory();
            var properties = ['currentExperimentsPage', 'currentUsersPage', 'currentUsersInApplicationPage'];

            properties.forEach(function(d){
                expect(factory).to.have.property(d).to.eql(0);
            });
        });
    });
});