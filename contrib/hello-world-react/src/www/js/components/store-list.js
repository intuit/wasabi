/*global WASABI*/
import React from 'react';

import { ItemTableComponent } from './item-table';
import { ItemCartComponent } from './item-cart';

const myFields = [
    {
        name: 'Name',
        fieldName: 'name',
        headerStyle: {
            width: '260px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: ''
    },
    {
        name: 'Cost',
        fieldName: 'cost',
        headerStyle: {
            width: '100px'
        },
        style: {
            textAlign: 'right',
            paddingRight: '10px'
        },
        dataPrefix: '$'
    },
    {
        name: 'Quantity',
        fieldName: 'quantityInput',
        headerStyle: {
        },
        style: {
            textAlign: 'center'
        },
        dataPrefix: ''
    },
    {
        name: ' ',
        fieldName: 'buyButton',
        headerStyle: {
            width: '50px'
        },
        style: {
            textAlign: 'center'
        },
        dataPrefix: ''
    }
];

export class StoreListComponent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            store: 'Online Product Store',
            items: [],
            myFields: myFields,
            cart: [],
            query: '',
            session: {
                login: {
                    'name': '',
                    'loggedIn': false
                },
                buttonColor: 'white',
                textColor: 'black'
            }
        };

        this.addToCart = this.addToCart.bind(this);
        this.removeFromCart = this.removeFromCart.bind(this);
        this.onChange = this.onChange.bind(this);

        WASABI.setOptions({
            'applicationName': 'MyStore',
            'experimentName': 'TestBuyButtonColor',
            'protocol': 'http',
            'host': 'localhost:8080'
        });
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
                    buttonColor: 'white', // Control value
                    textColor: 'black'
                });
                if (session.switches.buyButtonColor) {
                    this.setState({
                        buttonColor: session.switches.buyButtonColor, // Control value
                        textColor: 'white'
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

        // When the component mounts, resolve the promise from the fetch to get the list of items.
        this.props.items.then(items => {
            this.setState({
                items: items.concat(),
                originalItems: items.concat()
            });
        });
    }

    onChange(e) {
        // Save the new input where the input field pulls its display value.
        this.setState({
            [e.target.name]: e.target.value
        });
    }

    addToCart(order) {
        // The order object has the quantity to order and a clone of the item being ordered
        if (order.quantity === '' || order.quantity === 0) {
            // Just ignore it.
            return;
        }
        let product = order.item;
        // Find this product in the cart, if there, so we can update the quantity.
        var prodIndex = -1;
        for (var i = 0; i < this.state.cart.length; i++) {
            if (this.state.cart[i].name === product.name) {
                prodIndex = i;
            }
        }
        if (prodIndex >= 0) {
            // Just add to the product already in the cart
            let newCart = this.state.cart.concat();
            newCart[prodIndex].quantity = parseInt(newCart[prodIndex].quantity) + parseInt(order.quantity);
            this.setState({
                cart: newCart
            });
        }
        else {
            // Clone the product into the cart
            product.quantity = order.quantity;
            let newCart = this.state.cart.concat(JSON.parse(JSON.stringify(product)));
            this.setState({
                cart: newCart
            });
        }

        // Record the action that they clicked on the Buy button.
        WASABI.postAction(
            'ClickedOnBuy',
            null /* No extra action info */,
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

    removeFromCart(product) {
        let index = this.state.cart.findIndex(function(e) {
            if (e.name === product.name) {
                return true;
            }
        });
        if (index >= 0) {
            let newCart = this.state.cart.concat();
            newCart.splice(index, 1);
            this.setState({
                cart: newCart
            });
        }
    }

    render() {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');

        return <div>
            <div className="siteHeading">
                <h1>Welcome to our {this.state.store}, {this.state.session.login.name} </h1>
                <div className="logoutLink">
                    <a href="#" onClick={this.props.doLogout}>Logout</a>
                </div>
            </div>
            <div className="storeMain">
                <section>
                    <h2> Our Products </h2>
                    Search:     <input type="text" name="query" value={this.state.query} onChange={this.onChange} />
                <ItemTableComponent fields={this.state.myFields} buttonColor={this.state.buttonColor} textColor={this.state.textColor} buyFunc={this.addToCart} items={this.state.items} query={this.state.query} />
                </section>
                <section>
                    <h2>Cart</h2>
                    {
                        this.state.cart.length === 0
                            ? <span>Your cart is empty</span>
                            : <ItemCartComponent cart={this.state.cart} removeFunc={this.removeFromCart} />
                    }
                </section>
            </div>
        </div>;
    }
}
