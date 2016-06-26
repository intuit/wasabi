# Hello World: Client Demo App

##Navigate to the client-demo project

The 'client-demo' project is located under the 'contrib' directory.

```bash
% cd contrib/client-demo
```

##Setup an experiment

Note: make sure your Wasabi service is running (default: 192.168.99.100:8080)

```bash
% ./bin/setup.sh create
```

Create and start a 'BuyButton' experiment under 'Demo_App' application with 2 buckets:

* 'BucketA': green button, control bucket
* 'BucketB': orange button bucket
<div></div>

Note the experimentUUID if you want to remove it later.

##Launch your client app

```bash
% open index.html
```

Launch the client demo app and simulate a user getting into an experiment (assignment),
seeing an experience (impression), and clicking a button (action).
<div></div>

##Track experiment results

```bash
% open http://192.168.99.100:8080      note: sign in as admin/admin
```

Launch the Admin UI, navigate to the 'BuyButton' experiment.
<div></div>

Watch assignment, impression, and action counts increment with
users going through the client demo app.

You'll see which buckets are trending as users go through the buckets.

##Remove the sample experiment

```bash
% ./bin/setup.sh remove:<experimentUUID>
```
<div></div>
