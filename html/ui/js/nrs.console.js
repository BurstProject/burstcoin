/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.showConsole = function() {
		NRS.console = window.open("", "console", "width=750,height=400,menubar=no,scrollbars=yes,status=no,toolbar=no,resizable=yes");
		$(NRS.console.document.head).html("<title>" + $.t("console") + "</title><style type='text/css'>body { background:black; color:white; font-family:courier-new,courier;font-size:14px; } pre { font-size:14px; } #console { padding-top:15px; }</style>");
		$(NRS.console.document.body).html("<div style='position:fixed;top:0;left:0;right:0;padding:5px;background:#efefef;color:black;'>" + $.t("console_opened") + "<div style='float:right;text-decoration:underline;color:blue;font-weight:bold;cursor:pointer;' onclick='document.getElementById(\"console\").innerHTML=\"\"'>clear</div></div><div id='console'></div>");
	}

	NRS.addToConsole = function(url, type, data, response, error) {
		if (!NRS.console) {
			return;
		}

		if (!NRS.console.document || !NRS.console.document.body) {
			NRS.console = null;
			return;
		}

		url = url.replace(/&random=[\.\d]+/, "", url);

		NRS.addToConsoleBody(url + " (" + type + ") " + new Date().toString(), "url");

		if (data) {
			if (typeof data == "string") {
				var d = NRS.queryStringToObject(data);
				NRS.addToConsoleBody(JSON.stringify(d, null, "\t"), "post");
			} else {
				NRS.addToConsoleBody(JSON.stringify(data, null, "\t"), "post");
			}
		}

		if (error) {
			NRS.addToConsoleBody(response, "error");
		} else {
			NRS.addToConsoleBody(JSON.stringify(response, null, "\t"), (response.errorCode ? "error" : ""));
		}
	}

	NRS.addToConsoleBody = function(text, type) {
		var color = "";

		switch (type) {
			case "url":
				color = "#29FD2F";
				break;
			case "post":
				color = "lightgray";
				break;
			case "error":
				color = "red";
				break;
		}

		$(NRS.console.document.body).find("#console").append("<pre" + (color ? " style='color:" + color + "'" : "") + ">" + text.escapeHTML() + "</pre>");
	}

	NRS.queryStringToObject = function(qs) {
		qs = qs.split("&");

		if (!qs) {
			return {};
		}

		var obj = {};

		for (var i = 0; i < qs.length; ++i) {
			var p = qs[i].split('=');

			if (p.length != 2) {
				continue;
			}

			obj[p[0]] = decodeURIComponent(p[1].replace(/\+/g, " "));
		}

		if ("secretPhrase" in obj) {
			obj.secretPhrase = "***";
		}

		return obj;
	}

	return NRS;
}(NRS || {}, jQuery));