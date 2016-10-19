chrome.runtime.onMessage.addListener(
    function(request, sender, sendResponse) {
        chrome.storage.sync.get( function (config) {
            baseURL = config.baseURL;
            requestHeaders = {
                "Authorization": config.authHeader
            };
            listener(request, sendResponse);
        });
    }
);

var applyBorder = true;
var experimentIds = [];
var experiments = {};
var stats = {};
var requestHeaders = {};
var baseURL = "";

var state = {
    getExperimentIds: false,
    fetchExperiments: false,
    fetchBuckets: false
}

/* Fetch experiment objects and corresponding bucket names */
var fetchExperiments = function() {
    if(experimentIds.length < 1) {
        console.log("No experiments to fetch buckets");
        return;
    }
    var requests = [];
    for(var i = 0; i < experimentIds.length; i++) {

        var currentId = experimentIds[i];
        requests.push(
            $.ajax({
                url: baseURL + "api/v1/experiments/" + currentId + "/buckets/",
                headers: requestHeaders
            })
            .done(function(data) {
                if(data && data.buckets && data.buckets.length) {
                    experiments[data.buckets[0].experimentID] = data;
                }
            })
        );

        requests.push(
                $.ajax({
                    url: baseURL + "api/v1/analytics/experiments/" + currentId + "/statistics/",
                    headers: requestHeaders
                })
                .done(function(data, status, jqXHR) {
                    var expID = this.url.split("/experiments/")[1].split('/statistics/')[0];
                    stats[expID] = data.buckets;
                })
        )
    }

    $.when.apply(undefined, requests).then(
            function() {
                console.log("All experiments fetched");
                console.log(experiments);
                console.log(stats);
                postInit();
            }
    );
};


var getAllBuckets = function() {
    if(state.getExperimentIds) {
        return;
    }

    var testables = $('*[abtest]');
    if(testables && testables.length) {
        for (var i = 0; i < testables.length; i++) {
            experimentIds.push(testables[i].attributes.abtest.value);
        }
    }
    state.getExperimentIds = true;
    console.log(experimentIds);
};


var postInit = function() {

    console.log("Post init");
    Object.keys(experiments).forEach(function(key) {
        var currentBuckets = experiments[key].buckets;
        var experimentID = currentBuckets[0].experimentID

        var bucketMenuItems = {};
        /* Get sum of all estimate values */
        var jointEstimateSum = 0;
        for(var i = 0; i < currentBuckets.length; i++) {
            jointEstimateSum += stats[experimentID][currentBuckets[i].label].jointActionRate.estimate;
        };

        for(var i = 0; i < currentBuckets.length; i++) {
            var b = currentBuckets[i].label;
            var d = currentBuckets[i].description;
            bucketMenuItems[b] = {
                name: d || b,
                type:'label',
                actionRate: ((stats[experimentID][currentBuckets[i].label].jointActionRate.estimate) / jointEstimateSum) * 100
            };
        }

        $.contextMenu.types.label = function(item, opt, root) {
            console.log("Name " + item.name + " | Rate: " + item.actionRate);
            $('<div>'+item.name+'<div class="myProgress"><div class="myBar" style="width:'+item.actionRate+'%"></div>' + Math.floor(100*item.actionRate)/100 + ' % </div></div>')
                    .appendTo(this);

            this.addClass('labels').on('contextmenu:focus', function(e) {
                // setup some awesome stuff
            }).on('contextmenu:blur', function(e) {
                // tear down whatever you did
            }).on('keydown', function(e) {
                // some funky key handling, maybe?
            });
        };

        $.contextMenu({
            selector: '*[abtest=' + experimentID +']',
            delay: 100,
            autoHide:true,
            callback: function(key, options) {
                var selector = options.selector;
                var element = $(selector);
                var expID = selector && selector.split('=')[1].split(']')[0];
                var payloadText = _.find(experiments[expID].buckets, {label: key}).payload;
                var payload = JSON.parse(payloadText);

                if(payload) {
                    if(payload.text) {
                        element.text(payload.text);
                    }
                    if(payload.css) {
                        element.css(payload.css);
                    }
                }
            },
            items: bucketMenuItems
        });

    });
};

function listener (request, sendResponse) {
    if (request.greeting == "hello") {
        console.log("Wasabi-Eye activating...");
        

        getAllBuckets();
        fetchExperiments();
        if(applyBorder) {
            $('*[abtest]').addClass('wasabi-eye-border');
            $('*[abtest]').contextMenu(true);
        }
        else {
            $('*[abtest]').removeClass('wasabi-eye-border');
            $('*[abtest]').contextMenu(false);
        }
        applyBorder = !applyBorder;
        console.log("Received message. Sending Response.");
    }
    sendResponse({farewell: "goodbye"});
};

