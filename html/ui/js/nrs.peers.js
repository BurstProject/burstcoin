var NRS = (function(NRS, $, undefined) {
	NRS.pages.peers = function() {
		var response;

		NRS.pageLoading();

		NRS.sendRequest("getPeers+", function(response) {
			if (response.peers && response.peers.length) {
				var peers = {};
				var nr_peers = 0;

				for (var i = 0; i < response.peers.length; i++) {
					NRS.sendRequest("getPeer+", {
						"peer": response.peers[i]
					}, function(peer, input) {
						if (NRS.currentPage != "peers") {
							peers = {};
							return;
						}

						if (!peer.errorCode) {
							peers[input.peer] = peer;
						}

						nr_peers++;

						if (nr_peers == response.peers.length) {
							var rows = "";
							var uploaded = 0;
							var downloaded = 0;
							var connected = 0;
							var up_to_date = 0;
							var active_peers = 0;

							for (var i = 0; i < nr_peers; i++) {
								var peer = peers[response.peers[i]];

								if (!peer) {
									continue;
								}

								if (peer.state != 0) {
									active_peers++;
									downloaded += peer.downloadedVolume;
									uploaded += peer.uploadedVolume;
									if (peer.state == 1) {
										connected++;
									}

									//todo check if response.version ends with "e" then we compare with betaversion instead..
									if (NRS.versionCompare(peer.version, NRS.normalVersion.versionNr) >= 0) {
										up_to_date++;
									}

									rows += "<tr><td>" + (peer.state == 1 ? "<i class='fa fa-check-circle' style='color:#5cb85c' title='Connected'></i>" : "<i class='fa fa-times-circle' style='color:#f0ad4e' title='Disconnected'></i>") + "&nbsp;&nbsp;" + (peer.announcedAddress ? String(peer.announcedAddress).escapeHTML() : "No name") + "</td><td" + (peer.weight > 0 ? " style='font-weight:bold'" : "") + ">" + NRS.formatWeight(peer.weight) + "</td><td>" + NRS.formatVolume(peer.downloadedVolume) + "</td><td>" + NRS.formatVolume(peer.uploadedVolume) + "</td><td><span class='label label-" +
										(NRS.versionCompare(peer.version, NRS.normalVersion.versionNr) >= 0 ? "success" : "danger") + "'>" + (peer.application && peer.version ? String(peer.application).escapeHTML() + " " + String(peer.version).escapeHTML() : "?") + "</label></td><td>" + (peer.platform ? String(peer.platform).escapeHTML() : "?") + "</td></tr>";
								}
							}

							$("#peers_table tbody").empty().append(rows);
							NRS.dataLoadFinished($("#peers_table"));
							$("#peers_uploaded_volume").html(NRS.formatVolume(uploaded)).removeClass("loading_dots");
							$("#peers_downloaded_volume").html(NRS.formatVolume(downloaded)).removeClass("loading_dots");
							$("#peers_connected").html(connected).removeClass("loading_dots");
							$("#peers_up_to_date").html(up_to_date + '/' + active_peers).removeClass("loading_dots");

							peers = {};

							NRS.pageLoaded();
						}
					});

					if (NRS.currentPage != "peers") {
						peers = {};
						return;
					}
				}
			} else {
				$("#peers_table tbody").empty();
				NRS.dataLoadFinished($("#peers_table"));

				$("#peers_uploaded_volume, #peers_downloaded_volume, #peers_connected, #peers_up_to_date").html("0").removeClass("loading_dots");

				NRS.pageLoaded();
			}
		});
	}

	NRS.incoming.peers = function() {
		NRS.pages.peers();
	}

	return NRS;
}(NRS || {}, jQuery));