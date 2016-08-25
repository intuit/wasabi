'use strict';

// resolves path for the webpack configuration
// copied from the Angular.io webpack configuration document

const path = require('path');
const _root = path.resolve(__dirname, '..');

module.exports.root = function(args) {
  args = Array.prototype.slice.call(arguments, 0);
  return path.join.apply(path, [_root].concat(args));
};
