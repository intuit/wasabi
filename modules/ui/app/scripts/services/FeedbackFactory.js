'use strict';

angular.module('wasabi.services').factory('FeedbackFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/feedback', {}, {
            'sendFeedback': { method: 'POST',
                transformRequest: function (data) {
                    return typeof(data) === 'string' ? data : JSON.stringify(data);
                }
            },
            'getFeedback': { method: 'GET',
                transformResponse: function (data) {
                    var parsedData = $.parseJSON(data).feedback;

                    return parsedData;
                },
                isArray: true
            }
        });
    }]);