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
                errorMessage: 'Error: User ID must be present'
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
                },
                'userAgent': navigator.userAgent
            };
            sessionStorage.setItem('session', JSON.stringify(sessionObj));

            // Set up properties that will be the same on all Wasabi calls.
            WASABI.setOptions({
                'applicationName': 'PixLike',
                'experimentName': 'TestBackground',
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
                    let imageNum = 0;
                    switch (response.assignment) {
                        case 'Cat':
                            imageNum = 1;
                            break;
                        case 'Dog':
                            imageNum = 2;
                            break;
                        case 'Fish':
                            imageNum = 3;
                            break;
                    }
                    if (imageNum > 0) {
                        sessionObj.switches = {
                            'imageNum': imageNum
                        };
                    }
                    // if (response.assignment === 'ImageOne') {
                    //     sessionObj.switches = {
                    //         'imageNum': 1
                    //     };
                    // }
                    // else if (response.assignment === 'ImageTwo') {
                    //     sessionObj.switches = {
                    //         'imageNum': 2
                    //     };
                    // }
                    // else if (response.assignment === 'ImageThree') {
                    //     sessionObj.switches = {
                    //         'imageNum': 3
                    //     };
                    // }
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
        return false;
    }

    onChange(e) {
        this.setState({
            errorMessage: '',
            [e.target.name]: e.target.value
        });
    }

    _handleKeyPress = (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            this.submit();
        }
    }

    render() {

        return <div id="mainContent">
            <div id="main">
                <div className="signinBox">
                    <form>
                        <h1>Wasabi Service</h1>
                        <h2>Please sign in with your login.</h2>

                        <div className="signinLogo">
                            <input type="text" id="userId" name="username" value={this.state.username} placeholder="User ID" onChange={this.onChange} onKeyPress={this._handleKeyPress} />

                            <button type="button" className="blue" onClick={this.submit}>Sign In</button>
                            <div className="loginError">{this.state.errorMessage}</div>
                        </div>
                    </form>
                </div>
            </div>
        </div>;
    }
}
