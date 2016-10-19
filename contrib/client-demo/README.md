### Wasabi Client Demo App

#### Setup an experiment

Note: make sure your Wasabi service is running (default: localhost:8080)

Create and start a 'BuyButton' experiment under 'Demo_App' application with 2 buckets:

* 'BucketA': green button, control bucket
* 'BucketB': orange button bucket

```bash
% ./bin/setup.sh create
```
Note the experimentUUID if you want to remove it later.

#### Launch your client app

```bash
% open index.html
```

#### Remove the sample experiment

```bash
% ./bin/setup.sh remove:<experimentUUID>
```