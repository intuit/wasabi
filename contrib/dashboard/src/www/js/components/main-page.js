import React from 'react';

import { TabPageComponent } from './tab-page';

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

    render() {
        return <div>
            <div className="siteHeading">
                <div style={{fontSize: '30px'}}>Wasabi Dashboard</div>
                <div className="applicationNameContainer">
                    Application:
                    <select value={this.state.applicationName} onChange={this.handleSelectChange} style={{marginLeft: '10px'}}>
                        {this.state.applications.map((name, index) => <option value={name} key={index}>{name}</option>)}
                    </select>
                </div>
                <div className="userNameContainer">Welcome, {this.state.session.login.name} </div>
                <div className="logoutLink">
                    <a href="#" onClick={this.props.doLogout}>Logout</a>
                </div>
            </div>
            <div className="storeMain">
                <TabPageComponent applicationName={this.state.applicationName} />
            </div>
            <div className="modalPanel"></div>
        </div>;
    }
}
