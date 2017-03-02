'use strict';

angular.module('wasabi.controllers')
    .controller('MenuCtrl', ['$scope', '$rootScope', '$state',
            function ($scope, $rootScope, $state) {
                $('.sticky-nav-bar').width($(window).width());
                $scope.showMenu = function () {
                    if ($('#mainContent').hasClass('gridShowingMenu')) {
                        $('#mainContent').css('left', 0).width($(window).width()).height('auto');
                        $('.menuPanel').css('left', -300);
                    }
                    else {
                        var winHeight = $(window).height();
                        $('#mainContent').css('left', 300);
                        $('#mainContent').width($(window).width() - 300).height(winHeight);
                        //$('.menuPanel').height(winHeight - 60);
                        $('.menuPanel, .fixedPanel').css('left', 0);
                    }
                    $('.fixedPanel').toggle();
                    $('#mainContent').toggleClass('gridShowingMenu');
                    return false;
                };
            }]);
