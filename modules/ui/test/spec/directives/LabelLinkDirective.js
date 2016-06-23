'use strict';

describe('LabelLinkDirective', function () {

    var el, scope, template, compiler;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div><label-link state="{{experiment.state}}" exp-id="{{experiment.id}}" ' +
                    'exp-label="{{experiment.label}}" has-update="{{experiment.hasUpdate}}" read-only="{{experiment.readOnly}}"></label-link></div>'
        );
        
        compiler = $compile;
        scope = $rootScope.$new();
        scope.experiment = {
            'id': '1',
            'state': 'DRAFT',
            'hasUpdate': true,
            'readOnly': 'false',
            'applicationName': 'My app',
            'label': 'My experiment'
        };
        template = $compile(el)(scope);
        scope.$digest();
    }));
    
    
    it('should exist', function () {
        expect(template.find('#openExperiment').length).to.eql(1);
        expect(template.find('a').length).to.eql(1);
    });

    it('should handle status running and not readOnly', function () {
        template, el = null;
        el = angular.element(
            '<div><label-link state="{{experiment.state}}" exp-id="{{experiment.id}}" ' +
                    'exp-label="{{experiment.label}}" has-update="{{experiment.hasUpdate}}" read-only="{{experiment.readOnly}}"></label-link></div>'
        );

        scope.experiment = {
            'id': '1',
            'state': 'RUNNING',
            'hasUpdate': true,
            'readOnly': 'false',
            'applicationName': 'My app',
            'label': 'My experiment'
        };
        template = compiler(el)(scope);
        scope.$digest();

        expect(template.find('a').attr('href')).to.eql('#/experiments/1/false/');
    });

    it('should handle status draft and read only', function () {
        template, el = null;
        el = angular.element(
            '<div><label-link state="{{experiment.state}}" exp-id="{{experiment.id}}" ' +
                    'exp-label="{{experiment.label}}" has-update="{{experiment.hasUpdate}}" read-only="{{experiment.readOnly}}"></label-link></div>'
        );

        scope.experiment = {
            'id': '1',
            'state': 'DRAFT',
            'hasUpdate': 'false',
            'readOnly': 'true',
            'applicationName': 'My app',
            'label': 'My experiment'
        };
        template = compiler(el)(scope);
        scope.$digest();

        expect(template.find('a').length).to.eql(0);
    });

    it('should handle status running and readOnly', function () {
        template, el = null;
        el = angular.element(
            '<div><label-link state="{{experiment.state}}" exp-id="{{experiment.id}}" ' +
                    'exp-label="{{experiment.label}}" has-update="{{experiment.hasUpdate}}" read-only="{{experiment.readOnly}}"></label-link></div>'
        );

        scope.experiment = {
            'id': '1',
            'state': 'RUNNING',
            'hasUpdate': false,
            'readOnly': 'false',
            'applicationName': 'My app',
            'label': 'My experiment'
        };
        template = compiler(el)(scope);
        scope.$digest();

        expect(template.find('a').length).to.eql(1);
    });
});