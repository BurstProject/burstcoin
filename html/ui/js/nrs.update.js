var NRS = (function(NRS, $, undefined) {
	NRS.normalVersion = {};
	NRS.betaVersion = {};
	NRS.isOutdated = false;

	NRS.checkAliasVersions = function() {

	}

	NRS.checkForNewVersion = function() {

	}

	NRS.versionCompare = function(v1, v2) {
		if (v2 == undefined) {
			return -1;
		} else if (v1 == undefined) {
			return -1;
		}

		//https://gist.github.com/TheDistantSea/8021359 (based on)
		var v1last = v1.slice(-1);
		var v2last = v2.slice(-1);

		if (v1last == 'e') {
			v1 = v1.substring(0, v1.length - 1);
		} else {
			v1last = '';
		}

		if (v2last == 'e') {
			v2 = v2.substring(0, v2.length - 1);
		} else {
			v2last = '';
		}

		var v1parts = v1.split('.');
		var v2parts = v2.split('.');

		function isValidPart(x) {
			return /^\d+$/.test(x);
		}

		if (!v1parts.every(isValidPart) || !v2parts.every(isValidPart)) {
			return NaN;
		}

		v1parts = v1parts.map(Number);
		v2parts = v2parts.map(Number);

		for (var i = 0; i < v1parts.length; ++i) {
			if (v2parts.length == i) {
				return 1;
			}
			if (v1parts[i] == v2parts[i]) {
				continue;
			} else if (v1parts[i] > v2parts[i]) {
				return 1;
			} else {
				return -1;
			}
		}

		if (v1parts.length != v2parts.length) {
			return -1;
		}

		if (v1last && v2last) {
			return 0;
		} else if (v1last) {
			return 1;
		} else if (v2last) {
			return -1;
		} else {
			return 0;
		}
	}

	NRS.supportsUpdateVerification = function() {
		if ((typeof File !== 'undefined') && !File.prototype.slice) {
			if (File.prototype.webkitSlice) {
				File.prototype.slice = File.prototype.webkitSlice;
			}

			if (File.prototype.mozSlice) {
				File.prototype.slice = File.prototype.mozSlice;
			}
		}

		// Check for the various File API support.
		if (!window.File || !window.FileReader || !window.FileList || !window.Blob || !File.prototype.slice || !window.Worker) {
			return false;
		}

		return true;
	}

	NRS.verifyClientUpdate = function(e) {
		e.stopPropagation();
		e.preventDefault();

		var files = null;

		if (e.originalEvent.target.files && e.originalEvent.target.files.length) {
			files = e.originalEvent.target.files;
		} else if (e.originalEvent.dataTransfer.files && e.originalEvent.dataTransfer.files.length) {
			files = e.originalEvent.dataTransfer.files;
		}

		if (!files) {
			return;
		}

		$("#nrs_update_hash_progress").css("width", "0%");
		$("#nrs_update_hash_progress").show();

		var worker = new Worker("js/crypto/sha256worker.js");

		worker.onmessage = function(e) {
			if (e.data.progress) {
				$("#nrs_update_hash_progress").css("width", e.data.progress + "%");
			} else {
				$("#nrs_update_hash_progress").hide();
				$("#nrs_update_drop_zone").hide();

				if (e.data.sha256 == NRS.downloadedVersion.hash) {
					$("#nrs_update_result").html("The downloaded version has been verified, the hash is correct. You may proceed with the installation.").attr("class", " ");
				} else {
					$("#nrs_update_result").html("The downloaded version hash does not compare to the specified hash in the blockchain. DO NOT PROCEED.").attr("class", "incorrect");
				}

				$("#nrs_update_hash_version").html(NRS.downloadedVersion.versionNr);
				$("#nrs_update_hash_download").html(e.data.sha256);
				$("#nrs_update_hash_official").html(NRS.downloadedVersion.hash);
				$("#nrs_update_hashes").show();
				$("#nrs_update_result").show();

				NRS.downloadedVersion = {};

				$("body").off("dragover.nrs, drop.nrs");
			}
		};

		worker.postMessage({
			file: files[0]
		});
	}

	NRS.downloadClientUpdate = function(version) {

		return false;
	}

	return NRS;
}(NRS || {}, jQuery));
