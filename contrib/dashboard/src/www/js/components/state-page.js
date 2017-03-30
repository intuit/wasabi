/*global $:true*/
import React from 'react';

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
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');
        headers.append('Authorization', this.state.session.login.tokenType + ' ' + this.state.session.login.accessToken);

        fetch('http://localhost:8080/api/v1/applications/' + this.state.applicationName + '/experimentsByState', {
            method: 'GET',
            headers: headers
        }).then(res => res.json()).then(items => {
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
                //     [
                //     ['Task', 'Hours per Day'],
                //     ['Work',     11],
                //     ['Eat',      2],
                //     ['Commute',  2],
                //     ['Watch TV', 2],
                //     ['Sleep',    7]
                // ]);

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
