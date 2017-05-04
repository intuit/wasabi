/*global WASABI*/
import React from 'react';
import image1 from '../../assets/catPicture.jpg';
import image2 from '../../assets/dogPicture.jpg';
import image3 from '../../assets/fishPicture.jpg';

export class MainPageComponent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            session: {
                login: {
                    'name': '',
                    'loggedIn': false
                },
            },
            imageNum: '',
            whichPhone: 'other'
        };

        WASABI.setOptions({
            'applicationName': 'PixLike',
            'experimentName': 'TestBackground',
            'protocol': 'http',
            'host': 'localhost:8080'
        });

        this.doLikeIt = this.doLikeIt.bind(this);
        this.doHateIt = this.doHateIt.bind(this);
    }

    componentDidMount() {
        var session = sessionStorage.getItem('session');
        if (session) {
            session = JSON.parse(session);

            let whichPhone = 'other';
            if (session.userAgent.indexOf('iPhone') >= 0) {
                whichPhone = 'iPhone';
            }
            else if (session.userAgent.indexOf('Android') >= 0) {
                whichPhone = 'Android';
            }

            this.setState({
                session: session,
                whichPhone: whichPhone
            });
            if (session.switches) {
                if (session.switches.imageNum) {
                    this.setState({
                        imageNum: session.switches.imageNum
                    });
                }
                // Record impression, that we've shown them the tested experience.
                // NOTE: We're using the postAction() method of wasabi.js because
                // we want to record a payload with the impression.
                WASABI.postAction(
                    'IMPRESSION',
                    '{"userAgent": "' + whichPhone + '"}',
                    {
                        'userID': session.login.name
                    }
                ).then(
                    function (response) {
                        console.log('postImpression: success');
                        if (response) {
                            console.log(JSON.stringify(response));
                        }
                    },
                    function (error) {
                        console.log('postImpression: error');
                        console.dir(error);
                    }
                );
            }
        }
    }

    doThanks() {
        $('.thanksMessage').show();
        setTimeout(function() {
            $('.thanksMessage').hide();
        }, 3000);
    }

    doHateIt() {
        this.doThanks();
    }

    doLikeIt() {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');

        // Record the action that they clicked on the Like button.
        WASABI.postAction(
            'ClickedOnLike',
            '{"userAgent": "' + this.state.whichPhone + '"}',
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

        this.doThanks();
    }

    render() {
        const imageURL = (this.state.imageNum === 1 ? image1 : (this.state.imageNum === 2 ? image2 : image3));
        const divStyle = {
            backgroundImage: 'url(' + imageURL + ')',
            backgroundSize: $(window).width() + 'px ' + $(window).height() + 'px',
            backgroundRepeat: 'no-repeat',
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
                        left: '20px'
                    }} onClick={this.doLikeIt}>Great Pet!</button>
                <button style={{
                        position: 'absolute',
                        bottom: '90px',
                        right: '20px'
                    }} onClick={this.doHateIt}>Not For Me</button>
            </div>
            <div className="thanksMessage">
                Thanks for your feedback!
            </div>
        </div>;
    }
}
