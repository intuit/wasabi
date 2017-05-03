/*global google:false*/
'use strict';

angular.module('wasabi.directives').directive('googleChartsBarChart', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, c) {
            //google.charts.setOnLoadCallback(drawChart);

            scope.$watch(attrs.ngModel, function () {
                var today = new Date(),
                    todayUTC = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());

                if (c.$modelValue) {
                    function aggregateData(currentData) {
                        var data = [],
                            petIndex = 0;
                        for (var i = 0; i < currentData.length; i++) {
                            var currentPet = currentData[i],
                                name = 'Cat';
                            switch (currentPet.name) {
                                case 'ImageOne':
                                    name = 'Cat';
                                    break;
                                case 'ImageTwo':
                                    name = 'Dog';
                                    break;
                                case 'ImageThree':
                                    name = 'Fish';
                                    break;
                            }
                            data.push([name, 0, 0, 0]);
                            for (var j = 0; j < currentPet.actions.length; j++) {
                                switch (currentPet.actions[j].userAgent) {
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
                        var petData = [['Pets', 'iPhone', 'Android', 'Other']],
                            impressionData = c.$modelValue.impressionData,
                            actionData = c.$modelValue.actionData;

                        var impressionAggregate = aggregateData(impressionData),
                            actionAggregate = aggregateData(actionData);

                        // Calculate action rates from the data
                        for (var i = 0; i < impressionAggregate.length; i++) {
                            // Action rate is action number devided by total impressions
                            petData.push([
                                impressionAggregate[i][0],
                                (impressionAggregate[i][1] ? actionAggregate[i][1] / impressionAggregate[i][1] : 0),
                                (impressionAggregate[i][2] ? actionAggregate[i][2] / impressionAggregate[i][2] : 0),
                                (impressionAggregate[i][3] ? actionAggregate[i][3] / impressionAggregate[i][3] : 0)
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

                    google.charts.setOnLoadCallback(drawChart);
                }

            });
        }
    };
});
