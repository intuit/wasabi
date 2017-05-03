// extracts the "webServer" property of the package.json file, and returns it
// as an object for configuring the web server, and processing the webpack
// files

module.exports = require('lodash').pick(
	JSON.parse(require('fs').readFileSync(
		require('path').join(__dirname, './package.json')),
		function(propName, propValue) {
			if (propName === 'port') {
				return process.env.PORT || propValue;
			}
			return propValue;
		}), ['webServer', 'mongoServer']);
