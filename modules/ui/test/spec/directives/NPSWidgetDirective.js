'use strict';

describe('NPSWidgetDirective', function () {

    var el, scope, template, compiler;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div><ul id="nps" nps-widget>' +
                    '<li>0</li><li>1</li><li>2</li><li>3</li><li>4</li><li>5</li><li>6</li><li>7</li><li>8</li><li>9</li><li>10</li>' +
                    '</ul>' +
                    '</div>'
        );
        
        compiler = $compile;
        scope = $rootScope.$new();
        template = $compile(el)(scope);
        scope.$digest();
    }));
    
    
    it('should exist', function () {
        expect(template.find('#nps').length).to.eql(1);
    });

/*
Don't know how to get the $parent in the directive to work, get undefined error.
    it('should handle click', function () {
        //scope.$parent.feedback.score = 5;
        //expect(scope.$parent).to.eql(2);
        template.find('li').eq(0).trigger('click');
        scope.$digest();

        expect(template.find('.sel').length).to.eql(1);
    });
*/
});