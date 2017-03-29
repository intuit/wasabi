'use strict';

// loaded the web server settings from package.json
const config = require('../config');

// merge the common configuration with the environment specific configuration
module.exports = require('webpack-merge')(require('./webpack.common.js'), {

  // use inline source maps
  // this specific setting value is required to set breakpoints in the TypeScript
  // in the web browser for development
  // other source map settings do not allow debugging
  devtool: 'inline-source-map',

  // out file settings
  // path points to web server content folder where the web server will serve the files from
  // publicPath is the path to the files from the perspective of the web brower requesting
  // the files from the web server, this is used to insert the script elements into the index.html
  // file
  // file name is the name of the files, where [name] is the name of each entry point
  output: {
    path: require('./helpers').root(config.webServer.folder),
    publicPath: `${config.webServer.protocol}://${config.webServer.host}:${config.webServer.port}/`,
    filename: '[name].js'
  }

});
