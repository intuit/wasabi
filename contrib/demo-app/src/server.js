import http from 'http';
import express from 'express';
import bodyParser from 'body-parser';

export default function(config) {

    const app = express();
    const server = http.createServer(app);

    app.use(express.static(config.webServer.folder));

    server.listen(config.webServer.port, () =>
        console.log(`web server running on port ${config.webServer.port}`));
}
