// i18next, v1.7.3
// Copyright (c)2014 Jan Mühlemann (jamuhl).
// Distributed under MIT license
// http://i18next.com
(function() {
	//removed stuff that we don't need

	var root = this,
		$ = root.jQuery,
		i18n = {}, resStore = {}, currentLng, replacementCounter = 0,
		languages = [],
		initialized = false;


	$.i18n = $.i18n || i18n;

	// defaults
	var o = {
		lng: undefined,
		load: 'all',
		lowerCaseLng: false,
		returnObjectTrees: false,
		fallbackLng: ['dev'],
		fallbackNS: [],
		detectLngQS: 'setLng',
		detectLngFromLocalStorage: false,
		ns: 'translation',
		fallbackOnNull: true,
		fallbackOnEmpty: false,
		fallbackToDefaultNS: false,
		nsseparator: ':',
		keyseparator: '.',
		selectorAttr: 'data-i18n',
		debug: false,

		resGetPath: 'locales/__lng__/__ns__.json',
		resPostPath: 'locales/add/__lng__/__ns__',

		getAsync: true,
		postAsync: true,

		resStore: undefined,

		interpolationPrefix: '__',
		interpolationSuffix: '__',
		reusePrefix: '$t(',
		reuseSuffix: ')',
		pluralSuffix: '_plural',
		pluralNotFound: ['plural_not_found', Math.random()].join(''),
		contextNotFound: ['context_not_found', Math.random()].join(''),
		escapeInterpolation: false,

		setJqueryExt: true,
		defaultValueFromContent: true,
		useDataAttrOptions: false,

		objectTreeKeyHandler: undefined,
		parseMissingKey: undefined,

		shortcutFunction: 'defaultValue'
	};

	function _extend(target, source) {
		if (!source || typeof source === 'function') {
			return target;
		}

		for (var attr in source) {
			target[attr] = source[attr];
		}
		return target;
	}

	function _each(object, callback, args) {
		var name, i = 0,
			length = object.length,
			isObj = length === undefined || Object.prototype.toString.apply(object) !== '[object Array]' || typeof object === "function";

		if (args) {
			if (isObj) {
				for (name in object) {
					if (callback.apply(object[name], args) === false) {
						break;
					}
				}
			} else {
				for (; i < length;) {
					if (callback.apply(object[i++], args) === false) {
						break;
					}
				}
			}

			// A special, fast, case for the most common use of each
		} else {
			if (isObj) {
				for (name in object) {
					if (callback.call(object[name], name, object[name]) === false) {
						break;
					}
				}
			} else {
				for (; i < length;) {
					if (callback.call(object[i], i, object[i++]) === false) {
						break;
					}
				}
			}
		}

		return object;
	}

	var _entityMap = {
		"&": "&amp;",
		"<": "&lt;",
		">": "&gt;",
		'"': '&quot;',
		"'": '&#39;',
		"/": '&#x2F;'
	};

	function _escape(data) {
		if (typeof data === 'string') {
			return data.replace(/[&<>"'\/]/g, function(s) {
				return _entityMap[s];
			});
		} else {
			return data;
		}
	}

	// move dependent functions to a container so that
	// they can be overriden easier in no jquery environment (node.js)
	var f = {
		extend: $ ? $.extend : _extend,
		each: $ ? $.each : _each,
		ajax: $ ? $.ajax : (typeof document !== 'undefined' ? _ajax : function() {}),
		detectLanguage: detectLanguage,
		escape: _escape,
		log: function(str) {
			if (o.debug && typeof console !== "undefined") console.log(str);
		},
		toLanguages: function(lng) {
			var languages = [];
			if (typeof lng === 'string' && lng.indexOf('-') > -1) {
				var parts = lng.split('-');

				lng = o.lowerCaseLng ?
					parts[0].toLowerCase() + '-' + parts[1].toLowerCase() :
					parts[0].toLowerCase() + '-' + parts[1].toUpperCase();

				if (o.load !== 'unspecific') languages.push(lng);
				if (o.load !== 'current') languages.push(parts[0]);
			} else {
				languages.push(lng);
			}

			for (var i = 0; i < o.fallbackLng.length; i++) {
				if (languages.indexOf(o.fallbackLng[i]) === -1 && o.fallbackLng[i]) languages.push(o.fallbackLng[i]);
			}

			return languages;
		},
		regexEscape: function(str) {
			return str.replace(/[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|]/g, "\\$&");
		}
	};

	function init(options, cb) {

		if (typeof options === 'function') {
			cb = options;
			options = {};
		}
		options = options || {};

		// override defaults with passed in options
		f.extend(o, options);
		delete o.fixLng; /* passed in each time */

		// create namespace object if namespace is passed in as string
		if (typeof o.ns == 'string') {
			o.ns = {
				namespaces: [o.ns],
				defaultNs: o.ns
			};
		}

		// fallback namespaces
		if (typeof o.fallbackNS == 'string') {
			o.fallbackNS = [o.fallbackNS];
		}

		// fallback languages
		if (typeof o.fallbackLng == 'string' || typeof o.fallbackLng == 'boolean') {
			o.fallbackLng = [o.fallbackLng];
		}

		// escape prefix/suffix
		o.interpolationPrefixEscaped = f.regexEscape(o.interpolationPrefix);
		o.interpolationSuffixEscaped = f.regexEscape(o.interpolationSuffix);

		if (!o.lng) o.lng = f.detectLanguage();

		languages = f.toLanguages(o.lng);
		currentLng = languages[0];
		f.log('currentLng set to: ' + currentLng);

		if (o.detectLngFromLocalStorage && typeof document !== 'undefined' && window.localstorage) {
			window.localStorage.setItem('i18next_lng', currentLng);
		}

		var lngTranslate = translate;
		if (options.fixLng) {
			lngTranslate = function(key, options) {
				options = options || {};
				options.lng = options.lng || lngTranslate.lng;
				return translate(key, options);
			};
			lngTranslate.lng = currentLng;
		}

		pluralExtensions.setCurrentLng(currentLng);

		// add JQuery extensions
		if ($ && o.setJqueryExt) addJqueryFunct();

		// jQuery deferred
		var deferred;
		if ($ && $.Deferred) {
			deferred = $.Deferred();
		}

		// return immidiatly if res are passed in
		if (o.resStore) {
			resStore = o.resStore;
			initialized = true;
			if (cb) cb(lngTranslate);
			if (deferred) deferred.resolve(lngTranslate);
			if (deferred) return deferred.promise();
			return;
		}

		// languages to load
		var lngsToLoad = f.toLanguages(o.lng);

		// else load them
		i18n.sync.load(lngsToLoad, o, function(err, store) {
			resStore = store;
			initialized = true;

			if (cb) cb(lngTranslate);
			if (deferred) deferred.resolve(lngTranslate);
		});

		if (deferred) return deferred.promise();
	}

	function setLng(lng, options, cb) {
		if (typeof options === 'function') {
			cb = options;
			options = {};
		} else if (!options) {
			options = {};
		}

		options.lng = lng;
		return init(options, cb);
	}

	function lng() {
		return currentLng;
	}

	function addJqueryFunct() {
		// $.t shortcut
		$.t = $.t || translate;

		function parse(ele, key, options) {
			if (key.length === 0) return;

			var attr = 'text';

			if (key.indexOf('[') === 0) {
				var parts = key.split(']');
				key = parts[1];
				attr = parts[0].substr(1, parts[0].length - 1);
			}

			if (key.indexOf(';') === key.length - 1) {
				key = key.substr(0, key.length - 2);
			}

			var optionsToUse;
			if (attr === 'html') {
				optionsToUse = o.defaultValueFromContent ? $.extend({
					defaultValue: ele.html()
				}, options) : options;
				ele.html($.t(key, optionsToUse));
			} else if (attr === 'text') {
				optionsToUse = o.defaultValueFromContent ? $.extend({
					defaultValue: ele.text()
				}, options) : options;
				ele.text($.t(key, optionsToUse));
			} else if (attr === 'prepend') {
				optionsToUse = o.defaultValueFromContent ? $.extend({
					defaultValue: ele.html()
				}, options) : options;
				ele.prepend($.t(key, optionsToUse));
			} else if (attr === 'append') {
				optionsToUse = o.defaultValueFromContent ? $.extend({
					defaultValue: ele.html()
				}, options) : options;
				ele.append($.t(key, optionsToUse));
			} else if (attr.indexOf("data-") === 0) {
				var dataAttr = attr.substr(("data-").length);
				optionsToUse = o.defaultValueFromContent ? $.extend({
					defaultValue: ele.data(dataAttr)
				}, options) : options;
				var translated = $.t(key, optionsToUse);
				//we change into the data cache
				ele.data(dataAttr, translated);
				//we change into the dom
				ele.attr(attr, translated);
			} else {
				optionsToUse = o.defaultValueFromContent ? $.extend({
					defaultValue: ele.attr(attr)
				}, options) : options;
				ele.attr(attr, $.t(key, optionsToUse));
			}
		}

		function localize(ele, options) {
			var key = ele.attr(o.selectorAttr);

			if (!key && typeof key !== 'undefined' && key !== false) key = ele.text() || ele.val();
			if (!key) return;

			var target = ele,
				targetSelector = ele.data("i18n-target");
			if (targetSelector) {
				target = ele.find(targetSelector) || ele;
			}

			if (!options && o.useDataAttrOptions === true) {
				options = ele.data("i18n-options");
			}
			options = options || {};

			if (key.indexOf(';') >= 0) {
				var keys = key.split(';');

				$.each(keys, function(m, k) {
					if (k !== '') parse(target, k, options);
				});

			} else {
				parse(target, key, options);
			}

			if (o.useDataAttrOptions === true) ele.data("i18n-options", options);
		}

		// fn
		$.fn.i18n = function(options) {
			return this.each(function() {
				// localize element itself
				localize($(this), options);

				// localize childs
				var elements = $(this).find('[' + o.selectorAttr + ']');
				elements.each(function() {
					localize($(this), options);
				});
			});
		};
	}

	function applyReplacement(str, replacementHash, nestedKey, options) {
		if (!str) return str;

		options = options || replacementHash; // first call uses replacement hash combined with options
		if (str.indexOf(options.interpolationPrefix || o.interpolationPrefix) < 0) return str;

		var prefix = options.interpolationPrefix ? f.regexEscape(options.interpolationPrefix) : o.interpolationPrefixEscaped,
			suffix = options.interpolationSuffix ? f.regexEscape(options.interpolationSuffix) : o.interpolationSuffixEscaped,
			unEscapingSuffix = 'HTML' + suffix;

		f.each(replacementHash, function(key, value) {
			var nextKey = nestedKey ? nestedKey + o.keyseparator + key : key;
			if (typeof value === 'object' && value !== null) {
				str = applyReplacement(str, value, nextKey, options);
			} else {
				if (options.escapeInterpolation || o.escapeInterpolation) {
					str = str.replace(new RegExp([prefix, nextKey, unEscapingSuffix].join(''), 'g'), value);
					str = str.replace(new RegExp([prefix, nextKey, suffix].join(''), 'g'), f.escape(value));
				} else {
					str = str.replace(new RegExp([prefix, nextKey, suffix].join(''), 'g'), value);
				}
				// str = options.escapeInterpolation;
			}
		});
		return str;
	}

	// append it to functions
	f.applyReplacement = applyReplacement;

	function applyReuse(translated, options) {
		var comma = ',';
		var options_open = '{';
		var options_close = '}';

		var opts = f.extend({}, options);

		while (translated.indexOf(o.reusePrefix) != -1) {
			replacementCounter++;
			if (replacementCounter > o.maxRecursion) {
				break;
			} // safety net for too much recursion
			var index_of_opening = translated.lastIndexOf(o.reusePrefix);
			var index_of_end_of_closing = translated.indexOf(o.reuseSuffix, index_of_opening) + o.reuseSuffix.length;
			var token = translated.substring(index_of_opening, index_of_end_of_closing);
			var token_without_symbols = token.replace(o.reusePrefix, '').replace(o.reuseSuffix, '');


			if (token_without_symbols.indexOf(comma) != -1) {
				var index_of_token_end_of_closing = token_without_symbols.indexOf(comma);
				if (token_without_symbols.indexOf(options_open, index_of_token_end_of_closing) != -1 && token_without_symbols.indexOf(options_close, index_of_token_end_of_closing) != -1) {
					var index_of_opts_opening = token_without_symbols.indexOf(options_open, index_of_token_end_of_closing);
					var index_of_opts_end_of_closing = token_without_symbols.indexOf(options_close, index_of_opts_opening) + options_close.length;
					try {
						opts = f.extend(opts, JSON.parse(token_without_symbols.substring(index_of_opts_opening, index_of_opts_end_of_closing)));
						token_without_symbols = token_without_symbols.substring(0, index_of_token_end_of_closing);
					} catch (e) {}
				}
			}

			var translated_token = _translate(token_without_symbols, opts);
			translated = translated.replace(token, translated_token);
		}
		return translated;
	}

	function hasContext(options) {
		return (options.context && (typeof options.context == 'string' || typeof options.context == 'number'));
	}

	function needsPlural(options) {
		if (options.count == undefined) {
			return false;
		} else if (typeof options.count == 'string') {
			if (options.count !== "1") {
				return true;
			} else {
				return false;
			}
		} else {
			return (options.count !== 1);
		}
	}

	function exists(key, options) {
		options = options || {};

		var notFound = _getDefaultValue(key, options),
			found = _find(key, options);

		return found !== undefined || found === notFound;
	}

	function translate(key, options) {
		options = options || {};

		if (!initialized) {
			f.log('i18next not finished initialization. you might have called t function before loading resources finished.')
			return options.defaultValue || '';
		};
		replacementCounter = 0;
		return _translate.apply(null, arguments);
	}

	function _getDefaultValue(key, options) {
		return (options.defaultValue !== undefined) ? options.defaultValue : key;
	}

	function _translate(potentialKeys, options) {
		if (options && typeof options !== 'object') {
			if (o.shortcutFunction === 'defaultValue') {
				options = {
					defaultValue: options
				}
			}
		} else {
			options = options || {};
		}

		if (potentialKeys === undefined || potentialKeys === null) return '';

		if (typeof potentialKeys == 'string') {
			potentialKeys = [potentialKeys];
		}

		var key = potentialKeys[0];

		if (potentialKeys.length > 1) {
			for (var i = 0; i < potentialKeys.length; i++) {
				key = potentialKeys[i];
				if (exists(key, options)) {
					break;
				}
			}
		}

		var notFound = _getDefaultValue(key, options),
			found = _find(key, options),
			lngs = options.lng ? f.toLanguages(options.lng) : languages,
			ns = options.ns || o.ns.defaultNs,
			parts;

		// split ns and key
		if (key.indexOf(o.nsseparator) > -1) {
			parts = key.split(o.nsseparator);
			ns = parts[0];
			key = parts[1];
		}

		// process notFound if function exists
		var splitNotFound = notFound;
		if (notFound.indexOf(o.nsseparator) > -1) {
			parts = notFound.split(o.nsseparator);
			splitNotFound = parts[1];
		}
		if (splitNotFound === key && o.parseMissingKey) {
			notFound = o.parseMissingKey(notFound);
		}

		if (found === undefined) {
			notFound = applyReplacement(notFound, options);
			notFound = applyReuse(notFound, options);
		}

		return (found !== undefined) ? found : notFound;
	}

	function _find(key, options) {
		options = options || {};

		var optionWithoutCount, translated, notFound = _getDefaultValue(key, options),
			lngs = languages;

		if (!resStore) {
			return notFound;
		} // no resStore to translate from

		// CI mode
		if (lngs[0].toLowerCase() === 'cimode') return notFound;

		// passed in lng
		if (options.lng) {
			lngs = f.toLanguages(options.lng);

			if (!resStore[lngs[0]]) {
				var oldAsync = o.getAsync;
				o.getAsync = false;

				i18n.sync.load(lngs, o, function(err, store) {
					f.extend(resStore, store);
					o.getAsync = oldAsync;
				});
			}
		}

		var ns = options.ns || o.ns.defaultNs;
		if (key.indexOf(o.nsseparator) > -1) {
			var parts = key.split(o.nsseparator);
			ns = parts[0];
			key = parts[1];
		}

		if (hasContext(options)) {
			optionWithoutCount = f.extend({}, options);
			delete optionWithoutCount.context;
			optionWithoutCount.defaultValue = o.contextNotFound;

			var contextKey = ns + o.nsseparator + key + '_' + options.context;

			translated = translate(contextKey, optionWithoutCount);
			if (translated != o.contextNotFound) {
				return applyReplacement(translated, {
					context: options.context
				}); // apply replacement for context only
			} // else continue translation with original/nonContext key
		}

		if (needsPlural(options)) {
			optionWithoutCount = f.extend({}, options);
			delete optionWithoutCount.count;
			optionWithoutCount.defaultValue = o.pluralNotFound;

			var pluralKey = ns + o.nsseparator + key + o.pluralSuffix;
			var pluralExtension = pluralExtensions.get(lngs[0], options.count);
			if (pluralExtension >= 0) {
				var newPluralKey = pluralKey + '_' + pluralExtension;
				if (exists(newPluralKey)) {
					pluralKey = newPluralKey;
				}
			} else if (pluralExtension === 1) {
				pluralKey = ns + o.nsseparator + key; // singular
			}

			translated = translate(pluralKey, optionWithoutCount);
			if (translated != o.pluralNotFound) {
				return applyReplacement(translated, {
					count: options.count,
					interpolationPrefix: options.interpolationPrefix,
					interpolationSuffix: options.interpolationSuffix
				}); // apply replacement for count only
			} // else continue translation with original/singular key
		}

		var found;
		var keys = key.split(o.keyseparator);
		for (var i = 0, len = lngs.length; i < len; i++) {
			if (found !== undefined) break;

			var l = lngs[i];

			var x = 0;
			var value = resStore[l] && resStore[l][ns];
			while (keys[x]) {
				value = value && value[keys[x]];
				x++;
			}
			if (value !== undefined) {
				var valueType = Object.prototype.toString.apply(value);
				if (typeof value === 'string') {
					value = applyReplacement(value, options);
					value = applyReuse(value, options);
				} else if (valueType === '[object Array]' && !o.returnObjectTrees && !options.returnObjectTrees) {
					value = value.join('\n');
					value = applyReplacement(value, options);
					value = applyReuse(value, options);
				} else if (value === null && o.fallbackOnNull === true) {
					value = undefined;
				} else if (value !== null) {
					if (!o.returnObjectTrees && !options.returnObjectTrees) {
						if (o.objectTreeKeyHandler && typeof o.objectTreeKeyHandler == 'function') {
							value = o.objectTreeKeyHandler(key, value, l, ns, options);
						} else {
							value = 'key \'' + ns + ':' + key + ' (' + l + ')\' ' +
								'returned an object instead of string.';
							f.log(value);
						}
					} else if (valueType !== '[object Number]' && valueType !== '[object Function]' && valueType !== '[object RegExp]') {
						var copy = (valueType === '[object Array]') ? [] : {}; // apply child translation on a copy
						f.each(value, function(m) {
							copy[m] = _translate(ns + o.nsseparator + key + o.keyseparator + m, options);
						});
						value = copy;
					}
				}

				if (typeof value === 'string' && value.trim() === '' && o.fallbackOnEmpty === true)
					value = undefined;

				found = value;
			}
		}

		if (found === undefined && !options.isFallbackLookup && (o.fallbackToDefaultNS === true || (o.fallbackNS && o.fallbackNS.length > 0))) {
			// set flag for fallback lookup - avoid recursion
			options.isFallbackLookup = true;

			if (o.fallbackNS.length) {

				for (var y = 0, lenY = o.fallbackNS.length; y < lenY; y++) {
					found = _find(o.fallbackNS[y] + o.nsseparator + key, options);

					if (found) {
						/* compare value without namespace */
						var foundValue = found.indexOf(o.nsseparator) > -1 ? found.split(o.nsseparator)[1] : found,
							notFoundValue = notFound.indexOf(o.nsseparator) > -1 ? notFound.split(o.nsseparator)[1] : notFound;

						if (foundValue !== notFoundValue) break;
					}
				}
			} else {
				found = _find(key, options); // fallback to default NS
			}
		}

		return found;
	}

	function detectLanguage() {
		var detectedLng;

		// get from qs
		var qsParm = [];
		if (typeof window !== 'undefined') {
			(function() {
				var query = window.location.search.substring(1);
				var parms = query.split('&');
				for (var i = 0; i < parms.length; i++) {
					var pos = parms[i].indexOf('=');
					if (pos > 0) {
						var key = parms[i].substring(0, pos);
						var val = parms[i].substring(pos + 1);
						qsParm[key] = val;
					}
				}
			})();
			if (qsParm[o.detectLngQS]) {
				detectedLng = qsParm[o.detectLngQS];
			}
		}

		// get from localstorage
		if (!detectedLng && typeof document !== 'undefined' && window.localstorage && o.detectLngFromLocalStorage) {
			detectedLng = window.localStorage.getItem('i18next_lng');
		}

		// get from navigator
		if (!detectedLng && typeof navigator !== 'undefined') {
			detectedLng = (navigator.language) ? navigator.language : navigator.userLanguage;
		}

		//fallback
		if (!detectedLng) {
			detectedLng = o.fallbackLng[0];
		}

		if (detectedLng.indexOf("en-") == 0) {
			detectedLng = "en";
		} else if (detectedLng.indexOf("zh-") == 0 && detectedLng != "zh-tw") {
			detectedLng = "zh";
		}

		return detectedLng;
	}
	var sync = {

		load: function(lngs, options, cb) {
			sync._fetch(lngs, options, function(err, store) {
				cb(null, store);
			});
		},

		_fetch: function(lngs, options, cb) {
			var ns = options.ns,
				store = {};

			var todo = ns.namespaces.length * lngs.length,
				errors;

			// load each file individual
			f.each(ns.namespaces, function(nsIndex, nsValue) {
				f.each(lngs, function(lngIndex, lngValue) {

					// Call this once our translation has returned.
					var loadComplete = function(err, data) {
						if (err) {
							errors = errors || [];
							errors.push(err);
						}
						store[lngValue] = store[lngValue] || {};
						store[lngValue][nsValue] = data;

						todo--; // wait for all done befor callback
						if (todo === 0) cb(errors, store);
					};

					if (typeof options.customLoad == 'function') {
						// Use the specified custom callback.
						options.customLoad(lngValue, nsValue, options, loadComplete);
					} else {
						//~ // Use our inbuilt sync.
						sync._fetchOne(lngValue, nsValue, options, loadComplete);
					}
				});
			});
		},

		_fetchOne: function(lng, ns, options, done) {
			var url = applyReplacement(options.resGetPath, {
				lng: lng,
				ns: ns
			});
			f.ajax({
				url: url,
				success: function(data, status, xhr) {
					f.log('loaded: ' + url);
					done(null, data);
				},
				error: function(xhr, status, error) {
					if ((status && status == 200) || (xhr && xhr.status && xhr.status == 200)) {
						// file loaded but invalid json, stop waste time !
						f.log('There is a typo in: ' + url);
					} else if ((status && status == 404) || (xhr && xhr.status && xhr.status == 404)) {
						f.log('Does not exist: ' + url);
					} else {
						var theStatus = status ? status : ((xhr && xhr.status) ? xhr.status : null);
						f.log(theStatus + ' when loading ' + url);
					}

					done(error, {});
				},
				dataType: "json",
				async: options.getAsync
			});
		},
	};
	// definition http://translate.sourceforge.net/wiki/l10n/pluralforms
	var pluralExtensions = {

		rules: {
			"de": {
				"name": "German",
				"numbers": [
					1,
					2
				],
				"plurals": function(n) {
					return Number(n != 1);
				}
			},
			"en": {
				"name": "English",
				"numbers": [
					1,
					2
				],
				"plurals": function(n) {
					return Number(n != 1);
				}
			},
			"es": {
				"name": "Spanish",
				"numbers": [
					1,
					2
				],
				"plurals": function(n) {
					return Number(n != 1);
				}
			},
			"fr": {
				"name": "French",
				"numbers": [
					1,
					2
				],
				"plurals": function(n) {
					return Number(n > 1);
				}
			},
			"hr": {
				"name": "Croatian",
				"numbers": [
					1,
					2,
					5
				],
				"plurals": function(n) {
					return Number(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2);
				}
			},
			"ja": {
				"name": "Japanese",
				"numbers": [
					1
				],
				"plurals": function(n) {
					return 0;
				}
			},
			"lt": {
				"name": "Lithuanian",
				"numbers": [
					1,
					2,
					10
				],
				"plurals": function(n) {
					return Number(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2);
				}
			},
			"sk": {
				"name": "Slovak",
				"numbers": [
					1,
					2,
					5
				],
				"plurals": function(n) {
					return Number((n == 1) ? 0 : (n >= 2 && n <= 4) ? 1 : 2);
				}
			},
			"sr": {
				"name": "Serbian",
				"numbers": [
					1,
					2,
					5
				],
				"plurals": function(n) {
					return Number(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2);
				}
			},
			"ru": {
				"name": "Russian",
				"numbers": [
					1,
					2,
					5
				],
				"plurals": function(n) {
					return Number(n % 10 == 1 && n % 100 != 11 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2);
				}
			},
			"zh": {
				"name": "Chinese",
				"numbers": [
					1
				],
				"plurals": function(n) {
					return 0;
				}
			}
		},

		setCurrentLng: function(lng) {
			if (!pluralExtensions.currentRule || pluralExtensions.currentRule.lng !== lng) {
				var parts = lng.split('-');

				pluralExtensions.currentRule = {
					lng: lng,
					rule: pluralExtensions.rules[parts[0]]
				};
			}
		},

		get: function(lng, count) {
			var parts = lng.split('-');

			function getResult(l, c) {
				var ext;
				if (pluralExtensions.currentRule && pluralExtensions.currentRule.lng === lng) {
					ext = pluralExtensions.currentRule.rule;
				} else {
					ext = pluralExtensions.rules[l];
				}
				if (ext) {
					var i = ext.plurals(c);
					var number = ext.numbers[i];
					if (ext.numbers.length === 2 && ext.numbers[0] === 1) {
						if (number === 2) {
							number = -1; // regular plural
						} else if (number === 1) {
							number = 1; // singular
						}
					}
					return number;
				} else {
					return c === 1 ? '1' : '-1';
				}
			}

			return getResult(parts[0], count);
		}

	};

	// public api interface
	i18n.init = init;
	i18n.setLng = setLng;
	i18n.t = translate;
	i18n.translate = translate;
	i18n.exists = exists;
	i18n.detectLanguage = f.detectLanguage;
	i18n.pluralExtensions = pluralExtensions;
	i18n.sync = sync;
	i18n.functions = f;
	i18n.lng = lng;
	i18n.options = o;

})();