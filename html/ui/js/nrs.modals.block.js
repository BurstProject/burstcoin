var NRS = (function(NRS, $, undefined) {
	$("#blocks_table, #dashboard_blocks_table").on("click", "a[data-block]", function(event) {
		event.preventDefault();

		if (NRS.fetchingModalData) {
			return;
		}

		NRS.fetchingModalData = true;

		var blockHeight = $(this).data("block");

		var block = $(NRS.blocks).filter(function() {
			return parseInt(this.height) == parseInt(blockHeight);
		}).get(0);

		if (!block) {
			NRS.getBlock($(this).data("blockid"), function(response) {
				NRS.showBlockModal(response);
			});
		} else {
			NRS.showBlockModal(block);
		}
	});

	NRS.showBlockModal = function(block) {
		$("#block_info_modal_block").html(String(block.block).escapeHTML());

		$("#block_info_transactions_tab_link").tab("show");

		var blockDetails = $.extend({}, block);
		delete blockDetails.transactions;
		delete blockDetails.previousBlockHash;
		delete blockDetails.nextBlockHash;
		delete blockDetails.generationSignature;
		delete blockDetails.payloadHash;
		delete blockDetails.block;

		$("#block_info_details_table tbody").empty().append(NRS.createInfoTable(blockDetails));
		$("#block_info_details_table").show();

		if (block.transactions.length) {
			$("#block_info_transactions_none").hide();
			$("#block_info_transactions_table").show();

			var transactions = {};
			var nrTransactions = 0;

			for (var i = 0; i < block.transactions.length; i++) {
				NRS.sendRequest("getTransaction", {
					"transaction": block.transactions[i]
				}, function(transaction, input) {
					nrTransactions++;
					transactions[input.transaction] = transaction;

					if (nrTransactions == block.transactions.length) {
						var rows = "";

						for (var i = 0; i < nrTransactions; i++) {
							var transaction = transactions[block.transactions[i]];

							if (transaction.amountNQT) {
								transaction.amount = new BigInteger(transaction.amountNQT);
								transaction.fee = new BigInteger(transaction.feeNQT);
							}

							rows += "<tr><td>" + NRS.formatTime(transaction.timestamp) + "</td><td>" + NRS.formatAmount(transaction.amount) + "</td><td>" + NRS.formatAmount(transaction.fee) + "</td><td>" + NRS.getAccountTitle(transaction, "recipient") + "</td><td>" + NRS.getAccountTitle(transaction, "sender") + "</td></tr>";
						}

						$("#block_info_transactions_table tbody").empty().append(rows);
						$("#block_info_modal").modal("show");

						NRS.fetchingModalData = false;
					}
				});
			}
		} else {
			$("#block_info_transactions_none").show();
			$("#block_info_transactions_table").hide();
			$("#block_info_modal").modal("show");

			NRS.fetchingModalData = false;
		}
	}

	return NRS;
}(NRS || {}, jQuery));