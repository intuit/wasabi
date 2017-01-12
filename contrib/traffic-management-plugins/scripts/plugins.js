var wasabiUIPlugins = [
    {
        'pluginType': 'contributeSharedCode',
        'description': 'Shared code to be loaded by both the Traffic Management plugins.',
        'ctrl': 'plugins/traffic-management-shared/ctrl.js'
    },
    {
        'pluginType': 'contributeAppLevelFeature',
        'displayName': 'Traffic Estimator',
        'description': 'This is a plugin to aid in determining the sampling percentages for multiple, mutually exclusive experiments through the use of target sampling percentages.',
        'ctrlName': 'TrafficManagementCtrl',
        'ctrl': 'plugins/traffic-management/ctrl.js',
        'templateUrl': 'plugins/traffic-management/template.html'
    },
    {
        'pluginType': 'contributeAppLevelFeature',
        'displayName': 'Traffic Analyzer',
        'description': 'This plugin allows you to aid monitor the daily sampling percentages for multiple, mutually exclusive experiments.',
        'ctrlName': 'TrafficAnalysisCtrl',
        'ctrl': 'plugins/traffic-analysis/ctrl.js',
        'templateUrl': 'plugins/traffic-analysis/template.html'
    }
];
