var NRS = (function(NRS, $, undefined) {
	NRS.multiQueue = null;

	NRS.sendOutsideRequest = function(url, data, callback, async) {
		if ($.isFunction(data)) {
			async = callback;
			callback = data;
			data = {};
		} else {
			data = data || {};
		}

		$.support.cors = true;

		$.ajax({
			url: url,
			crossDomain: true,
			dataType: "json",
			type: "GET",
			timeout: 30000,
			async: (async === undefined ? true : async),
			data: data
		}).done(function(json) {
			if (json.errorCode && !json.errorDescription) {
				json.errorDescription = (json.errorMessage ? json.errorMessage : "Unknown error occured.");
			}
			if (callback) {
				callback(json, data);
			}
		}).fail(function(xhr, textStatus, error) {
			if (callback) {
				callback({
					"errorCode": -1,
					"errorDescription": error
				}, {});
			}
		});
	}

	NRS.sendRequest = function(requestType, data, callback, async) {
		if (requestType == undefined) {
			return;
		}

		if ($.isFunction(data)) {
			async = callback;
			callback = data;
			data = {};
		} else {
			data = data || {};
		}

		$.each(data, function(key, val) {
			if (key != "secretPhrase") {
				if (typeof val == "string") {
					data[key] = $.trim(val);
				}
			}
		});

		//convert NXT to NQT...
		try {
			var nxtFields = ["feeNXT", "amountNXT"];

			for (var i = 0; i < nxtFields.length; i++) {
				var nxtField = nxtFields[i];
				var field = nxtField.replace("NXT", "");

				if (nxtField in data) {
					data[field + "NQT"] = NRS.convertToNQT(data[nxtField]);
					delete data[nxtField];
				}
			}
		} catch (err) {
			if (callback) {
				callback({
					"errorCode": 1,
					"errorDescription": err + " (Field: " + field + ")"
				});
			}

			return;
		}

		//gets account id from secret phrase client side, used only for login.
		if (requestType == "getAccountId") {
			var accountId = NRS.generateAccountId(data.secretPhrase, true);

			if (callback) {
				callback({
					"accountId": accountId
				});
			}
			return;
		}

		//check to see if secretPhrase supplied matches logged in account, if not - show error.
		if ("secretPhrase" in data) {
			var accountId = NRS.generateAccountId(NRS.rememberPassword ? sessionStorage.getItem("secret") : data.secretPhrase);
			if (accountId != NRS.account) {
				if (callback) {
					callback({
						"errorCode": 1,
						"errorDescription": "Incorrect secret phrase."
					});
				}
				return;
			} else {
				//ok, accountId matches..continue with the real request.
				NRS.processAjaxRequest(requestType, data, callback, async);
			}
		} else {
			NRS.processAjaxRequest(requestType, data, callback, async);
		}
	}

	NRS.processAjaxRequest = function(requestType, data, callback, async) {
		if (!NRS.multiQueue) {
			NRS.multiQueue = $.ajaxMultiQueue(8);
		}

		if (data["_extra"]) {
			var extra = data["_extra"];
			delete data["_extra"];
		} else {
			var extra = null;
		}

		var currentPage = currentSubPage = null;

		//means it is a page request, not a global request.. Page requests can be aborted.
		if (requestType.slice(-1) == "+") {
			requestType = requestType.slice(0, -1);
			currentPage = NRS.currentPage;
		} else {
			//not really necessary... we can just use the above code..
			var plusCharacter = requestType.indexOf("+");

			if (plusCharacter > 0) {
				var subType = requestType.substr(plusCharacter);
				requestType = requestType.substr(0, plusCharacter);
				currentPage = NRS.currentPage;
			}
		}

		if (currentPage && NRS.currentSubPage) {
			currentSubPage = NRS.currentSubPage;
		}

		var type = ("secretPhrase" in data ? "POST" : "GET");
		var url = NRS.server + "/burst?requestType=" + requestType;

		if (type == "GET") {
			if (typeof data == "string") {
				data += "&random=" + Math.random();
			} else {
				data.random = Math.random();
			}
		}

		var secretPhrase = "";

		if (!NRS.isLocalHost && type == "POST" && requestType != "startForging" && requestType != "stopForging") {
			if (NRS.rememberPassword) {
				secretPhrase = sessionStorage.getItem("secret");
			} else {
				secretPhrase = data.secretPhrase;
			}

			delete data.secretPhrase;

			if (NRS.accountInfo && NRS.accountInfo.publicKey) {
				data.publicKey = NRS.accountInfo.publicKey;
			} else {
				data.publicKey = NRS.generatePublicKey(secretPhrase);
				NRS.accountInfo.publicKey = data.publicKey;
			}
		} else if (type == "POST" && NRS.rememberPassword) {
			data.secretPhrase = sessionStorage.getItem("secret");
		}

		$.support.cors = true;

		if (type == "GET") {
			var ajaxCall = NRS.multiQueue.queue;
		} else {
			var ajaxCall = $.ajax;
		}

		//workaround for 1 specific case.. ugly
		if (data.querystring) {
			data = data.querystring;
			type = "POST";
		}

		ajaxCall({
			url: url,
			crossDomain: true,
			dataType: "json",
			type: type,
			timeout: 30000,
			async: (async === undefined ? true : async),
			currentPage: currentPage,
			currentSubPage: currentSubPage,
			shouldRetry: (type == "GET" ? 2 : undefined),
			data: data
		}).done(function(response, status, xhr) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, response);
			}

			if (typeof data == "object" && "recipient" in data) {
				if (/^BURST\-/i.test(data.recipient)) {
					data.recipientRS = data.recipient;

					var address = new NxtAddress();

					if (address.set(data.recipient)) {
						data.recipient = address.account_id();
					}
				} else {
					var address = new NxtAddress();

					if (address.set(data.recipient)) {
						data.recipientRS = address.toString();
					}
				}
			}

			if (secretPhrase && response.unsignedTransactionBytes && !response.errorCode) {
				var publicKey = NRS.generatePublicKey(secretPhrase);
				var signature = nxtCrypto.sign(response.unsignedTransactionBytes, converters.stringToHexString(secretPhrase));

				if (!nxtCrypto.verify(signature, response.unsignedTransactionBytes, publicKey)) {
					if (callback) {
						callback({
							"errorCode": 1,
							"errorDescription": "Could not verify signature (client side)."
						}, data);
					} else {
						$.growl("Could not verify signature.", {
							"type": "danger"
						});
					}
					return;
				} else {
					var payload = response.unsignedTransactionBytes.substr(0, 192) + signature + response.unsignedTransactionBytes.substr(320);

					if (!NRS.verifyTransactionBytes(payload, requestType, data)) {
						if (callback) {
							callback({
								"errorCode": 1,
								"errorDescription": "Could not verify transaction bytes (server side)."
							}, data);
						} else {
							$.growl("Could not verify transaction bytes.", {
								"type": "danger"
							});
						}
						return;
					} else {
						if (callback) {
							if (extra) {
								data["_extra"] = extra;
							}

							NRS.broadcastTransactionBytes(payload, callback, response, data);
						} else {
							NRS.broadcastTransactionBytes(payload);
						}
					}
				}
			} else {
				if (response.errorCode && !response.errorDescription) {
					response.errorDescription = (response.errorMessage ? response.errorMessage : "Unknown error occured.");
				}

				if (callback) {
					if (extra) {
						data["_extra"] = extra;
					}
					callback(response, data);
				}
			}
		}).fail(function(xhr, textStatus, error) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, error, true);
			}

			if ((error == "error" || textStatus == "error") && (xhr.status == 404 || xhr.status == 0)) {
				if (type == "POST") {
					$.growl("Could not connect.", {
						"type": "danger",
						"offset": 10
					});
				}
			}

			if (error == "abort") {
				return;
			} else if (callback) {
				if (error == "timeout") {
					error = "The request timed out. Warning: This does not mean the request did not go through. You should wait a couple of blocks and see if your request has been processed.";
				}
				callback({
					"errorCode": -1,
					"errorDescription": error
				}, {});
			}
		});
	}

	NRS.verifyTransactionBytes = function(transactionBytes, requestType, data) {
		var transaction = {};

		var byteArray = converters.hexStringToByteArray(transactionBytes);

		transaction.type = byteArray[0];
		transaction.subtype = byteArray[1];
		transaction.timestamp = String(converters.byteArrayToSignedInt32(byteArray, 2));
		transaction.deadline = String(converters.byteArrayToSignedShort(byteArray, 6));
		transaction.senderPublicKey = converters.byteArrayToHexString(byteArray.slice(8, 40));
		transaction.recipient = String(converters.byteArrayToBigInteger(byteArray, 40));
		transaction.amountNQT = String(converters.byteArrayToBigInteger(byteArray, 48));
		transaction.feeNQT = String(converters.byteArrayToBigInteger(byteArray, 56));

		var refHash = byteArray.slice(64, 96);
		transaction.referencedTransactionFullHash = converters.byteArrayToHexString(refHash);
		if (transaction.referencedTransactionFullHash == "0") {
			transaction.referencedTransactionFullHash = null;
		} else {
			transaction.referencedTransactionId = converters.byteArrayToBigInteger([refHash[7], refHash[6], refHash[5], refHash[4], refHash[3], refHash[2], refHash[1], refHash[0]], 0);
		}

		if (!("amountNQT" in data)) {
			data.amountNQT = "0";
		}

		if (!("recipient" in data)) {
			//recipient == genesis
			data.recipient = "1739068987193023818";
			data.recipientRS = "NXT-MRCC-2YLS-8M54-3CMAJ";
		}

		if (transaction.senderPublicKey != NRS.accountInfo.publicKey) {
			return false;
		}

		if (transaction.deadline !== data.deadline || transaction.recipient !== data.recipient) {
			return false;
		}

		if (transaction.amountNQT !== data.amountNQT || transaction.feeNQT !== data.feeNQT) {
			return false;
		}

		if ("referencedTransactionFullHash" in data && transaction.referencedTransactionFullHash !== data.referencedTransactionFullHash) {
			return false;
		}
		if ("referencedTransactionId" in data && transaction.referencedTransactionId !== data.referencedTransactionId) {
			return false;
		}

		var pos = 160;

		switch (requestType) {
			case "sendMoney":
				if (transaction.type !== 0 || transaction.subtype !== 0) {
					return false;
				}
				break;
			case "sendMessage":
				if (transaction.type !== 1 || transaction.subtype !== 0) {
					return false;
				}

				var messageLength = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				var slice = byteArray.slice(pos, pos + messageLength);

				transaction.message = converters.byteArrayToHexString(slice);

				if (transaction.message !== data.message) {
					return false;
				}
				break;
			case "setAlias":
				if (transaction.type !== 1 || transaction.subtype !== 1) {
					return false;
				}

				var aliasLength = parseInt(byteArray[pos], 10);

				pos++;

				transaction.aliasName = converters.byteArrayToString(byteArray, pos, aliasLength);

				pos += aliasLength;

				var uriLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.aliasURI = converters.byteArrayToString(byteArray, pos, uriLength);

				if (transaction.aliasName !== data.aliasName || transaction.aliasURI !== data.aliasURI) {
					return false;
				}
				break;
			case "createPoll":
				if (transaction.type !== 1 || transaction.subtype !== 2) {
					return false;
				}

				var nameLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);

				pos += nameLength;

				var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);

				pos += descriptionLength;

				var nr_options = byteArray[pos];

				pos++;

				for (var i = 0; i < nr_options; i++) {
					var optionLength = converters.byteArrayToSignedShort(byteArray, pos);

					pos += 2;

					transaction["option" + i] = converters.byteArrayToString(byteArray, pos, optionLength);

					pos += optionLength;
				}

				transaction.minNumberOfOptions = String(byteArray[pos]);

				pos++;

				transaction.maxNumberOfOptions = String(byteArray[pos]);

				pos++;

				transaction.optionsAreBinary = String(byteArray[pos]);

				if (transaction.name !== data.name || transaction.description !== data.description || transaction.minNumberOfOptions !== data.minNumberOfOptions || transaction.maxNumberOfOptions !== data.maxNumberOfOptions || transaction.optionsAreBinary !== data.optionsAreBinary) {
					return false;
				}

				for (var i = 0; i < nr_options; i++) {
					if (transaction["option" + i] !== data["option" + i]) {
						return false;
					}
				}

				if (("option" + i) in data) {
					return false;
				}

				break;
			case "castVote":
				if (transaction.type !== 1 || transaction.subtype !== 3) {
					return false;
				}

				transaction.poll = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var voteLength = byteArray[pos];

				pos++;

				transaction.votes = [];

				for (var i = 0; i < voteLength; i++) {
					transaction.votes.push(bytesArray[pos]);

					pos++;
				}

				return false;

				break;
			case "hubAnnouncement":
				if (transaction.type !== 1 || transaction.subtype != 4) {
					return false;
				}

				var minFeePerByte = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var numberOfUris = parseInt(byteArray[pos], 10);

				pos++;

				var uris = [];

				for (var i = 0; i < numberOfUris; i++) {
					var uriLength = parseInt(byteArray[pos], 10);

					pos++;

					uris[i] = converters.byteArrayToString(byteArray, pos, uriLength);

					pos += uriLength;
				}

				//do validation

				return false;

				break;
			case "setAccountInfo":
				if (transaction.type !== 1 || transaction.subtype != 5) {
					return false;
				}

				var nameLength = parseInt(byteArray[pos], 10);

				pos++;

				transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);

				pos += nameLength;

				var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);

				pos += descriptionLength;

				if (transaction.name !== data.name || transaction.description !== data.description) {
					return false;
				}

				break;
			case "issueAsset":
				if (transaction.type !== 2 || transaction.subtype !== 0) {
					return false;
				}

				var nameLength = byteArray[pos];

				pos++;

				transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);

				pos += nameLength;

				var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);

				pos += descriptionLength;

				transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.name !== data.name || transaction.description !== data.description || transaction.quantityQNT !== data.quantityQNT) {
					return false;
				}
				break;
			case "transferAsset":
				if (transaction.type !== 2 || transaction.subtype !== 1) {
					return false;
				}

				transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var commentLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.comment = converters.byteArrayToString(byteArray, pos, commentLength);

				if (transaction.asset !== data.asset || transaction.quantityQNT !== data.quantityQNT || transaction.comment !== data.comment) {
					return false;
				}
				break;
			case "placeAskOrder":
			case "placeBidOrder":
				if (transaction.type !== 2) {
					return false;
				} else if (requestType == "placeAskOrder" && transaction.subtype !== 2) {
					return false;
				} else if (requestType == "placeBidOrder" && transaction.subtype !== 3) {
					return false;
				}

				transaction.asset = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.quantityQNT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.asset !== data.asset || transaction.quantityQNT !== data.quantityQNT || transaction.priceNQT !== data.priceNQT) {
					return false;
				}
				break;
			case "cancelAskOrder":
			case "cancelBidOrder":
				if (transaction.type !== 2) {
					return false;
				} else if (requestType == "cancelAskOrder" && transaction.subtype !== 4) {
					return false;
				} else if (requestType == "cancelBidOrder" && transaction.subtype !== 5) {
					return false;
				}

				transaction.order = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.order !== data.order) {
					return false;
				}

				break;
			case "digitalGoodsListing":
				if (transaction.type !== 3 && transaction.subtype != 0) {
					return false;
				}

				var nameLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.name = converters.byteArrayToString(byteArray, pos, nameLength);

				pos += nameLength;

				var descriptionLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.description = converters.byteArrayToString(byteArray, pos, descriptionLength);

				pos += descriptionLength;

				var tagsLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.tags = converters.byteArrayToString(byteArray, pos, tagsLength);

				pos += tagsLength;

				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.name !== data.name || transaction.description !== data.description || transaction.tags !== data.tags || transaction.quantity !== data.quantity || transaction.priceNQT !== data.priceNQT) {
					return false;
				}

				break;
			case "digitalGoodsDelisting":
				if (transaction.type !== 3 && transaction.subtype !== 1) {
					return false;
				}

				transaction.goodsId = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.goodsId !== data.goodsId) {
					return false;
				}

				break;
			case "digitalGoodsPriceChange":
				if (transaction.type !== 3 && transaction.subtype !== 2) {
					return false;
				}

				transaction.goodsId = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.goodsId !== data.goodsId || transaction.priceNQT !== data.priceNQT) {
					return false;
				}

				break;
			case "digitalGoodsQuantityChange":
				if (transaction.type !== 3 && transaction.subtype !== 3) {
					return false;
				}

				transaction.goodsId = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.deltaQuantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				if (transaction.goodsId !== data.goodsId || transaction.deltaQuantity !== data.deltaQuantity) {
					return false;
				}

				break;
			case "digitalGoodsPurchase":
				if (transaction.type !== 3 && transaction.subtype !== 4) {
					return false;
				}

				transaction.goodsId = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.quantity = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				transaction.priceNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.deliveryDeadline = String(converters.byteArrayToSignedInt32(byteArray, pos));

				pos += 4;

				var noteLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.note = converters.byteArrayToString(byteArray, pos, noteLength);

				pos += noteLength;

				transaction.noteNonce = converters.byteArrayToString(byteArray, pos, 32);
				//XoredData note = new XoredData(noteBytes, noteNonceBytes);

				if (transaction.goodsId !== data.goodsId || transaction.quantity !== data.quantity || transaction.priceNQT !== data.priceNQT || transaction.deliveryDeadline !== data.deliveryDeadline || transaction.note !== data.note || transaction.noteNonce !== data.noteNonce) {
					return false;
				}

				break;
			case "digitalGoodsDelivery":
				if (transaction.type !== 3 && transaction.subtype !== 5) {
					return false;
				}

				transaction.goodsId = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var goodsLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.goods = converters.byteArrayToString(byteArray, pos, goodsLength);

				pos += goodsLength;

				transaction.goodsNonce = converters.byteArrayToString(byteArray, pos, 32);
				//XoredData goods = new XoredData(goodsBytes, goodsNonceBytes);

				pos += 32;

				transaction.discountNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				if (transaction.goodsId !== data.goodsId || transaction.goods !== data.goods || transaction.goodsNonce !== data.goodsNonce || transaction.discountNQT !== data.discountNQT) {
					return false;
				}

				break;
			case "digitalGoodsFeedback":
				if (transaction.type !== 3 && transaction.subtype !== 6) {
					return false;
				}

				transaction.purchaseId = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var noteLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.note = converters.byteArrayToString(byteArray, pos, noteLength);

				pos += noteLength;

				transaction.noteNonce = converters.byteArrayToString(byteArray, pos, 32);
				//XoredData note = new XoredData(noteBytes, noteNonceBytes);

				if (transaction.purchaseId !== data.purchaseId || transaction.note !== data.note || transaction.noteNonce !== data.noteNonce) {
					return false;
				}

				break;
			case "digitalGoodsRefund":
				if (transaction.type !== 3 && transaction.subtype !== 7) {
					return false;
				}

				transaction.purchaseId = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				transaction.refundNQT = String(converters.byteArrayToBigInteger(byteArray, pos));

				pos += 8;

				var noteLength = converters.byteArrayToSignedShort(byteArray, pos);

				pos += 2;

				transaction.note = converters.byteArrayToString(byteArray, pos, noteLength);

				pos += noteLength;

				transaction.noteNonce = converters.byteArrayToString(byteArray, pos, 32);
				//XoredData note = new XoredData(noteBytes, noteNonceBytes);

				if (transaction.purchaseId !== data.purchaseId || transaction.refundNQT !== data.refundNQT || transaction.note !== data.note || transaction.noteNonce !== data.noteNonce) {
					return false;
				}

				break;
			case "leaseBalance":
				if (transaction.type !== 4 && transaction.subtype !== 0) {
					return false;
				}

				transaction.period = String(converters.byteArrayToSignedShort(byteArray, pos));

				if (transaction.period !== data.period) {
					return false;
				}

				break;
			default:
				//invalid requestType..
				return false;
		}
		return true;
	}

	NRS.broadcastTransactionBytes = function(transactionData, callback, original_response, original_data) {
		$.ajax({
			url: NRS.server + "/burst?requestType=broadcastTransaction",
			crossDomain: true,
			dataType: "json",
			type: "POST",
			timeout: 30000,
			async: true,
			data: {
				"transactionBytes": transactionData
			}
		}).done(function(response, status, xhr) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, response);
			}

			if (callback) {
				if (response.errorCode && !response.errorDescription) {
					response.errorDescription = (response.errorMessage ? response.errorMessage : "Unknown error occured.");
					callback(response, original_data);
				} else if (response.error) {
					response.errorCode = 1;
					response.errorDescription = response.error;
					callback(response, original_data);
				} else {
					if ("transactionBytes" in original_response) {
						delete original_response.transactionBytes;
					}
					original_response.broadcasted = true;
					original_response.transaction = response.transaction;
					original_response.fullHash = response.fullHash;
					callback(original_response, original_data);
				}
			}
		}).fail(function(xhr, textStatus, error) {
			if (NRS.console) {
				NRS.addToConsole(this.url, this.type, this.data, error, true);
			}

			if (callback) {
				if (error == "timeout") {
					error = "The request timed out. Warning: This does not mean the request did not go through. You should a few blocks and see if your request has been processed before trying to submit it again.";
				}
				callback({
					"errorCode": -1,
					"errorDescription": error
				}, {});
			}
		});
	}

	return NRS;
}(NRS || {}, jQuery));
