'use strict';

describe('LabelLinkContentDirective', function () {

    var el, scope, template;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div><label-link-content href="{{customHref}}" class="exprWrapper" exp-id="{{experiment.id}}" has-update="{{experiment.hasUpdate}}" read-only="{{experiment.readOnly}}" state="RUNNING">' +
                    '<h3>{{experiment.applicationName}}</h3>' +
                    '<h2>{{experiment.label}}</h2>' +
                    '</label-link-content></div>'
        );
        
        scope = $rootScope.$new();
        scope.experiment = {
            'id': '1',
            'homePageTooltip': 'My tooltip1',
            'hasUpdate': 'true',
            'readOnly': 'false',
            'applicationName': 'My app',
            'label': 'My experiment'
        };
        template = $compile(el)(scope);
        scope.$digest();
    }));
    
    
    it('should exist', function () {
        expect(template.find('.exprWrapper').length).to.eql(1);
        expect(template.find('a').length).to.eql(1);
    });

    it('should handle handle has update', function () {
        expect(template.find('a').attr('href')).to.eql('#/experiments/1/false/');
    });

    it('should handle read only', function () {
        scope.experiment.hasUpdate = 'false';
        scope.experiment.readOnly = 'false';
        scope.$digest();

        expect(template.find('a').length).to.eql(1);
    });
});