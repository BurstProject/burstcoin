/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.lastTransactions = "";

	NRS.unconfirmedTransactions = [];
	NRS.unconfirmedTransactionIds = "";
	NRS.unconfirmedTransactionsChange = true;

	NRS.transactionsPageType = null;

	NRS.getInitialTransactions = function() {
		NRS.sendRequest("getAccountTransactions", {
			"account": NRS.account,
			"firstIndex": 0,
			"lastIndex": 9
		}, function(response) {
			if (response.transactions && response.transactions.length) {
				var transactions = [];
				var transactionIds = [];

				for (var i = 0; i < response.transactions.length; i++) {
					var transaction = response.transactions[i];

					transaction.confirmed = true;
					transactions.push(transaction);

					transactionIds.push(transaction.transaction);
				}

				NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
					NRS.handleInitialTransactions(transactions.concat(unconfirmedTransactions), transactionIds);
				});
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

			if (transactionIds.length) {
				NRS.lastTransactions = transactionIds.toString();
			}

			for (var i = 0; i < transactions.length; i++) {
				var transaction = transactions[i];

				var receiving = transaction.recipient == NRS.account;

				var account = (receiving ? "sender" : "recipient");

				if (transaction.amountNQT) {
					transaction.amount = new BigInteger(transaction.amountNQT);
					transaction.fee = new BigInteger(transaction.feeNQT);
				}

				rows += "<tr class='" + (!transaction.confirmed ? "tentative" : "confirmed") + "'><td><a href='#' data-transaction='" + String(transaction.transaction).escapeHTML() + "' data-timestamp='" + String(transaction.timestamp).escapeHTML() + "'>" + NRS.formatTimestamp(transaction.timestamp) + "</a></td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td><span" + (transaction.type == 0 && receiving ? " style='color:#006400'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</span> <span" + ((!receiving && transaction.type == 0) ? " style='color:red'" : "") + ">+</span> <span" + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</span></td><td>" + NRS.getAccountLink(transaction, account) + "</td><td class='confirmations' data-confirmations='" + String(transaction.confirmations).escapeHTML() + "' data-content='" + NRS.formatAmount(transaction.confirmations) + " confirmations' data-container='body' data-initial='true'>" + (transaction.confirmations > 10 ? "10+" : String(transaction.confirmations).escapeHTML()) + "</td></tr>";
			}

			$("#dashboard_transactions_table tbody").empty().append(rows);
		}

		NRS.dataLoadFinished($("#dashboard_transactions_table"));
	}

	NRS.getNewTransactions = function() {
		//check if there is a new transaction..
		NRS.sendRequest("getAccountTransactionIds", {
			"account": NRS.account,
			"timestamp": NRS.blocks[0].timestamp + 1,
			"firstIndex": 0,
			"lastIndex": 0
		}, function(response) {
			//if there is, get latest 10 transactions
			if (response.transactionIds && response.transactionIds.length) {
				NRS.sendRequest("getAccountTransactions", {
					"account": NRS.account,
					"firstIndex": 0,
					"lastIndex": 9
				}, function(response) {
					if (response.transactions && response.transactions.length) {
						var transactionIds = [];

						$.each(response.transactions, function(key, transaction) {
							transactionIds.push(transaction.transaction);
							response.transactions[key].confirmed = true;
						});

						NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
							NRS.handleIncomingTransactions(response.transactions.concat(unconfirmedTransactions), transactionIds);
						});
					} else {
						NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
							NRS.handleIncomingTransactions(unconfirmedTransactions);
						});
					}
				});
			} else {
				NRS.getUnconfirmedTransactions(function(unconfirmedTransactions) {
					NRS.handleIncomingTransactions(unconfirmedTransactions);
				});
			}
		});
	}

	NRS.getUnconfirmedTransactions = function(callback) {
		NRS.sendRequest("getUnconfirmedTransactions", {
			"account": NRS.account
		}, function(response) {
			if (response.unconfirmedTransactions && response.unconfirmedTransactions.length) {
				var unconfirmedTransactions = [];
				var unconfirmedTransactionIds = [];

				response.unconfirmedTransactions.sort(function(x, y) {
					if (x.timestamp < y.timestamp) {
						return 1;
					} else if (x.timestamp > y.timestamp) {
						return -1;
					} else {
						return 0;
					}
				});

				for (var i = 0; i < response.unconfirmedTransactions.length; i++) {
					var unconfirmedTransaction = response.unconfirmedTransactions[i];

					unconfirmedTransaction.confirmed = false;
					unconfirmedTransaction.unconfirmed = true;
					unconfirmedTransaction.confirmations = "/";

					if (unconfirmedTransaction.attachment) {
						for (var key in unconfirmedTransaction.attachment) {
							if (!unconfirmedTransaction.hasOwnProperty(key)) {
								unconfirmedTransaction[key] = unconfirmedTransaction.attachment[key];
							}
						}
					}

					unconfirmedTransactions.push(unconfirmedTransaction);
					unconfirmedTransactionIds.push(unconfirmedTransaction.transaction);
				}

				NRS.unconfirmedTransactions = unconfirmedTransactions;

				var unconfirmedTransactionIdString = unconfirmedTransactionIds.toString();

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
		}

		if (confirmedTransactionIds.length || NRS.unconfirmedTransactionsChange) {
			transactions.sort(NRS.sortArray);

			NRS.incoming.updateDashboardTransactions(transactions, confirmedTransactionIds.length == 0);
		}

		//always refresh peers and unconfirmed transactions..
		if (NRS.currentPage == "peers") {
			NRS.incoming.peers();
		} else if (NRS.currentPage == "transactions" && NRS.transactionsPageType == "unconfirmed") {
			NRS.incoming.transactions();
		} else {
			if (NRS.currentPage != 'messages' && (!oldBlock || NRS.unconfirmedTransactionsChange)) {
				if (NRS.incoming[NRS.currentPage]) {
					NRS.incoming[NRS.currentPage](transactions);
				}
			}
		}
		// always call incoming for messages to enable message notifications
		if (!oldBlock || NRS.unconfirmedTransactionsChange) {
			NRS.incoming['messages'](transactions);
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

				rows += "<tr class='" + (!transaction.confirmed ? "tentative" : "confirmed") + "'><td><a href='#' data-transaction='" + String(transaction.transaction).escapeHTML() + "' data-timestamp='" + String(transaction.timestamp).escapeHTML() + "'>" + NRS.formatTimestamp(transaction.timestamp) + "</a></td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td><span" + (transaction.type == 0 && receiving ? " style='color:#006400'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</span> <span" + ((!receiving && transaction.type == 0) ? " style='color:red'" : "") + ">+</span> <span" + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</span></td><td>" + NRS.getAccountLink(transaction, account) + "</td><td class='confirmations' data-confirmations='" + String(transaction.confirmations).escapeHTML() + "' data-content='" + (transaction.confirmed ? NRS.formatAmount(transaction.confirmations) + " " + $.t("confirmations") : $.t("unconfirmed_transaction")) + "' data-container='body' data-initial='true'>" + (transaction.confirmations > 10 ? "10+" : String(transaction.confirmations).escapeHTML()) + "</td></tr>";
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
			NRS.displayUnconfirmedTransactions();
			return;
		}

		var rows = "";

		var params = {
			"account": NRS.account,
			"firstIndex": 0,
			"lastIndex": 100
		};

		if (NRS.transactionsPageType) {
			params.type = NRS.transactionsPageType.type;
			params.subtype = NRS.transactionsPageType.subtype;
			var unconfirmedTransactions = NRS.getUnconfirmedTransactionsFromCache(params.type, params.subtype);
		} else {
			var unconfirmedTransactions = NRS.unconfirmedTransactions;
		}

		if (unconfirmedTransactions) {
			for (var i = 0; i < unconfirmedTransactions.length; i++) {
				rows += NRS.getTransactionRowHTML(unconfirmedTransactions[i]);
			}
		}

		NRS.sendRequest("getAccountTransactions+", params, function(response) {
			if (response.transactions && response.transactions.length) {
				for (var i = 0; i < response.transactions.length; i++) {
					var transaction = response.transactions[i];

					transaction.confirmed = true;

					rows += NRS.getTransactionRowHTML(transaction);
				}

				NRS.dataLoaded(rows);
			} else {
				NRS.dataLoaded(rows);
			}
		});
	}

	NRS.incoming.transactions = function(transactions) {
		NRS.loadPage("transactions");
	}

	NRS.displayUnconfirmedTransactions = function() {
		NRS.sendRequest("getUnconfirmedTransactions", function(response) {
			var rows = "";

			if (response.unconfirmedTransactions && response.unconfirmedTransactions.length) {
				for (var i = 0; i < response.unconfirmedTransactions.length; i++) {
					rows += NRS.getTransactionRowHTML(response.unconfirmedTransactions[i]);
				}
			}

			NRS.dataLoaded(rows);
		});
	}

	NRS.getTransactionRowHTML = function(transaction) {
		var transactionType = $.t("unknown");

		if (transaction.type == 0) {
			transactionType = $.t("ordinary_payment");
		} else if (transaction.type == 1) {
			switch (transaction.subtype) {
				case 0:
					transactionType = $.t("arbitrary_message");
					break;
				case 1:
					transactionType = $.t("alias_assignment");
					break;
				case 2:
					transactionType = $.t("poll_creation");
					break;
				case 3:
					transactionType = $.t("vote_casting");
					break;
				case 4:
					transactionType = $.t("hub_announcements");
					break;
				case 5:
					transactionType = $.t("account_info");
					break;
				case 6:
					if (transaction.attachment.priceNQT == "0") {
						if (transaction.sender == NRS.account && transaction.recipient == NRS.account) {
							transactionType = $.t("alias_sale_cancellation");
						} else {
							transactionType = $.t("alias_transfer");
						}
					} else {
						transactionType = $.t("alias_sale");
					}
					break;
				case 7:
					transactionType = $.t("alias_buy");
					break;
			}
		} else if (transaction.type == 2) {
			switch (transaction.subtype) {
				case 0:
					transactionType = $.t("asset_issuance");
					break;
				case 1:
					transactionType = $.t("asset_transfer");
					break;
				case 2:
					transactionType = $.t("ask_order_placement");
					break;
				case 3:
					transactionType = $.t("bid_order_placement");
					break;
				case 4:
					transactionType = $.t("ask_order_cancellation");
					break;
				case 5:
					transactionType = $.t("bid_order_cancellation");
					break;
			}
		} else if (transaction.type == 3) {
			switch (transaction.subtype) {
				case 0:
					transactionType = $.t("marketplace_listing");
					break;
				case 1:
					transactionType = $.t("marketplace_removal");
					break;
				case 2:
					transactionType = $.t("marketplace_price_change");
					break;
				case 3:
					transactionType = $.t("marketplace_quantity_change");
					break;
				case 4:
					transactionType = $.t("marketplace_purchase");
					break;
				case 5:
					transactionType = $.t("marketplace_delivery");
					break;
				case 6:
					transactionType = $.t("marketplace_feedback");
					break;
				case 7:
					transactionType = $.t("marketplace_refund");
					break;
			}
		} else if (transaction.type == 4) {
			switch (transaction.subtype) {
				case 0:
					transactionType = $.t("balance_leasing");
					break;
			}
		} else if (transaction.type == 20) {
			switch (transaction.subtype) {
				case 0:
					transactionType = "Reward Recipient Assignment";
					break;
			}
		} else if (transaction.type == 21) {
			switch (transaction.subtype) {
				case 0:
					transactionType = "Escrow Creation";
					break;
				case 1:
					transactionType = "Escrow Signing";
					break;
				case 2:
					transactionType = "Escrow Result";
					break;
				case 3:
					transactionType = "Subscription Subscribe";
					break;
				case 4:
					transactionType = "Subscription Cancel";
					break;
				case 5:
					transactionType = "Subscription Payment";
					break;
			}
		} else if (transaction.type == 22) {
			switch (transaction.subtype) {
				case 0:
					transactionType = "AT Creation";
					break;
				case 1:
					transactionType = "AT Payment";
					break;
			}
		}

		var receiving = transaction.recipient == NRS.account;
		var account = (receiving ? "sender" : "recipient");

		if (transaction.amountNQT) {
			transaction.amount = new BigInteger(transaction.amountNQT);
			transaction.fee = new BigInteger(transaction.feeNQT);
		}

		var hasMessage = false;

		if (transaction.attachment) {
			if (transaction.attachment.encryptedMessage || transaction.attachment.message) {
				hasMessage = true;
			} else if (transaction.sender == NRS.account && transaction.attachment.encryptToSelfMessage) {
				hasMessage = true;
			}
		}

		return "<tr " + (!transaction.confirmed && (transaction.recipient == NRS.account || transaction.sender == NRS.account) ? " class='tentative'" : "") + "><td><a href='#' data-transaction='" + String(transaction.transaction).escapeHTML() + "'>" + String(transaction.transaction).escapeHTML() + "</a></td><td>" + (hasMessage ? "<i class='fa fa-envelope-o'></i>&nbsp;" : "/") + "</td><td>" + NRS.formatTimestamp(transaction.timestamp) + "</td><td>" + transactionType + "</td><td style='width:5px;padding-right:0;'>" + (transaction.type == 0 ? (receiving ? "<i class='fa fa-plus-circle' style='color:#65C62E'></i>" : "<i class='fa fa-minus-circle' style='color:#E04434'></i>") : "") + "</td><td " + (transaction.type == 0 && receiving ? " style='color:#006400;'" : (!receiving && transaction.amount > 0 ? " style='color:red'" : "")) + ">" + NRS.formatAmount(transaction.amount) + "</td><td " + (!receiving ? " style='color:red'" : "") + ">" + NRS.formatAmount(transaction.fee) + "</td><td>" + NRS.getAccountLink(transaction, account) + "</td><td class='confirmations' data-content='" + (transaction.confirmed ? NRS.formatAmount(transaction.confirmations) + " " + $.t("confirmations") : $.t("unconfirmed_transaction")) + "' data-container='body' data-placement='left'>" + (!transaction.confirmed ? "/" : (transaction.confirmations > 1440 ? "1440+" : NRS.formatAmount(transaction.confirmations))) + "</td></tr>";
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

		NRS.loadPage("transactions");
	});

	return NRS;
}(NRS || {}, jQuery));
