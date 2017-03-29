import React from 'react';

import { PagesPageComponent } from './pages-page';

export class MainPageComponent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            applications: [],
            applicationName: '',
            session: {
                login: {
                    'name': '',
                    'loggedIn': false
                }
            }
        };

        this.handleSelectChange = this.handleSelectChange.bind(this);
    }

    componentDidMount() {
        var session = sessionStorage.getItem('session');
        if (session) {
            session = JSON.parse(session);
            this.setState({
                session: session
            });
        }

        this.props.applications.then(items => {
            this.setState({
                applications: items.concat(),
                applicationName: items[0]
            });
        });
    }

    handleSelectChange(event) {
        this.setState({applicationName: event.target.value});
    }

    /*
    <PagesPageComponent applicationName={this.state.applicationName} pages={fetch('http://localhost:8080/api/v1/applications/' + this.state.applicationName + '/pages', {
        method: 'GET',
        headers: headers
    }).then(res => res.json())} />

    */

    render() {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');
        headers.append('Authorization', this.state.session.login.tokenType + ' ' + this.state.session.login.accessToken);

        return <div>
            <div className="siteHeading">
                <div className="applicationNameContainer">
                    Application:
                    <select value={this.state.applicationName} onChange={this.handleSelectChange}>
                        {this.state.applications.map((name, index) => <option value={name} key={index}>{name}</option>)}
                    </select>
                </div>
                <div className="userNameContainer">Welcome, {this.state.session.login.name} </div>
                <div className="logoutLink">
                    <a href="#" onClick={this.props.doLogout}>Logout</a>
                </div>
            </div>
            <div className="storeMain">
                <PagesPageComponent applicationName={this.state.applicationName} />
            </div>
            <div className="modalPanel"></div>
        </div>;
    }
}
