'use strict';

describe('CustomTooltip', function () {

    var el, scope, template, compiler, MockTooltipFactory;
    beforeEach(function() {
        module('wasabi.directives');

        var mockTooltipFactory = {
            createAndShowTooltip: function(tooltipText, element) {
                return $('<div class="tooltip"><span class="arrow"></span>' + tooltipText + '</div>').appendTo($('body'));
            },
            hideTooltip: function($tip) {
                $tip.remove();
            }
        };
        module(function ($provide) {
            $provide.value('TooltipFactory', mockTooltipFactory);
        });
    });
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<input class="customTooltipTest" custom-tooltip="My tooltip" type="text" />'
        );

        compiler = $compile;
        scope = $rootScope.$new();
        template = $compile(el)(scope);
    }));
    
    
    it('should exist', function () {
        expect(template.hasClass('customTooltipTest')).to.eql(true);
    });

    it('should handle mouseenter', function () {
        template.triggerHandler('mouseenter');
        expect($('body').find('.tooltip').length).to.eql(1);
        expect($('body').find('.tooltip').text()).to.eql('My tooltip');
        $('body').find('.tooltip').remove();
    });

    it('should handle mouseleave', function () {
        if ($('body').find('.tooltip').length !== 1) {
            template.triggerHandler('mouseenter');
        }
        expect($('body').find('.tooltip').length).to.eql(1);
        template.triggerHandler('mouseleave');
        scope.$digest();
        expect($('body').find('.tooltip').length).to.eql(0);
    });

    it('should handle click', function () {
        if ($('body').find('.tooltip').length !== 1) {
            template.triggerHandler('mouseenter');
        }
        expect($('body').find('.tooltip').length).to.eql(1);
        template.triggerHandler('click');
        scope.$digest();
        expect($('body').find('.tooltip').length).to.eql(0);
    });

    // TODO: These should now be tests on TooltipFactory, which CustomTooltip was refactored to use.
/*
    it('should handle mouseenter and tipwidth', function () {
        template, el = null;
        el = angular.element(
            '<input class="customTooltipTest2" tipwidth="20px" custom-tooltip="My tooltip2" type="text" />'
        );

        template = compiler(el)(scope);
        scope.$digest();
        expect(template.hasClass('customTooltipTest2')).to.eql(true);

        template.triggerHandler('mouseenter');
        expect($('body').find('.tooltip').length).to.eql(1);
        expect($('body').find('.tooltip').css('width')).to.eql('20px');
        $('body').find('.tooltip').remove();
    });

    it('should handle mouseenter and displayRight', function () {
        template, el = null;
        el = angular.element(
            '<input class="customTooltipTest3" displayRight="true" custom-tooltip="My tooltip2" type="text" />'
        );

        template = compiler(el)(scope);
        scope.$digest();
        expect(template.hasClass('customTooltipTest3')).to.eql(true);

        template.triggerHandler('mouseenter');
        expect($('body').find('.tooltip').length).to.eql(1);
        $('body').find('.tooltip').remove();
    });

    it('should handle mouseenter and displayLeft', function () {
        template, el = null;
        el = angular.element(
            '<input class="customTooltipTest4" displayLeft="true" custom-tooltip="My tooltip2" type="text" />'
        );

        template = compiler(el)(scope);
        scope.$digest();
        expect(template.hasClass('customTooltipTest4')).to.eql(true);

        template.triggerHandler('mouseenter');
        expect($('body').find('.tooltip').length).to.eql(1);
    });
*/
});