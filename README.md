Getting Started with Slate
------------------------------

### Prerequisites

You're going to need:

 - **Linux or OS X** — Windows may work, but is unsupported.
 - **Ruby, version 2.0 or newer**
 - **Bundler** — If Ruby is already installed, but the `bundle` command doesn't work, just run `gem install bundler` in a terminal.

### Getting Set Up

* Install dependencies
```shell
% cd slate
% bundle install
```
* If you want to view the documentation live:
```shell
% bundle exec middleman server
```
You can now see the docs at http://localhost:4567.

### Building the static html content
* From the project's root directory:
```shell
% ./bin/build.sh
```
This will build the static html content in `v1/guide` directory.

* View the generated html documents:
```shell
% open v1/guide/index.html
```
