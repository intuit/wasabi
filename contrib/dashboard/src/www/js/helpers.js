/*global $:false*/

/*
  This allows things that should be saved across usage to be defined once, such as applicationName.
  For example:
    wasabiOptions = {
        'applicationName': 'CTG',
        'experimentName': 'test_feedback_link',
        'protocol': 'http',
        'host': 'localhost:8080'
    };
  From then on, you don't need to provide those options in other calls.
 */
const wasabiOptions = {
    'protocol': 'http',
    'host': 'localhost:8080'
};

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


exports.doWasabiOperation = function(urlTemplate, options, postBody) {
    var opts = (options || {});
    $.extend(opts, wasabiOptions);

    var url = buildURL(urlTemplate, opts);

    var ajaxParams = {
        'headers': {
            'Content-Type': 'application/json'
        }
    };
    // Sign in needs special handling
    if (opts.hasOwnProperty('authOn') && opts.authOn === true) {
        ajaxParams = {
            'headers': {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Authorization': 'Basic ' + btoa(opts.username + ':' + opts.password)
            }
        };
        postBody = {
            'grant_type': 'client_credentials'
        };
    }
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
