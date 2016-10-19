///////////////////////////////////
//Configuration
var server = "localhost:8080";
var appName = "Demo_App";
var expLabel = "BuyButton";
var buckets = [ "BucketA", "BucketB" ];
var colors = [ 'DarkSeaGreen', 'Peru' ];

// Experiment related Information
var userID;
var experimentID;
var bucket;

var response;
var curPage = 1;

$.ajaxSetup({
	cache : false
});


// LOGIN-Button. Get an assignment for the user.
$(document)
		.ready(
				function() {
					$("#loginAssignment")
							.click(
									function() {

										if ($('#userIDtext').val() == "") {
											alert("Please enter any UserID");
											return;
										}

										userID = escape($('#userIDtext').val());
										$
												.getJSON(
														"http://"
																+ server
																+ "/api/v1/assignments/applications/"
																+ appName
																+ "/experiments/"
																+ expLabel
																+ "/users/"
																+ userID,
														function(data, textStatus, jqXHR) {
															response = jqXHR.status + " " + jqXHR.statusText + "<br/>" + $.param(data);
															bucket = data.assignment;
                                                            $('#responseFromService').html(response);
														});
										$("#next").show();
									});
				});

// BUY-Button. Sending actions to the server
$(document).ready(
		function() {
			$("#buy")
					.click(
							function() {
								$.ajax({
									url : "http://" + server
											+ "/api/v1/events/applications/"
											+ appName + "/experiments/" + expLabel
											+ "/users/" + userID,
									type : 'post',
									data : '{"events":[{"name":"BuyClicked"}]}',
									contentType : "application/json",
									complete: function(xhr, textStatus) {
										  var msg = xhr.status + " " + xhr.statusText;
                      console.log(msg);
                      $('#responseFromService2').html(msg);
                  },

								});

							});
		});


var SwitchPages = function(){
	switch (curPage) {
	case 1:
		$('#BucketCont').slideUp();
		$('#WelcomeCont').show();
		break;
	case 2:
		$('#WelcomeCont').slideUp();
		$('#BucketCont').show();
		// record Impression, this means that the user takes
		// part in the experiment
		$.ajax({
			url : "http://" + server
					+ "/api/v1/events/applications/"
					+ appName + "/experiments/" + expLabel
					+ "/users/" + userID,
			type : 'post',
			data : '{"events":[{"name":"IMPRESSION"}]}',
			contentType : "application/json"
		});
		// set color of button
		var i;
		for (i = 0; i < buckets.length; ++i) {
			if (buckets[i] == bucket)
				$("#buy").css('background', colors[i]);
		}

		$("#next").hide();

		break;
	}

}

// NEXT-Button. Switch between the views
$(document).ready(
		function() {
			$("#next").click(
					function() {
						curPage++;
						SwitchPages();
					});
		});

//Try another User
$(document).ready(
		function() {
			$("#newUser").click(
                function() {
                    $('#responseFromService').html("");
                    $('#responseFromService2').html("");
                    curPage--;
                    SwitchPages();
                });
		});
