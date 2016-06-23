'use strict';

describe('ConvertPercentDirective', function () {

    var el, scope;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<input class="convertPercentTest" convert-percent type="text" ng-model="myModel" />'
        );
        
        scope = $rootScope.$new();
        $compile(el)(scope);
    }));
    
    
    it('should exist', function () {
        scope.myModel = '';
        scope.$digest();

        var value = el.val();
        expect(value).to.eql('0');
    });

    it('should handle number', function () {
        scope.myModel = .053;
        scope.$digest();

        var value = el.val();
        expect(value).to.eql('5.3');
    });

    it('should handle string', function () {
        scope.myModel = '100%';
        scope.$digest();

        var value = el.val();
        expect(value).to.eql('100%');
    });
});