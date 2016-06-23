'use strict';

describe('EnsureNumber', function () {

    var el, scope, form, template;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<form name="myForm"><input class="ensureNumberTest" name="ensureNumberInput" ensure-number type="text" ng-model="myModel" /></form>'
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
        form.ensureNumberInput.$setViewValue('3');
        scope.$digest();

        expect(scope.myModel).to.eql('3');
    });

    it('should handle non-number', function () {
        form.ensureNumberInput.$setViewValue('abc');
        scope.$digest();

        expect(scope.myModel).to.eql('abc');
    });
});