"use strict";

(function() {
    angular.module('storeApp')
    .factory('ProductsFactory', ['$http', function ($http) {
        var factory = this;
        factory._products = [];

        return {
            loadProducts: function() {
                return $http.get('http://localhost:3000/products');
                // .success(function(data) {
                //   store.products = data;
                // })
                //.catch(function(info) {
                //     console.log(info);
                // }).finally(function() {
                //     // finally will get called after success or catch. Useful for perfoming cleanup operations
                //     console.log('From finally: I always run');
                // })
            },
            setProducts: function(products) {
                factory._products = products;
            },
            getProducts: function() {
                return factory._products;
            },
            newProduct: function(newProduct) {
                $http.post('http://localhost:3000/products', newProduct)
                .success(function(data) {
                    console.log('New product added into factory:');
                    factory._products.push(newProduct);
                }).catch(function(info) {
                    console.log(info);
                });
            }
        }
    }]);
})();
