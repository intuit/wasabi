'use strict';

describe('EnsureStartGreaterEndTime', function () {

    var el, scope, form, template;
    beforeEach(module('wasabi.directives'));
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<form name="myForm"><input type="text" id="startTime" name="startTime" ng-model="experiment.startTime"/><input type="text" id="endTime" name="endTime" ng-model="experiment.endTime" ensure-start-greater-end-time/></form>'
        );
        
        scope = $rootScope.$new();
        scope.experiment = {
            'startTime': '2015-08-04T11:00:00Z',
            'endTime': '2015-08-05T11:00:00Z'
        };
        template = $compile(el)(scope);
        scope.$digest();
        form = scope.myForm;
    }));
    
    
    it('should exist', function () {
        var value = template.find('input').eq(1).val();
        expect(value).to.eql('2015-08-05T11:00:00Z');
    });

    it('should handle end date before start', function () {
        form.endTime.$setViewValue('2015-08-03T11:00:00Z');
        scope.$digest();

        expect(scope.experiment.endTime).to.eql('2015-08-03T11:00:00Z');
        expect(form.$invalid).to.eql(true);
    });

    it('should handle start date after end date', function () {
        scope.experiment.startTime = '2015-09-03T11:00:00Z';
        scope.$digest();

        expect(scope.experiment.startTime).to.eql('2015-09-03T11:00:00Z');
        expect(form.$invalid).to.eql(true);
    });
});