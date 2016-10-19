"use strict";

(function() {
    angular.module('storeApp')
    .controller('StoreController', ['$http', '$location', '$window', 'ProductsFactory', function ($http, $location, $window, ProductsFactory) {
        var store = this;
        store.store = 'Online Product Store';
        store.products = [];
        store.cart = [];

        store.loadProducts = function() {
            ProductsFactory.loadProducts()
            .success(function(products) {
                store.products = products;
            })
            .catch(function(err) {
                console.log('Problem getting products');
                console.dir(err);
            });
        };

        store.addToCart = function(product) {
            if (product.quantity === '' || product.quantity === 0) {
                // Just ignore it.
                return;
            }
            // Find this product in the cart, if there, so we can update the quantity.
            var prodIndex = -1;
            for (var i = 0; i < store.cart.length; i++) {
                if (store.cart[i].name === product.name) {
                    prodIndex = i;
                }
            }
            if (prodIndex >= 0) {
                // Just add to the product already in the cart
                store.cart[prodIndex].quantity += product.quantity;
            }
            else {
                // Clone the product into the cart
                store.cart.push(JSON.parse(JSON.stringify(product)));
            }
            product.quantity = 0;

            // Record the action that they clicked on the Buy button.
            WASABI.postAction(
            'ClickedOnBuy',
            null /* No extra action info */,
            {
                'userID': session.login.name
            }).then(
                function (response) {
                    console.log('postAction: success');
                    if (response) {
                        console.log(JSON.stringify(response));
                    }
                },
                function (error) {
                    console.log('postAction: error');
                }
            );
        };

        store.removeFromCart = function(product) {
            var index = store.cart.findIndex(function(e, index, arr) {
                if (e.name === product.name) {
                    return true;
                }
            });
            if (index >= 0) {
                store.cart.splice(index, 1);
            }
        };

        var session = sessionStorage.getItem('session');
        if (session) {
            session = JSON.parse(session);
        }
        else {
            session = {
                'login': {
                    'name': '',
                    'loggedIn': false
                }
            };
        }

        if (!session.login.loggedIn) {
          console.log('not logged in');
          $window.location.href = '/#/login';
          return;
        }

        // Set up properties that will be the same on all Wasabi calls.
        // We need to do this here in case the user has refreshed the browser.
        // TODO: Maybe a better way to handle this?
        WASABI.setOptions({
            'applicationName': 'MyStore',
            'experimentName': 'TestBuyButtonColor',
            'protocol': 'http',
            'host': 'localhost:8080'
        });

        store.username = session.login.name;

        // Button color test
        if (session.switches) {
            store.buttonColor = 'white'; // Control value
            store.textColor = 'black';
            if (session.switches.buyButtonColor) {
                store.buttonColor = session.switches.buyButtonColor;
                store.textColor = 'white';
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
                }
            );
        }

        store.loadProducts();

    }]);

})();
