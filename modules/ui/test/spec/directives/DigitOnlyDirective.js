'use strict';

describe('DigitOnlyDirective', function () {

    var el, scope, form, template;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<form name="myForm"><input class="digitOnlyTest" name="digitOnlyInput" digit-only type="text" ng-model="myModel" /></form>'
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
        form.digitOnlyInput.$setViewValue('3');

        expect(scope.myModel).to.eql('3');
    });

    it('should handle non-number', function () {
        form.digitOnlyInput.$setViewValue('abc');

        expect(scope.myModel).to.eql('');
    });
});