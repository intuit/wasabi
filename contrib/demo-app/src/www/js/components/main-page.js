/*global WASABI*/
import React from 'react';
import sunsetImage from '../../assets/austin_at_sunset.jpg';
import daytimeImage from '../../assets/austin_and_bridge.jpg';

export class MainPageComponent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            session: {
                login: {
                    'name': '',
                    'loggedIn': false
                },
                skylineImage: ''
            }
        };

        WASABI.setOptions({
            'applicationName': 'PixLike',
            'experimentName': 'TestAustinSkyline',
            'protocol': 'http',
            'host': 'localhost:8080'
        });

        this.doLikeIt = this.doLikeIt.bind(this);
    }

    componentDidMount() {
        var session = sessionStorage.getItem('session');
        if (session) {
            session = JSON.parse(session);
            this.setState({
                session: session
            });
            if (session.switches) {
                this.setState({
                    skylineImage: ''
                });
                if (session.switches.skylineImage) {
                    this.setState({
                        skylineImage: session.switches.skylineImage
                    });
                }
                // Record impression, that we've shown them the tested experience.
                WASABI.postImpression({
                    'userID': session.login.name
                }).then(
                    function(response) {
                        console.log('postImpression: success');
                        if (response) {
                            console.log(JSON.stringify(response));
                        }
                    },
                    function(error) {
                        console.log('postImpression: error');
                        console.dir(error);
                    }
                );
            }
        }
    }

    doLikeIt() {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');

        let whichPhone = 'other';
        if (this.state.session.userAgent.indexOf('iPhone')) {
            whichPhone = 'iPhone';
        }
        else if (this.state.session.userAgent.indexOf('Android')) {
            whichPhone = 'Android';
        }
        // Record the action that they clicked on the Like button.
        WASABI.postAction(
            'ClickedOnLike',
            '{"userAgent": "' + whichPhone + '"}',
            {
                'userID': this.state.session.login.name
            }
        ).then(
            function (response) {
                console.log('postAction: success');
                if (response) {
                    console.log(JSON.stringify(response));
                }
            },
            function (error) {
                console.log('postAction: error');
                console.dir(error);
            }
        );
    }

    render() {
        const imageURL = (this.state.skylineImage === 'night' ? sunsetImage : daytimeImage);
        const divStyle = {
            backgroundImage: 'url(' + imageURL + ')',
            backgroundSize: '375px 667px',
            width: $(window).width(),
            height: $(window).height()
        };

        return <div>
            <div className="logoutLink">
                <a href="#" onClick={this.props.doLogout}>Logout</a>
            </div>
            <div style={divStyle}>
                <button style={{
                        position: 'absolute',
                        bottom: '90px',
                        right: '40px'
                    }} onClick={this.doLikeIt}>I Like It!</button>
            </div>
        </div>;
    }
}
