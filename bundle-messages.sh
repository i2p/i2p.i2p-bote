#!/bin/sh
#
# Update messages_xx.po and messages_xx.class files,
# from both java and jsp sources.
# Requires installed programs xgettext, msgfmt, msgmerge, msguniq, find,
# and the Java class i2p.bote.ant.JspStrings.
# On Linux, the programs xgettext and msg* can be found in the gettext package.
#
# usage:
#    bundle-messages.sh (generates the resource bundle from the .po file)
#    bundle-messages.sh -p (updates the .po file from the source tags, then generates the resource bundle)
#
# zzz - public domain
#
JAVA=java
CLASS=i2p.bote.locale.Messages
TMPFILE=ant_build/javafiles.txt
export TZ=UTC

if [ "$1" = "-p" ]
then
	POUPDATE=1
fi

# add ../java/ so the refs will work in the po file
JPATHS="src"
JSPPATHS="WebContent"
for i in locale/messages_*.po
do
	# get language
	LG=${i#locale/messages_}
	LG=${LG%.po}

	if [ "$POUPDATE" = "1" ]
	then
		# make list of java files newer than the .po file
		find $JPATHS -name *.java -newer $i > $TMPFILE
        find $JSPPATHS -name *.java -newer $i >> $TMPFILE
	fi

	if [ -s ant_build/classes/i2p/bote/locale/messages_$LG.class -a \
	     ant_build/classes/i2p/bote/locale/messages_$LG.class -nt $i -a \
	     ! -s $TMPFILE ]
	then
		continue
	fi

	if [ "$POUPDATE" = "1" ]
	then
	 	echo "Updating the $i file from the tags..."
		# extract strings from java and jsp files, and update messages.po files
		# translate calls must be one of the forms:
		# _("foo")
		# _x("foo")
		# To start a new translation, copy the header from an old translation to the new .po file,
		# then ant distclean poupdate.
		find $JPATHS -name *.java > $TMPFILE
		xgettext -f $TMPFILE -F -L java --from-code=UTF-8 \
	                 --keyword=_ --keyword=_x \
		         -o ${i}t
		if [ $? -ne 0 ]
		then
			echo 'Warning - xgettext failed, not updating translations'
			rm -f ${i}t
			break
		fi
		
		# extract strings from jsp files
                $JAVA -cp ant_build/classes i2p.bote.ant.JspStrings WebContent >> ${i}t
		if [ $? -ne 0 ]
		then
			echo 'Warning - JspStrings failed, not updating translations'
			rm -f ${i}t
			break
		fi

		msguniq -o ${i}t ${i}t
		msgmerge -U --backup=none $i ${i}t
		if [ $? -ne 0 ]
		then
			echo 'Warning - msgmerge failed, not updating translations'
			rm -f ${i}t
			break
		fi
		rm -f ${i}t
		# so we don't do this again
		touch $i
	fi

	echo "Generating ${CLASS}_$LG ResourceBundle..."

	# convert to class files in ant_build/classes
	msgfmt --java --statistics -r $CLASS -l $LG -d ant_build/classes $i
	if [ $? -ne 0 ]
	then
		echo 'Warning - msgfmt failed, not updating translations'
		break
	fi
done
rm -f $TMPFILE
# todo: return failure
exit 0
