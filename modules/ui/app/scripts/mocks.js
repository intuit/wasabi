'use strict';

var myRegExp = function(pattern) {
    var re =  new RegExp(pattern);
    return re;
};

angular.module('mocks', ['ngMockE2E', 'wasabi.services'])
        .run(['$httpBackend', 'ConfigFactory', function($httpBackend, ConfigFactory) {
    // Do your mock

    // Experiment Pages (Tags) Mocks
/*
    var pages = '{"pages":[' +
            ' {"name":"page_name1"},' +
            ' {"name":"page_name2"},' +
            ' {"name":"page_name3"},' +
            ' {"name":"page_name4"},' +
            ' {"name":"page_name5"}' +
            ']}';
    var pages2 = '{"pages":[' +
            ' {"name":"page_name1",' +
            '  "allowNewAssignment":true' +
            ' },' +
            ' {"name":"page_name2",' +
            '  "allowNewAssignment":false' +
            ' }' +
            ']}';

    console.log('Mocking: ' + ConfigFactory.baseUrl() + '/applications/.*//*
pages');
    $httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/applications/.*//*
pages')).respond(pages);

    $httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/experiments/.*//*
pages')).respond(pages2);
    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/experiments/.*//*
exclusions/.*showAll=true.*')).respond(mutualExclusions);
    $httpBackend.when('POST', new RegExp(ConfigFactory.baseUrl() + '/experiments/.*//*
pages')).respond(function(method, url, data, headers) {
        console.log('POST: Received these data:', method, url, data, headers);
        return [200, {}, {}];
    });
    $httpBackend.when('DELETE', new RegExp(ConfigFactory.baseUrl() + '/experiments/.*//*
pages/.*')).respond(function(method, url, data, headers) {
        console.log('DELETE: Received these data:', method, url, data, headers);
        return [200, {}, {}];
    });
*/

    // Mutual Exclusion Mocks
/*
    var mutualExclusions = '{' +
            '"experiments": [' +
            '{' +
            '"id":"111",' +
            '"label":"experimentLabel",' +
            '"applicationName":"things",' +
            '"description":"super descriptive string",' +
            '"state":"RUNNING"' +
            '}' +
            ']}';
    var mutualExclusions2 = '{' +
            '"experiments": [' +
            '{' +
            '"id":"2222",' +
            '"label":"notYetExclusiveExp",' +
            '"applicationName":"things",' +
            '"description":"This experiment is not, yet, ME with the current experiment.",' +
            '"state":"RUNNING"' +
            '}' +
            ']}';
*/
    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/experiments/.*/exclusions/.*showAll=true.*exclusive=false')).respond(mutualExclusions2);
    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/experiments/.*/exclusions/.*showAll=true.*')).respond(mutualExclusions);
    /*
     $httpBackend.when('POST', new RegExp(ConfigFactory.baseUrl() + '/experiments/.
     exclusions')).respond(function(method, url, data, headers){
     console.log('POST: Received these data:', method, url, data, headers);
     return [200, {}, {}];
     });
     $httpBackend.when('DELETE', new RegExp(ConfigFactory.baseUrl() + '/experiments/.
     exclusions/.*')).respond(function(method, url, data, headers){
     console.log('DELETE: Received these data:', method, url, data, headers);
     return [200, {}, {}];
     });
     */

    // Priorities Mocks
/*
    var priorities = '{' +
            '"experiments": [' +
            '{' +
            '"id":"930d9a63-b47b-4b83-909b-7ada1152a140",' +
            '"label":"Test1",' +
            '"applicationName":"QBO",' +
            '"description":"Much longer and more interesting description that will hopefully extend over more than one line so that we can test the dialog to it\'s limits.",' +
            '"state":"DRAFT",' +
            '"priority":1' +
            '},' +
            '{' +
            '"id":"249485e7-1e71-4377-9387-d477513f8f0d",' +
            '"label":"AnotherTest",' +
            '"applicationName":"QBO",' +
            '"description":"super descriptive string2",' +
            '"state":"DRAFT",' +
            '"priority":2' +
            '},' +
            '{' +
            '"id":"30a24912-4187-4fe8-8994-7a9b0186bc4a",' +
            '"label":"ThirdTest",' +
            '"applicationName":"QBO",' +
            '"description":"super descriptive string3",' +
            '"state":"PAUSED",' +
            '"priority":3' +
            '},' +
            '{' +
            '"id":"444",' +
            '"label":"experimentLabel4",' +
            '"applicationName":"QBO",' +
            '"description":"super descriptive string4",' +
            '"state":"RUNNING",' +
            '"priority":4' +
            '}' +
            ']}';
*/
    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/applications/.*/priorities')).respond(priorities);
    /*
     $httpBackend.when('PUT', new RegExp(ConfigFactory.baseUrl() + '/applications/.*/
    /*
     priorities')).respond(function(method, url, data, headers){
     console.log('Priorities PUT: Received these data:', method, url, data, headers);
     return [200, {}, {}];
     });
     */

/*
    var qboUserRoles = '{ "rolesList": ' +
        '[' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "mmouse1" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "admin", ' +
            '    "userID": "mmouse2" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readonly", ' +
            '    "userID": "mmouse3" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "mmouse4" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "mmouse5" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "dduck" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "dduck1" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "dduck2" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "dduck3" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "dduck4" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "dduck5" ' +
            '},' +
            '{' +
            '    "applicationName": "QBO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "dduck6" ' +
            '}' +
        ']}';

    var ttoUserRoles = '{ "rolesList": ' +
        '[' +
            '{' +
            '    "applicationName": "TTO", ' +
            '    "role": "admin", ' +
            '    "userID": "mmouse1" ' +
            '},' +
            '{' +
            '    "applicationName": "TTO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "mmouse2" ' +
            '},' +
            '{' +
            '    "applicationName": "TTO", ' +
            '    "role": "admin", ' +
            '    "userID": "mmouse3" ' +
            '},' +
            '{' +
            '    "applicationName": "TTO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "mmouse4" ' +
            '},' +
            '{' +
            '    "applicationName": "TTO", ' +
            '    "role": "readwrite", ' +
            '    "userID": "mmouse5" ' +
            '}' +
        ']}';
*/

    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/authorization/applications/QBO')).respond(qboUserRoles);
    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/authorization/applications/TTO')).respond(ttoUserRoles);


    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/authentication/users/scressler')).respond(singleUser);

    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/authentication/users/lskywalker')).respond(singleUser2);

/*
    $httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/users/.*application.*')).respond(function(method, url, data, headers){
        console.log('/users/application GET: Received these data:', method, url, data, headers);
        return [200, users];
    });

    $httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/users/.*')).respond(400,
        '{"errors": {"error": [{"detail": "Fake error message."}]}}');

    $httpBackend.when('PUT', new RegExp(ConfigFactory.baseUrl() + '/users/.*')).respond(function(method, url, data, headers){
        console.log('/users PUT: Received these data:', method, url, data, headers);
        return [200, {}, {}];
    });

    $httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/users')).respond(users);
*/

    /*
    {permissionsList: [{applicationName: appname, permissions:[perm1, perm2]}, {applicationname: app2, permissions [perm2, perm3]}]}
     */
/*
    var adminPerms = '{"permissionsList": [' +
        '{' +
        '    "applicationName": "QBO",' +
        '    "permissions": ["create", "read", "update", "delete", "admin"]' +
        '},' +
        '{' +
        '    "applicationName": "TTO",' +
        '    "permissions": ["create", "read", "update", "delete", "admin"]' +
        '}' +
        ']}';
    var writePerms = '{"permissionsList": [' +
        '{' +
        '    "applicationName": "QBO",' +
        '    "permissions": ["create", "read", "update", "delete"]' +
        '},' +
        '{' +
        '    "applicationName": "TTO",' +
        '    "permissions": ["create", "read", "update", "delete"]' +
        '}' +
        ']}';
    var readPerms = '{"permissionsList": [' +
        '{' +
        '    "applicationName": "QBO",' +
        '    "permissions": ["read"]' +
        '},' +
        '{' +
        '    "applicationName": "TTO",' +
        '    "permissions": ["read"]' +
        '}' +
        ']}';
    var mixedPerms = '{"permissionsList": [' +
        '{' +
        '    "applicationName": "QBO",' +
        '    "permissions": ["read"]' +
        '},' +
        '{' +
        '    "applicationName": "TTO",' +
        '    "permissions": ["create", "read", "update", "delete"]' +
        '}' +
        ']}';
    var ttoOnlyPerms = '{"permissionsList": [' +
        '{' +
        '    "applicationName": "TTO",' +
        '    "permissions": ["create", "read", "update", "delete"]' +
        '}' +
        ']}';
    var ttoOnlyAdminPerms = '{"permissionsList": [' +
        '{' +
        '    "applicationName": "TTO",' +
        '    "permissions": ["create", "read", "update", "delete", "admin"]' +
        '}' +
        ']}';
*/
    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/authorization/users/.*/permissions')).respond(adminPerms);

/*
    var roles = '{"rolesList": [' +
            '{' +
            '    "applicationName": "TTO",' +
            '    "role": "admin"' +
            '},' +
            '{' +
            '    "applicationName": "QBO",' +
            '    "role": "readwrite"' +
            '}' +
        ']}';
*/
    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/authorization/users/.*/roles')).respond(roles);

/*
    $httpBackend.when('DELETE', new RegExp(ConfigFactory.baseUrl() + '/authorization/users/.*//*
applications/.*//*
roles')).respond(function(method, url, data, headers){
        console.log('DELETE role: Received these data:', method, url, data, headers);
        return [200, {}, {}];
    });
*/

/*
    $httpBackend.when('POST', new RegExp(ConfigFactory.baseUrl() + '/authorization/roles')).respond(function(method, url, data, headers){
        console.log('POST assign role: Received these data:', method, url, data, headers);
        return [200, {}, {}];
    });
*/

/*
    $httpBackend.when('PUT', new RegExp(ConfigFactory.baseUrl() + '/experiments/.*//*
buckets/.*//*
CLOSED')).passThrough();

    $httpBackend.when('PUT', new RegExp(ConfigFactory.baseUrl() + '/experiments/.*//*
buckets')).respond(function(method, url, data, headers){
    console.log('Buckets PUT: Received these data:', method, url, data, headers);
    return [200, {}, {}];
    });
*/

/*

    $httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/applications/.*//*
users')).respond(users);

    var applications = '{"applications": [' +
            '{' +
            '    "applicationName": "TTO"' +
            '},' +
            '{' +
            '    "applicationName": "QBO"' +
            '},' +
            '{' +
            '    "applicationName": "TTO2"' +
            '},' +
            '{' +
            '    "applicationName": "QBO2"' +
            '},' +
            '{' +
            '    "applicationName": "TTO3"' +
            '},' +
            '{' +
            '    "applicationName": "QBO3"' +
            '},' +
            '{' +
            '    "applicationName": "TTO4"' +
            '},' +
            '{' +
            '    "applicationName": "QBO4"' +
            '},' +
            '{' +
            '    "applicationName": "TTO5"' +
            '},' +
            '{' +
            '    "applicationName": "QBO5"' +
            '},' +
            '{' +
            '    "applicationName": "TTO6"' +
            '},' +
            '{' +
            '    "applicationName": "QBO6"' +
            '},' +
            '{' +
            '    "applicationName": "TTO7"' +
            '},' +
            '{' +
            '    "applicationName": "QBO7"' +
            '},' +
            '{' +
            '    "applicationName": "TTO8"' +
            '},' +
            '{' +
            '    "applicationName": "QBO8"' +
            '},' +
            '{' +
            '    "applicationName": "TTO9"' +
            '},' +
            '{' +
            '    "applicationName": "QBO9"' +
            '}' +
        ']}';
*/

    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/applications')).respond(applications);


    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/logs/applications/.*')).respond(logs1);
    //$httpBackend.when('GET', myRegExp(ConfigFactory.baseUrl() + '/logs/applications/.*page.*1&.*')).respond(logs1);
    //$httpBackend.when('GET', new RegExp(ConfigFactory.baseUrl() + '/logs/applications/.*page.*2&.*')).respond(logs2);

    $httpBackend.when('GET', /.*/).passThrough();
    $httpBackend.when('POST', /.*/).passThrough();
    $httpBackend.when('PUT', /.*/).passThrough();
    $httpBackend.when('DELETE', /.*/).passThrough();

    /*var axelStudent = {
     Education: [{...}],
     Person: {...}
     };
     var femaleStudent = {
     Education: [{...}],
     Person: {...}
     };
     $httpBackend.whenGET(baseApiUrl + 'students/?searchString=axe&amp;')
     .respond([axelStudent, femaleStudent]);

     // Don't mock the html views
     $httpBackend.whenGET(/views\/\w+.*/
    /*).passThrough();

     // For everything else, don't mock
     $httpBackend.whenGET(/^\w+.*/
    /*).passThrough();
     $httpBackend.whenPOST(/^\w+.*/
    /*).passThrough();
     */
}]);

//angular.module('wasabi').requires.push('mocks');
