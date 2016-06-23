'use strict';

describe('NoSpacesDirective', function () {

    var el, scope, template, compiler, form;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div><form name="myForm"><input id="txtBucketName" type="text" class="form-control text" name="label" no-spaces ng-model="bucket" />' +
                    '</form></div>'
        );
        
        compiler = $compile;
        scope = $rootScope.$new();
        scope.bucket = {};
        template = $compile(el)(scope);
        scope.$digest();
        form = scope.myForm;
    }));
    
    
    it('should exist', function () {
        expect(template.find('#txtBucketName').length).to.eql(1);
    });

    it('should handle value', function () {
        form.label.$setViewValue(' abc ');
        scope.$digest();

        expect(template.find('input').val()).to.eql('abc');
    });
});