/*global google:false*/
'use strict';

angular.module('wasabi.directives').directive('googleChartsBarChart', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, c) {
            //google.charts.setOnLoadCallback(drawChart);

            function aggregateData(currentData) {
                var data = [],
                    petIndex = 0;
                for (var i = 0; i < currentData.length; i++) {
                    var currentPet = currentData[i];
                    data.push([currentPet.bucketName, 0, 0, 0]);
                    for (var j = 0; j < currentPet.actions.length; j++) {
                        var parts = currentPet.actions[j].payload.split(':');
                        var userAgent = parts[1].substring(parts[1].indexOf('"') + 1, parts[1].lastIndexOf('"'));
                        switch (userAgent) {
                            case 'Android':
                                data[petIndex][1]++;
                                break;
                            case 'iPhone':
                                data[petIndex][2]++;
                                break;
                            case 'other':
                                data[petIndex][3]++;
                                break;
                        }
                    }
                    petIndex++;
                }
                return data;
            }

            function drawChart() {
                // Count user agent entries for each bucket
                var petData = [['Pets', 'Android', 'iPhone', 'Other']],
                    impressionData = c.$modelValue.impressionData,
                    actionData = c.$modelValue.actionData;

                var impressionAggregate = aggregateData(impressionData),
                    actionAggregate = aggregateData(actionData);

                // Let's brute force this...
                var catImpIndex, dogImpIndex, fishImpIndex,
                    catActIndex, dogActIndex, fishActIndex;
                catImpIndex = dogImpIndex = fishImpIndex = catActIndex = dogActIndex = fishActIndex = -1;
                for (var j = 0; j < impressionAggregate.length; j++) {
                    if (impressionAggregate[j][0] === 'Cat') {
                        catImpIndex = j;
                    }
                    else if (impressionAggregate[j][0] === 'Dog') {
                        dogImpIndex = j;
                    }
                    else if (impressionAggregate[j][0] === 'Fish') {
                        fishImpIndex = j;
                    }
                }
                if (impressionAggregate.length < 3) {
                    // Need to fake out data for other one(s)
                    if (catImpIndex === -1) {
                        impressionAggregate.push(['Cat', 0, 0, 0]);
                        catImpIndex = impressionAggregate.length - 1;
                    }
                    if (dogImpIndex === -1) {
                        impressionAggregate.push(['Dog', 0, 0, 0]);
                        dogImpIndex = impressionAggregate.length - 1;
                    }
                    if (fishImpIndex === -1) {
                        impressionAggregate.push(['Fish', 0, 0, 0]);
                        fishImpIndex = impressionAggregate.length - 1;
                    }
                }
                for (var k = 0; k < actionAggregate.length; k++) {
                    if (actionAggregate[k][0] === 'Cat') {
                        catActIndex = k;
                    }
                    else if (actionAggregate[k][0] === 'Dog') {
                        dogActIndex = k;
                    }
                    else if (actionAggregate[k][0] === 'Fish') {
                        fishActIndex = k;
                    }
                }
                if (actionAggregate.length < 3) {
                    // Need to fake out data for other one(s)
                    if (catActIndex === -1) {
                        actionAggregate.push(['Cat', 0, 0, 0]);
                        catActIndex = actionAggregate.length - 1;
                    }
                    if (dogActIndex === -1) {
                        actionAggregate.push(['Dog', 0, 0, 0]);
                        dogActIndex = actionAggregate.length - 1;
                    }
                    if (fishActIndex === -1) {
                        actionAggregate.push(['Fish', 0, 0, 0]);
                        fishActIndex = actionAggregate.length - 1;
                    }
                }

                // Calculate action rates from the data
                for (var i = 0; i < impressionAggregate.length; i++) {
                    // Action rate is action number devided by total impressions
                    var impIndx = 0, actIndx = 0;
                    if (impressionAggregate[i][0] === 'Cat') {
                        impIndx = catImpIndex;
                        actIndx = catActIndex;
                    }
                    else if (impressionAggregate[i][0] === 'Dog') {
                        impIndx = dogImpIndex;
                        actIndx = dogActIndex;
                    }
                    else if (impressionAggregate[i][0] === 'Fish') {
                        impIndx = fishImpIndex;
                        actIndx = fishActIndex;
                    }
                    petData.push([
                        impressionAggregate[impIndx][0],
                        (impressionAggregate[impIndx][1] ? (actionAggregate[actIndx][1] / impressionAggregate[impIndx][1]) * 100.0 : 0),
                        (impressionAggregate[impIndx][2] ? (actionAggregate[actIndx][2] / impressionAggregate[impIndx][2]) * 100.0 : 0),
                        (impressionAggregate[impIndx][3] ? (actionAggregate[actIndx][3] / impressionAggregate[impIndx][3]) * 100.0 : 0)
                    ]);
                }

                var data = google.visualization.arrayToDataTable(petData);

                var options = {
                    chart: {
                        title: 'Popular Pets',
                        subtitle: 'Wasabi Demonstration',
                    },
                    bars: 'horizontal' // Required for Material Bar Charts.
                };

                var chart = new google.charts.Bar(document.getElementById(element.attr('id')));

                chart.draw(data, google.charts.Bar.convertOptions(options));
            }

            scope.$watch(attrs.ngModel, function () {
                var today = new Date(),
                    todayUTC = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());

                if (c.$modelValue) {
                    google.charts.setOnLoadCallback(drawChart);
                }

            });
        }
    };
});
