var NRS = (function(NRS, $, undefined) {
	NRS.loadContacts = function() {
		NRS.contacts = {};

		NRS.database.select("contacts", null, function(error, contacts) {
			if (contacts.length) {
				$.each(contacts, function(index, contact) {
					NRS.contacts[contact.account] = contact;
				});
			}
		});
	}

	NRS.pages.contacts = function() {
		NRS.pageLoading();

		if (!NRS.databaseSupport) {
			$("#contact_page_database_error").show();
			$("#contacts_table_container").hide();
			$("#add_contact_button").hide();
			NRS.pageLoaded();
			return;
		}

		$("#contacts_table_container").show();
		$("#contact_page_database_error").hide();

		NRS.database.select("contacts", null, function(error, contacts) {
			if (contacts.length) {
				var rows = "";

				contacts.sort(function(a, b) {
					if (a.name.toLowerCase() > b.name.toLowerCase()) {
						return 1;
					} else if (a.name.toLowerCase() < b.name.toLowerCase()) {
						return -1;
					} else {
						return 0;
					}
				});

				$.each(contacts, function(index, contact) {
					var contactDescription = contact.description;

					if (contactDescription.length > 100) {
						contactDescription = contactDescription.substring(0, 100) + "...";
					} else if (!contactDescription) {
						contactDescription = "-";
					}

					rows += "<tr><td><a href='#' data-toggle='modal' data-target='#update_contact_modal' data-contact='" + String(contact.id).escapeHTML() + "'>" + contact.name.escapeHTML() + "</a></td><td><a href='#' data-user='" + NRS.getAccountFormatted(contact, "account") + "' class='user_info'>" + NRS.getAccountFormatted(contact, "account") + "</a></td><td>" + (contact.email ? contact.email.escapeHTML() : "-") + "</td><td>" + contactDescription.escapeHTML() + "</td><td style='white-space:nowrap'><a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#send_money_modal' data-contact='" + String(contact.name).escapeHTML() + "'>Send Burst</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#send_message_modal' data-contact='" + String(contact.name).escapeHTML() + "'>Message</a> <a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#delete_contact_modal' data-contact='" + String(contact.id).escapeHTML() + "'>Delete</a></td></tr>";
				});

				$("#contacts_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#contacts_table"));

				NRS.pageLoaded();
			} else {
				$("#contacts_table tbody").empty();
				NRS.dataLoadFinished($("#contacts_table"));

				NRS.pageLoaded();
			}
		});
	}

	NRS.forms.addContact = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.account_id = String(data.account_id);

		if (!data.name) {
			return {
				"error": "Contact name is a required field."
			};
		} else if (!data.account_id) {
			return {
				"error": "Account ID is a required field."
			};
		}

		if (/^\d+$/.test(data.name) || /^BURST\-/i.test(data.name)) {
			return {
				"error": "Contact name must contain alphabetic characters."
			};
		}

		if (data.email && !/@/.test(data.email)) {
			return {
				"error": "Email address is incorrect."
			};
		}

		if (data.account_id.charAt(0) == '@') {
			var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
			if (convertedAccountId) {
				data.account_id = convertedAccountId;
			} else {
				return {
					"error": "Invalid account ID."
				};
			}
		}

		if (/^BURST\-/i.test(data.account_id)) {
			data.account_rs = data.account_id;

			var address = new NxtAddress();

			if (address.set(data.account_rs)) {
				data.account = address.account_id();
			} else {
				return {
					"error": "Invalid account ID."
				};
			}
		} else {
			var address = new NxtAddress();

			if (address.set(data.account_id)) {
				data.account_rs = address.toString();
			} else {
				return {
					"error": "Invalid account ID."
				};
			}
		}

		NRS.sendRequest("getAccount", {
			"account": data.account_id
		}, function(response) {
			if (!response.errorCode) {
				if (response.account != data.account || response.accountRS != data.account_rs) {
					return {
						"error": "Invalid account ID."
					};
				}
			}
		}, false);

		var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal], .ignore)");

		NRS.database.select("contacts", [{
			"account": data.account_id
		}, {
			"name": data.name
		}], function(error, contacts) {
			if (contacts.length) {
				if (contacts[0].name == data.name) {
					$modal.find(".error_message").html("A contact with this name already exists.").show();
				} else {
					$modal.find(".error_message").html("A contact with this account ID already exists.").show();
				}
				$btn.button("reset");
				$modal.modal("unlock");
			} else {
				NRS.database.insert("contacts", {
					name: data.name,
					email: data.email,
					account: data.account_id,
					accountRS: data.account_rs,
					description: data.description
				}, function(error) {
					NRS.contacts[data.account_id] = {
						name: data.name,
						email: data.email,
						account: data.account_id,
						accountRS: data.account_rs,
						description: data.description
					};

					setTimeout(function() {
						$btn.button("reset");
						$modal.modal("unlock");
						$modal.modal("hide");
						$.growl("Contact added successfully.", {
							"type": "success"
						});

						if (NRS.currentPage == "contacts") {
							NRS.pages.contacts();
						} else if (NRS.currentPage == "messages" && NRS.selectedContext) {
							var heading = NRS.selectedContext.find("h4.list-group-item-heading");
							if (heading.length) {
								heading.html(data.name.escapeHTML());
							}
							NRS.selectedContext.data("context", "messages_sidebar_update_context");
						}
					}, 50);
				});
			}
		});
	}

	$("#update_contact_modal").on('show.bs.modal', function(e) {
		var $invoker = $(e.relatedTarget);

		var contactId = parseInt($invoker.data("contact"), 10);

		if (!contactId && NRS.selectedContext) {
			var accountId = NRS.selectedContext.data("account");

			var dbKey = (/^BURST\-/i.test(accountId) ? "accountRS" : "account");

			var dbQuery = {};
			dbQuery[dbKey] = accountId;

			NRS.database.select("contacts", [dbQuery], function(error, contact) {
				contact = contact[0];

				$("#update_contact_id").val(contact.id);
				$("#update_contact_name").val(contact.name);
				$("#update_contact_email").val(contact.email);
				if (NRS.settings["reed_solomon"]) {
					$("#update_contact_account_id").val(contact.accountRS);
				} else {
					$("#update_contact_account_id").val(contact.account);
				}
				$("#update_contact_description").val(contact.description);
			});
		} else {
			$("#update_contact_id").val(contactId);

			NRS.database.select("contacts", [{
				"id": contactId
			}], function(error, contact) {
				contact = contact[0];

				$("#update_contact_name").val(contact.name);
				$("#update_contact_email").val(contact.email);
				if (NRS.settings["reed_solomon"]) {
					$("#update_contact_account_id").val(contact.accountRS);
				} else {
					$("#update_contact_account_id").val(contact.account);
				}
				$("#update_contact_description").val(contact.description);
			});
		}
	});

	NRS.forms.updateContact = function($modal) {
		var data = NRS.getFormData($modal.find("form:first"));

		data.account_id = String(data.account_id);

		if (!data.name) {
			return {
				"error": "Contact name is a required field."
			};
		} else if (!data.account_id) {
			return {
				"error": "Account ID is a required field."
			};
		}

		if (data.account_id.charAt(0) == '@') {
			var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
			if (convertedAccountId) {
				data.account_id = convertedAccountId;
			} else {
				return {
					"error": "Invalid account ID."
				};
			}
		}

		var contactId = parseInt($("#update_contact_id").val(), 10);

		if (!contactId) {
			return {
				"error": "Invalid contact."
			};
		}

		if (/^BURST\-/i.test(data.account_id)) {
			data.account_rs = data.account_id;

			var address = new NxtAddress();

			if (address.set(data.account_rs)) {
				data.account_id = address.account_id();
			} else {
				return {
					"error": "Invalid account ID."
				};
			}
		} else {
			var address = new NxtAddress();

			if (address.set(data.account_id)) {
				data.account_rs = address.toString();
			} else {
				return {
					"error": "Invalid account ID."
				};
			}
		}

		NRS.sendRequest("getAccount", {
			"account": data.account_id
		}, function(response) {
			if (!response.errorCode) {
				if (response.account != data.account_id || response.accountRS != data.account_rs) {
					return {
						"error": "Invalid account ID."
					};
				}
			}
		}, false);

		var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");

		NRS.database.select("contacts", [{
			"account": data.account_id
		}], function(error, contacts) {
			if (contacts.length && contacts[0].id != contactId) {
				$modal.find(".error_message").html("A contact with this account ID already exists.").show();
				$btn.button("reset");
				$modal.modal("unlock");
			} else {
				NRS.database.update("contacts", {
					name: data.name,
					email: data.email,
					account: data.account_id,
					accountRS: data.account_rs,
					description: data.description
				}, [{
					"id": contactId
				}], function(error) {
					if (contacts.length && data.account_id != contacts[0].accountId) {
						delete NRS.contacts[contacts[0].accountId];
					}

					NRS.contacts[data.account_id] = {
						name: data.name,
						email: data.email,
						account: data.account_id,
						accountRS: data.account_rs,
						description: data.description
					};

					setTimeout(function() {
						$btn.button("reset");
						$modal.modal("unlock");
						$modal.modal("hide");
						$.growl("Contact updated successfully.", {
							"type": "success"
						});

						if (NRS.currentPage == "contacts") {
							NRS.pages.contacts();
						} else if (NRS.currentPage == "messages" && NRS.selectedContext) {
							var heading = NRS.selectedContext.find("h4.list-group-item-heading");
							if (heading.length) {
								heading.html(data.name.escapeHTML());
							}
						}
					}, 50);
				});
			}
		});
	}

	$("#delete_contact_modal").on('show.bs.modal', function(e) {
		var $invoker = $(e.relatedTarget);

		var contactId = $invoker.data("contact");

		$("#delete_contact_id").val(contactId);

		NRS.database.select("contacts", [{
			"id": contactId
		}], function(error, contact) {
			contact = contact[0];

			$("#delete_contact_name").html(contact.name.escapeHTML());
			$("#delete_contact_account_id").val(contact.accountId);
		});
	});

	NRS.forms.deleteContact = function($modal) {
		var id = parseInt($("#delete_contact_id").val(), 10);

		NRS.database.delete("contacts", [{
			"id": id
		}], function() {
			delete NRS.contacts[$("#delete_contact_account_id").val()];

			setTimeout(function() {
				$.growl("Contact deleted successfully.", {
					"type": "success"
				});

				if (NRS.currentPage == "contacts") {
					NRS.pages.contacts();
				}
			}, 50);
		});

		return {
			"stop": true
		};
	}

	return NRS;
}(NRS || {}, jQuery));
