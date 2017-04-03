/*global $:true*/
import React from 'react';
const helpers = require('../helpers.js');

export class StatePageComponent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            states: [],
            session: {
                login: {
                    'name': '',
                    'loggedIn': false
                }
            }
        };

        this.renderGraph = this.renderGraph.bind(this);
    }

    componentDidMount() {
        var session = sessionStorage.getItem('session');
        if (session) {
            session = JSON.parse(session);
            this.setState({
                session: session
            });
        }

    }

    componentWillReceiveProps(nextProps) {
        if (!nextProps.hasOwnProperty('applicationName') || nextProps.applicationName === '') {
            return;
        }

        this.setState({
            applicationName: nextProps.applicationName
        });
    }

    componentDidUpdate() {
        google.charts.setOnLoadCallback(this.renderGraph);
    }

    renderGraph() {
        helpers.doWasabiOperation('/api/v1/applications/%APPLICATION_NAME%/experimentsByState',
            {
                'APPLICATION_NAME': this.state.applicationName
            }
        ).then(items => {
            if (items && !items.hasOwnProperty('error')) {
                var data = [
                    ['Status', 'Experiments']
                ];

                for (let state in items) {
                    let stateName = (state === 'PAUSED' ? 'STOPPED' : state);
                    data.push([stateName, items[state].size]);
                }

                var chartOptions = {
                    title: 'Experiments by state'
                };
                var pieData = google.visualization.arrayToDataTable(data);

                var wrap = new google.visualization.ChartWrapper({
                    'chartType': 'PieChart',
                    'dataTable': pieData,
                    'containerId': 'chartContainer',
                    'options': chartOptions
                });
                wrap.draw();
            }
        });
    }

    render() {
        return <div>
                <div id={'chartContainer'} style={{width: '900px', height: '500px'}}></div>
        </div>;
    }
}
