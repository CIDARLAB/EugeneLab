
$(document).ready(function() {
	
	// for the eugenelab.html web site, 
	// we disable all File buttons if the user is not logged in,
	// i.e. there's no cookie set
	if(document.location.pathname == '/EugeneLab/eugenelab.html') {
		var cookie = getCookie("eugenelab");
		
		// the user is not logged in
		// -> default user
		// disable buttons
		if(cookie === null || cookie === '') {
			
		    $('#btnNewFile').attr("disabled", "disabled");
		    $('#btnNewFile').prop("disabled", true);
		    
		    $('#btnUploadFile').attr("disabled", "disabled");
		    $('#btnUploadFile').prop("disabled", true);
		
		    $('#btnDeleteFile').attr("disabled", "disabled");
		    $('#btnDeleteFile').prop("disabled", true);

		    $('#btnSave').attr("disabled", "disabled");
		    $('#btnSave').prop("disabled", true);
		    
		    // disable the library tree
		    $('#libraryTree').removeClass("active");
		    $('#libraryTree').prop("disable", true);
		    $('#libraryTree').attr("disable", "disabled");
		    
		    $('#libraryTreeLink').prop("disable", true);
		    $('#libraryTreeLink').removeAttr("href");
		    $('#libraryTreeLink').attr("disable", "disabled");
		    
		    $('#fileTree').addClass("active");
		    
		} else {
			// the user is logged in, 
			// enable the buttons
		    $('#btnNewFile').removeAttr("disabled");
		    $('#btnNewFile').prop("disabled", false);
		    
		    $('#btnUploadFile').removeAttr("disabled");
		    $('#btnUploadFile').prop("disabled", false);
		
		    $('#btnDeleteFile').removeAttr("disabled");
		    $('#btnDeleteFile').prop("disabled", false);

		    $('#btnSave').removeAttr("disabled");
		    $('#btnSave').prop("disabled", false);
		}
	}
	
	function setCookie(c_name, value, exdays) {
		var exdate = new Date();
		exdate.setDate(exdate.getDate() + exdays);
		var c_value = escape(value) + ((exdays === null) ? "" : "; expires=" + exdate.toUTCString());
		document.cookie = c_name + "=" + c_value;
	}
	
	function getCookie(c_name) {
		var c_value = document.cookie;
		var c_start = c_value.indexOf(" " + c_name + "=");
		if (c_start === -1) {
			c_start = c_value.indexOf(c_name + "=");
		}
		if (c_start === -1) {
			c_value = null;
		}
		else {
			c_start = c_value.indexOf("=", c_start) + 1;
			var c_end = c_value.indexOf(";", c_start);
			if (c_end === -1) {
				c_end = c_value.length;
			}
			c_value = unescape(c_value.substring(c_start, c_end));
		}

		return c_value;
	}
	
	function deleteCookie(key) {
		// Delete a cookie by setting the expiration date to yesterday
		date = new Date();
		date.setDate(date.getDate() - 1);
		document.cookie = escape(key) + '=;expires=' + date;
	}
		
	if (getCookie("eugenelab") !== "authenticated") {
		deleteCookie("user");
	}
	
	if (getCookie("eugenelab") === "authenticated") {
		$('#loginArea').html('<div id="loginArea" class="navbar-form pull-right">'+
						'You are logged in as <strong>' + getCookie("user") + '</strong>&nbsp;&nbsp;&nbsp;&nbsp;'+
						'<button id="btnLogout" class="btn btn-primary btn-warning">Logout</button>');
		
		if(document.location.pathname == '/EugeneLab/index.html') {
			// disable the "Try it for free!" button
			$('#tryIt').html('');
			
			// forward the user to the eugenelab.html site
			window.location.replace('eugenelab.html');
		}
	} else if (getCookie("authenticate") === "failed") {
		window.location.replace("index.html");
	}

	
	//-----------------------------------------------
	// AUTHENTICATION
	//-----------------------------------------------
	
	// SIGNUP Button
	$('#btnSignUp').click(function() {
		var username = $('#signup_username').val();
		var jsonRequest =  {"command": "signup", 
							"username": username, 
							"password": $('#signup_password').val()};
		
		$.post("AuthenticationServlet", jsonRequest, function(response) {
			
			// if there was an error, then we display the error
			if(response['status'] === 'exception') {			
				$('#signupError').html('<div class="alert alert-danger">' + response['result'] + '</div>');
			} else {
				$('#signupError').html('<div class="alert alert-success"> Success! </div>');

				// set the cookie
				setCookie("user", username, 1);
				setCookie("eugenelab", "authenticated", 1);
				
				window.location.replace('eugenelab.html');
			}
		});
	});
	
	// LOGIN button
	$('#btnLogin').click(function() {
		var username = $('#login_username').val();
		var jsonRequest =  {"command": "login", 
							"username": username, 
							"password": $('#login_password').val()};
		
		$.post("AuthenticationServlet", jsonRequest, function(response) {
			
			// if there was an error, then we display the error
			if(response['status'] === 'exception') {			
				$('#loginError').html('<div class="alert alert-danger">' + response['result'] + '</div>');
			} else {
				$('#loginError').html('');

				// set the cookie
				setCookie("user", username, 1);
				setCookie("eugenelab", "authenticated", 1);

				//window.location.replace('eugenelab.html');
				$('#loginArea').html('<div id="loginArea" class="navbar-form pull-right">'+
						'You are logged in as <strong>' + getCookie("user") + '</strong>&nbsp;&nbsp;&nbsp;&nbsp;'+
						'<button id="btnLogout" class="btn btn-primary btn-warning">Logout</button>');
				
				location.reload();
				//<p class="pull-right" style="margin-top:10px">You are logged in as <strong>' + getCookie("user") + '</strong> <a id="logout">Log Out</a></p>');
			}
		});
	});
	
	// LOGOUT Button
	$('#btnLogout').click(function() {
		
		var user = getCookie("user");
		var jsonRequest =  {"command": "logout", 
				"username":getCookie("user")};
		$.post("AuthenticationServlet", jsonRequest, function(response) {
			// if there was an error, then we display the error
			if(response['status'] === 'exception') {			
				$('#loginError').html('<div class="alert alert-danger">' + response['result'] + '</div>');
			} else {
				deleteCookie("user");
				deleteCookie("eugenelab");
				
				window.location.replace('index.html');
			}
		});
	});
	
	
});