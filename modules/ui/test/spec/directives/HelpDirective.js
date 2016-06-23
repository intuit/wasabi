'use strict';

describe('HelpDirective', function () {

    var el, scope, template;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div><span class="info" help help-content="My help"></span><div id="help"><div id="helpContent"></div></div></div>'
        );
        
        scope = $rootScope.$new();
        template = $compile(el)(scope);
        scope.$digest();
    }));
    
    
    it('should exist', function () {
        expect(template.find('.info').length).to.eql(1);
        expect(template.css('visibility')).to.eql('');
    });

    it('should handle click', function () {
        template.find('.info').eq(0).trigger('click');
        scope.$digest();

        expect(template.find('.info').length).to.eql(1);
        //expect(template.find('#helpContent').text()).to.eql('My help');
    });

    it('should handle click on help dialog', function () {
        expect(template.find('#help').length).to.eql(1);
        template.find('#help').trigger('click');
        scope.$digest();

        expect(template.find('.info').length).to.eql(1);
        //expect(template.find('#helpContent').text()).to.eql('My help');
    });
});