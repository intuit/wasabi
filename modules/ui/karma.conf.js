// Karma configuration
// http://karma-runner.github.io/0.10/config/configuration-file.html

module.exports = function(config) {
  config.set({
    // base path, that will be used to resolve files and exclude
    basePath: '',

    // testing framework to use (jasmine/mocha/qunit/...)
    frameworks: ['mocha', 'chai', 'sinon'],

    // list of files / patterns to load in the browser
    files: [
      'app/bower_components/jquery/dist/jquery.js',
      'app/bower_components/angular/angular.js',
      'app/bower_components/angular-mocks/angular-mocks.js',
      'app/bower_components/angular-resource/angular-resource.js',
      'https://www.gstatic.com/charts/loader.js',
      'app/scripts/config.js',
      'app/scripts/app.js',
      'app/scripts/*.js',
      'app/scripts/**/*.js',
      // 'test/mock/**/*.js',
      'app/bower_components/angular-cookies/angular-cookies.js',
      'app/bower_components/angular-bootstrap/ui-bootstrap.min.js',
      'app/bower_components/angular-bootstrap/ui-bootstrap-tpls.js',
      'app/bower_components/angular-ui-router/release/angular-ui-router.js',
      'app/bower_components/jquery-ui/jquery-ui.js',
      'https://dwum8argi892z.cloudfront.net/js/cdc_lib_min_latest.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/affix.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/alert.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/button.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/carousel.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/collapse.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/dropdown.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/tab.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/transition.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/scrollspy.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/modal.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/tooltip.js',
      'app/bower_components/bootstrap-sass-official/vendor/assets/javascripts/bootstrap/popover.js',
      'app/bower_components/moment/moment.js',
      'node_modules/sinon/pkg/sinon.js',
      'node_modules/chai/chai.js',
      'node_modules/sinon-chai/lib/sinon-chai.js',
      'test/spec/app.js',
      'test/spec/controllers/U*.js',
      'test/spec/controllers/A*.js',
      'test/spec/controllers/B*.js',
      'test/spec/controllers/C*.js',
      'test/spec/controllers/D*.js',
      'test/spec/controllers/E*.js',
      'test/spec/controllers/F*.js',
      'test/spec/controllers/M*.js',
      'test/spec/controllers/P*.js',
      'test/spec/controllers/S*.js',
      'test/spec/controllers/T*.js'
    ],

    concurrency: 1,

    // list of files / patterns to exclude
    exclude: [
      'app/scripts/dynamicEdit-0.1.js',
      'app/scripts/directives/unused/*.js',
      'app/scripts/mocks.js'
    ],

    preprocessors: {
      '**/scripts/**/*.js': ['coverage']
    },

      junitReporter: {
          outputFile: 'results/TEST-units.xml',
          suite: ''
      },

    coverageReporter: {
      reporters: [
        {
          type: 'html',
          subdir: 'report-html'
        },
        {
          type: 'text',
          subdir: '.',
          file: 'coverage.txt'
        },
          {
              type: 'lcov',
              dir: 'results/',
              subdir: '.'
          },
        {
          type: 'text-summary'
        }
      ],
      dir: 'coverage/'
    },

    reporters: ['progress', 'coverage'],

    colors: true,

    // web server port
    port: 8080,

    // level of logging
    // possible values: LOG_DISABLE || LOG_ERROR || LOG_WARN || LOG_INFO || LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,

    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera
    // - Safari (only Mac)
    // - PhantomJS
    // - IE (only Windows)
    browsers: ['PhantomJS']


    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    // singleRun: true
  });
};
