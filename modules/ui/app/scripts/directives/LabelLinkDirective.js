'use strict';

angular.module('wasabi.directives').directive('labelLink', ['$compile',
    function ($compile) {
        return {
            restrict: 'E',
            link: function (scope, element, attrs) {
                if (attrs.state.toUpperCase() === 'DRAFT') {
                    if (attrs.hasUpdate === 'true' && attrs.readOnly !== 'true') {
                        var openString = 'openExperimentModal(experiment)'; // The command to execute when user clicks on draft experiment
                        if (attrs.hasOwnProperty('openString')) {
                            // This needs to be different in the case of opening a second level dialog, like for
                            // Mutual Exclusions.
                            openString = attrs.openString;
                        }
                        element.append('<a id="openExperiment" href="#" onclick="return false;" title="Edit ' +
                                attrs.expLabel +
                                '" ng-click="' + openString + '">' +
                            attrs.expLabel + '</a>');
                    }
                    else {
                        element.append('<span title="' + attrs.expLabel + '">' + attrs.expLabel + '</span>');
                    }
                } else {
                    if (!attrs.openedFromModal || attrs.openedFromModal !== 'true' && (attrs.hasUpdate === 'true' || attrs.readOnly === 'true')) {
                        var linkStr = '<a href="#/experiments/' + attrs.expId + '/';
                        if (attrs.inModal !== undefined && attrs.inModal === 'true') {
                            linkStr += 'true/true';
                        }
                        else {
                            linkStr += (attrs.readOnly.toLowerCase() === 'true') + '/';
                        }
                        linkStr += '" title="Edit ' + attrs.expLabel + '">' + attrs.expLabel + '</a>';
                        element.append(linkStr);
                    }
                    else {
                        element.append('<span title="' + attrs.expLabel + '">' + attrs.expLabel + '</span>');
                    }
                }
                $compile(element.contents())(scope);
            }
        };
    }]);