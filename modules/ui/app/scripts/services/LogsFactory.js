/*global $:false */

'use strict';

angular.module('wasabi.services').factory('LogsFactory', ['$resource', 'ConfigFactory',
    function ($resource, ConfigFactory) {
        return $resource(ConfigFactory.baseUrl() + '/logs/applications/:applicationName?page=:page&per_page=10&filter=:filter&sort=:sort&timezone=' + (new Date().toString().match(/([-\+][0-9]+)\s/)[1]).replace('+', '%2B'), {}, {
            /*
            page
            per_page
            filter
            sort
             */
            'query': { method: 'GET', params: {applicationName: '@applicationName', page: '@page', filter: '@filter', sort: '@sort'},
                transformResponse: function (data) {
                    return $.parseJSON(data);
                }
            }
        });
    }]);
