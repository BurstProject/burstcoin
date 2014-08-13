var NRS = (function(NRS, $, undefined) {
	NRS.pages.aliases = function() {
		NRS.pageLoading();

		NRS.sendRequest("getAliases+", {
			"account": NRS.account,
			"timestamp": 0
		}, function(response) {
			if (response.aliases && response.aliases.length) {
				var aliases = response.aliases;

				if (NRS.unconfirmedTransactions.length) {
					for (var i = 0; i < NRS.unconfirmedTransactions.length; i++) {
						var unconfirmedTransaction = NRS.unconfirmedTransactions[i];

						if (unconfirmedTransaction.type == 1 && unconfirmedTransaction.subtype == 1) {
							var found = false;

							for (var j = 0; j < aliases.length; j++) {
								if (aliases[j].aliasName == unconfirmedTransaction.attachment.alias) {
									aliases[j].aliasURI = unconfirmedTransaction.attachment.uri;
									aliases[j].tentative = true;
									found = true;
									break;
								}
							}

							if (!found) {
								aliases.push({
									"aliasName": unconfirmedTransaction.attachment.alias,
									"aliasURI": unconfirmedTransaction.attachment.uri,
									"tentative": true
								});
							}
						}
					}
				}

				aliases.sort(function(a, b) {
					if (a.aliasName.toLowerCase() > b.aliasName.toLowerCase()) {
						return 1;
					} else if (a.aliasName.toLowerCase() < b.aliasName.toLowerCase()) {
						return -1;
					} else {
						return 0;
					}
				});

				var rows = "";

				var alias_account_count = 0,
					alias_uri_count = 0,
					empty_alias_count = 0,
					alias_count = aliases.length;

				for (var i = 0; i < alias_count; i++) {
					var alias = aliases[i];

					rows += "<tr" + (alias.tentative ? " class='tentative'" : "") + " data-alias='" + String(alias.aliasName).toLowerCase().escapeHTML() + "'><td class='alias'>" + String(alias.aliasName).escapeHTML() + (alias.tentative ? " -  <strong>Pending</strong>" : "") + "</td><td>" + (alias.aliasURI.indexOf("http") === 0 ? "<a href='" + String(alias.aliasURI).escapeHTML() + "' target='_blank'>" + String(alias.aliasURI).escapeHTML() + "</a>" : String(alias.aliasURI).escapeHTML()) + "</td><td><a href='#' data-toggle='modal' data-alias='" + String(alias.aliasName).escapeHTML() + "' data-target='#register_alias_modal'>Edit</a></td></tr>";
					if (!alias.aliasURI) {
						empty_alias_count++;
					} else if (alias.aliasURI.indexOf("http") === 0) {
						alias_uri_count++;
					} else if (alias.aliasURI.indexOf("acct:") === 0 || alias.aliasURI.indexOf("nacc:") === 0) {
						alias_account_count++;
					}
				}

				$("#aliases_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#aliases_table"));

				$("#alias_account_count").html(alias_account_count).removeClass("loading_dots");
				$("#alias_uri_count").html(alias_uri_count).removeClass("loading_dots");
				$("#empty_alias_count").html(empty_alias_count).removeClass("loading_dots");
				$("#alias_count").html(alias_count).removeClass("loading_dots");
			} else {
				$("#aliases_table tbody").empty();
				NRS.dataLoadFinished($("#aliases_table"));

				$("#alias_account_count, #alias_uri_count, #empty_alias_count, #alias_count").html("0").removeClass("loading_dots");
			}

			NRS.pageLoaded();
		});
	}

	$("#register_alias_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var alias = $invoker.data("alias");

		if (alias) {
			alias = String(alias);

			NRS.sendRequest("getAlias", {
				"aliasName": alias
			}, function(response) {
				if (/http:\/\//i.test(response.aliasURI)) {
					NRS.forms.setAliasType("uri");
				} else if (/acct:(\d+)@burst/.test(response.aliasURI) || /nacc:(\d+)/.test(response.aliasURI)) {
					NRS.forms.setAliasType("account");
				} else {
					NRS.forms.setAliasType("general");
				}

				$("#register_alias_modal h4.modal-title").html("Update Alias");
				$("#register_alias_modal .btn-primary").html("Update");
				$("#register_alias_alias").val(alias.escapeHTML()).hide();
				$("#register_alias_alias_noneditable").html(alias.escapeHTML()).show();
				$("#register_alias_alias_update").val(1);
				$("#register_alias_uri").val(response.aliasURI);
			});
		} else {
			$("#register_alias_modal h4.modal-title").html("Register Alias");
			$("#register_alias_modal .btn-primary").html("Register");
			$("#register_alias_alias").val("").show();
			$("#register_alias_alias_noneditable").html("").hide();
			$("#register_alias_alias_update").val(0);
			NRS.forms.setAliasType("uri");
		}
	});

	NRS.incoming.aliases = function(transactions) {
		if (transactions || NRS.unconfirmedTransactionsChange || NRS.state.isScanning) {
			NRS.pages.aliases();
		}
	}

	NRS.forms.setAlias = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.uri = $.trim(data.uri);

		if (data.type == "account") {
			if (!(/acct:(\d+)@burst/.test(data.uri)) && !(/nacc:(\d+)/.test(data.uri))) {
				if (/^\d+$/.test(data.uri)) {
					data.uri = "acct:" + data.uri + "@burst";
				} else {
					return {
						"error": "Invalid account ID."
					};
				}
			}

		}
		delete data["type"];

		if ($("#register_alias_alias_update").val() == 1) {
			return {
				"data": data,
				"successMessage": "Alias updated successfully"
			};
		} else {
			return {
				"data": data
			};
		}
	}

	NRS.forms.setAliasType = function(type, uri) {
		$("#register_alias_type").val(type);

		if (type == "uri") {
			$("#register_alias_uri_label").html("URI");
			$("#register_alias_uri").prop("placeholder", "URI");
			if (uri) {
				if (!/https?:\/\//i.test(uri)) {
					$("#register_alias_uri").val("http://" + uri);
				} else {
					$("#register_alias_uri").val(uri);
				}
			} else {
				$("#register_alias_uri").val("http://");
			}
			$("#register_alias_help").hide();
		} else if (type == "account") {
			$("#register_alias_uri_label").html("Account ID");
			$("#register_alias_uri").prop("placeholder", "Account ID");
			$("#register_alias_uri").val("");
			if (uri) {
				if (!(/acct:(\d+)@burst/.test(uri)) && !(/nacc:(\d+)/.test(uri))) {
					if (/^\d+$/.test(uri)) {
						$("#register_alias_uri").val("acct:" + uri + "@burst");
					} else {
						$("#register_alias_uri").val("");
					}
				} else {
					$("#register_alias_uri").val("");
				}
			} else {
				$("#register_alias_uri").val("");
			}
			$("#register_alias_help").html("The alias will reference the account number entered and can be used to send Burst to, messages, etc..").show();
		} else {
			$("#register_alias_uri_label").html("Data");
			$("#register_alias_uri").prop("placeholder", "Data");
			if (uri) {
				$("#register_alias_uri").val(uri);
			} else {
				$("#register_alias_uri").val("");
			}
			$("#register_alias_help").html("The alias can contain any data you want.").show();
		}
	}

	$("#register_alias_type").on("change", function() {
		var type = $(this).val();
		NRS.forms.setAliasType(type, $("#register_alias_uri").val());
	});

	NRS.forms.setAliasComplete = function(response, data) {
		if (response.alreadyProcessed) {
			return;
		}

		if (NRS.currentPage == "aliases") {
			var $table = $("#aliases_table tbody");

			var $row = $table.find("tr[data-alias=" + String(data.alias).toLowerCase().escapeHTML() + "]");

			if ($row.length) {
				$row.addClass("tentative");
				$row.find("td.alias").html(data.alias.escapeHTML() + " - <strong>Pending</strong>");

				if (data.uri && data.uri.indexOf("http") === 0) {
					$row.find("td.uri").html("<a href='" + String(data.uri).escapeHTML() + "' target='_blank'>" + String(data.uri).escapeHTML() + "</a>");
				} else {
					$row.find("td.uri").html(String(data.uri).escapeHTML());
				}
			} else {
				var $rows = $table.find("tr");

				var rowToAdd = "<tr class='tentative' data-alias='" + String(data.alias).toLowerCase().escapeHTML() + "'><td class='alias'>" + data.alias.escapeHTML() + " -  <strong>Pending</strong></td><td class='uri'>" + (data.uri && data.uri.indexOf("http") === 0 ? "<a href='" + String(data.uri).escapeHTML() + "' target='_blank'>" + data.uri.escapeHTML() + "</a>" : String(data.uri).escapeHTML()) + "</td><td>Edit</td></tr>";

				var rowAdded = false;

				var newAlias = String(data.alias).toLowerCase();

				if ($rows.length) {
					$rows.each(function() {
						var alias = $(this).data("alias");

						if (newAlias < alias) {
							$(this).before(rowToAdd);
							rowAdded = true;
							return false;
						}
					});
				}

				if (!rowAdded) {
					$table.append(rowToAdd);
				}

				if ($("#aliases_table").parent().hasClass("data-empty")) {
					$("#aliases_table").parent().removeClass("data-empty");
				}
			}
		}
	}

	$("#alias_search").on("submit", function(e) {
		e.preventDefault();

		if (NRS.fetchingModalData) {
			return;
		}

		NRS.fetchingModalData = true;

		var alias = $.trim($("#alias_search input[name=q]").val());

		$("#alias_info_table tbody").empty();

		NRS.sendRequest("getAlias", {
			"aliasName": alias
		}, function(response, input) {
			if (response.errorCode) {
				$.growl("Could not find alias.", {
					"type": "danger"
				});
				NRS.fetchingModalData = false;
			} else {
				$("#alias_info_modal_alias").html(String(response.aliasName).escapeHTML());

				var data = {
					"Account": NRS.getAccountTitle(response, "account"),
					"Last Updated": NRS.formatTimestamp(response.timestamp),
					"DataFormattedHTML": String(response.aliasURI).autoLink()
				}

				$("#alias_info_table tbody").append(NRS.createInfoTable(data));

				$("#alias_info_modal").modal("show");
				NRS.fetchingModalData = false;
			}
		});
	});

	return NRS;
}(NRS || {}, jQuery));
