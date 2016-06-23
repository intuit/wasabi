'use strict';

describe('EnsureInterval', function () {

    var el, scope, form, template;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<form name="myForm"><input class="ensureIntervalTest" name="ensureIntervalInput" ensure-interval type="text" ng-model="myModel" /></form>'
        );
        
        scope = $rootScope.$new();
        template = $compile(el)(scope);
        scope.$digest();
        form = scope.myForm;
    }));
    
    
    it('should exist', function () {
        scope.myModel = '';
        scope.$digest();

        var value = template.find('input').val();
        expect(value).to.eql('');
    });

    it('should handle number', function () {
        form.ensureIntervalInput.$setViewValue('3');
        scope.$digest();

        expect(scope.myModel).to.eql('3');
    });

    it('should handle number in range', function () {
        form.ensureIntervalInput.$setViewValue(.5);
        scope.$digest();

        expect(scope.myModel).to.eql(0.5);
    });

    it('should handle non-number', function () {
        form.ensureIntervalInput.$setViewValue('abc');
        scope.$digest();

        expect(scope.myModel).to.eql('abc');
    });
});