const helpers = require('../helpers.js');

import React from 'react';

export class LoginComponent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            username: '',
            password: '',
            errorMessage: ''
        };

        this.onChange = this.onChange.bind(this);
        this.validate = this.validate.bind(this);
        this.submit = this.submit.bind(this);
    }

    validate() {
        if (this.state.username === '' || !this.state.username) {
            this.setState({
                errorMessage: 'Error: name must be present'
            });
            return false;
        }
        if (this.state.password === '' || !this.state.password) {
            this.setState({
                errorMessage: 'Error: password must be present'
            });
            return false;
        }
        this.setState({
            errorMessage: ''
        });
        return true;
    }

    submit() {
        if (this.validate()) {
            console.log('Login submitted');

            const opts = {
                login: true,
                username: this.state.username,
                password: this.state.password
            };

            helpers.doWasabiOperation(
                '/api/v1/authentication/login',
                opts /* Don't need to pass body as it is added automatically */
            ).then(response => {
                    console.log('login: success');
                    console.log(JSON.stringify(response));

                    let sessionObj = {
                        'login': {
                            'name': this.state.username,
                            'loggedIn': true,
                            'tokenType': response.token_type,
                            'accessToken': response.access_token
                        }
                    };
                    sessionStorage.setItem('session', JSON.stringify(sessionObj));

                    this.props.setLoggedInFunc();
            }).catch(error => {
                console.log('getAssignment: error');
                console.dir(error);
            });

        }
        else {
            console.log('invalid');
        }
    }

    onChange(e) {
        this.setState({
            errorMessage: '',
            [e.target.name]: e.target.value
        });
    }

    render() {
        return <section>
            <section className="loginBox">
                <div className="loginTitle">Wasabi Dashboard Login </div>
                <div className="loginPrompt">Please use your Corp Login</div>
                <form>
                    <div className="loginDiv">
                        <label>User Name: </label> <input type="text" name="username" value={this.state.username} onChange={this.onChange} />
                    </div>
                    <div>
                        <label>Password: </label> <input type="password" name="password" value={this.state.password} onChange={this.onChange} />
                    </div>
                </form>
                <div><button className="loginBtn" onClick={this.submit}>Submit</button></div>
                <div className="loginError">{this.state.errorMessage}</div>
            </section>
        </section>;
    }
}
