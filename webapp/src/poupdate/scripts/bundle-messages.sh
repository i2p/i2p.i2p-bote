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
RC=0

if [ "$1" = "-p" ]
then
	POUPDATE=1
fi

# on windows, one must specify the path of commnad find
# since windows has its own version of find.
if which find|grep -q -i windows ; then
	export PATH=.:/bin:/usr/local/bin:$PATH
fi
# Fast mode - update ondemond
echo setting the environment variable LG2={LangCode}
echo will limit .po file update to the language specified by {LangCode}.

# add ../java/ so the refs will work in the po file
JPATHS="src/main/java"
JSPPATHS="src/main/webapp"
for i in locale/messages_*.po
do
	# get language
	LG=${i#locale/messages_}
	LG=${LG%.po}

	# skip, if specified
	if [ $LG2 ]; then
		[ $LG != $LG2 ] && continue || echo INFO: Language update is set to [$LG2] only.
	fi

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
		# _t("foo")
		# _x("foo")
		# To start a new translation, copy the header from an old translation to the new .po file,
		# then ant distclean poupdate.
		find $JPATHS -name *.java > $TMPFILE
		xgettext -f $TMPFILE -F -L java --from-code=UTF-8 \
	                 --keyword=_t --keyword=_x \
		         -o ${i}t
		if [ $? -ne 0 ]
		then
			echo "ERROR - xgettext failed on ${i}, not updating translations"
			rm -f ${i}t
			RC=1
			break
		fi
		
		# extract strings from jsp files
		$JAVA -cp ant_build/classes:$I2P/lib/i2p.jar i2p.bote.ant.JspStrings $JSPPATHS >> ${i}t
		if [ $? -ne 0 ]
		then
			echo "ERROR - JspStrings failed on ${i}, not updating translations"
			rm -f ${i}t
			RC=1
			break
		fi

		msguniq -o ${i}t ${i}t
		msgmerge -U --backup=none $i ${i}t
		if [ $? -ne 0 ]
		then
			echo "ERROR - msgmerge failed on ${i}, not updating translations"
			rm -f ${i}t
			RC=1
			break
		fi
		rm -f ${i}t
		# so we don't do this again
		touch $i
	fi

    if [ "$LG" != "en" ]
    then
        # only generate for non-source language
        echo "Generating ${CLASS}_$LG ResourceBundle..."

        msgfmt -V | grep -q '0\.19'
        if [ $? -ne 0 ]
        then
            # slow way
            # convert to class files in ant_build/classes
            msgfmt --java --statistics -r $CLASS -l $LG -d ant_build/classes $i
            if [ $? -ne 0 ]
            then
                echo "ERROR - msgfmt failed on ${i}, not updating translations"
                # msgfmt leaves the class file there so the build would work the next time
                find ant_build -name Messages_${LG}.class -exec rm -f {} \;
                RC=1
                break
            fi
        else
            # fast way
            # convert to java files in ant_build/messages-src
            TD=ant_build/messages-src-tmp
            TDX=$TD/i2p/bote/locale
            TD2=ant_build/messages-src
            TDY=$TD2/i2p/bote/locale
            rm -rf $TD
            mkdir -p $TD $TDY
            msgfmt --java --statistics --source -r $CLASS -l $LG -d $TD $i
            if [ $? -ne 0 ]
            then
                echo "ERROR - msgfmt failed on ${i}, not updating translations"
                # msgfmt leaves the class file there so the build would work the next time
                find ant_build/obj -name Messages_${LG}.class -exec rm -f {} \;
                RC=1
                break
            fi
            mv $TDX/Messages_$LG.java $TDY
            rm -rf $TD
        fi
    fi
done
rm -f $TMPFILE
exit $RC
