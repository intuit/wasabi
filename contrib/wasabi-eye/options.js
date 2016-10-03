var save_settings = function () {
    console.log("Storing settings.");

    var url = document.getElementById('wasabi-base-url').value;

    chrome.storage.sync.set({
        baseURL: url,
    }, function() {
        console.log("Settings stored.");
        document.getElementById('status')
                .textContent = 'Settings stored.';
        setTimeout(function() {
            document.getElementById('status')
                    .textContent = '';
        }, 2000);
    });
};

var save_auth = function () {
    console.log("Creating and storing Authorization header.");

    var user = document.getElementById('wasabi-auth-user').value;
    var password = document.getElementById('wasabi-auth-pass').value;

    chrome.storage.sync.set({
        authHeader: "Basic " + window.btoa(user + ":" + password),
    }, function() {
        console.log("Authorization header stored.");
        document.getElementById('status')
                .textContent = 'Auth header generated and stored.';
        setTimeout(function() {
            document.getElementById('status')
                    .textContent = '';
        }, 2000);
    });
};

function restore_all_options () {
  // Use default value color = 'red' and likesColor = true.
  chrome.storage.sync.get({
    authHeader: "Basic dXNlcjpwYXNzd29yZA==",
    baseURL: "https://localhost:8080/",
  }, function(config) {
    console.log(window.atob(config.authHeader.substring(6)).split(":")[0]);
    document.getElementById('wasabi-auth-user').value =
        window.atob(config.authHeader.substring(6)).split(":")[0];
    document.getElementById('wasabi-base-url').value = config.baseURL;
    console.log(config.baseURL);
  });
};

document.addEventListener('DOMContentLoaded', restore_all_options);

document.getElementById('save-auth-button').addEventListener('click', save_auth);
document.getElementById('save-settings-button').addEventListener('click', save_settings);
