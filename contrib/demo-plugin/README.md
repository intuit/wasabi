#Wasabi UI Plugin Demo

##Overview

The Wasabi Admin UI has a (simple) plugin architecture that allows you to add
features to the dialogs for editing a Draft experiment or a non-Draft (e.g., Running, Stopped, etc.)
experiment.  Since the interaction for editing Draft experiments is different from
editing experiments in other states, there are different dialogs for each.  Because
of that, the plugins are separated into plugins for the Draft dialog and plugins
for the non-Draft dialog.

The Wasabi Admin UI is built using Angular JS.  For this reason, the best way to build
a plugin is to also use Angular JS.  If you are not familiar with Angular JS, you should
look at the example and read enough to understand what it is doing.  Programming a UI using
Angular JS can be quite powerful, but also very foreign if you are not used to it.

##Lifecycle of a Plugin

Support for a plugin is built in to the Wasabi UI.  When the UI starts in a browser,
e.g., the user goes to the main page, the initialization code of the application
(in app.js) is executed.  Within that is some code that looks at a global variable
created by the scripts/plugins.js file.  That file is always included by the index.html,
but by default, it creates a variable named wasabiUIPlugins in the global namespace
that is an empty array.  If any plugins are defined, they will be described by
an object in this array.  For example:

```
var wasabiUIPlugins = [
  {
    "pluginType": "contributeDraftTab",
    "displayName": "Get/Set Priority",
    "ctrlName": "DemoPluginCtrl",
    "ctrl": "plugins/demo-plugin/ctrl.js",
    "templateUrl": "plugins/demo-plugin/template.html"
  }
];
```

This is part of the definition of the plugins in this demo.  

Before examining what each of these properties does, we need to examine how the plugins
manifest in the UI.  

If there are any plugins that modify the dialog used to edit Draft experiments (pluginType of contributeDraftTab),
then they will appear on the Plugins tab of the Draft dialog.  Each plugin appears as
a button when you select that tab.  If you click on that button, the UI defined by the
plugin is displayed in a modal dialog.

For that to work, since we are using Angular JS, we need to have loaded the controller code
so that the controller can be used by the template when the plugin dialog is displayed.  That is
done automatically when the UI starts up by the initialization code mentioned above.  The file
specified by the ctrl property of the plugin definition (plugins/demo-plugin/ctrl.js in the example above)
is loaded into the browser (basically by dynamically constructing a script tag and inserting it into the
DOM to cause the browser to load and interpret the file).  If this is successful,
the controller will now be known and available to Angular JS.

The other properties define the string displayed on the button on the Plugins tab (displayName),
the name of the controller created as described above (ctrlName, this is used by the Draft dialog when
it displays the plugin modal dialog), and the URL used to load the template of the
plugin UI (templateUrl).

Similarly, if you want to contribute a plugin to the dialog used to edit non-Draft
experiments, the plugin definition in the plugins.js file will look like this:

```
  {
    "pluginType": "contributeDetailsTab",
    "displayName": "Get/Set Priority",
    "ctrlName": "DemoPluginDetailsCtrl",
    "ctrl": "plugins/demo-plugin/detailsCtrl.js",
    "templateUrl": "plugins/demo-plugin/detailsTemplate.html"
  }
```

This is used in exactly the same way as the Draft plugin definitions.  These plugins
result in a button being added to the Plugins tab of the non-Draft dialog.  The
main difference is in how the plugin works, not in how the plugin is defined.
