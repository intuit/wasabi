'use strict';

describe('FadeInOutDirective', function () {

    var el, scope, template;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div id="saveMsg" fade-in-out fade-model-name="experiment.id">Experiment saved. You may now add buckets.</div>'
        );
        
        scope = $rootScope.$new();
        scope.experiment = {
            'id': null
        };
        template = $compile(el)(scope);
        scope.$digest();
    }));
    
    
    it('should exist', function () {
        expect(template.css('visibility')).to.eql('');
    });

    it('should handle number', function () {
        scope.experiment.id = '123';
        scope.$digest();

        expect(template.css('visibility')).to.eql('');
    });
});