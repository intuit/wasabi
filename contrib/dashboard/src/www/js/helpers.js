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

var getSession = function() {
    var session = sessionStorage.getItem('session');
    if (session) {
        session = JSON.parse(session);
    }
    return session;
};

exports.doWasabiOperation = function(urlTemplate, options, postBody) {
    var opts = (options || {}),
        session = getSession();
    $.extend(opts, wasabiOptions);

    var url = buildURL(urlTemplate, opts);

    var ajaxParams = {
        headers: {
            'Content-Type': 'application/json'
        }
    };
    // Sign in needs special handling
    if (opts.hasOwnProperty('login') && opts.login === true) {
        ajaxParams = {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'Authorization': 'Basic ' + btoa(opts.username + ':' + opts.password)
            }
        };
        postBody = {
            grant_type: 'client_credentials'
        };
    }
    else {
        ajaxParams.headers['Authorization'] = session.login.tokenType + ' ' + session.login.accessToken;
    }
    if (postBody) {
        $.extend(ajaxParams, {
            'method': 'POST',
            'body': JSON.stringify(postBody)
        });
    }
    else if (opts.hasOwnProperty('method')) {
        $.extend(ajaxParams, {
            'method': opts.method
        });
    }

    return fetch(url, ajaxParams).then(
        res => {
            if (res.status && res.status !== 201 && res.status !== 204) {
                return res.json()
            }
        }
    );
};
