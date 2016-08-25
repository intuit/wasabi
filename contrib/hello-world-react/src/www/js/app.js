import '../css/styles.scss';

import React from 'react';
import ReactDOM from 'react-dom';

import { LoginComponent } from './components/login';
import { StoreListComponent } from './components/store-list';

class AppMain extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            session: {
                login: {
                    name: '',
                    loggedIn: false
                }
            }
        };

        this.setLoggedIn = this.setLoggedIn.bind(this);
        this.doLogout = this.doLogout.bind(this);
    }

    componentDidMount() {
        var session = sessionStorage.getItem('session');
        if (session) {
            this.setState({
                session: JSON.parse(session)
            });
        }
    }

    setLoggedIn() {
        let currentSession = sessionStorage.getItem('session');
        if (currentSession) {
            currentSession = JSON.parse(currentSession);
            currentSession.login.loggedIn = true;
            sessionStorage.setItem('session', JSON.stringify(currentSession));
            this.setState({
                session: currentSession
            });
        }
        else {
            this.setState({
                session: {
                    login: {
                        name: '',
                        loggedIn: false
                    }
                }
            });
        }
    }

    doLogout() {
        let currentSession = this.state.session;
        currentSession.login.loggedIn = false;
        sessionStorage.setItem('session', JSON.stringify(currentSession));
        this.setState({
            session: currentSession
        });
    }

    render() {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');

        if (this.state.session.login.loggedIn) {
            return <div>
                <StoreListComponent doLogout={this.doLogout} items={fetch('/products', {
                    method: 'GET',
                    headers: headers
                }).then(res => res.json())} />
            </div>;
        }
        else {
            return <div>
                <LoginComponent setLoggedInFunc={this.setLoggedIn} />
            </div>;
        }
    }
}

ReactDOM.render(<AppMain />, document.querySelector('main'));
