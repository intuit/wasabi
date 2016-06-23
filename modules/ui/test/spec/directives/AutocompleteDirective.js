'use strict';

describe('AutocompleteDirective', function () {

    var el, scope;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<input class="abtestAutocomplete" ng-show="!readOnly" list="pagesData.groupPages" select-function="selectPage" enter-function="selectPageOnEnter" id="txtAddPage" type="text" class="text" placeholder="Add Page" />'
            //'<input ng-show="!readOnly" abtest-autocomplete list="pagesData.groupPages" select-function="selectPage" enter-function="selectPageOnEnter" id="txtAddPage" type="text" class="text" placeholder="Add Page" />'
            //'<div class="abtestAutocomplete" abtest-autocomplete listSource="test" selectFunction="test" enterFunction="test"></div>'
        );
        
        scope = $rootScope.$new();
        $compile(el)(scope);
    }));
    
    
    it('should exist', function () {
        scope.pagesData = {
            groupPages: [
                'page1'
            ]
        };
        scope.selectPage = function(){};
        scope.selectPageOnEnter = function(){};
        scope.$digest();
        
        expect($(el)[0].className.indexOf('abtestAutocomplete')).to.be.above(-1);
        expect($(el)[0].className.indexOf('ng-scope')).to.be.above(-1);
    });
});