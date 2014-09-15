/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	$("#blocks_table, #dashboard_blocks_table").on("click", "a[data-block]", function(event) {
		event.preventDefault();

		if (NRS.fetchingModalData) {
			return;
		}

		NRS.fetchingModalData = true;

		var blockHeight = $(this).data("block");

		NRS.sendRequest("getBlock+", {
			"height": blockHeight,
			"includeTransactions": "true"
		}, function(response) {
			NRS.showBlockModal(response);
		});
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

			var rows = "";

			block.transactions.sort(function(a, b) {
				return a.timestamp - b.timestamp;
			});

			for (var i = 0; i < block.transactions.length; i++) {
				var transaction = block.transactions[i];

				if (transaction.amountNQT) {
					transaction.amount = new BigInteger(transaction.amountNQT);
					transaction.fee = new BigInteger(transaction.feeNQT);
				}

				rows += "<tr><td>" + NRS.formatTime(transaction.timestamp) + "</td><td>" + NRS.formatAmount(transaction.amount) + "</td><td>" + NRS.formatAmount(transaction.fee) + "</td><td>" + NRS.getAccountTitle(transaction, "recipient") + "</td><td>" + NRS.getAccountTitle(transaction, "sender") + "</td></tr>";
			}

			$("#block_info_transactions_table tbody").empty().append(rows);
			$("#block_info_modal").modal("show");

			NRS.fetchingModalData = false;
		} else {
			$("#block_info_transactions_none").show();
			$("#block_info_transactions_table").hide();
			$("#block_info_modal").modal("show");

			NRS.fetchingModalData = false;
		}
	}

	return NRS;
}(NRS || {}, jQuery));