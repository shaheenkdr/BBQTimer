#!/bin/sh
# Broadcasts a standard Intent to the app to test its response.
#
# Cf. http://developer.android.com/tools/help/shell.html#IntentSpec
#
# Arguments:
#   $1 -- optional Intent action like TIME_SET or TIMEZONE_CHANGED.
#         Defaults to MY_PACKAGE_REPLACED.
#   $2 -- optional flags like "-d" to force it to use an ADB USB Device.

ACTION="android.intent.action.${1:-MY_PACKAGE_REPLACED}"
PACKAGE="com.onefishtwo.bbqtimer"
echo Broadcasting ${ACTION} to ${PACKAGE}

adb "${1:--e}" shell am broadcast -a "${ACTION}" "${PACKAGE}"