'use strict';

const webpack = require('webpack');
const helpers = require('./helpers');

// use to insert script elements points to webpack bundles in the main index.html file
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = {

  // entry points for the three bundles, order does not matter
  entry: {
    'app': './src/www/js/app.js'
  },

  // allows us to require modules using
  // import { someExport } from './my-module';
  // instead of
  // import { someExport } from './my-module.ts';
  // with the extensions in the list, it can be omitted from the import
  // root is an absolute path to the folder containing our application modules
  resolve: {
    extensions: ['', '.js', '.json'], // order matters, resolves left to right
		root: helpers.root('src','www','js')
  },

  module: {
    loaders: [
      // process all JavaScript files through the Babel preprocessor
      // this enables support for all of ES2015 including modules
      {
        test: /\.js$/,
        exclude: /node_modules/,
        loader: 'babel-loader',
        query: {
          passPerPreset: true,
          presets: [
            // Babel Relay Plugin path is relative to /src/www/js
            // { 'plugins': [ '../../../build/babelRelayPlugin' ] },
            'react', 'es2015', 'stage-0']
        }
      },
      // processes JSON files, useful for config files and mock data
      {
        test: /\.json$/,
        loader: 'json'
      },
      // processes HTML files into JavaScript files
      // useful for bundling HTML with Angular Component
      {
        test: /\.html$/,
        exclude: [ helpers.root('src','www','index.html') ],
        loader: 'html'
      },
      // outputs images and font files to a common assets folder
      // updates HTML and CSS paths which reference these files
      {
        test: /\.(png|jpe?g|gif|svg|woff|woff2|ttf|eot|ico)$/,
        loader: 'file?name=assets/[name].[hash].[ext]'
      },
      // transpiles global SASS stylesheets
			{
			  test: /\.scss$/,
			  loaders: ['style','css','postcss','sass'] // loader order is executed right to left
			}
    ]
  },

  // configuration for the postcss loader which modifies CSS after
  // processing
  // autoprefixer plugin for postcss adds vendor specific prefixing for
  // non-standard or experimental css properties
  postcss: [ require('autoprefixer') ],

  // gives an annoying warning from postcss which cannot be resolved
  // so we are choosing to ignore it
  resolveUrlLoader: { silent: true },

  plugins: [

    // this one is a little complex to understand
    // when app bundle code imports vendor bundle code, webpack will want to include the
    // the vendor code in the app bundle, this makes sense when we think of webpack as a
    // bundler which bundles all imported code together
    // but this is NOT what we want webpack to do
    // instead, we want webpack to keep the three bundles separate, and once all are loaded
    // in the web browser we trust it will all work, and all three will work together
    // as expect
    // so this plugin keeps the three bundle separated and does not put the code of one,
    // in the code of another

    // order of the names does matter
    // order establishes a hierarchy
    // app -> vendor -> polyfills
    // which means app depends upon vendor, and vendor upon polyfills
    // therefore, polyfills will be loaded first (script element appear first in index.html)
    // vendor will be loaded next, and finally app
    new webpack.optimize.CommonsChunkPlugin({
      name: ['app']
    }),

    // configure the file to have the bundle script elements injected
    // this is almost always the main html for the initial loading of 
    // the site
    new HtmlWebpackPlugin({
      template: './src/www/index.html'
    }),

    // configure the new fetch function for browsers which do not support it
    new webpack.ProvidePlugin({
      'Promise': 'exports?global.Promise!es6-promise',
      'fetch': 'imports?this=>global!exports?global.fetch!whatwg-fetch',
      'window.fetch': 'imports?this=>global!exports?global.fetch!whatwg-fetch'
    })   
  ]

};
