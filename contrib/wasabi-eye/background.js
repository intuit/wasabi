chrome.browserAction.onClicked.addListener(function(tab) {
    chrome.tabs.sendMessage(tab.id, {greeting: "hello"}, function(response) {
        console.log("Response = " + response);
    });
});
