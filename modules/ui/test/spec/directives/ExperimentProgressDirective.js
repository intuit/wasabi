'use strict';

describe('ExperimentProgressDirective', function () {

    var el, scope, form, template;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div experiment-progress progress-value-model="experiment.progress" progress-days-left="{{experiment.progressDaysLeft}}" class="exprHead"><canvas width="70" height="70"></canvas></div>'
        );
        
        scope = $rootScope.$new();
        scope.experiment = {
            'progress': 3,
            'progressDaysLeft': 15
        };
        template = $compile(el)(scope);
        scope.$digest();
        form = scope.myForm;
    }));
    
    
    it('should exist', function () {
        expect(template.find('canvas').length).to.eql(1);
    });

    it('should handle value change', function () {
        scope.experiment.progress = 6;
        scope.$digest();

        expect(template.find('canvas').length).to.eql(1);
    });
});