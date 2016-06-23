'use strict';

describe('SearchDirective', function () {

    var el, scope, template, compiler, form;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div style="padding-bottom:10px">' +
                    '<form name="myForm">' +
                '<div class="search">' +
                    '<input search type="text" id="txtAddAppSearch" name="txtAddAppSearch" autocorrect="off" autocapitalize="off" placeholder="Search"' +
                    '  ng-model="data.query"' +
                    '  ng-change="search()"/>' +
                    '<a href="#" class="clear">Clear Search</a>' +
                    '</div></form></div>'
        );
        
        compiler = $compile;
        scope = $rootScope.$new();
        scope.data = {
            'query': ''
        };
        scope.search = function() {};
        template = $compile(el)(scope);
        scope.$digest();
        form = scope.myForm;
    }));
    
    
    it('should exist', function () {
        expect(template.find('#txtAddAppSearch').length).to.eql(1);
    });

    it('should handle click', function () {
        template.find('input').trigger('click');
        scope.$digest();

        expect(template.find('input').val()).to.eql('');
    });

    it('should handle focusin', function () {
        template.find('input').trigger('focusin');
        scope.$digest();

        expect(template.find('input').val()).to.eql('');
    });

    it('should handle focusout', function () {
        template.find('input').trigger('focusout');
        scope.$digest();

        expect(template.find('input').val()).to.eql('');
    });

    it('should handle clear', function () {
        template.find('.clear').trigger('click');
        scope.$digest();

        expect(template.find('input').val()).to.eql('');
    });
});