#!/bin/bash
# Extracts strings from <ib:message> tags and prints them
# to stdout in .po format (the .po header is not printed).
# Limitations:
#  * Only <ib:message key="....."/> and <ib:message key='.....'/>
#    are supported, <ib:message>.....</ib:message> is not.
#  * The key="....." parameter must be on one line.
#
# How it works:
#  * The while loop greps all ib:message lines, extracts the
#    "key" parameter, and prints to stdout
#  * The first awk removes duplicate message keys
#  * the second awk makes a .po entry for each message key
# 
find "$@" -name '*.jsp' -o -name '*.tag' | while read i; do
  echo "#################### $i ####################";
  grep "<ib:message key=\"" "$i" | sed "s#.*\<ib:message key=\""## | sed "s#\".*##"; grep "<ib:message key='" "$i" | sed "s#.*\<ib:message key='"## | sed "s#'.*##" | sort
done | awk ' !x[$0]++' | awk '{ if (index($0, "###")==0) { print ("msgid \""$0"\""); print ("msgstr \"\""); print(""); } else { print $0; } }';
