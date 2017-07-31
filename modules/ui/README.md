# Wasabi User Interface

## User Interface (UI) Prerequisites

Install NodeJS and NPM

```
% brew install node
```

Install Yo, Grunt, Bower, and Compass

```
% npm install -g yo grunt-cli bower grunt-contrib-compass
% sudo gem install compass
```

# Building the UI

```
% cd modules/ui
# install tool dependencies, e.g. grunt plugins
% npm install
# install js-dependencies, as angular.js, highcharts.js and such
% bower install
# deploy UI to dist/, all css/js are minified, images optimized
% grunt build
```

### Run the UI

```
% grunt serve
```

### Run the Production Version of the UI

If you want to test how the UI runs from the combined and minified files (which you would generally use
when you go to Production), you will need to do the following (after you have done the build steps above):

Edit default_constants.json with the following change to the apiHostBaseUrlValue value, since you would be running the
Wasabi server on localhost:8080, but the UI will be served on localhost:9000.  This is then produced in the
dist/scripts/config.js file, which causes the backend API URLs to start with that value, and hit your docker container:

```javascript
{
    supportEmail: 'you@example.com',
    apiHostBaseUrlValue: 'http://localhost:8080/api/v1'
}
```

Then:

```
% grunt serve:dist
```

This will build the UI into the dist folder and then start a web server, serving the UI from that folder on
http://localhost:9000 .

### Troubleshooting

If you happen to get some errors or problems with building a new version of the Wasabi UI after an update, you should
follow these steps to refresh the build-time and run-time libraries:

```
% cd modules/ui
% rm -rf node_modules
% rm -rf app/bower_components
% npm install
% bower install
```

This will ensure that the versions of all the libraries are consistent with the current version of the Wasabi
UI code.


