/*global WASABI*/
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
        if (typeof WASABI === 'undefined') {
            console.log('The wasabi.js library was not loaded.');
            this.setState({
                errorMessage: 'The required version of the Wasabi server is missing!'
            });
            return false;
        }

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
            // TODO: We would usually actually do a login, but for this test app, we let them in.
            // var data = JSON.stringify({name: login.name, cost: login.cost});
            // $http.post('http://localhost:3000/login', data).success(function(data) {
            // console.log(data);
            // });
            let sessionObj = {
                'login': {
                    'name': this.state.username,
                    'loggedIn': true
                }
            };
            sessionStorage.setItem('session', JSON.stringify(sessionObj));

            // Set up properties that will be the same on all Wasabi calls.
            WASABI.setOptions({
                'applicationName': 'MyStore',
                'experimentName': 'TestBuyButtonColor',
                'protocol': 'http',
                'host': 'localhost:8080'
            });

            // Check Wasabi to see if this user should be in the test and which bucket.
            WASABI.getAssignment({
                'userID': this.state.username
            }).then(
                (response) => {
                    console.log('getAssignment: success');
                    console.log(JSON.stringify(response));
                    // This object will include the assignment made and the status, which might tell you the experiment
                    // has not been started, etc.
                    // Note that if the experiment doesn't exist or hasn't been started, response.assignment is undefined, which is OK.
                    sessionObj.switches = {};
                    if (response.assignment === 'BlueButton') {
                        sessionObj.switches = {
                            'buyButtonColor': '#7474EA'
                        };
                    }
                    else if (response.assignment === 'GreenButton') {
                        sessionObj.switches = {
                            'buyButtonColor': 'green'
                        };
                    }
                    // else the user got the Control bucket, and we don't do anything.

                    sessionStorage.setItem('session', JSON.stringify(sessionObj));

                    this.props.setLoggedInFunc();
                },
                (error) => {
                    console.log('getAssignment: error');
                    console.dir(error);
                    sessionStorage.setItem('session', JSON.stringify(sessionObj));

                    this.props.setLoggedInFunc();
                }
            );
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
                <div className="loginTitle">Welcome to our Store </div>
                <div className="loginPrompt">Please Login</div>
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
