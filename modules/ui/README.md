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
# install tool dependencies, e.g. grunt plungins
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
