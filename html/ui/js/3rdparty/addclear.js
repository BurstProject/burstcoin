// Author: Stephen Korecky
// Website: http://stephenkorecky.com
// Plugin Website: http://github.com/skorecky/Add-Clear

(function($) {
	$.fn.extend({
		addClear: function(options) {
			var selector = this.selector;

			var options = $.extend({
				closeSymbol: "<i class='fa fa-times'></i>",
        color: "#CCC",
				top: 1,
				right: 4,
				returnFocus: true,
				showOnLoad: false,
				onClear: null,
				hideOnBlur: false
			}, options);

			$(this).wrap("<span style='position:relative;' class='add-clear-span'>");
			$(this).after("<a href='#clear'>" + options.closeSymbol + "</a>");
			$("a[href='#clear']").css({
				color: options.color,
				'text-decoration': 'none',
				display: 'none',
				'line-height': 1,
				overflow: 'hidden',
				position: 'absolute',
				right: options.right,
				top: options.top
			}, this);

			if ($(this).val().length >= 1 && options.showOnLoad === true) {
				$(this).siblings("a[href='#clear']").show();
			}

			$(this).focus(function() {
				$(this).closest(".input-group").find(".input-group-btn .btn").addClass("focus"); //added by wesley
				if ($(this).val().length >= 1) {
					$(this).siblings("a[href='#clear']").show();
				}
			});

			$(this).blur(function() {
				var self = this;

				$(self).closest(".input-group").find(".input-group-btn .btn").removeClass("focus"); //added by wesley

				if (options.hideOnBlur) {
					setTimeout(function() {
						$(self).siblings("a[href='#clear']").hide();
					}, 50);
				}
			});

			$(this).keyup(function() {
				if ($(this).val().length >= 1) {
					$(this).siblings("a[href='#clear']").show();
				} else {
					$(this).siblings("a[href='#clear']").hide();
				}
			});

			$("a[href='#clear']").click(function() {
				$(this).siblings(selector).val("");
				$(this).hide();
				if (options.returnFocus === true) {
					$(this).siblings(selector).focus();
				}
				if (options.onClear) {
					options.onClear($(this).siblings("input"));
				}
				return false;
			});
			return this;
		}
	});
})(jQuery);