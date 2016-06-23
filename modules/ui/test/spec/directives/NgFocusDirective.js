'use strict';

describe('NgFocusDirective', function () {

    var el, scope, template, compiler;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div><input id="txtBucketName" type="text" class="form-control text" name="label" ng-focus ng-model="bucket" />' +
                    '</div>'
        );
        
        compiler = $compile;
        scope = $rootScope.$new();
        scope.bucket = {};
        template = $compile(el)(scope);
        scope.$digest();
    }));
    
    
    it('should exist', function () {
        expect(template.find('#txtBucketName').length).to.eql(1);
    });

    it('should handle blur', function () {
        template.find('input').eq(0).trigger('blur');
        scope.$digest();

        expect(template.find('input').length).to.eql(1);
    });
});