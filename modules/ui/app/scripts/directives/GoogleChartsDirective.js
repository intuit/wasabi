/*global google:false*/
'use strict';

angular.module('wasabi.directives').directive('googleCharts', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, c) {
            //google.charts.setOnLoadCallback(drawChart);

            scope.$watch(attrs.ngModel, function () {
                var today = new Date(),
                    todayUTC = Date.UTC(today.getFullYear(), today.getMonth(), today.getDate());

                if (c.$modelValue) {
                    var buckets = c.$modelValue.days[0].perDay.buckets;
                    if (buckets) {
                        var chartData = new google.visualization.DataTable();

                        // Converts the date in the data so we can use it for comparison.
                        var convertDate = function (date) {
                            var dateArray = date.split('-');
                            return Date.UTC(dateArray[0], dateArray[1] - 1, dateArray[2]);
                        };

                        // Converts the date in the data for use by Google Charts.
                        var convertToGChartsDate = function(date) {
                            var dateArray = date.split('-');
                            return new Date(dateArray[0], dateArray[1] - 1, dateArray[2]);
                        };

                        // Packages the data that came from the API call into the DataTable object required by
                        // Google Charts.
                        var getChartData = function (data) {
                            var myDays = data.days;
                            var bucketNames = [];
                            var bucket = null, myKey = null;

                            // Define the first column in the DataTable as being a date.  This is the X-axis of the chart.
                            chartData.addColumn('date', 'Date');

                            // Go through the data finding the buckets and adding columns for each bucket and the error
                            // intervals for each bucket.
                            var i = 0;
                            for (bucket in myDays[0].cumulative.buckets) {
                                myKey = myDays[0].cumulative.buckets[bucket].label;
                                bucketNames[i] = myKey;
                                chartData.addColumn('number', myKey);
                                chartData.addColumn({id:'i' + (i++), type:'number', role:'interval'});
                                chartData.addColumn({id:'i' + (i++), type:'number', role:'interval'});

                            }

                            // Now go through each day's worth of data, adding rows to the DataTable for the date,
                            // and the action rate for each bucket and the error lower and upper bound.
                            for (var day in myDays) {
                                if (convertDate(myDays[day].date) > todayUTC) {
                                    // Only want the data up to today in the graph.
                                    break;
                                }
                                // Each row will have a cell with the date and then 3 cells for each bucket, one with
                                // the bucket's action rate and two more with the error lower and upper bound.
                                var currentRow = [convertToGChartsDate(myDays[day].date)];
                                for (bucket in myDays[day].cumulative.buckets) {
                                    myKey = myDays[day].cumulative.buckets[bucket].label;
                                    var estimate = myDays[day].cumulative.buckets[bucket].jointActionRate.estimate;
                                    var lowerBound = myDays[day].cumulative.buckets[bucket].jointActionRate.lowerBound || 0.0;
                                    var upperBound = myDays[day].cumulative.buckets[bucket].jointActionRate.upperBound || 0.0;
                                    var estimateValue = 0.0;
                                    if (isNaN(estimate) || estimate === null || estimate === undefined) {
                                        // If we don't actually have a data point for this date, set the error bars to 0.
                                        lowerBound = 0.0;
                                        upperBound = 0.0;
                                    }
                                    else {
                                        estimateValue = parseFloat((estimate * 100).toFixed(3));
                                    }

                                    if (lowerBound < 0) {
                                        lowerBound = 0.0;
                                    }
                                    if (upperBound > 100) {
                                        upperBound = 100.0;
                                    }

                                    currentRow.push(parseFloat((estimate * 100).toFixed(3)));
                                    currentRow.push(parseFloat((upperBound * 100).toFixed(3)));
                                    currentRow.push(parseFloat((lowerBound * 100).toFixed(3)));
                                }
                                chartData.addRow(currentRow);
                            }
                        };

                        // These options control the creation of the chart.  The intervals style of area is how we get
                        // the shaded shape between the error bars.
                        var chartOptions = {
                            title:'Performance Across Test Buckets',
                            width: 750,
                            height: 350,
                            curveType:'function',
                            series: [{'color': '#F1CA3A'}],
                            intervals: { 'style':'area' },
                            legend: {position: 'bottom'},
                            vAxis: {title: 'Cumulative Action Rate'}
                        };

                        // Put all the data in the DataTable.
                        getChartData(c.$modelValue);

                        // This function is called when the page has loaded to draw the chart using the DataTable and
                        // the options.
                        var drawChart = function() {
                            var wrap = new google.visualization.ChartWrapper({
                                'chartType': 'LineChart',
                                'dataTable': chartData,
                                'containerId': element.attr('id'),
                                'options': chartOptions
                            });
                            wrap.draw();
                        };

                        // This is necessary because if we don't wait for the DIV to be rendered, the legends don't
                        // render well (they overlap).
                        google.charts.setOnLoadCallback(drawChart);

                    } else {
                        element.append('No data for buckets');
                    }
                }
            });
        }
    };
});
