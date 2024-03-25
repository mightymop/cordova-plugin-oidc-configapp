var exec = require('cordova/exec');
var PLUGIN_NAME = 'oidcconfigapp';

var oidcconfigapp = {
	version: function (success, error) {
		exec(success, error, PLUGIN_NAME, 'version', []);
	},
};

module.exports = oidcconfigapp;