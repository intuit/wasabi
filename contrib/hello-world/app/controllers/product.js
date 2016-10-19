"use strict";

(function() {
    angular.module('storeApp')
    .controller('ProductController', ['$http', 'ProductsFactory', function ($http, ProductsFactory) {
      var product = this;
      product.name = '';
      product.cost = '';
      product.errorMessage = '';
      product.store = null;

      product.setStore = function(store) {
          product.store = store;
      };

      product.submit = function() {
        if (validate()) {
          console.log('submitted');
          ProductsFactory.newProduct({name: product.name, cost: product.cost});
          if (product.store) {
              product.store.loadProducts();
          }

          product.name = '';
          product.cost = '';
        //   var data = JSON.stringify({name: product.name, cost: product.cost});
        //   $http.post('http://localhost:3000/products', data).success(function(data) {
        //     console.log(data);
        //   });
        }
        else {
          console.log('invalid');
        }
      };

      function validate() {
        if (product.name == '' || !product.name) {
          product.errorMessage = 'name must be present';
          return false;
        }
        if (product.cost == '' || !product.cost) {
          product.errorMessage = 'cost must be present';
          return false;
        }
        product.errorMessage = '';
        return true;
      }
    }]);
})();
