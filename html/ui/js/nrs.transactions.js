var NRS = (function(NRS, $, undefined) {
	NRS.lastTransactionsTimestamp = 0;
	NRS.lastTransactions = "";

	NRS.unconfirmedTransactions = [];
	NRS.unconfirmedTransactionIds = "";
	NRS.unconfirmedTransactionsChange = true;

	NRS.transactionsPageType = null;

	NRS.getInitialTransactions = function() {
		NRS.sendRequest("getAccountTransactionIds", {
			"account": NRS.account,
			"timestamp": 0
		}, function(response) {
			if (response.transactionIds && response.transactionIds.length) {
				var transactionIds = response.transactionIds.reverse().slice(0, 10);
				var nrTransactions = 0;
				var transactions = [];

				for (var i = 0; i < transactionIds.length; i++) {
					NRS.sendRequest("getTransaction", {
						"transaction": transactionIds[i]
					}, function(transaction, input) {
						nrTransactions++;

						transaction.transaction = input.transaction;
						transaction.confirmed = true;
						transactions.push(transaction);

						if (nrTransactions == transactionIds.length) {
							NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
								NRS.handleInitialTransactions(transactions.concat(unconfirmedTransactions), transactionIds);
							});
						}
					});
				}
			} else {
				NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
					NRS.handleInitialTransactions(unconfirmedTransactions, []);
				});
			}
		});
	}

	NRS.handleInitialTransactions = function(transactions, transactionIds) {
		if (transactions.length) {
			var rows = "";

			transactions.sort(NRS.sortArray);

			if (transactions.length >= 1) {
				NRS.lastTransactions = transactionIds.toString();

				for (var i = transactions.length - 1; i >= 0; i--) {
					if (transactions[i].confirmed) {
						NRS.lastTransactionsTimestamp = transactions[i].timestamp;
						break;
					}
				}
			}

			for (var i = 0; i < transactions.length; i++) {
				var transaction = transactions[i];

				var receiving = transaction.recipient == NRS.account;

				var account = (receiving ? "sender" : "recipient");

				if (transaction.amountNQT) {
					transaction.amount = new BigInteger(transaction.amountNQT);
					transaction.fee = new BigInteger(transaction.feeNQT);
				}

				rows += "<tr class='" + (!transaction.confirmed ? "tentative" : "confirmed") + "'><td><a href='#' data-transaction='" + String(transaction.transaction).escapeHTML() + "'>" + NRS.formatTimestamp(transaction.timestamp) + "</a></td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td><span" + (transaction.type == 0 && receiving ? " style='color:#006400'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</span> <span" + ((!receiving && transaction.type == 0) ? " style='color:red'" : "") + ">+</span> <span" + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</span></td><td>" + (transaction[account] != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(transaction, account) + "' data-user-id='" + String(transaction[account]).escapeHTML() + "' data-user-rs='" + String(transaction[account + "RS"]).escapeHTML() + "' class='user_info'>" + NRS.getAccountTitle(transaction, account) + "</a>" : "Genesis") + "</td><td class='confirmations' data-confirmations='" + String(transaction.confirmations).escapeHTML() + "' data-content='" + NRS.formatAmount(transaction.confirmations) + " confirmations' data-container='body' data-initial='true'>" + (transaction.confirmations > 10 ? "10+" : String(transaction.confirmations).escapeHTML()) + "</td></tr>";
			}

			$("#dashboard_transactions_table tbody").empty().append(rows);
		}

		NRS.dataLoadFinished($("#dashboard_transactions_table"));
	}

	NRS.getNewTransactions = function() {
		NRS.sendRequest("getAccountTransactionIds", {
			"account": NRS.account,
			"timestamp": NRS.lastTransactionsTimestamp
		}, function(response) {
			if (response.transactionIds && response.transactionIds.length) {
				var transactionIds = response.transactionIds.reverse().slice(0, 10);

				if (transactionIds.toString() == NRS.lastTransactions) {
					NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
						NRS.handleIncomingTransactions(unconfirmedTransactions);
					});
					return;
				}

				NRS.transactionIds = transactionIds;

				var nrTransactions = 0;

				var newTransactions = [];

				//if we have a new transaction, we just get them all.. (10 max)
				for (var i = 0; i < transactionIds.length; i++) {
					NRS.sendRequest('getTransaction', {
						"transaction": transactionIds[i]
					}, function(transaction, input) {
						nrTransactions++;

						transaction.transaction = input.transaction;
						transaction.confirmed = true;
						newTransactions.push(transaction);

						if (nrTransactions == transactionIds.length) {
							NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
								NRS.handleIncomingTransactions(newTransactions.concat(unconfirmedTransactions), transactionIds);
							});
						}
					});
				}
			} else {
				NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
					NRS.handleIncomingTransactions(unconfirmedTransactions);
				});
			}
		});
	}

	NRS.getUnconfirmedTransactions = function(callback) {
		NRS.sendRequest("getUnconfirmedTransactionIds", {
			"account": NRS.account
		}, function(response) {
			if (response.unconfirmedTransactionIds && response.unconfirmedTransactionIds.length) {
				var unconfirmedTransactionIds = response.unconfirmedTransactionIds.reverse();

				var nr_transactions = 0;

				var unconfirmedTransactions = [];
				var unconfirmedTransactionIdArray = [];

				for (var i = 0; i < unconfirmedTransactionIds.length; i++) {
					NRS.sendRequest('getTransaction', {
						"transaction": unconfirmedTransactionIds[i]
					}, function(transaction, input) {
						nr_transactions++;

						transaction.transaction = input.transaction;
						transaction.confirmed = false;
						transaction.unconfirmed = true;
						transaction.confirmations = "/";

						if (transaction.attachment) {
							for (var key in transaction.attachment) {
								if (!transaction.hasOwnProperty(key)) {
									transaction[key] = transaction.attachment[key];
								}
							}
						}

						unconfirmedTransactions.push(transaction);
						unconfirmedTransactionIdArray.push(transaction.transaction);

						if (nr_transactions == unconfirmedTransactionIds.length) {
							NRS.unconfirmedTransactions = unconfirmedTransactions;

							var unconfirmedTransactionIdString = unconfirmedTransactionIdArray.toString();

							if (unconfirmedTransactionIdString != NRS.unconfirmedTransactionIds) {
								NRS.unconfirmedTransactionsChange = true;
								NRS.unconfirmedTransactionIds = unconfirmedTransactionIdString;
							} else {
								NRS.unconfirmedTransactionsChange = false;
							}

							if (callback) {
								callback(unconfirmedTransactions);
							} else if (NRS.unconfirmedTransactionsChange) {
								NRS.incoming.updateDashboardTransactions(unconfirmedTransactions, true);
							}
						}
					});
				}
			} else {
				NRS.unconfirmedTransactions = [];

				if (NRS.unconfirmedTransactionIds) {
					NRS.unconfirmedTransactionsChange = true;
				} else {
					NRS.unconfirmedTransactionsChange = false;
				}

				NRS.unconfirmedTransactionIds = "";

				if (callback) {
					callback([]);
				} else if (NRS.unconfirmedTransactionsChange) {
					NRS.incoming.updateDashboardTransactions([], true);
				}
			}
		});
	}

	NRS.handleIncomingTransactions = function(transactions, confirmedTransactionIds) {
		var oldBlock = (confirmedTransactionIds === false); //we pass false instead of an [] in case there is no new block..

		if (typeof confirmedTransactionIds != "object") {
			confirmedTransactionIds = [];
		}

		if (confirmedTransactionIds.length) {
			NRS.lastTransactions = confirmedTransactionIds.toString();

			for (var i = transactions.length - 1; i >= 0; i--) {
				if (transactions[i].confirmed) {
					NRS.lastTransactionsTimestamp = transactions[i].timestamp;
					break;
				}
			}
		}

		if (confirmedTransactionIds.length || NRS.unconfirmedTransactionsChange) {
			transactions.sort(NRS.sortArray);

			NRS.incoming.updateDashboardTransactions(transactions, confirmedTransactionIds.length == 0);
		}

		//always refresh peers and unconfirmed transactions..
		if (NRS.currentPage == "peers" || (NRS.currentPage == "transactions" && NRS.transactionsPageType == "unconfirmed")) {
			NRS.incoming.unconfirmed_transactions();
		} else {
			if (!oldBlock || NRS.unconfirmedTransactionsChange || NRS.state.isScanning) {
				if (NRS.incoming[NRS.currentPage]) {
					NRS.incoming[NRS.currentPage](transactions);
				}
			}
		}
	}

	NRS.sortArray = function(a, b) {
		return b.timestamp - a.timestamp;
	}

	NRS.incoming.updateDashboardTransactions = function(newTransactions, unconfirmed) {
		var newTransactionCount = newTransactions.length;

		if (newTransactionCount) {
			var rows = "";

			var onlyUnconfirmed = true;

			for (var i = 0; i < newTransactionCount; i++) {
				var transaction = newTransactions[i];

				var receiving = transaction.recipient == NRS.account;
				var account = (receiving ? "sender" : "recipient");

				if (transaction.confirmed) {
					onlyUnconfirmed = false;
				}

				if (transaction.amountNQT) {
					transaction.amount = new BigInteger(transaction.amountNQT);
					transaction.fee = new BigInteger(transaction.feeNQT);
				}

				rows += "<tr class='" + (!transaction.confirmed ? "tentative" : "confirmed") + "'><td><a href='#' data-transaction='" + String(transaction.transaction).escapeHTML() + "'>" + NRS.formatTimestamp(transaction.timestamp) + "</a></td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td><span" + (transaction.type == 0 && receiving ? " style='color:#006400'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</span> <span" + ((!receiving && transaction.type == 0) ? " style='color:red'" : "") + ">+</span> <span" + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</span></td><td>" + (transaction[account] != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(transaction, account) + "' data-user-id='" + String(transaction[account]).escapeHTML() + "' data-user-rs='" + String(transaction[account + "RS"]).escapeHTML() + "' class='user_info'>" + NRS.getAccountTitle(transaction, account) + "</a>" : "Genesis") + "</td><td class='confirmations' data-confirmations='" + String(transaction.confirmations).escapeHTML() + "' data-content='" + (transaction.confirmed ? NRS.formatAmount(transaction.confirmations) + " confirmations" : "Unconfirmed transaction") + "' data-container='body' data-initial='true'>" + (transaction.confirmations > 10 ? "10+" : String(transaction.confirmations).escapeHTML()) + "</td></tr>";
			}

			if (onlyUnconfirmed) {
				$("#dashboard_transactions_table tbody tr.tentative").remove();
				$("#dashboard_transactions_table tbody").prepend(rows);
			} else {
				$("#dashboard_transactions_table tbody").empty().append(rows);
			}

			var $parent = $("#dashboard_transactions_table").parent();

			if ($parent.hasClass("data-empty")) {
				$parent.removeClass("data-empty");
				if ($parent.data("no-padding")) {
					$parent.parent().addClass("no-padding");
				}
			}
		} else if (unconfirmed) {
			$("#dashboard_transactions_table tbody tr.tentative").remove();
		}
	}

	//todo: add to dashboard? 
	NRS.addUnconfirmedTransaction = function(transactionId, callback) {
		NRS.sendRequest("getTransaction", {
			"transaction": transactionId
		}, function(response) {
			if (!response.errorCode) {
				response.transaction = transactionId;
				response.confirmations = "/";
				response.confirmed = false;
				response.unconfirmed = true;

				if (response.attachment) {
					for (var key in response.attachment) {
						if (!response.hasOwnProperty(key)) {
							response[key] = response.attachment[key];
						}
					}
				}

				var alreadyProcessed = false;

				try {
					var regex = new RegExp("(^|,)" + transactionId + "(,|$)");

					if (regex.exec(NRS.lastTransactions)) {
						alreadyProcessed = true;
					} else {
						$.each(NRS.unconfirmedTransactions, function(key, unconfirmedTransaction) {
							if (unconfirmedTransaction.transaction == transactionId) {
								alreadyProcessed = true;
								return false;
							}
						});
					}
				} catch (e) {}

				if (!alreadyProcessed) {
					NRS.unconfirmedTransactions.unshift(response);
				}

				if (callback) {
					callback(alreadyProcessed);
				}

				NRS.incoming.updateDashboardTransactions(NRS.unconfirmedTransactions, true);

				NRS.getAccountInfo();
			} else if (callback) {
				callback(false);
			}
		});
	}

	NRS.pages.transactions = function() {
		if (NRS.transactionsPageType == "unconfirmed") {
			NRS.pages.unconfirmed_transactions();
			return;
		}

		NRS.pageLoading();

		var params = {
			"account": NRS.account,
			"timestamp": 0
		};

		if (NRS.transactionsPageType) {
			params.type = NRS.transactionsPageType.type;
			params.subtype = NRS.transactionsPageType.subtype;
		}

		var rows = "";

		if (NRS.unconfirmedTransactions.length) {
			for (var j = 0; j < NRS.unconfirmedTransactions.length; j++) {
				var unconfirmedTransaction = NRS.unconfirmedTransactions[j];

				if (NRS.transactionsPageType) {
					if (unconfirmedTransaction.type != params.type || unconfirmedTransaction.subtype != params.subtype) {
						continue;
					}
				}

				rows += NRS.getTransactionRowHTML(unconfirmedTransaction);
			}
		}

		NRS.sendRequest("getAccountTransactionIds+", params, function(response) {
			if (response.transactionIds && response.transactionIds.length) {
				var transactions = {};
				var nr_transactions = 0;

				var transactionIds = response.transactionIds.reverse().slice(0, 100);

				for (var i = 0; i < transactionIds.length; i++) {
					NRS.sendRequest("getTransaction+", {
						"transaction": transactionIds[i]
					}, function(transaction, input) {
						if (NRS.currentPage != "transactions") {
							transactions = {};
							return;
						}

						transaction.transaction = input.transaction;
						transaction.confirmed = true;

						transactions[input.transaction] = transaction;
						nr_transactions++;

						if (nr_transactions == transactionIds.length) {
							for (var i = 0; i < nr_transactions; i++) {
								var transaction = transactions[transactionIds[i]];

								rows += NRS.getTransactionRowHTML(transaction);

							}

							$("#transactions_table tbody").empty().append(rows);
							NRS.dataLoadFinished($("#transactions_table"));

							NRS.pageLoaded();
						}
					});

					if (NRS.currentPage != "transactions") {
						transactions = {};
						return;
					}
				}
			} else {

				$("#transactions_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#transactions_table"));

				NRS.pageLoaded();
			}
		});
	}

	NRS.incoming.transactions = function(transactions) {
		NRS.pages.transactions();
	}

	NRS.pages.unconfirmed_transactions = function() {
		NRS.pageLoading();

		NRS.sendRequest("getUnconfirmedTransactions", function(response) {
			if (response.unconfirmedTransactions && response.unconfirmedTransactions.length) {
				rows = "";

				for (var i = 0; i < response.unconfirmedTransactions.length; i++) {
					var unconfirmedTransaction = response.unconfirmedTransactions[i];

					rows += NRS.getTransactionRowHTML(unconfirmedTransaction);
				}

				$("#transactions_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#transactions_table"));

				NRS.pageLoaded();
			} else {
				$("#transactions_table tbody").empty();
				NRS.dataLoadFinished($("#transactions_table"));

				NRS.pageLoaded();
			}
		});
	}

	NRS.incoming.unconfirmed_transactions = function() {
		NRS.pages.unconfirmed_transactions();
	}

	NRS.getTransactionRowHTML = function(transaction) {
		var transactionType = "Unknown";

		if (transaction.type == 0) {
			transactionType = "Ordinary payment";
		} else if (transaction.type == 1) {
			switch (transaction.subtype) {
				case 0:
					transactionType = "Arbitrary message";
					break;
				case 1:
					transactionType = "Alias assignment";
					break;
				case 2:
					transactionType = "Poll creation";
					break;
				case 3:
					transactionType = "Vote casting";
					break;
				case 4:
					transactionType = "Hub Announcement";
					break;
				case 5:
					transactionType = "Account Info";
					break;
			}
		} else if (transaction.type == 2) {
			switch (transaction.subtype) {
				case 0:
					transactionType = "Asset issuance";
					break;
				case 1:
					transactionType = "Asset transfer";
					break;
				case 2:
					transactionType = "Ask order placement";
					break;
				case 3:
					transactionType = "Bid order placement";
					break;
				case 4:
					transactionType = "Ask order cancellation";
					break;
				case 5:
					transactionType = "Bid order cancellation";
					break;
			}
		} else if (transaction.type == 3) {
			switch (transaction.subtype) {
				case 0:
					transactionType = "Digital Goods Listing";
					break;
				case 1:
					transactionType = "Digital Goods Delisting";
					break;
				case 2:
					transactionType = "Digtal Goods Price Change";
					break;
				case 3:
					transactionType = "Digital Goods Quantity Change";
					break;
				case 4:
					transactionType = "Digital Goods Purchase";
					break;
				case 5:
					transactionType = "Digital Goods Delivery";
					break;
				case 6:
					transactionType = "Digital Goods Feedback";
					break;
				case 7:
					transactionType = "Digital Goods Refund";
					break;
			}
		} else if (transaction.type == 4) {
			switch (transaction.subtype) {
				case 0:
					transactionType = "Balance Leasing";
					break;
			}
		}

		var receiving = transaction.recipient == NRS.account;
		var account = (receiving ? "sender" : "recipient");

		if (transaction.amountNQT) {
			transaction.amount = new BigInteger(transaction.amountNQT);
			transaction.fee = new BigInteger(transaction.feeNQT);
		}

		return "<tr " + (!transaction.confirmed && (transaction.recipient == NRS.account || transaction.sender == NRS.account) ? " class='tentative'" : "") + "><td><a href='#' data-transaction='" + String(transaction.transaction).escapeHTML() + "'>" + String(transaction.transaction).escapeHTML() + "</a></td><td>" + NRS.formatTimestamp(transaction.timestamp) + "</td><td>" + transactionType + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td " + (transaction.type == 0 && receiving ? " style='color:#006400;'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</td><td " + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</td><td>" + (transaction[account] != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(transaction, account) + "' class='user_info'>" + NRS.getAccountTitle(transaction, account) + "</a>" : "Genesis") + "</td><td class='confirmations' data-content='" + (transaction.confirmed ? NRS.formatAmount(transaction.confirmations) + " confirmations" : "Unconfirmed transaction") + "' data-container='body' data-placement='left'>" + (!transaction.confirmed ? "/" : (transaction.confirmations > 1440 ? "1440+" : NRS.formatAmount(transaction.confirmations))) + "</td></tr>";
	}

	$("#transactions_page_type li a").click(function(e) {
		e.preventDefault();

		var type = $(this).data("type");

		if (!type) {
			NRS.transactionsPageType = null;
		} else if (type == "unconfirmed") {
			NRS.transactionsPageType = "unconfirmed";
		} else {
			type = type.split(":");
			NRS.transactionsPageType = {
				"type": type[0],
				"subtype": type[1]
			};
		}

		$(this).parents(".btn-group").find(".text").text($(this).text());

		$(".popover").remove();

		NRS.pages.transactions();
	});

	return NRS;
}(NRS || {}, jQuery));