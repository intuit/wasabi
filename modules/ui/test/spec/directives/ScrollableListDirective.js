'use strict';

describe('ScrollableListDirective', function () {

    var el, scope, template, compiler;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div style="padding-bottom:10px">' +
                    '        <div id="addApplicationList" scrollable-list>' +
                    '<div class="scrollListHeader">' +
                    '</div>' +
                    '<div class="scrollListBody">' +
                    '</div></div></div>'
        );
        
        compiler = $compile;
        scope = $rootScope.$new();
        scope.data = {
            'addAdminPrivileges': false
        };
        template = $compile(el)(scope);
        scope.$digest();
    }));
    
    
    it('should exist', function () {
        expect(template.find('#addApplicationList').length).to.eql(1);
    });
});