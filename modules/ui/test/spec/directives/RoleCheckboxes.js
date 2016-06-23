'use strict';

describe('RoleCheckboxes', function () {

    var el, scope, template, compiler, form;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div style="padding-bottom:10px">' +
                    '<form name="myForm">' +
                    '<input type="checkbox" id="chkAddAppWrite" ng-model="data.addWritePrivileges" ng-class="{disabled: data.addAdminPrivileges}" ng-disabled="data.addAdminPrivileges" >' +
                    '<input type="checkbox" id="chkAddAppAdmin" name="chkAddAppAdmin" role-checkboxes="chkAddAppWrite" ng-model="data.addAdminPrivileges">' +
                    '</form></div>'
        );
        
        compiler = $compile;
        scope = $rootScope.$new();
        scope.data = {
            'addAdminPrivileges': false
        };
        template = $compile(el)(scope);
        scope.$digest();
        form = scope.myForm;
    }));
    
    
    it('should exist', function () {
        expect(template.find('#chkAddAppAdmin').length).to.eql(1);
    });

    it('should handle value', function () {
        form.chkAddAppAdmin.$setViewValue(true);
        scope.$digest();

        expect(template.find('input').val()).to.eql('on');
    });
});