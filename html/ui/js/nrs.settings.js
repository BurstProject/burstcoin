var NRS = (function(NRS, $, undefined) {
	NRS.defaultSettings = {
		"submit_on_enter": 0,
		"reed_solomon": 1,
		"animate_forging": 1,
		"news": -1,
		"fee_warning": "100000000000",
		"amount_warning": "10000000000000",
		"asset_transfer_warning": "10000"
	};

	NRS.defaultColors = {
		"background": "#F9F9F9",
		"header": "#408EBA",
		"sidebar": "#F4F4F4",
		"page_header": "#FBFBFB",
		"box": "#fff",
		"table": "#F3F4F5"
	};

	NRS.userStyles = {};

	NRS.styleOptions = {};

	NRS.styleOptions.header = ["header_bg", {
			"key": "header_bg_gradient",
			"type": "gradient",
			"optional": true
		}, {
			"key": "logo_bg",
			"optional": "true"
		}, {
			"key": "logo_bg_gradient",
			"type": "gradient",
			"optional": true
		},
		"link_txt",
		"link_txt_hover",
		"link_bg_hover", {
			"key": "link_bg_hover_gradient",
			"type": "gradient",
			"optional": true
		},
		"toggle_icon", {
			"key": "toggle_icon_hover",
			"optional": true
		}, {
			"key": "link_border",
			"optional": true
		}, {
			"key": "link_border_inset",
			"optional": true
		}, {
			"key": "header_border",
			"optional": true
		}
	];

	NRS.styleOptions.sidebar = ["sidebar_bg", {
			"key": "user_panel_bg",
			"optional": true
		}, {
			"key": "user_panel_bg_gradient",
			"type": "gradient",
			"optional": true
		},
		"user_panel_txt",
		"user_panel_link", {
			"key": "sidebar_top_border",
			"optional": true
		}, {
			"key": "sidebar_bottom_border",
			"optional": true
		}, {
			"key": "menu_item_top_border",
			"optional": true
		}, {
			"key": "menu_item_bottom_border",
			"optional": true
		},
		"menu_item_txt",
		"menu_item_bg", {
			"key": "menu_item_bg_gradient",
			"type": "gradient",
			"optional": true
		},
		"menu_item_txt_hover",
		"menu_item_bg_hover", {
			"key": "menu_item_bg_hover_gradient",
			"type": "gradient",
			"optional": true
		},
		"menu_item_txt_active",
		"menu_item_bg_active", {
			"key": "menu_item_bg_active_gradient",
			"type": "gradient",
			"optional": true
		}, {
			"key": "menu_item_border_active",
			"optional": true
		}, {
			"key": "menu_item_border_hover",
			"optional": true
		}, {
			"key": "menu_item_border_size",
			"type": "number",
			"optional": "true"
		}, {
			"key": "submenu_item_top_border",
			"optional": true
		}, {
			"key": "submenu_item_bottom_border",
			"optional": true
		},
		"submenu_item_txt",
		"submenu_item_bg", {
			"key": "submenu_item_bg_gradient",
			"type": "gradient",
			"optional": true
		},
		"submenu_item_txt_hover",
		"submenu_item_bg_hover", {
			"key": "submenu_item_bg_hover_gradient",
			"type": "gradient",
			"optional": true
		}
	];

	NRS.styleOptions.background = ["bg", {
			"key": "bg_image",
			"type": "select",
			"values": ["always_grey", "back_pattern", "blu_stripes", "brickwall", "bright_squares", "carbon_fibre_v2", "circles", "climpek", "cubes", "dark_matter", "denim", "ecailles", "escheresque_ste", "escheresque", "furley_bg", "gplaypattern", "grey_sandbag", "grey", "grid_noise", "gun_metal", "hexellence", "hoffman", "knitting250px", "light_grey", "lil_fiber", "noisy_grid", "old_moon", "pixel_weave", "polaroid", "ps_neutral", "pw_maze_white", "px_by_Gre3g", "random_grey_variations", "ricepaper_v3", "scribble_light", "shinedotted", "square_bg", "swirl", "tiny_grid", "weave", "white_brick_wall", "white_leather", "worn_dots"],
			"optional": true
		},
		"txt",
		"link"
	];

	NRS.styleOptions.page_header = ["bg", {
			"key": "bg_gradient",
			"type": "gradient",
			"optional": true
		},
		"txt", {
			"key": "border",
			"optional": "true"
		}
	];

	NRS.styleOptions.box = ["bg",
		"txt", {
			"key": "border_size",
			"type": "number",
			"optional": true
		},
		"border_color", {
			"key": "rounded_corners",
			"optional": true,
			"type": "number"
		},
		"header_background",
		"header_txt"
	];

	NRS.styleOptions.table = ["bg",
		"header_txt",
		"rows_txt",
		"row_separator",
		"header_separator", {
			"key": "row_separator_size",
			"type": "number"
		}, {
			"key": "header_separator_size",
			"type": "number"
		}, {
			"key": "header_bold",
			"type": "boolean"
		}
	];

	NRS.userStyles.header = {
		"green": {
			"header_bg": "#29BB9C",
			"logo_bg": "#26AE91",
			"link_bg_hover": "#1F8E77"
		},
		"red": {
			"header_bg": "#cb4040",
			"logo_bg": "#9e2b2b",
			"link_bg_hover": "#9e2b2b",
			"toggle_icon": "#d97474"
		},
		"brown": {
			"header_bg": "#ba5d32",
			"logo_bg": "#864324",
			"link_bg_hover": "#864324",
			"toggle_icon": "#d3815b"
		},
		"purple": {
			"header_bg": "#86618f",
			"logo_bg": "#614667",
			"link_bg_hover": "#614667",
			"toggle_icon": "#a586ad"
		},
		"gray": {
			"header_bg": "#575757",
			"logo_bg": "#363636",
			"link_bg_hover": "#363636",
			"toggle_icon": "#787878"
		},
		"pink": {
			"header_bg": "#b94b6f",
			"logo_bg": "#8b3652",
			"link_bg_hover": "#8b3652",
			"toggle_icon": "#cc7b95"
		},
		"bright-blue": {
			"header_bg": "#2494F2",
			"logo_bg": "#2380cf",
			"link_bg_hover": "#36a3ff",
			"toggle_icon": "#AEBECD"
		},
		"dark-blue": {
			"header_bg": "#25313e",
			"logo_bg": "#1b252e",
			"link_txt": "#AEBECD",
			"link_bg_hover": "#1b252e",
			"link_txt_hover": "#fff",
			"toggle_icon": "#AEBECD"
		}
	};
	NRS.userStyles.sidebar = {
		"dark-gray": {
			"sidebar_bg": "#272930",
			"user_panel_txt": "#fff",
			"sidebar_top_border": "#1a1c20",
			"sidebar_bottom_border": "#2f323a",
			"menu_item_top_border": "#32353e",
			"menu_item_bottom_border": "#1a1c20",
			"menu_item_txt": "#c9d4f6",
			"menu_item_bg_hover": "#2a2c34",
			"menu_item_border_active": "#2494F2",
			"submenu_item_bg": "#2A2A2A",
			"submenu_item_txt": "#fff",
			"submenu_item_bg_hover": "#222222"
		},
		"dark-blue": {
			"sidebar_bg": "#34495e",
			"user_panel_txt": "#fff",
			"sidebar_top_border": "#142638",
			"sidebar_bottom_border": "#54677a",
			"menu_item_top_border": "#54677a",
			"menu_item_bottom_border": "#142638",
			"menu_item_txt": "#fff",
			"menu_item_bg_hover": "#3d566e",
			"menu_item_bg_active": "#2c3e50",
			"submenu_item_bg": "#ECF0F1",
			"submenu_item_bg_hover": "#E0E7E8",
			"submenu_item_txt": "#333333"
		}
	};

	NRS.userStyles.background = {
		"black": {
			"bg": "#000",
			"txt": "#fff",
			"link": "#fff"
		},
		"light-gray": {
			"bg": "#f9f9f9",
			"txt": "#000"
		},
		"light-gray-2": {
			"bg": "#ECF0F1",
			"txt": "#000"
		},
		"white": {
			"bg": "#fff",
			"txt": "#000"
		},
		"dark-blue": {
			"bg": "#3E4649",
			"txt": "#fff"
		},
		"dark-gray": {
			"bg": "#333333",
			"txt": "#fff"
		},
		"blue": {
			"bg": "#58C0D4",
			"txt": "#fff"
		},
		"light-blue": {
			"bg": "#D7DDE2",
			"txt": "#000"
		}
	};

	NRS.userStyles.page_header = {
		"light-gray": {
			"bg": "#ECF0F1",
			"txt": "#000"
		}
	};

	NRS.userStyles.box = {
		"black": {
			"bg": "#000",
			"txt": "#fff",
			"border_size": "2",
			"border_color": "red",
			"rounded_corners": "2",
			"header_background": "#F3F3F3",
			"header_txt": "#333"
		}
	};

	NRS.userStyles.table = {
		"light_gray": {
			"bg": "#FAFAFA",
			"header_txt": "#000",
			"rows_txt": "#949494",
			"row_separator": "#EBEBEB",
			"header_separator": "#EBEBEB",
			"row_separator_size": "1",
			"header_separator_size": "3",
			"header_bold": true
		},
		"black": {
			"bg": "#000",
			"header_txt": "#fff",
			"rows_bg": "#000",
			"rows_txt": "#fff",
			"row_separator_size": "1",
			"row_separator": "#ADD0E4",
			"header_bold": true
		},


	};

	NRS.pages.settings = function() {
		for (var style in NRS.userStyles) {
			var $dropdown = $("#" + style + "_color_scheme");

			$dropdown.empty();

			$dropdown.append("<li><a href='#' data-color=''><span class='color' style='background-color:" + NRS.defaultColors[style] + ";border:1px solid black;'></span>Default</a></li>");

			$.each(NRS.userStyles[style], function(key, value) {
				var bg = "";
				if (value.bg) {
					bg = value.bg;
				} else if (value.header_bg) {
					bg = value.header_bg;
				} else if (value.sidebar_bg) {
					bg = value.sidebar_bg;
				}

				$dropdown.append("<li><a href='#' data-color='" + key + "'><span class='color' style='background-color: " + bg + ";border:1px solid black;'></span> " + key.replace("-", " ") + "</a></li>");
			});

			$dropdown.append("<li><a href='#' data-color='custom'><span class='color'></span>Custom...</a></li>");

			var $span = $dropdown.closest(".btn-group.colors").find("span.text");

			var color = NRS.settings[style + "_color"];

			if (!color) {
				colorTitle = "Default";
			} else {
				var colorTitle = color.replace(/-/g, " ");
				colorTitle = colorTitle.replace(/\w\S*/g, function(txt) {
					return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
				});
			}

			$span.html(colorTitle);
		}

		for (var key in NRS.settings) {
			if (/_warning/i.test(key) && key != "asset_transfer_warning") {
				if ($("#settings_" + key).length) {
					$("#settings_" + key).val(NRS.convertToNXT(NRS.settings[key]));
				}
			} else if (!/_color/i.test(key)) {
				if ($("#settings_" + key).length) {
					$("#settings_" + key).val(NRS.settings[key]);
				}
			}
		}

		if (NRS.settings["news"] != -1) {
			$("#settings_news_initial").remove();
		}
	}

	NRS.cssGradient = function(start, stop) {
		var output = "";

		output += "background-image: -moz-linear-gradient(top, " + start + ", " + stop + ");";
		output += "background-image: -ms-linear-gradient(top, " + start + ", " + stop + ");";
		output += "background-image: -webkit-gradient(linear, 0 0, 0 100%, from(" + start + "), to(" + stop + "));";
		output += "background-image: -webkit-linear-gradient(top, " + start + ", " + stop + ");";
		output += "background-image: -o-linear-gradient(top, " + start + ", " + stop + ");";
		output += "background-image: linear-gradient(top, " + start + ", " + stop + ");";
		output += "filter: progid:dximagetransform.microsoft.gradient(startColorstr='" + start + "', endColorstr='" + stop + "', GradientType=0);";

		return output;
	}

	NRS.updateStyle = function(type, color) {
		var css = "";

		if ($.isPlainObject(color)) {
			var colors = color;
		} else {
			var colors = NRS.userStyles[type][color];
		}

		if (colors) {
			switch (type) {
				case "table":
					if (!colors.header_bg) {
						colors.header_bg = colors.bg;
					}
					if (!colors.rows_bg) {
						colors.rows_bg = colors.bg;
					}

					css += ".table > thead > tr > th { background: " + colors.header_bg + "; color: " + colors.header_txt + (colors.header_bold ? "; font-weight:bold" : "; font-weight:normal") + " }";

					if (!colors.rows_even_bg && !colors.rows_odd_bg) {
						css += ".table > tbody > tr > td { background: " + colors.rows_bg + " !important; color: " + colors.rows_txt + " !important }";
					} else {
						if (!colors.rows_even_txt && !colors.rows_odd_txt) {
							colors.rows_even_txt = colors.rows_txt;
							colors.rows_odd_txt = colors.rows_txt;
						}

						css += ".table > tbody > tr >td { background: " + colors.rows_even_bg + "; color: " + colors.rows_even_txt + " }";
						css += ".table > tbody > tr:nth-child(odd) > td { background: " + colors.rows_odd_bg + "; color: " + colors.rows_odd_txt + " }";
					}

					if (colors.header_separator) {
						css += ".table > thead > tr > th { border-bottom: " + colors.header_separator_size + "px solid " + colors.header_separator + " }";
					} else {
						css += ".table > thead > tr > th { border-bottom: none !important; border-top:none !important; }";
					}

					if (colors.row_separator) {
						css += ".table > tbody > tr > td { border-bottom: " + colors.row_separator_size + "px solid " + colors.row_separator + " }";
					} else {
						css += ".table > tbody > tr > td { border-bottom: none !important; border-top:none !important; }";
					}

					break;
				case "box":
					css += ".box { background: " + colors.bg + "; color: " + colors.txt + "; -moz-border-radius: " + colors.rounded_corners + "px; -webkit-border-radius: " + colors.rounded_corners + "px; border-radius: " + colors.rounded_corners + "px; border: " + colors.border_size + "px solid " + colors.border_color + " !important }";

					if (colors.header_background) {
						css += ".box .box-header { background: " + colors.header_background + (colors.header_txt ? "; color: " + colors.header_txt + "; " : "") + " }";
					}

					//box-shadow: 0px 1px 3px rgba(0, 0, 0, 0.1);
					break;
				case "page_header":
					if (!colors.link) {
						colors.link = colors.txt;
					}

					css += ".right-side > .page > .content-header { background: " + colors.bg + "; color: " + colors.txt + (colors.border ? "; border-bottom: 1px solid " + colors.border : "") + " }";

					if (colors.bg_gradient) {
						css += ".right-side > .page > .content-header { " + NRS.cssGradient(colors.bg, colors.bg_gradient) + " }";
					}

					break;
				case "background":
					if (!colors.link) {
						colors.link = colors.txt;
					}

					css += "body, html, .content { background: " + colors.bg + "; color: " + colors.txt + " }";
					css += "a, a:active { color: " + colors.link + " }";

					if (colors.bg_image) {
						css += "body, html, .content { background-image: url('http://subtlepatterns.com/patterns/" + colors.bg_image + ".png') }";
					}
					break;
				case "header":
					if (!colors.link_txt) {
						colors.link_txt = "#fff";
					}
					if (!colors.toggle_icon) {
						colors.toggle_icon = "#fff";
					}
					if (!colors.toggle_icon_hover) {
						colors.toggle_icon_hover = "#fff";
					}
					if (!colors.link_txt_hover) {
						colors.link_txt_hover = colors.link_txt;
					}
					if (!colors.link_bg_hover && colors.link_bg) {
						colors.link_bg_hover = colors.link_bg;
					}


					if (!colors.logo_bg) {
						css += ".header { background:" + colors.header_bg + " }";
						if (colors.header_bg_gradient) {
							css += ".header { " + NRS.cssGradient(colors.header_bg, colors.header_bg_gradient) + " }";
						}
						css += ".header .navbar { background: inherit }";
						css += ".header .logo { background: inherit }";
					} else {
						css += ".header .navbar { background:" + colors.header_bg + " }";

						if (colors.header_bg_gradient) {
							css += ".header .navbar { " + NRS.cssGradient(colors.header_bg, colors.header_bg_gradient) + " }";
						}

						css += ".header .logo { background: " + colors.logo_bg + " }";

						if (colors.logo_bg_gradient) {
							css += ".header .logo { " + NRS.cssGradient(colors.logo_bg, colors.logo_bg_gradient) + " }";
						}
					}

					css += ".header .navbar .nav a { color: " + colors.link_txt + (colors.link_bg ? "; background:" + colors.link_bg : "") + " }";
					css += ".header .navbar .nav > li > a:hover { color: " + colors.link_txt_hover + (colors.link_bg_hover ? "; background:" + colors.link_bg_hover : "") + " }";

					if (colors.link_bg_hover) {
						css += ".header .navbar .nav > li > a:hover { " + NRS.cssGradient(colors.link_bg_hover, colors.link_bg_hover_gradient) + " }";
					}

					css += ".header .navbar .sidebar-toggle .icon-bar { background: " + colors.toggle_icon + " }";
					css += ".header .navbar .sidebar-toggle:hover .icon-bar { background: " + colors.toggle_icon_hover + " }";

					if (colors.link_border) {
						css += ".header .navbar .nav > li { border-left: 1px solid " + colors.link_border + " }";
					}

					if (colors.link_border_inset) {
						css += ".header .navbar .nav > li { border-right: 1px solid " + colors.link_border_inset + " }";
						css += ".header .navbar .nav > li:last-child { border-right:none }";
						css += ".header .navbar .nav { border-left: 1px solid " + colors.link_border_inset + " }";
					}

					/*
			    	if (colors.logo_border && colors.link_border) {
			    		if (colors.link_border_inset) {
			    			css += ".header .logo { border-right: 1px solid " + colors.link_border_inset + "}";
				    		css += ".header .navbar { border-left: 1px solid " + colors.link_border + " }";
			    		} else {
			    			css += ".header .navbar { border-left: 1px solid " + colors.link_border + " }";
			    		}
			    	}*/

					if (colors.header_border) {
						css += ".header { border-bottom: 1px solid " + colors.header_border + " }";
					}
					break;
				case "sidebar":
					if (!colors.user_panel_link) {
						colors.user_panel_link = colors.user_panel_txt;
					}
					if (!colors.menu_item_bg) {
						colors.menu_item_bg = colors.sidebar_bg;
					}
					if (!colors.menu_item_bg_active) {
						colors.menu_item_bg_active = colors.menu_item_bg_hover;
					}
					if (!colors.menu_item_txt_hover) {
						colors.menu_item_txt_hover = colors.menu_item_txt;
					}
					if (!colors.menu_item_txt_active) {
						colors.menu_item_txt_active = colors.menu_item_txt_hover;
					}
					if (!colors.menu_item_border_active && colors.menu_item_border_hover) {
						colors.menu_item_border_active = colors.menu_item_border_hover;
					}
					if (!colors.menu_item_border_size) {
						colors.menu_item_border_size = 1;
					}

					css += ".left-side { background: " + colors.sidebar_bg + " }";

					css += ".left-side .user-panel > .info { color: " + colors.user_panel_txt + " }";

					if (colors.user_panel_bg) {
						css += ".left-side .user-panel { background: " + colors.user_panel_bg + " }";
						if (colors.user_panel_bg_gradient) {
							css += ".left-side .user-panel { " + NRS.cssGradient(colors.user_panel_bg, colors.user_panel_bg_gradient) + " }";
						}
					}

					css += ".left-side .user-panel a { color:" + colors.user_panel_link + " }";

					if (colors.sidebar_top_border || colors.sidebar_bottom_border) {
						css += ".left-side .sidebar > .sidebar-menu { " + (colors.sidebar_top_border ? "border-top: 1px solid " + colors.sidebar_top_border + "; " : "") + (colors.sidebar_bottom_border ? "border-bottom: 1px solid " + colors.sidebar_bottom_border : "") + " }";
					}

					css += ".left-side .sidebar > .sidebar-menu > li > a { background: " + colors.menu_item_bg + "; color: " + colors.menu_item_txt + (colors.menu_item_top_border ? "; border-top:1px solid " + colors.menu_item_top_border : "") + (colors.menu_item_bottom_border ? "; border-bottom: 1px solid " + colors.menu_item_bottom_border : "") + " }";

					if (colors.menu_item_bg_gradient) {
						css += ".left-side .sidebar > .sidebar-menu > li > a { " + NRS.cssGradient(colors.menu_item_bg, colors.menu_item_bg_gradient) + " }";
					}

					css += ".left-side .sidebar > .sidebar-menu > li.active > a { background: " + colors.menu_item_bg_active + "; color: " + colors.menu_item_txt_active + (colors.menu_item_border_active ? "; border-left: " + colors.menu_item_border_size + "px solid " + colors.menu_item_border_active : "") + " }";

					if (colors.menu_item_border_hover || colors.menu_item_border_active) {
						css += ".left-side .sidebar > .sidebar-menu > li > a { border-left: " + colors.menu_item_border_size + "px solid transparent }";
					}

					if (colors.menu_item_bg_active_gradient) {
						css += ".left-side .sidebar > .sidebar-menu > li.active > a { " + NRS.cssGradient(colors.menu_item_bg_active, colors.menu_item_bg_active_gradient) + " }";
					}

					css += ".left-side .sidebar > .sidebar-menu > li > a:hover { background: " + colors.menu_item_bg_hover + "; color: " + colors.menu_item_txt_hover + (colors.menu_item_border_hover ? "; border-left: " + colors.menu_item_border_size + "px solid " + colors.menu_item_border_hover : "") + " }";

					if (colors.menu_item_bg_hover_gradient) {
						css += ".left-side .sidebar > .sidebar-menu > li > a:hover { " + NRS.cssGradient(colors.menu_item_bg_hover, colors.menu_item_bg_hover_gradient) + " }";
					}

					css += ".sidebar .sidebar-menu .treeview-menu > li > a { background: " + colors.submenu_item_bg + "; color: " + colors.submenu_item_txt + (colors.submenu_item_top_border ? "; border-top:1px solid " + colors.submenu_item_top_border : "") + (colors.submenu_item_bottom_border ? "; border-bottom: 1px solid " + colors.submenu_item_bottom_border : "") + " }";

					if (colors.submenu_item_bg_gradient) {
						css += ".sidebar .sidebar-menu .treeview-menu > li > a { " + NRS.cssGradient(colors.submenu_item_bg, colors.submenu_item_bg_gradient) + " }";
					}

					css += ".sidebar .sidebar-menu .treeview-menu > li > a:hover { background: " + colors.submenu_item_bg_hover + "; color: " + colors.submenu_item_txt_hover + " }";

					if (colors.submenu_item_bg_hover_gradient) {
						css += ".sidebar .sidebar-menu .treeview-menu > li > a:hover { " + NRS.cssGradient(colors.submenu_item_bg_hover, colors.submenu_item_bg_hover_gradient) + " }";
					}

					break;
			}
		}

		var $style = $("#user_" + type + "_style");

		if ($style[0].styleSheet) {
			$style[0].styleSheet.cssText = css;
		} else {
			$style.text(css);
		}
	}

	$("ul.color_scheme_editor").on("click", "li a", function(e) {
		e.preventDefault();

		var color = $(this).data("color");

		var scheme = $(this).closest("ul").data("scheme");

		var $span = $(this).closest(".btn-group.colors").find("span.text");

		if (!color) {
			colorTitle = "Default";
		} else {
			var colorTitle = color.replace(/-/g, " ");
			colorTitle = colorTitle.replace(/\w\S*/g, function(txt) {
				return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
			});
		}

		$span.html(colorTitle);

		if (color == "custom") {
			color = NRS.settings[scheme + "_color"];

			if (!color) {
				color = "default"; //how??...
			}

			var style = NRS.userStyles[scheme][color];

			var output = "";

			var sorted_keys = [];

			for (var key in style) {
				sorted_keys.push(key);
			}

			sorted_keys.sort();

			var options = NRS.styleOptions[scheme];

			output = "<table class='settings'>";

			$.each(options, function(i, definition) {
				var value = "";
				var optional = false;
				var has_value = false;
				var type = "color";
				var key = "";

				if ($.isPlainObject(definition)) {
					key = definition.key;

					if (key in style) {
						value = style[key];
						has_value = true;
					}

					if ("type" in definition) {
						type = definition["type"];

						if (value === "") {
							if (type == "number") {
								value = 0;
							} else if (type == "boolean") {
								value = false;
							} else if (type == "select") {
								value = "";
							} else {
								value = "#fff";
							}
						} else {
							has_value = false;
						}
					} else {
						type = "color";
						if (value === "") {
							value = "#fff";
						} else {
							has_value = true;
						}
					}

					if ("optional" in definition) {
						optional = true;
					}
				} else {
					key = definition;
					type = "color";

					if (key in style) {
						value = style[key];
						has_value = true;
					} else {
						value = "#fff";
					}
				}

				var title = key.replace(/_/g, " ");
				title = title.replace(/\w\S*/g, function(txt) {
					return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
				});

				title = title.replace("Bg", "BG");
				title = title.replace("Txt", "Text");

				if (type == "boolean") {
					output += "<tr><td><label class='control-label' style='text-align:left;width:180px;font-weight:normal;'>" + title + "</label></td><td><div class='input-group' style='><input type='checkbox' name='" + key + "' value='1' class='form-control' " + (value ? " checked='checked'" : "") + " /></div></td></tr>";
				} else if (type == "number") {
					output += "<tr><td><label class='control-label' style='text-align:left;width:180px;font-weight:normal;'>" + title + "</label></td><td><div class='input-group' style=''><input type='" + type + "' name='" + key + "' value='" + value + "' class='form-control' style='width:140px' " + (optional && !has_value ? " disabled" : "") + " />" + (optional ? " <input type=checkbox style='margin-left:10px' class='color_scheme_enable' " + (has_value ? " checked='checked'" : "") + " />" : "") + "</div></td></tr>";
				} else if (type == "select") {
					output += "<tr><td><label class='control-label' style='text-align:left;width:180px;font-weight:normal;'>" + title + "</label></td><td><div class='input-group'><select name='" + key + "' class='form-control' style='width:140px' " + (optional && !has_value ? " disabled" : "") + ">";
					for (var i = 0; i < definition.values.length; i++) {
						output += "<option value='" + definition.values[i] + "'>" + definition.values[i] + "</option>";
					}
					output += "</select>" + (optional ? " <input type=checkbox style='margin-left:10px' class='color_scheme_enable' " + (has_value ? " checked='checked'" : "") + " />" : "") + "</div></td></tr>";
				} else {
					output += "<tr><td><label class='control-label' style='text-align:left;width:180px;font-weight:normal;'>" + title + "</label></td><td><div class='input-group color_scheme_picker'><input type='text' name='" + key + "' value='" + value + "' class='form-control'" + (optional && !has_value ? " disabled" : "") + " style='width:100px' /><span class='input-group-addon'" + (optional && !has_value ? " disabled" : "") + "><i></i></span>" + (optional ? " <input type=checkbox style='margin-left:10px' class='color_scheme_enable' " + (has_value ? " checked='checked'" : "") + " />" : "") + "</div></td></tr>";
				}
			});

			output += "</table>";

			$("#" + scheme + "_custom_scheme").empty().append(output);
			$("#" + scheme + "_custom_scheme .color_scheme_picker").colorpicker().on("changeColor", function(e) {
				NRS.updateColorScheme(e);
			});

			$("#" + scheme + "_custom_scheme_group").show();
		} else {
			$("#" + scheme + "_custom_scheme_group").hide();

			if (color) {
				NRS.updateSettings(scheme + "_color", color);
				NRS.updateStyle(scheme, color);
			} else {
				NRS.updateSettings(scheme + "_color");
				NRS.updateStyle(scheme);
			}
		}
	});

	$(".custom_color_scheme").on("change", ".color_scheme_enable", function(e) {
		e.preventDefault();

		var $field = $(this).closest(".input-group").find(":input.form-control", 0);
		var $color = $(this).closest(".input-group").find("span.input-group-addon", 0);

		$field.prop("disabled", !this.checked);
		if ($color) {
			$color.prop("disabled", !this.checked);
		}

		NRS.updateColorScheme(e);
	});

	$("#settings_box select").on("change", function(e) {
		e.preventDefault();

		var key = $(this).attr("name");
		var value = parseInt($(this).val(), 10);

		NRS.updateSettings(key, value);
	});

	$("#settings_box input[type=text]").on("input", function(e) {
		var key = $(this).attr("name");
		var value = $(this).val();

		if (/_warning/i.test(key) && key != "asset_transfer_warning") {
			value = NRS.convertToNQT(value);
		}
		NRS.updateSettings(key, value);
	});

	NRS.updateColorScheme = function(e) {
		var $color_scheme = $(e.target).closest(".custom_color_scheme");

		var scheme = $color_scheme.data("scheme");

		var $inputs = $color_scheme.find(":input:enabled");

		var values = {};

		$inputs.each(function() {
			values[this.name] = $(this).val();
		});

		NRS.updateStyle(scheme, values);
	}

	NRS.getSettings = function() {
		if (NRS.databaseSupport) {
			NRS.database.select("data", [{
				"id": "settings"
			}], function(error, result) {
				if (result.length) {
					NRS.settings = $.extend({}, NRS.defaultSettings, JSON.parse(result[0].contents));
				} else {
					NRS.database.insert("data", {
						id: "settings",
						contents: "{}"
					});
					NRS.settings = NRS.defaultSettings;
				}
				NRS.applySettings();
			});
		} else {
			if (NRS.hasLocalStorage) {
				NRS.settings = $.extend({}, NRS.defaultSettings, JSON.parse(localStorage.getItem("settings")));
			} else {
				NRS.settings = NRS.defaultSettings;
			}
			NRS.applySettings();
		}
	}

	NRS.applySettings = function(key) {
		if (!key || key == "submit_on_enter") {
			if (NRS.settings["submit_on_enter"]) {
				$(".modal form").on("submit.onEnter", function(e) {
					e.preventDefault();
					NRS.submitForm($(this).closest(".modal"));
				});
			} else {
				$(".modal form").off("submit.onEnter");
			}
		}

		if (!key || key == "animate_forging") {
			if (NRS.settings["animate_forging"]) {
				$("#forging_indicator").addClass("animated");
			} else {
				$("#forging_indicator").removeClass("animated");
			}
		}

		if (!key || key == "news") {
			if (NRS.settings["news"] == 0) {
				$("#news_link").hide();
			} else if (NRS.settings["news"] == 1) {
				$("#news_link").show();
			}
		}

		if (!key || key == "reed_solomon") {
			if (NRS.settings["reed_solomon"]) {
				$("#account_id_prefix").hide();
				$("#account_id").html(NRS.getAccountFormatted(NRS.accountRS)).css("font-size", "12px");
				$("body").addClass("reed_solomon");
				$("#message_sidebar").css("width", "245px");
				$("#message_content").css("left", "245px");
				$("#inline_message_form").css("left", "485px");
			} else {
				$("#account_id_prefix").show();
				$("#account_id").html(NRS.getAccountFormatted(NRS.account)).css("font-size", "14px");
				$("body").removeClass("reed_solomon");
				$("#message_sidebar").css("width", "200px");
				$("#message_content").css("left", "200px");
				$("#inline_message_form").css("left", "440px");
			}

			var $dashboard_account_links = $("#dashboard_transactions_table a.user_info");

			$.each($dashboard_account_links, function(key, value) {
				if (NRS.settings["reed_solomon"]) {
					var account = $(this).data("user-rs");
				} else {
					var account = $(this).data("user-id");
				}

				$(this).data("user", account);
				$(this).html(String(account).escapeHTML());
			});

			//todo: wider message sidebar
		}
	}

	NRS.updateSettings = function(key, value) {
		if (key) {
			NRS.settings[key] = value;
		}

		if (NRS.databaseSupport) {
			NRS.database.update("data", {
				contents: JSON.stringify(NRS.settings)
			}, [{
				id: "settings"
			}]);
		} else if (NRS.hasLocalStorage) {
			localStorage.setItem("settings", JSON.stringify(NRS.settings));
		}

		NRS.applySettings(key);
	}

	return NRS;
}(NRS || {}, jQuery));