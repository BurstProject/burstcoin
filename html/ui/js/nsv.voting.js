var NSV = (function(NSV, $, undefined) {	
	
	var NSV_vote_create_poll_arr = [];
	var NSV_votes_arr = [];
	var NSV_vote_poll_account = "";

	
	NSV.vote_create_init = function () {
		$('#nsv_vote_create_error_message').hide();
		$('#nsv_vote_create_succ_message').hide();
		document.getElementById("nsv_vote_create_asset").value = "";
	};
	
	NSV.vote_create_activate = function () {
		
		if (NRS.downloadingBlockchain) {
			$("#nsv_vote_create_error_message").html("Please wait until the blockchain has finished downloading.");
			$("#nsv_vote_create_error_message").show();
			return;
		} else if (NRS.state.isScanning) {
			$("#nsv_vote_create_error_message").html("The blockchain is currently being rescanned. Please wait a minute and then try submitting again.");
			$("#nsv_vote_create_error_message").show();
			return;
		}	
		
		$('#nsv_vote_create_error_message').hide();
		$('#nsv_vote_create_succ_message').hide();
		//var recipient_secret = "shf;we943jr2432k";
		var recipient_secret = NSV.random_secret_simple();

		var err_message = "";
		
		var poll_message_json = {};
		
		poll_message_json.Q = document.getElementById("nsv_vote_create_question").value;
		poll_message_json.O1 = document.getElementById("nsv_vote_create_option1").value;
		poll_message_json.O2 = document.getElementById("nsv_vote_create_option2").value;
		poll_message_json.O3 = document.getElementById("nsv_vote_create_option3").value;
		poll_message_json.O4 = document.getElementById("nsv_vote_create_option4").value;
		poll_message_json.O5 = document.getElementById("nsv_vote_create_option5").value;
		poll_message_json.O6 = document.getElementById("nsv_vote_create_option6").value;
		poll_message_json.O7 = document.getElementById("nsv_vote_create_option7").value;
		poll_message_json.O8 = document.getElementById("nsv_vote_create_option8").value;

		if (poll_message_json.O1 === "") {
			err_message = "Option 1 must have a choice";
		}
		
		if (poll_message_json.Q === "") {
			err_message = "Poll question not set";
		}
		
		if (err_message !== "") {
			$('#nsv_vote_create_error_message').html(err_message);
			$('#nsv_vote_create_error_message').show();
			return;
		}
		
		var secret; 

		var poll_acc, poll_pk;

		//poll_pk = "8a951c444e7a1b77da0802ea7e99c5aee2ffdcb6e61dd8ee04756f17c496ec36";
		
		if (!NRS.rememberPassword) {
			secret = document.getElementById("nsv_vote_create_password").value;
			secret = $.trim(secret);
			if (secret === "") {
				err_message = "Password not specified";
			} else {						
				var accountId = NRS.getAccountId(secret);
				if (accountId != NRS.account) {
					err_message = "Password doesn't match";
				}
			}
		} else {
			secret = NRS._password;
		}
		
		var recip_accountId = NRS.getAccountId(recipient_secret);
		var nxtAddress = new NxtAddress();
		if (nxtAddress.set(recip_accountId)) {
			var poll_RS = nxtAddress.toString();
		} else {
			err_message = "Unable to get RS account number";
		}
		poll_pk = NRS.generatePublicKey(recipient_secret);

		
		var dP_asset = document.getElementById("nsv_vote_create_asset").value;
		if (dP_asset != "1" && dP_asset != "0") {
			if (dP_asset === "") {
				err_message = "Weighting not set";
			} else if (!(/^\d+$/.test(dP_asset))) {
				err_message = "Weighting Asset ID is Invalid";
			}
			else {
				NRS.sendRequest("getAsset", {
					"asset": dP_asset
				}, function(response) {
					if (response.errorCode) {
						err_message = "Incorrect Weighting Asset ID";
					}
				},false);	
			
			}
		}
		
		if (err_message !== "") {
			$('#nsv_vote_create_error_message').html(err_message);
			$('#nsv_vote_create_error_message').show();
			return;
		}

		poll_message_json.divPlus_asset = dP_asset;

		poll_message = JSON.stringify(poll_message_json);
						
		NRS.sendRequest("sendMessage", {"secretPhrase":secret,feeNQT:"100000000",deadline:"1440",recipient:poll_RS,recipientPublicKey:poll_pk,
		  	message:poll_message
		}, function(response, input) {
			if (response.errorCode) {
				err_message = "Problem setting up poll, Reason: " + response.errorDescription;
			} else {
				$('#nsv_vote_create_succ_message').html("Poll Created, Nxt ID: " + poll_RS);
				$('#nsv_vote_create_succ_message').show();
			}											
		},false);		
		
		if (err_message !== "") {
			$('#nsv_vote_create_error_message').html(err_message);
			$('#nsv_vote_create_error_message').show();
			return;
		}		

		$('#nsv_vote_create_succ_message').html("Poll Created, Nxt ID: " + poll_RS);
		$('#nsv_vote_create_succ_message').show();		
		document.getElementById("nsv_vote_create_account").value = poll_RS;
	};

	NSV.vote_cast_init = function () {
		$('#nsv_vote_cast_error_message').hide();
		$('#nsv_vote_cast_succ_message').hide();
		document.getElementById("nsv_vote_cast_but").disabled=true;
		document.getElementById("nsv_vote_cast_option1_lb").innerHTML = "Option 1";
		document.getElementById("nsv_vote_cast_option1").disabled=true;
		document.getElementById("nsv_vote_cast_option1").checked=false;		
		document.getElementById("nsv_vote_cast_option1").style.display = 'inline';
		document.getElementById("nsv_vote_cast_option1_lb").style.display = 'inline';		
		document.getElementById("nsv_vote_cast_option2_lb").innerHTML = "Option 2";
		document.getElementById("nsv_vote_cast_option2").disabled=true;	
		document.getElementById("nsv_vote_cast_option2").checked=false;	
		document.getElementById("nsv_vote_cast_option2").style.display = 'inline';
		document.getElementById("nsv_vote_cast_option2_lb").style.display = 'inline';		
		document.getElementById("nsv_vote_cast_option3_lb").innerHTML = "Option 3";
		document.getElementById("nsv_vote_cast_option3").disabled=true;
		document.getElementById("nsv_vote_cast_option3").checked=false;		
		document.getElementById("nsv_vote_cast_option3").style.display = 'inline';
		document.getElementById("nsv_vote_cast_option3_lb").style.display = 'inline';		
		document.getElementById("nsv_vote_cast_option4_lb").innerHTML = "Option 4";
		document.getElementById("nsv_vote_cast_option4").disabled=true;
		document.getElementById("nsv_vote_cast_option4").checked=false;		
		document.getElementById("nsv_vote_cast_option4").style.display = 'inline';
		document.getElementById("nsv_vote_cast_option4_lb").style.display = 'inline';		
		document.getElementById("nsv_vote_cast_option5_lb").innerHTML = "Option 5";
		document.getElementById("nsv_vote_cast_option5").disabled=true;
		document.getElementById("nsv_vote_cast_option5").checked=false;		
		document.getElementById("nsv_vote_cast_option5").style.display = 'inline';
		document.getElementById("nsv_vote_cast_option5_lb").style.display = 'inline';		
		document.getElementById("nsv_vote_cast_option6_lb").innerHTML = "Option 6";
		document.getElementById("nsv_vote_cast_option6").disabled=true;
		document.getElementById("nsv_vote_cast_option6").checked=false;		
		document.getElementById("nsv_vote_cast_option6").style.display = 'inline';
		document.getElementById("nsv_vote_cast_option6_lb").style.display = 'inline';		
		document.getElementById("nsv_vote_cast_option7_lb").innerHTML = "Option 7";
		document.getElementById("nsv_vote_cast_option7").disabled=true;
		document.getElementById("nsv_vote_cast_option7").checked=false;		
		document.getElementById("nsv_vote_cast_option7").style.display = 'inline';
		document.getElementById("nsv_vote_cast_option7_lb").style.display = 'inline';		
		document.getElementById("nsv_vote_cast_option8_lb").innerHTML = "Option 8";
		document.getElementById("nsv_vote_cast_option8").disabled=true;
		document.getElementById("nsv_vote_cast_option8").checked=false;		
		document.getElementById("nsv_vote_cast_option8").style.display = 'inline';
		document.getElementById("nsv_vote_cast_option8_lb").style.display = 'inline';		
		
	};
	
	NSV.vote_check_activate = function () {

		if (NRS.downloadingBlockchain) {
			$("#nsv_vote_check_error_message").html("Please wait until the blockchain has finished downloading.");
			$("#nsv_vote_check_error_message").show();
			return;
		} else if (NRS.state.isScanning) {
			$("#nsv_vote_check_error_message").html("The blockchain is currently being rescanned. Please wait a minute and then try submitting again.");
			$("#nsv_vote_check_error_message").show();
			return;
		}	
		$('#nsv_vote_check_error_message').hide();
		$('#nsv_vote_check_succ_message').hide();
		
		NSV_vote_create_poll_arr = [];
		NSV_votes_arr = [];
		var out_message = "";
		var poll_account = $.trim(document.getElementById("nsv_vote_check_account").value);
		//var poll_account = "NXT-22ET-QA68-G4PU-2XK8G";
		var err_message = "";
		var present_time = "";

		NRS.sendOutsideRequest("/nxt?requestType=" + "getAccountTransactionIds", {
			"account":poll_account , "type":"1", "subtype":"0"
		}, function(response,input) {
			if (response.errorCode) {
				err_message = "Poll address doesn't seem valid.";
				return;					
			} else {
				tran_arr = response.transactionIds;
				tran_len = tran_arr.length;
				for (var j=0; j<tran_len; j++) {
					var tmp_tran = tran_arr[j];
					NSV.sendOutsideRequest("/nxt?requestType=" + "getTransaction", {
						"transaction": tmp_tran
					}, function(response, input) {
						if (response.errorCode) {
							err_message = "Unknown bug. Problem accessing getTransaction.";
							return;
						} else {							
							if (response.attachment.messageIsText === true) {
								NSV.parse_poll_message(response.attachment.message,	response.senderRS,response.timestamp);
							}
						}	
						
					},false);						 										
			
				}
				
			}						
		},false);	

		NRS.sendOutsideRequest("/nxt?requestType=" + "getTime", {}, function(response) {
			if (response.errorCode) {
				err_message = "Unknown error, couldn't getTime.";
			} else {
				present_time = response.time;
			}
		},false);		
		
		if (NSV_vote_create_poll_arr.length === 0) {
			err_message = "Couldn't find a poll at that account, recheck account number.";
		} else if (NSV_vote_create_poll_arr.length > 1) {
			NSV_vote_create_poll_arr.sort(function(a,b) {return b.time < a.time; });
		}
		
		
		if (err_message !== "") {
			$('#nsv_vote_check_error_message').html(err_message);
			$('#nsv_vote_check_error_message').show();
			return;
		}

		out_message = "Poll_account: " + poll_account + "\n";
		out_message = out_message + "Question: " + NSV_vote_create_poll_arr[0].Q + "\n";
		out_message = out_message + "Timestamp: " + present_time + "\n";
		if (NSV_vote_create_poll_arr[0].divPlus_asset == "0") {
			var asset_message = "Weighting via NXT\n"; 
		} else if (NSV_vote_create_poll_arr[0].divPlus_asset == "1") {
			asset_message = "Weighting via number of votes, one vote per account\n";
		} else {
			asset_message = "Weighting via amount of asset in account, assetID=" + NSV_vote_create_poll_arr[0].divPlus_asset + "\n";
		}
		out_message = out_message + asset_message;
		out_message = out_message + "--------VOTES----------\n";
		
		var total_weight = 0;
		var vote_results_array = [0,0,0,0,0,0,0,0];
		if (NSV_votes_arr.length === 0) {
			out_message = "No Votes Found.\n";
		} else {
			NSV_votes_arr.sort(function(a,b) {return b.sender < a.sender; });
			for (var k=0; k<NSV_votes_arr.length-1; k++) {
				if (NSV_votes_arr[k].sender == NSV_votes_arr[k+1].sender) {
					if (NSV_votes_arr[k+1].time > NSV_votes_arr[k].time) {
						NSV_votes_arr.splice(k,1);
					} else {
						NSV_votes_arr.splice(k+1,1);
					}
					k--;
				}
			}
			for (k=0; k<NSV_votes_arr.length; k++) {
				if (NSV_vote_create_poll_arr[0].divPlus_asset == "1") {
					NSV_votes_arr[k].weight = 1;
				} else {
					NRS.sendOutsideRequest("/nxt?requestType=" + "getAccount", {
						"account":NSV_votes_arr[k].sender
					}, function(response,input) {
						if (response.errorCode) {
							err_message = "Unknown bug. Problem accessing getAccount.";
							return;
						} else {							
							if (NSV_vote_create_poll_arr[0].divPlus_asset == "0") {
								NSV_votes_arr[k].weight = parseInt(response.unconfirmedBalanceNQT, 10);
							} else {
								NSV_votes_arr[k].weight = 0;
								if (response.unconfirmedAssetBalances) {
									var asset_arr = response.unconfirmedAssetBalances;
									for (var j=0; j<asset_arr.length; j++) {
										if (asset_arr[j].asset == NSV_vote_create_poll_arr[0].divPlus_asset) {
											NSV_votes_arr[k].weight = parseInt(asset_arr[j].unconfirmedBalanceQNT, 10 );
										}
									}																
								}
							}
						}							
					},false);
				}
			}

			
			for (k=0; k<NSV_votes_arr.length; k++) {
				if (NSV_votes_arr[k].divPlus_vote ==  NSV_vote_create_poll_arr[0].O1) {
					vote_results_array[0] = vote_results_array[0] + NSV_votes_arr[k].weight;
					total_weight = total_weight + NSV_votes_arr[k].weight;
				} else if (NSV_votes_arr[k].divPlus_vote ==  NSV_vote_create_poll_arr[0].O2) {
					vote_results_array[1] = vote_results_array[1] + NSV_votes_arr[k].weight;
					total_weight = total_weight + NSV_votes_arr[k].weight;
				} else if (NSV_votes_arr[k].divPlus_vote ==  NSV_vote_create_poll_arr[0].O3) {
					vote_results_array[2] = vote_results_array[2] + NSV_votes_arr[k].weight;
					total_weight = total_weight + NSV_votes_arr[k].weight;
				} else if (NSV_votes_arr[k].divPlus_vote ==  NSV_vote_create_poll_arr[0].O4) {
					vote_results_array[3] = vote_results_array[3] + NSV_votes_arr[k].weight;
					total_weight = total_weight + NSV_votes_arr[k].weight;
				} else if (NSV_votes_arr[k].divPlus_vote ==  NSV_vote_create_poll_arr[0].O5) {
					vote_results_array[4] = vote_results_array[4] + NSV_votes_arr[k].weight;
					total_weight = total_weight + NSV_votes_arr[k].weight;
				} else if (NSV_votes_arr[k].divPlus_vote ==  NSV_vote_create_poll_arr[0].O6) {
					vote_results_array[5] = vote_results_array[5] + NSV_votes_arr[k].weight;
					total_weight = total_weight + NSV_votes_arr[k].weight;
				} else if (NSV_votes_arr[k].divPlus_vote ==  NSV_vote_create_poll_arr[0].O7) {
					vote_results_array[6] = vote_results_array[6] + NSV_votes_arr[k].weight;
					total_weight = total_weight + NSV_votes_arr[k].weight;
				} else if (NSV_votes_arr[k].divPlus_vote ==  NSV_vote_create_poll_arr[0].O8) {
					vote_results_array[7] = vote_results_array[7] + NSV_votes_arr[k].weight;
					total_weight = total_weight + NSV_votes_arr[k].weight;
				} else {
					NSV_votes_arr[k].divPlus_vote = "INVALID VOTE";
				}
				out_message = out_message + NSV_votes_arr[k].sender + ", vote=" + NSV_votes_arr[k].divPlus_vote + ", weight=" + NSV_votes_arr[k].weight + "\n";				
			}
			
		}
		var vote_percent_str = ["","","","","","","",""];
		var percent_vote;
		for (k=0; k<8; k++) {
			if (total_weight === 0) {
				percent_vote = "0";
			} else {
				percent_vote = 100*vote_results_array[k]/total_weight;
				percent_vote = percent_vote.toFixed(2);
			}
			vote_percent_str[k] = percent_vote.toString() + "%";									
		}
		
		out_message = out_message + "-----------------------\n";
		var sum_res_str = NSV.setup_check_page(vote_percent_str);
		out_message = out_message + sum_res_str;
		
		
		
		document.getElementById("nsv_vote_check_details").value = out_message;			
		
		if (err_message !== "") {
			$('#nsv_vote_check_error_message').html(err_message);
			$('#nsv_vote_check_error_message').show();
		}
		
	};
	
	
	NSV.vote_initialize = function () {

		if (NRS.downloadingBlockchain) {
			$("#nsv_vote_create_error_message").html("Please wait until the blockchain has finished downloading.");
			$("#nsv_vote_create_error_message").show();
			return;
		} else if (NRS.state.isScanning) {
			$("#nsv_vote_create_error_message").html("The blockchain is currently being rescanned. Please wait a minute and then try submitting again.");
			$("#nsv_vote_create_error_message").show();
			return;
		}	
		$('#nsv_vote_check_error_message').hide();
		$('#nsv_vote_check_succ_message').hide();
			
		NSV_vote_create_poll_arr = [];
		var poll_account = $.trim(document.getElementById("nsv_vote_cast_account").value);
		var err_message = "";
		NRS.sendOutsideRequest("/nxt?requestType=" + "getAccountTransactionIds", {
			"account":poll_account , "type":"1", "subtype":"0"
		}, function(response,input) {
			if (response.errorCode) {
				err_message = "Poll address doesn't seem valid.";
				return;					
			} else {
				tran_arr = response.transactionIds;
				tran_len = tran_arr.length;
				for (var j=0; j<tran_len; j++) {
					var tmp_tran = tran_arr[j];
					NSV.sendOutsideRequest("/nxt?requestType=" + "getTransaction", {
						"transaction": tmp_tran
					}, function(response, input) {
						if (response.errorCode) {
							err_message = "Unknown bug. Problem accessing getTransaction.";
						} else {							
							if (response.attachment.messageIsText === true) {
								NSV.parse_poll_message(response.attachment.message,	response.senderRS,response.timestamp);
							}
						}	
						
					},false);						 													
				}								
			}						
		},false);	
		
		
		
		if (NSV_vote_create_poll_arr.length > 0 ) {
			if (NSV_vote_create_poll_arr.length > 1) {
				NSV_vote_create_poll_arr.sort(function(a,b) {return b.time < a.time; });
			}
			NSV.setup_cast_page();
		} else {
			err_message = "Couldn't find a poll at that account, recheck account number.";
		}
		if (err_message !== "") {
			$('#nsv_vote_cast_error_message').html(err_message);
			$('#nsv_vote_cast_error_message').show();
			return;
		}		
	};

	
	NSV.vote_check_init = function () {
		$('#nsv_vote_check_error_message').hide();
		$('#nsv_vote_check_succ_message').hide();
		document.getElementById("nsv_vote_check_question").value = "";
		document.getElementById("nsv_vote_check_option1_lb").innerHTML = "Option 1";
		document.getElementById("nsv_vote_check_option1").value = "";
		document.getElementById("nsv_vote_check_option1_lb").style.display = 'block';
		document.getElementById("nsv_vote_check_option1").style.display = 'block';
		document.getElementById("nsv_vote_check_option2_lb").innerHTML = "Option 2";
		document.getElementById("nsv_vote_check_option2").value = "";
		document.getElementById("nsv_vote_check_option2_lb").style.display = 'block';
		document.getElementById("nsv_vote_check_option2").style.display = 'block';
		document.getElementById("nsv_vote_check_option3_lb").innerHTML = "Option 3";
		document.getElementById("nsv_vote_check_option3").value = "";
		document.getElementById("nsv_vote_check_option3_lb").style.display = 'block';
		document.getElementById("nsv_vote_check_option3").style.display = 'block';
		document.getElementById("nsv_vote_check_option4_lb").innerHTML = "Option 4";
		document.getElementById("nsv_vote_check_option4").value = "";
		document.getElementById("nsv_vote_check_option4_lb").style.display = 'block';
		document.getElementById("nsv_vote_check_option4").style.display = 'block';
		document.getElementById("nsv_vote_check_option5_lb").innerHTML = "Option 5";
		document.getElementById("nsv_vote_check_option5").value = "";
		document.getElementById("nsv_vote_check_option5_lb").style.display = 'block';
		document.getElementById("nsv_vote_check_option5").style.display = 'block';
		document.getElementById("nsv_vote_check_option6_lb").innerHTML = "Option 6";
		document.getElementById("nsv_vote_check_option6").value = "";
		document.getElementById("nsv_vote_check_option6_lb").style.display = 'block';
		document.getElementById("nsv_vote_check_option6").style.display = 'block';
		document.getElementById("nsv_vote_check_option7_lb").innerHTML = "Option 7";
		document.getElementById("nsv_vote_check_option7").value = "";
		document.getElementById("nsv_vote_check_option7_lb").style.display = 'block';
		document.getElementById("nsv_vote_check_option7").style.display = 'block';
		document.getElementById("nsv_vote_check_option8_lb").innerHTML = "Option 8";
		document.getElementById("nsv_vote_check_option8").value = "";
		document.getElementById("nsv_vote_check_option8_lb").style.display = 'block';
		document.getElementById("nsv_vote_check_option8").style.display = 'block';
	};

		
	NSV.vote_cast = function () {
	
		if (NRS.downloadingBlockchain) {
			$("#nsv_vote_cast_error_message").html("Please wait until the blockchain has finished downloading.");
			$("#nsv_vote_cast_error_message").show();
			return;
		} else if (NRS.state.isScanning) {
			$("#nsv_vote_cast_error_message").html("The blockchain is currently being rescanned. Please wait a minute and then try submitting again.");
			$("#nsv_vote_cast_error_message").show();
			return;
		}	
		$('#nsv_vote_cast_error_message').hide();
		$('#nsv_vote_cast_succ_message').hide();

	
		var secret;		
		var option_choosen = "";
		var err_message = "";
		
		if (!NRS.rememberPassword) {
			secret = document.getElementById("nsv_vote_cast_password").value;
			secret = $.trim(secret);
			if (secret === "") {
				err_message = "Password not specified";
			} else {						
				var accountId = NRS.getAccountId(secret);
				if (accountId != NRS.account) {
					err_message = "Password doesn't match";
				}
			}
		} else {
			secret = NRS._password;
		}
		poll_account = $.trim(document.getElementById("nsv_vote_cast_account").value);
		//var poll_account = "NXT-22ET-QA68-G4PU-2XK8G";		
		
		poll_details = NSV_vote_create_poll_arr[0];
		if (document.getElementById("nsv_vote_cast_option1").checked) {
			option_choosen = poll_details.O1;
		} else if (document.getElementById("nsv_vote_cast_option2").checked) {
			option_choosen = poll_details.O2;
		} else if (document.getElementById("nsv_vote_cast_option3").checked) {
			option_choosen = poll_details.O3;		
		} else if (document.getElementById("nsv_vote_cast_option4").checked) {
			option_choosen = poll_details.O4;
		} else if (document.getElementById("nsv_vote_cast_option5").checked) {
			option_choosen = poll_details.O5;
		} else if (document.getElementById("nsv_vote_cast_option6").checked) {
			option_choosen = poll_details.O6;			
		} else if (document.getElementById("nsv_vote_cast_option7").checked) {
			option_choosen = poll_details.O7;
		} else if (document.getElementById("nsv_vote_cast_option8").checked) {
			option_choosen = poll_details.O8;			
		}
		
		if (option_choosen === "") {
			err_message = "Poll option not choosen.";
		}
		if (err_message !== "") {
			$('#nsv_vote_cast_error_message').html(err_message);
			$('#nsv_vote_cast_error_message').show();
			return;
		}
		
		var vote_obj = {"divPlus_vote":option_choosen};
		var vote_message = JSON.stringify(vote_obj);

		NRS.sendRequest("sendMessage", {"secretPhrase":secret,feeNQT:"100000000",deadline:"1440",recipient:poll_account,message:vote_message
		}, function(response, input) {
			if (response.errorCode) {
				err_message = "Problem voting, Reason: " + response.errorDescription;
			} else {
				
				$('#nsv_vote_cast_succ_message').html("Vote Sent: " + option_choosen);
				$('#nsv_vote_cast_succ_message').show();
			}										
		},false);
		
		if (err_message !== "") {
			$('#nsv_vote_cast_error_message').html(err_message);
			$('#nsv_vote_cast_error_message').show();
		}		
	};	

	NSV.random_secret_simple = function () {

		var keylist="abcdefghijklmnopqrstuvwxyz123456789";
		var temp='';
		var plength = 25;

		for (var i=0;i<plength;i++) {
			temp+=keylist.charAt(Math.floor(Math.random()*keylist.length));
		}
		return temp;
	};	

	NSV.parse_poll_message = function (message,sender,time) {
		var message_json = JSON.parse(message);
		if (message_json !== null && typeof message_json === 'object') {
			if (message_json.divPlus_asset) {
				//It's the message that creates the poll
				message_json.time = time;
				NSV_vote_create_poll_arr.push(message_json);

			}
			if (message_json.divPlus_vote) {
				//this is a vote message
				message_json.time = time;
				message_json.sender = sender;				
				NSV_votes_arr.push(message_json);				
			}
		}
	
	};

	NSV.setup_cast_page = function () {
		document.getElementById("nsv_vote_cast_but").disabled=false;		
		message_json = NSV_vote_create_poll_arr[0];
		if (message_json.Q) { document.getElementById("nsv_vote_cast_question").value = message_json.Q; }
		if (message_json.O1) {
			document.getElementById("nsv_vote_cast_option1_lb").innerHTML = message_json.O1;
			document.getElementById("nsv_vote_cast_option1").disabled=false;		
		} else {
			document.getElementById("nsv_vote_cast_option1").style.display = 'none';
			document.getElementById("nsv_vote_cast_option1_lb").style.display = 'none';	
		}
		if (message_json.O2) {
			document.getElementById("nsv_vote_cast_option2_lb").innerHTML = message_json.O2;
			document.getElementById("nsv_vote_cast_option2").disabled=false;					
		} else {
			document.getElementById("nsv_vote_cast_option2").style.display = 'none';
			document.getElementById("nsv_vote_cast_option2_lb").style.display = 'none';
		}
		if (message_json.O3) {
			document.getElementById("nsv_vote_cast_option3_lb").innerHTML = message_json.O3;
			document.getElementById("nsv_vote_cast_option3").disabled=false;								
		} else {
			document.getElementById("nsv_vote_cast_option3").style.display = 'none';
			document.getElementById("nsv_vote_cast_option3_lb").style.display = 'none';
		}
		if (message_json.O4) {
			document.getElementById("nsv_vote_cast_option4_lb").innerHTML = message_json.O4;
			document.getElementById("nsv_vote_cast_option4").disabled=false;
		} else {
			document.getElementById("nsv_vote_cast_option4").style.display = 'none';
			document.getElementById("nsv_vote_cast_option4_lb").style.display = 'none';			
		}
		if (message_json.O5) {
			document.getElementById("nsv_vote_cast_option5_lb").innerHTML = message_json.O5;
			document.getElementById("nsv_vote_cast_option5").disabled=false;	
		} else {
			document.getElementById("nsv_vote_cast_option5").style.display = 'none';
			document.getElementById("nsv_vote_cast_option5_lb").style.display = 'none';
		}
		if (message_json.O6) {						
			document.getElementById("nsv_vote_cast_option6_lb").innerHTML = message_json.O6;
			document.getElementById("nsv_vote_cast_option6").disabled=false;	
		} else {
			document.getElementById("nsv_vote_cast_option6").style.display = 'none';
			document.getElementById("nsv_vote_cast_option6_lb").style.display = 'none';
		}
		if (message_json.O7) {
			document.getElementById("nsv_vote_cast_option7_lb").innerHTML = message_json.O7;
			document.getElementById("nsv_vote_cast_option7").disabled=false;	
		} else {
			document.getElementById("nsv_vote_cast_option7").style.display = 'none';
			document.getElementById("nsv_vote_cast_option7_lb").style.display = 'none';
		}
		if (message_json.O8) {
			document.getElementById("nsv_vote_cast_option8_lb").innerHTML = message_json.O8;
			document.getElementById("nsv_vote_cast_option8").disabled=false;
		} else {
			document.getElementById("nsv_vote_cast_option8").style.display = 'none';
			document.getElementById("nsv_vote_cast_option8_lb").style.display = 'none';
		}	
	};
	



	NSV.setup_check_page = function (vote_results_arr) {
		var summary_results = "";
		message_json = NSV_vote_create_poll_arr[0];
		if (message_json.Q) { document.getElementById("nsv_vote_check_question").value = message_json.Q; }
		if (message_json.O1) {
			document.getElementById("nsv_vote_check_option1_lb").innerHTML = message_json.O1;
			document.getElementById("nsv_vote_check_option1").value = vote_results_arr[0];
			summary_results = summary_results + message_json.O1 + ": " + vote_results_arr[0] + ", ";
		} else {
			document.getElementById("nsv_vote_check_option1_lb").style.display = 'none';
			document.getElementById("nsv_vote_check_option1").style.display = 'none';
		}
		if (message_json.O2) {
			document.getElementById("nsv_vote_check_option2_lb").innerHTML = message_json.O2;
			document.getElementById("nsv_vote_check_option2").value = vote_results_arr[1];
			summary_results = summary_results + message_json.O2 + ": " + vote_results_arr[1] + ", ";
		} else {
			document.getElementById("nsv_vote_check_option2_lb").style.display = 'none';
			document.getElementById("nsv_vote_check_option2").style.display = 'none';
		}
		if (message_json.O3) {
			document.getElementById("nsv_vote_check_option3_lb").innerHTML = message_json.O3;
			document.getElementById("nsv_vote_check_option3").value = vote_results_arr[2];
			summary_results = summary_results + message_json.O3 + ": " + vote_results_arr[2] + ", ";
		} else {
			document.getElementById("nsv_vote_check_option3_lb").style.display = 'none';
			document.getElementById("nsv_vote_check_option3").style.display = 'none';
		}
		if (message_json.O4) {
			document.getElementById("nsv_vote_check_option4_lb").innerHTML = message_json.O4;
			document.getElementById("nsv_vote_check_option4").value = vote_results_arr[3];
			summary_results = summary_results + message_json.O4 + ": " + vote_results_arr[3] + ", ";
		} else {
			document.getElementById("nsv_vote_check_option4_lb").style.display = 'none';
			document.getElementById("nsv_vote_check_option4").style.display = 'none';
		}
		if (message_json.O5) {
			document.getElementById("nsv_vote_check_option5_lb").innerHTML = message_json.O5;
			document.getElementById("nsv_vote_check_option5").value = vote_results_arr[4];
			summary_results = summary_results + message_json.O5 + ": " + vote_results_arr[4] + ", ";
		} else {
			document.getElementById("nsv_vote_check_option5_lb").style.display = 'none';
			document.getElementById("nsv_vote_check_option5").style.display = 'none';
		}
		if (message_json.O6) {
			document.getElementById("nsv_vote_check_option6_lb").innerHTML = message_json.O6;
			document.getElementById("nsv_vote_check_option6").value = vote_results_arr[5];
			summary_results = summary_results + message_json.O6 + ": " + vote_results_arr[5] + ", ";
		} else {
			document.getElementById("nsv_vote_check_option6_lb").style.display = 'none';
			document.getElementById("nsv_vote_check_option6").style.display = 'none';
		}
		if (message_json.O7) {
			document.getElementById("nsv_vote_check_option7_lb").innerHTML = message_json.O7;
			document.getElementById("nsv_vote_check_option7").value = vote_results_arr[6];
			summary_results = summary_results + message_json.O7 + ": " + vote_results_arr[6] + ", ";
		} else {
			document.getElementById("nsv_vote_check_option7_lb").style.display = 'none';
			document.getElementById("nsv_vote_check_option7").style.display = 'none';
		}
		if (message_json.O8) {
			document.getElementById("nsv_vote_check_option8_lb").innerHTML = message_json.O8;
			document.getElementById("nsv_vote_check_option8").value = vote_results_arr[7];
			summary_results = summary_results + message_json.O8 + ": " + vote_results_arr[7] + ", ";
		} else {
			document.getElementById("nsv_vote_check_option8_lb").style.display = 'none';
			document.getElementById("nsv_vote_check_option8").style.display = 'none';
		}
		summary_results = summary_results.substring(0, summary_results.length - 2);
		summary_results = summary_results + "\n";
		return summary_results;
		
	};
	



	NSV.timestamp_to_time = function (timestamp) {
		var nxt_gen = new Date("November 24, 2013, 12:00:00 UTC");
		var timestamp_msec = timestamp*1000;
		
		var d = new Date(nxt_gen.getTime() + timestamp_msec);
		return(d.toUTCString());
	};


//Alias Voting section
	NSV.aliasvote_check_activate = function () {

		var NSV_aliasvote_create_poll_arr = [];
		var NSV_aliasvotes_arr = [];	
		var err_message = "";

		if (NRS.downloadingBlockchain) {
			$("#nsv_aliasvote_check_error_message").html("Please wait until the blockchain has finished downloading.");
			$("#nsv_aliasvote_check_error_message").show();
			return;
		} else if (NRS.state.isScanning) {
			$("#nsv_aliasvote_check_error_message").html("The blockchain is currently being rescanned. Please wait a minute and then try submitting again.");
			$("#nsv_aliasvote_check_error_message").show();
			return;
		}	
		
		$('#nsv_aliasvote_check_error_message').hide();
		$('#nsv_aliasvote_check_succ_message').hide();

		var dP_asset = document.getElementById("nsv_aliasvote_weighting").value;
		var issuing_acc;
		if (dP_asset != "1" && dP_asset != "0") {
			if (dP_asset === "") {
				err_message = "Weighting not set";
			} else if (!(/^\d+$/.test(dP_asset))) {
				err_message = "Weighting Asset ID is Invalid";
			}
			else {
				NRS.sendRequest("getAsset", {
					"asset": dP_asset
				}, function(response) {
					if (response.errorCode) {
						err_message = "Incorrect Weighting Asset ID";
					} else {
						issuing_acc = response.accountRS;
					}					
				},false);	
			
			}
		}
		if (err_message !== "") {
			$('#nsv_aliasvote_check_error_message').html(err_message);
			$('#nsv_aliasvote_check_error_message').show();
			return;
		}
		
		NSV_aliasvote_create_poll_arr = [];
		NSV_aliasvotes_arr = [];
		var out_message = "";

		var option1 = document.getElementById("nsv_aliasvote_check_option1").value;
		var option2 = document.getElementById("nsv_aliasvote_check_option2").value;
		var option3 = document.getElementById("nsv_aliasvote_check_option3").value;
		var option4 = document.getElementById("nsv_aliasvote_check_option4").value;
		
		var alias1, alias2, alias3, alias4;

		if (option2 === "") {
			err_message = "Option 2 account must be specified";
		} else {
			NRS.sendOutsideRequest("/nxt?requestType=" + "getAliases", {
				"account":option2
			}, function(response,input) {
				if (response.errorCode) {
					err_message = "Option2 account doesn't seem valid.";
					return;					
				} else {
					if (response.aliases.length === 0) {
						err_message = "Option 2 account has no alias, must have exactly one alias";
					} else if (response.aliases.length > 1) {
						err_message = "Option 2 account has more than one alias, must have exactly one alias";
					} else {
						alias2 = response.aliases[0].aliasName;
					}
				
				}
			},false);
		}		
		if (option1 === "") {
			err_message = "Option 1 account must be specified";
		} else {
			NRS.sendOutsideRequest("/nxt?requestType=" + "getAliases", {
				"account":option1
			}, function(response,input) {
				if (response.errorCode) {
					err_message = "Option1 account doesn't seem valid.";
					return;					
				} else {
					if (response.aliases.length === 0) {
						err_message = "Option 1 account has no alias, must have exactly one alias";
					} else if (response.aliases.length > 1) {
						err_message = "Option 1 account has more than one alias, must have exactly one alias";
					} else {
						alias1 = response.aliases[0].aliasName;
					}
				
				}
			},false);
		}
		if (err_message !== "") {
			$('#nsv_aliasvote_check_error_message').html(err_message);
			$('#nsv_aliasvote_check_error_message').show();
			return;
		}		
		if (option3 === "") {
			alias3 = "";
		} else {
			NRS.sendOutsideRequest("/nxt?requestType=" + "getAliases", {
				"account":option3
			}, function(response,input) {
				if (response.errorCode) {
					err_message = "Option3 account doesn't seem valid.";
					return;					
				} else {
					if (response.aliases.length === 0) {
						err_message = "Option 3 account has no alias, must have exactly one alias";
					} else if (response.aliases.length > 1) {
						err_message = "Option 3 account has more than one alias, must have exactly one alias";
					} else {
						alias3 = response.aliases[0].aliasName;
					}
				
				}
			},false);
		}
		if (err_message !== "") {
			$('#nsv_aliasvote_check_error_message').html(err_message);
			$('#nsv_aliasvote_check_error_message').show();
			return;
		}		
		if (option4 === "") {
			alias4 = "";
		} else {
			if (alias3 === "") {
				err_message = "Must specify option 3 before using option 4";
			} else {
				NRS.sendOutsideRequest("/nxt?requestType=" + "getAliases", {
					"account":option4
				}, function(response,input) {
					if (response.errorCode) {
						err_message = "Option4 account doesn't seem valid.";
						return;					
					} else {
						if (response.aliases.length === 0) {
							err_message = "Option 4 account has no alias, must have exactly one alias";
						} else if (response.aliases.length > 1) {
							err_message = "Option 4 account has more than one alias, must have exactly one alias";
						} else {
							alias4 = response.aliases[0].aliasName;
						}
					
					}
				},false);
			}
		}
		if (err_message !== "") {
			$('#nsv_aliasvote_check_error_message').html(err_message);
			$('#nsv_aliasvote_check_error_message').show();
			return;
		}
		document.getElementById("nsv_aliasvote_check_percent1_lb").innerHTML = alias1;
		document.getElementById("nsv_aliasvote_check_percent2_lb").innerHTML = alias2;
		document.getElementById("nsv_aliasvote_check_percent3_lb").innerHTML = alias3;
		document.getElementById("nsv_aliasvote_check_percent4_lb").innerHTML = alias4;
		
		NRS.sendOutsideRequest("/nxt?requestType=" + "getAccountTransactionIds", {
			"account":option1
		}, function(response,input) {
			if (response.errorCode) {
				err_message = "Unexpected error. Option1 account doesn't seem valid for getAccountTransactionIds.";
				return;					
			} else {
				tran_arr = response.transactionIds;
				tran_len = tran_arr.length;
				for (var j=0; j<tran_len; j++) {
					var tmp_tran = tran_arr[j];
					NSV.sendOutsideRequest("/nxt?requestType=" + "getTransaction", {
						"transaction": tmp_tran
					}, function(response, input) {
						if (response.errorCode) {
							err_message = "Unknown bug. Problem accessing getTransaction.";
							return;
						} else {							
							if (response.timestamp && response.senderRS) {
								if (response.senderRS != option1) {
									var vote_obj = {};
									vote_obj.time = response.timestamp;
									vote_obj.sender = response.senderRS;
									vote_obj.vote = alias1;
									NSV_aliasvotes_arr.push(vote_obj);
								}
							}
						}								
					},false);						 														
				}					
			}						
		},false);
		NRS.sendOutsideRequest("/nxt?requestType=" + "getAccountTransactionIds", {
			"account":option2
		}, function(response,input) {
			if (response.errorCode) {
				err_message = "Unexpected error. Option2 account doesn't seem valid for getAccountTransactionIds.";
				return;					
			} else {
				tran_arr = response.transactionIds;
				tran_len = tran_arr.length;
				for (var j=0; j<tran_len; j++) {
					var tmp_tran = tran_arr[j];
					NSV.sendOutsideRequest("/nxt?requestType=" + "getTransaction", {
						"transaction": tmp_tran
					}, function(response, input) {
						if (response.errorCode) {
							err_message = "Unknown bug. Problem accessing getTransaction.";
							return;
						} else {							
							if (response.timestamp && response.senderRS) {
								if (response.senderRS != option2) {
									var vote_obj = {};
									vote_obj.time = response.timestamp;
									vote_obj.sender = response.senderRS;
									vote_obj.vote = alias2;
									NSV_aliasvotes_arr.push(vote_obj);
								}
							}
						}								
					},false);						 														
				}					
			}						
		},false);
		if (alias3 !== "") {
			NRS.sendOutsideRequest("/nxt?requestType=" + "getAccountTransactionIds", {
				"account":option3
			}, function(response,input) {
				if (response.errorCode) {
					err_message = "Unexpected error. Option3 account doesn't seem valid for getAccountTransactionIds.";
					return;					
				} else {
					tran_arr = response.transactionIds;
					tran_len = tran_arr.length;
					for (var j=0; j<tran_len; j++) {
						var tmp_tran = tran_arr[j];
						NSV.sendOutsideRequest("/nxt?requestType=" + "getTransaction", {
							"transaction": tmp_tran
						}, function(response, input) {
							if (response.errorCode) {
								err_message = "Unknown bug. Problem accessing getTransaction.";
								return;
							} else {							
								if (response.timestamp && response.senderRS) {
									if (response.senderRS != option3) {
										var vote_obj = {};
										vote_obj.time = response.timestamp;
										vote_obj.sender = response.senderRS;
										vote_obj.vote = alias3;
										NSV_aliasvotes_arr.push(vote_obj);
									}
								}
							}								
						},false);						 														
					}					
				}						
			},false);		
		}
		if (alias4 !== "") {
			NRS.sendOutsideRequest("/nxt?requestType=" + "getAccountTransactionIds", {
				"account":option4
			}, function(response,input) {
				if (response.errorCode) {
					err_message = "Unexpected error. Option4 account doesn't seem valid for getAccountTransactionIds.";
					return;					
				} else {
					tran_arr = response.transactionIds;
					tran_len = tran_arr.length;
					for (var j=0; j<tran_len; j++) {
						var tmp_tran = tran_arr[j];
						NSV.sendOutsideRequest("/nxt?requestType=" + "getTransaction", {
							"transaction": tmp_tran
						}, function(response, input) {
							if (response.errorCode) {
								err_message = "Unknown bug. Problem accessing getTransaction.";
								return;
							} else {							
								if (response.timestamp && response.senderRS) {
									if (response.senderRS != option4) {
										var vote_obj = {};
										vote_obj.time = response.timestamp;
										vote_obj.sender = response.senderRS;
										vote_obj.vote = alias4;
										NSV_aliasvotes_arr.push(vote_obj);
									}
								}
							}								
						},false);						 														
					}					
				}						
			},false);		
		}		
		if (err_message !== "") {
			$('#nsv_aliasvote_check_error_message').html(err_message);
			$('#nsv_aliasvote_check_error_message').show();
			return;
		}
		
		var excl_iss_account = document.getElementById("nsv_aliasvote_issuer").checked;
		
		var present_time = "";
		NRS.sendOutsideRequest("/nxt?requestType=" + "getTime", {}, function(response) {
			if (response.errorCode) {
				err_message = "Unknown error, couldn't getTime.";
			} else {
				present_time = response.time;
			}
		},false);		
		var datetime = NSV.timestamp_to_time(present_time);
		
		out_message = alias1 + ": " + option1 + ", " + alias2 + ": " + option2;
		if (alias3 !== "") {
			out_message = out_message + ", " + alias3 + ": " + option3;
		}
		if (alias4 !== "") {
			out_message = out_message + ", " + alias4 + ": " + option4;
		}		
		out_message = out_message + "\n" + "Timestamp: " + present_time + " (" + datetime + ")\n";
		if (dP_asset == "0") {
			var asset_message = "Weighting via NXT\n"; 
		} else if (dP_asset == "1") {
			asset_message = "Weighting via number of votes, one vote per account\n";
		} else {
			asset_message = "Weighting via amount of asset in account, assetID=" + dP_asset + "\n";
		}
		out_message = out_message + asset_message;
		out_message = out_message + "--------VOTES----------\n";
		
		var total_weight = 0;
		var aliasvote_results_array = [0,0,0,0];
		if (NSV_aliasvotes_arr.length === 0) {
			out_message = "No Votes Found.\n";
		} else {
			NSV_aliasvotes_arr.sort(function(a,b) {return b.sender < a.sender; });
			for (var k=0; k<NSV_aliasvotes_arr.length-1; k++) {
				if (NSV_aliasvotes_arr[k].sender == NSV_aliasvotes_arr[k+1].sender) {
					if (NSV_aliasvotes_arr[k+1].time > NSV_aliasvotes_arr[k].time) {
						NSV_aliasvotes_arr.splice(k,1);
					} else {
						NSV_aliasvotes_arr.splice(k+1,1);
					}
					k--;
				}
			}
			for (k=0; k<NSV_aliasvotes_arr.length; k++) {
				if (dP_asset == "1") {
					NSV_aliasvotes_arr[k].weight = 1;
				} else {
					NRS.sendOutsideRequest("/nxt?requestType=" + "getAccount", {
						"account":NSV_aliasvotes_arr[k].sender
					}, function(response,input) {
						if (response.errorCode) {
							err_message = "Unknown bug. Problem accessing getAccount.";
							return;
						} else {							
							if (dP_asset == "0") {
								NSV_aliasvotes_arr[k].weight = parseInt(response.unconfirmedBalanceNQT, 10);
							} else {
								NSV_aliasvotes_arr[k].weight = 0;
								if (response.unconfirmedAssetBalances) {
									var asset_arr = response.unconfirmedAssetBalances;
									for (var j=0; j<asset_arr.length; j++) {
										if (asset_arr[j].asset == dP_asset) {
											NSV_aliasvotes_arr[k].weight = parseInt(asset_arr[j].unconfirmedBalanceQNT, 10 );
										}
									}
									if (excl_iss_account) {
										if (input.account == issuing_acc) {
											NSV_aliasvotes_arr[k].weight = 0;
											NSV_aliasvotes_arr[k].vote = "NOT COUNTED, ISSUING ACCOUNT";
										}
									}
								}
							}
						}							
					},false);
				}
			}

			
			for (k=0; k<NSV_aliasvotes_arr.length; k++) {
				if (NSV_aliasvotes_arr[k].vote ==  alias1) {
					aliasvote_results_array[0] = aliasvote_results_array[0] + NSV_aliasvotes_arr[k].weight;
					total_weight = total_weight + NSV_aliasvotes_arr[k].weight;
				} else if (NSV_aliasvotes_arr[k].vote ==  alias2) {
					aliasvote_results_array[1] = aliasvote_results_array[1] + NSV_aliasvotes_arr[k].weight;
					total_weight = total_weight + NSV_aliasvotes_arr[k].weight;
				} else if (NSV_aliasvotes_arr[k].vote ==  alias3) {
					aliasvote_results_array[2] = aliasvote_results_array[2] + NSV_aliasvotes_arr[k].weight;
					total_weight = total_weight + NSV_aliasvotes_arr[k].weight;
				} else if (NSV_aliasvotes_arr[k].vote ==  alias4) {
					aliasvote_results_array[3] = aliasvote_results_array[3] + NSV_aliasvotes_arr[k].weight;
					total_weight = total_weight + NSV_aliasvotes_arr[k].weight;
				}
				out_message = out_message + NSV_aliasvotes_arr[k].sender + ", vote=" + NSV_aliasvotes_arr[k].vote + ", weight=" + NSV_aliasvotes_arr[k].weight + "\n";				
			}
			
		}
		var aliasvote_percent_str = ["","","",""];
		var percent_aliasvote;
		for (k=0; k<4; k++) {
			if (total_weight === 0) {
				percent_aliasvote = 0;
			} else {
				percent_aliasvote = 100*aliasvote_results_array[k]/total_weight;
				percent_aliasvote = percent_aliasvote.toFixed(2);
			}
			aliasvote_percent_str[k] = percent_aliasvote.toString() + "%";									
		}
		
		out_message = out_message + "-----------------------\n";


		var summary_results = "Voting weight: " + total_weight.toString() + ", ";

		document.getElementById("nsv_aliasvote_check_percent1").value = aliasvote_percent_str[0];
		summary_results = summary_results + alias1 + ": " + aliasvote_percent_str[0] + ", ";
		document.getElementById("nsv_aliasvote_check_percent2").value = aliasvote_percent_str[1];
		summary_results = summary_results + alias2 + ": " + aliasvote_percent_str[1] + ", ";
		if (alias3 !== "") {
			document.getElementById("nsv_aliasvote_check_percent3").value = aliasvote_percent_str[2];
			summary_results = summary_results + alias3 + ": " + aliasvote_percent_str[2] + ", ";
		}
		if (alias4 !== "") {
			document.getElementById("nsv_aliasvote_check_percent4").value = aliasvote_percent_str[3];
			summary_results = summary_results + alias4 + ": " + aliasvote_percent_str[3] + ", ";
		}	
		summary_results = summary_results.substring(0, summary_results.length - 2);
		summary_results = summary_results + "\n";

		out_message = out_message + summary_results;		
		document.getElementById("nsv_aliasvote_check_details").value = out_message;			
		
		if (err_message !== "") {
			$('#nsv_aliasvote_check_error_message').html(err_message);
			$('#nsv_aliasvote_check_error_message').show();
		}
		
	};

	
    return NSV;
	
}(NSV || {}, jQuery));
