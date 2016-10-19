"use strict";

var express = require('express');
var bodyParser = require('body-parser');
var app = express();

var products = [
    {name: 'iphone 6', cost: 300, quantity: 0},
    {name: 'soap', cost: 2, quantity: 0},
    {name: 'juice', cost: 5, quantity: 0},
    {name: 'batteries', cost: 5, quantity: 0}
];

app.use(function(req, res, next) {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE');
  res.header('Access-Control-Allow-Headers', 'Content-Type');

  next();
});

app.use(bodyParser.json());
app.use(express.static('app'));

app.get('/products', function(req, res) {
  res.json(products);
});

app.post('/products', function(req, res) {
  console.log(req.body);
  products.push(req.body);
  console.log(products);
  res.json({msg: 'new product received'});
});

var port = 3000;
app.listen(port, function() {
  console.log('server listening on port ' + port);
});
