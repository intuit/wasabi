# CTO Dev Innovation Days
# A(B)-Team
# Wasabi Use Optimization Dashboard

##Prerequisites
```shell
node
npm
```

##Configuration

You need to have a copy of Wasabi running someplace.  It can either be on your
local system or on an external, available server.  That server will be used by
this dashboard UI.

Run the UI like this:

```shell
npm install && npm start
```

In a different terminal window, if you plan on changing the Demo app, you can also run:

```shell
npm run webpack
```

That will run webpack so that it watches the source and processes any changed files.  You don't need to do this if you
just want to run the Demo app.

##Running the Demo

In case you have stopped the server, you can start it again as follows:

```shell
% cd innovation-days
% npm start
```

That will start a node server that will both serve the UI files and act as a REST API server.  
Point your browser to localhost:3000 to bring up the UI.

##What is Demoed

...
