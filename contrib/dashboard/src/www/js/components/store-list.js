import React from 'react';

const helpers = require('../helpers.js');

import { ItemTableComponent } from './item-table';
import { ItemCartComponent } from './item-cart';

const myFields = [
    {
        name: 'Name',
        fieldName: 'label',
        headerStyle: {
            width: '260px',
            textAlign: 'left'
        },
        style: {
            paddingLeft: '10px'
        },
        dataPrefix: ''
    // },
    // {
    //     name: 'Cost',
    //     fieldName: 'cost',
    //     headerStyle: {
    //         width: '100px'
    //     },
    //     style: {
    //         textAlign: 'right',
    //         paddingRight: '10px'
    //     },
    //     dataPrefix: '$'
    // },
    // {
    //     name: 'Quantity',
    //     fieldName: 'quantityInput',
    //     headerStyle: {
    //     },
    //     style: {
    //         textAlign: 'center'
    //     },
    //     dataPrefix: ''
    // },
    // {
    //     name: ' ',
    //     fieldName: 'buyButton',
    //     headerStyle: {
    //         width: '50px'
    //     },
    //     style: {
    //         textAlign: 'center'
    //     },
    //     dataPrefix: ''
    }
];

export class StoreListComponent extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            items: [],
            myFields: myFields,
            cart: [],
            query: '',
            pageName: '',
            pages: [],
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

        // When the component mounts, resolve the promise from the fetch to get the list of items.
        this.props.items.then(items => {
            this.setState({
                items: items.experiments.concat(),
                originalItems: items.experiments.concat()
            });
        });
        this.props.pages.then(items => {
            this.setState({
                pages: items.concat()
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

    handleChange(event) {
        this.setState({pageName: event.target.value});
    }

    render() {
        const headers = new Headers();
        headers.append('Content-Type', 'application/json');

        return <div>
            <div className="siteHeading">
                <h1>{this.state.session.login.name} </h1>
                <div className="logoutLink">
                    <a href="#" onClick={this.props.doLogout}>Logout</a>
                </div>
            </div>
            <div className="storeMain">
                <section>
                    <h2> Our Products </h2>
                    Page:     <select value={this.state.pageName} onChange={this.handleSelectChange}>
                                {this.props.pages.map((pageName, index) => <option value={pageName} key={index}>{pageName}</option>)}
                              </select>
                    <ItemTableComponent fields={this.state.myFields} buttonColor={this.state.buttonColor} textColor={this.state.textColor} buyFunc={this.addToCart} items={this.state.items} query={this.state.query} />
                </section>
            </div>
        </div>;
    }
}
