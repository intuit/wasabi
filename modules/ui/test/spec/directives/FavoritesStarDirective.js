'use strict';

describe('FavoritesStarDirective', function () {

    var el, scope, template, UtilitiesFactory;
    beforeEach(function() {
        module('wasabi.directives');

        var mockUtilFactory = {
            saveFavorite: sinon.stub(),
            removeFavorite: sinon.stub()
        };
        module(function ($provide) {
            $provide.value('UtilitiesFactory', mockUtilFactory);
        });
    });
    beforeEach(inject(function($rootScope, $compile){
        el = angular.element(
            '<div><ul id="exprSummary"><li id="container"><a href="#" favorites-star favorites-star-application-name="experiment.applicationName" favorites-star-experiment-name="experiment.label" ng-attr-is-favorite="{{' + 'experiment.isFavorite' + '}}" class="star" title="Mark as Favorite"></a></li>' +
            '<li id="container2"><a href="#" favorites-star favorites-star-application-name="experiment2.applicationName" favorites-star-experiment-name="experiment2.label" ng-attr-is-favorite="{{' + 'experiment2.isFavorite' + '}}" class="star" title="Mark as Favorite"></a></li></ul></div>'
        );
        
        scope = $rootScope.$new();
        scope.experiment = {
            'applicationName': 'App Name',
            'label': 'Exp Name',
            'isFavorite': false
        };
        scope.experiment2 = {
            'applicationName': 'App Name2',
            'label': 'Exp Name2',
            'isFavorite': false
        };

        template = $compile(el)(scope);
        scope.$digest();
    }));
    
    
    it('should exist', function () {
        expect(template.css('visibility')).to.eql('');
    });

    it('should handle number', function () {
        scope.experiment.isFavorite = true;
        scope.$digest();

        expect(template.find('.star').eq(0).attr('is-favorite')).to.eql('true');
    });

    it('should handle clicks', function () {
        template.find('.star').eq(0).triggerHandler('click');
        scope.$digest();
        //expect(template.find('.favorite').length).to.eql(8);

        expect(template.find('.star').eq(0).attr('title')).to.eql('Undo Mark as Favorite');

        template.find('.star').eq(1).triggerHandler('click');
        scope.$digest();

        expect(template.find('.star').eq(1).attr('title')).to.eql('Undo Mark as Favorite');
        //expect(template.find('a').attr('title')).to.eql('Undo Mark as Favorite');


        template.find('.star').eq(0).triggerHandler('click');
        scope.$digest();

        expect(template.find('.star').eq(0).attr('title')).to.eql('Mark as Favorite');
        expect(template.find('.star').eq(1).attr('title')).to.eql('Undo Mark as Favorite');

        template.find('.star').eq(1).triggerHandler('click');
        scope.$digest();

        expect(template.find('.star').eq(0).attr('title')).to.eql('Mark as Favorite');
        expect(template.find('.star').eq(1).attr('title')).to.eql('Mark as Favorite');

        template.find('.star').eq(0).triggerHandler('click');
        scope.$digest();

        expect(template.find('.star').eq(0).attr('title')).to.eql('Undo Mark as Favorite');
        expect(template.find('.star').eq(1).attr('title')).to.eql('Mark as Favorite');
    });
});