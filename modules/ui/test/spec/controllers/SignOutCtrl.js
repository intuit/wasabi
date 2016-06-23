'use strict';

describe('SignOutCtrl', function () {

    var scope, $location, createController;
    
    beforeEach(module('wasabi', 'ui.router', 'wasabi.controllers'));
    beforeEach(inject(function($rootScope, $controller, _$location_){
        $location = _$location_;
        scope = $rootScope.$new();
        
        
        createController = function () {
            return $controller('SignOutCtrl', {
                '$scope': scope
            });
        };
    }));
    
    // // There is an issue since this test runner is not asynchronous
    // //    setTimeout will not always called at the right time
    // it('should redirect to /signin', function () {
    //     var controller = createController();
    //     $location.path('/signout');
        
    //     scope.signOut();
    //     setTimeout(function(){
    //         expect($location.path()).to.eql('/signin');
    //     }, 1000);
    //     controller = null;
    // });
});