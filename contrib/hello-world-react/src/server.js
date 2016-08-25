import http from 'http';
import express from 'express';
import bodyParser from 'body-parser';

export default function(config) {

    const app = express();
    const server = http.createServer(app);

    let products = [
        {id: '1', name: 'iphone 6', cost: 300},
        {id: '2', name: 'soap', cost: 2},
        {id: '3', name: 'juice', cost: 5},
        {id: '4', name: 'batteries', cost: 5}
    ];

    app.use(function(req, res, next) {
        res.header('Access-Control-Allow-Origin', '*');
        res.header('Access-Control-Allow-Methods', 'GET,PUT,POST,DELETE');
        res.header('Access-Control-Allow-Headers', 'Content-Type');

        next();
    });

    app.use(bodyParser.json());

    app.get('/products', function(req, res) {
        res.json(products);
    });

    app.post('/products', function(req, res) {
        console.log(req.body);
        products.push(req.body);
        console.log(products);
        res.json({msg: 'new product received'});
    });

    app.use(express.static(config.webServer.folder));

    server.listen(config.webServer.port, () =>
        console.log(`web server running on port ${config.webServer.port}`));
}
