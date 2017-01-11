/*global Promise:false*/
/*global jQuery:false*/
'use strict';
// Wasabi module
// Supports easy use of the Wasabi A/B Testing service from JavaScript.

// TODO: May need to add es6-promise to package.json to support Promise in IE.

var WASABI = (function(wasabi, $) {
    /*
      This allows things that should be saved across usage to be defined once, such as applicationName.
      For example, you can call this once in your app:
        WASABI.setOptions({
            'applicationName': 'CTG',
            'experimentName': 'test_feedback_link',
            'protocol': 'http',
            'host': 'localhost:8080'
        });
      From then on, you don't need to provide those options in other calls.
      These can also be set and retrieved using setOptions() and getOptions().
     */
    wasabi.wasabiOptions = (typeof wasabi.getOptions === 'function' ? wasabi.getOptions() : {});

    var buildURL = function(path, options) {
        var url = '',
            defaults = $.extend({
                protocol: 'http',
                host: 'localhost:8080'
            }, options);

        url += defaults.protocol + '://';
        url += defaults.host;

        url += path.replace(/%\w+%/g, function(all) {
            var replaceStr = all.replace(/%/g, '');
            return (replaceStr in defaults ? defaults[replaceStr] : replaceStr);
        });
        return url;
    };

    var doWasabiOperation = function(urlTemplate, funcName, requiredOptions, options, postBody) {
        var opts = (options || {});
        $.extend(opts, wasabi.wasabiOptions);
        for (var i=0; i < requiredOptions.length; i++) {
            if (!opts.hasOwnProperty(requiredOptions[i])) {
                throw new Error('Missing parameter ' + requiredOptions[i] + ' for ' + funcName);
            }
        }

        var url = buildURL(urlTemplate, opts);

        var ajaxParams = {
            'headers': {
                'Content-Type': 'application/json'
            }
        };
        if (postBody) {
            $.extend(ajaxParams, {
                'method': 'POST',
                'data': postBody
            });
        }

        // In order to solve the problem that jQuery.ajax() interprets the return of a 201 with
        // empty content as an error, we check for that and if we get it, we call the success result.
        return new Promise(function(resolve, reject) {
            $.ajax(url, ajaxParams)
            .done(function(data) {
                resolve(data);
            })
            .fail(function(jqXHR, textStatus, errorThrown) {
                if (jqXHR.status === 201) {
                    resolve(null);
                }
                reject(errorThrown);
            });
        });
    };

    /*
    Assuming you have called wasabiOptions with applicationName, experimentName, protocol and host, an example use
    of this function would be:
        WASABI.getAssignment({
            'userID': 'myid-1'
        }).then(
            function(response) {
                console.log('getAssignment: success');
                console.log(JSON.stringify(response));
                // This object will include the assignment made and the status, which might tell you the experiment
                // has not been started, etc.
            },
            function(error) {
                console.log('getAssignment: error');
            }
        );
     */
    wasabi.getAssignment = function(options) {
        return doWasabiOperation('/api/v1/assignments/applications/%applicationName%/experiments/%experimentName%/users/%userID%',
                'getAssignment',
                [ 'applicationName', 'experimentName', 'userID' ],
                options);
    };

    /*
    Example:
        WASABI.getAssignmentWithSegmentation(
        '{\
            "profile": {\
                "platform": "mac"\
            }\
        }',
        {
            'userID': 'myid-2'
        }).then(
            function(response) {
                console.log('getAssignmentWithSegmentation: success');
                console.log(JSON.stringify(response));
            },
            function(error) {
                console.log('getAssignmentWithSegmentation: error');
            }
        );
    Where the "profile" contains the attributes to be plugged into your Segmentation Rule.
     */
    wasabi.getAssignmentWithSegmentation = function(profile, options) {
        return doWasabiOperation('/api/v1/assignments/applications/%applicationName%/experiments/%experimentName%/users/%userID%',
                'getAssignmentWithSegmentation',
                [ 'applicationName', 'experimentName', 'userID' ],
                options,
                profile);
    };

    /*
    Example:
        WASABI.getPageAssignment({
            'userID': 'myid-3',
            'pageName': 'test_page'
        }).then(
            function(response) {
                console.log('getPageAssignment: success');
                console.log(JSON.stringify(response));
            },
            function(error) {
                console.log('getPageAssignment: error');
            }
        );
     */
    wasabi.getPageAssignment = function(options) {
        return doWasabiOperation('/api/v1/assignments/applications/%applicationName%/pages/%pageName%/users/%userID%',
                'getPageAssignment',
                [ 'applicationName', 'pageName', 'userID' ],
                options);
    };

    /*
    Example:
        WASABI.getPageAssignmentWithSegmentation(
        '{\
            "profile": {\
                "platform": "windows"\
            }\
        }',
        {
            'userID': 'myid-4',
            'pageName': 'test_page'
        }).then(
            function(response) {
                console.log('getPageAssignmentWithSegmentation: success');
                console.log(JSON.stringify(response));
            },
            function(error) {
                console.log('getPageAssignmentWithSegmentation: error');
            }
        );
     */
    wasabi.getPageAssignmentWithSegmentation = function(profile, options) {
        return doWasabiOperation('/api/v1/assignments/applications/%applicationName%/pages/%pageName%/users/%userID%',
                'getPageAssignmentWithSegmentation',
                [ 'applicationName', 'pageName', 'userID' ],
                options,
                profile);
    };

    /*
    Example:
        WASABI.postImpression({
            'userID': 'myid-2'
        }).then(
            function(response) {
                console.log('postImpression: success');
                console.log(JSON.stringify(response));
            },
            function(error) {
                console.log('postImpression: error');
            }
        );
     */
    wasabi.postImpression = function(options) {
        var eventJSON = '{"events":[{"name":"IMPRESSION"}]}';
        return doWasabiOperation('/api/v1/events/applications/%applicationName%/experiments/%experimentName%/users/%userID%',
                'postImpression',
                [ 'applicationName', 'experimentName', 'userID' ],
                options,
                eventJSON);
    };

    /*
    Example:
        WASABI.postAction(
        'MY_EVENT',
        '{\\"more things\\":\\"more stuff\\"}',
        {
            'userID': 'myid-2'
        }).then(
            function(response) {
                console.log('postAction: success');
                console.log(JSON.stringify(response));
            },
            function(error) {
                console.log('postAction: error');
            }
        );
    Where 'MY_EVENT' is the action name, '{"more things": "more stuff"} is an OPTIONAL JSON object (suitably escaped
    above) that is saved with the action for this user and can be retrieved from the data.  If you don't need to pass
    the eventPayload object, pass null.
     */
    wasabi.postAction = function(eventName, eventPayload, options) {
        var eventObject = {
            'events': [
                {
                    'name': eventName
                }
            ]
        };
        if (eventPayload) {
            eventObject.events[0].payload = eventPayload;
        }

        return doWasabiOperation('/api/v1/events/applications/%applicationName%/experiments/%experimentName%/users/%userID%',
                'postAction',
                [ 'applicationName', 'experimentName', 'userID' ],
                options,
                JSON.stringify(eventObject));
    };

    /*
    Allows the caller to set the values used in each call to Wasabi, for example, the applicationName.  The "options"
    object will have fields for those values.
     */
    wasabi.setOptions = function(options) {
        wasabi.wasabiOptions = options;
    };

    /*
    Allows the caller to retrieve the values used in each call to this object.
     */
    wasabi.getOptions = function() {
        return wasabi.wasabiOptions;
    };

    return wasabi;

}(WASABI || {}, jQuery));
