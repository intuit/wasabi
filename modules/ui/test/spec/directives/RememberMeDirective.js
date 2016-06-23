'use strict';

describe('RememberMeDirective', function () {

    var el, scope, template, compiler, form;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div style="padding-bottom:10px">' +
                    '<form name="myForm"><input remember-me type="checkbox" id="chkRememberMe" ng-model="credentials.rememberMe" name="remember"><label for="chkRememberMe" class="checkboxLabel">Remember me</label>' +
                    '</form></div>'
        );
        
        compiler = $compile;
        scope = $rootScope.$new();
        scope.credentials = {
            'rememberMe': false
        };
        template = $compile(el)(scope);
        scope.$digest();
        form = scope.myForm;
    }));
    
    
    it('should exist', function () {
        expect(template.find('#chkRememberMe').length).to.eql(1);
    });

    it('should handle value', function () {
        form.remember.$setViewValue(true);
        scope.$digest();

        expect(template.find('input').val()).to.eql('on');
    });

    it('should handle click', function () {
        template.find('input').trigger('click');
        scope.$digest();

        expect(template.find('input').val()).to.eql('on');
    });
});