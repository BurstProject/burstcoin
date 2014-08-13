var NRS = (function(NRS, $, undefined) {
	NRS.automaticallyCheckRecipient = function() {
		$("#send_money_recipient, #transfer_asset_recipient, #send_message_recipient, #add_contact_account_id, #update_contact_account_id, #lease_balance_recipient").blur(function() {
			var value = $(this).val();
			var modal = $(this).closest(".modal");

			if (value) {
				NRS.checkRecipient(value, modal);
			} else {
				modal.find(".account_info").hide();
			}
		});
	}

	$("#send_message_modal, #send_money_modal, #add_contact_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var account = $invoker.data("account");

		if (!account) {
			account = $invoker.data("contact");
		}

		if (account) {
			$(this).find("input[name=recipient], input[name=account_id]").val(account).trigger("blur");
		}
	});

	$("#send_money_amount").on("input", function(e) {
		var amount = parseInt($(this).val(), 10);
		$("#send_money_fee").val(isNaN(amount) ? "1" : (amount < 500 ? 1 : Math.round(amount / 1000)));
	});

	//todo later: http://twitter.github.io/typeahead.js/
	$("span.recipient_selector button").on("click", function(e) {
		if (!Object.keys(NRS.contacts).length) {
			e.preventDefault();
			e.stopPropagation();
			return;
		}

		var $list = $(this).parent().find("ul");

		$list.empty();

		for (var accountId in NRS.contacts) {
			$list.append("<li><a href='#' data-contact='" + String(NRS.contacts[accountId].name).escapeHTML() + "'>" + String(NRS.contacts[accountId].name).escapeHTML() + "</a></li>");
		}
	});

	$("span.recipient_selector").on("click", "ul li a", function(e) {
		e.preventDefault();
		$(this).closest("form").find("input[name=converted_account_id]").val("");
		$(this).closest("form").find("input[name=recipient],input[name=account_id]").val($(this).data("contact")).trigger("blur");
	});

	NRS.forms.sendMoneyComplete = function(response, data) {
		if (!(data["_extra"] && data["_extra"].convertedAccount) && !(data.recipient in NRS.contacts)) {
			$.growl("BURST has been sent! <a href='#' data-account='" + NRS.getAccountFormatted(data, "recipient") + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>Add recipient to contacts?</a>", {
				"type": "success"
			});
		} else {
			$.growl("BUSRT has been sent!", {
				"type": "success"
			});
		}
	}

	NRS.sendMoneyShowAccountInformation = function(accountId) {
		NRS.getAccountError(accountId, function(response) {
			if (response.type == "success") {
				$("#send_money_account_info").hide();
			} else {
				$("#send_money_account_info").html(response.message).show();

			}
		});
	}

	NRS.getAccountError = function(accountId, callback) {
		NRS.sendRequest("getAccount", {
			"account": accountId
		}, function(response) {
			if (response.publicKey) {
				callback({
					"type": "info",
					"message": "The recipient account has a public key and a balance of " + NRS.formatAmount(response.unconfirmedBalanceNQT, false, true) + "BURST.",
					"account": response
				});
			} else {
				if (response.errorCode) {
					if (response.errorCode == 4) {
						callback({
							"type": "danger",
							"message": "The recipient account is malformed, please adjust." + (!/^(BURST\-)/i.test(accountId) ? " If you want to type an alias, prepend it with the @ character." : ""),
							"account": null
						});
					} else if (response.errorCode == 5) {
						callback({
							"type": "warning",
							"message": "The recipient account is an unknown account, meaning it has never had an incoming or outgoing transaction. Please double check your recipient address before submitting.",
							"account": null
						});
					} else {
						callback({
							"type": "danger",
							"message": "There is a problem with the recipient account: " + response.errorDescription,
							"account": null
						});
					}
				} else {
					callback({
						"type": "warning",
						"message": "The recipient account does not have a public key, meaning it has never had an outgoing transaction. The account has a balance of " + NRS.formatAmount(response.unconfirmedBalanceNQT, false, true) + " BURST. Please double check your recipient address before submitting.",
						"account": response
					});
				}
			}
		});
	}

	NRS.correctAddressMistake = function(el) {
		$(el).closest(".modal-body").find("input[name=recipient],input[name=account_id]").val($(el).data("address")).trigger("blur");
	}

	NRS.checkRecipient = function(account, modal) {
		var classes = "callout-info callout-danger callout-warning";

		var callout = modal.find(".account_info").first();
		var accountInputField = modal.find("input[name=converted_account_id]");

		accountInputField.val("");

		account = $.trim(account);

		//solomon reed. Btw, this regex can be shortened..
		if (/^(BURST\-)?[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+/i.test(account)) {
			var address = new NxtAddress();

			if (address.set(account)) {
				NRS.getAccountError(account, function(response) {
					if (response.account) {
						var message = "The recipient address translates to account <strong>" + String(response.account.account).escapeHTML() + "</strong>, " + response.message.replace("The recipient account", "which").escapeHTML();
					} else {
						var message = response.message.escapeHTML();
					}

					callout.removeClass(classes).addClass("callout-" + response.type).html(message).show();
				});
			} else {
				if (address.guess.length == 1) {

					callout.removeClass(classes).addClass("callout-danger").html("The recipient address is malformed, did you mean <span class='malformed_address' data-address='" + String(address.guess[0]).escapeHTML() + "' onclick='NRS.correctAddressMistake(this);'>" + address.format_guess(address.guess[0], account) + "</span> ?").show();
				} else if (address.guess.length > 1) {
					var html = "The recipient address is malformed, did you mean:<ul>";
					for (var i = 0; i < adr.guess.length; i++) {
						html += "<li><span clas='malformed_address' data-address='" + String(address.guess[i]).escapeHTML() + "' onclick='NRS.correctAddressMistake(this);'>" + adddress.format_guess(address.guess[i], account) + "</span></li>";
					}

					callout.removeClass(classes).addClass("callout-danger").html(html).show();
				} else {
					callout.removeClass(classes).addClass("callout-danger").html("The recipient address is malformed, please adjust.").show();
				}
			}
		} else if (!(/^\d+$/.test(account))) {
			if (NRS.databaseSupport && account.charAt(0) != '@') {
				NRS.database.select("contacts", [{
					"name": account
				}], function(error, contact) {
					if (!error && contact.length) {
						contact = contact[0];
						NRS.getAccountError((NRS.settings["reed_solomon"] ? contact.accountRS : contact.account), function(response) {
							callout.removeClass(classes).addClass("callout-" + response.type).html("The contact links to account <strong>" + NRS.getAccountFormatted(contact, "account") + "</strong>. " + response.message.escapeHTML()).show();

							if (response.type == "info" || response.type == "warning") {
								if (NRS.settings["reed_solomon"]) {
									accountInputField.val(contact.accountRS);
								} else {
									accountInputField.val(contact.account);
								}
							}
						});
					} else if (/^[a-z0-9]+$/i.test(account)) {
						NRS.checkRecipientAlias(account, modal);
					} else {
						callout.removeClass(classes).addClass("callout-danger").html("The recipient account is malformed, please adjust.").show();
					}
				});
			} else if (/^[a-z0-9@]+$/i.test(account)) {
				if (account.charAt(0) == '@') {
					account = account.substring(1);
					NRS.checkRecipientAlias(account, modal);
				}
			} else {
				callout.removeClass(classes).addClass("callout-danger").html("The recipient account is malformed, please adjust.").show();
			}
		} else {
			NRS.getAccountError(account, function(response) {
				callout.removeClass(classes).addClass("callout-" + response.type).html(response.message.escapeHTML()).show();
			});
		}
	}

	NRS.checkRecipientAlias = function(account, modal) {
		var classes = "callout-info callout-danger callout-warning";
		var callout = modal.find(".account_info").first();
		var accountInputField = modal.find("input[name=converted_account_id]");

		accountInputField.val("");

		NRS.sendRequest("getAlias", {
			"aliasName": account
		}, function(response) {
			if (response.errorCode) {
				callout.removeClass(classes).addClass("callout-danger").html(response.errorDescription ? "Error: " + response.errorDescription.escapeHTML() : "The alias does not exist.").show();
			} else {
				if (response.aliasURI) {
					var alias = String(response.aliasURI);
					var timestamp = response.timestamp;

					var regex_1 = /acct:(\d+)@burst/;
					var regex_2 = /nacc:(\d+)/;

					var match = alias.match(regex_1);

					if (!match) {
						match = alias.match(regex_2);
					}

					if (match && match[1]) {
						NRS.getAccountError(match[1], function(response) {
							accountInputField.val(match[1].escapeHTML());
							callout.html("The alias links to account <strong>" + match[1].escapeHTML() + "</strong>, " + response.message.replace("The recipient account", "which") + " The alias was last adjusted on " + NRS.formatTimestamp(timestamp) + ".").removeClass(classes).addClass("callout-" + response.type).show();
						});
					} else {
						callout.removeClass(classes).addClass("callout-danger").html("The alias does not link to an account. " + (!alias ? "The URI is empty." : "The URI is '" + alias.escapeHTML() + "'")).show();
					}
				} else if (response.aliasName) {
					callout.removeClass(classes).addClass("callout-danger").html("The alias links to an empty URI.").show();
				} else {
					callout.removeClass(classes).addClass("callout-danger").html(response.errorDescription ? "Error: " + response.errorDescription.escapeHTML() : "The alias does not exist.").show();
				}
			}
		});
	}

	return NRS;
}(NRS || {}, jQuery));
