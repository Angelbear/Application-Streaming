#!/bin/bash
. `dirname $0`/configuration

if [ $# -lt 2 ]; then  
    echo  'Usage: [srcApkFile] [dstApkFile]'
elif [ -f $1 ]; then
    PACKGE_NAME=`$AAPT dump badging $1  | grep "package: name=" | sed  "s/.*name='//g" | sed "s/'.*version.*//g"`
    ACTIVITY_CLASSES=`$AAPT dump badging $1 | grep "launchable.*activity" | sed  "s/.*name='//g" | sed "s/'.*label.*//g"`
    ICONS=`$AAPT dump badging $1 | grep application-icon- | sed "s/.*:'//g" | sed "s/'//g"`
    APPLICATION=`$AAPT2 dump components $1 | grep application: | sed "s/application: //g"`
    for a in $ACTIVITY_CLASSES; do
        ACTIVITY_NAME=`echo $a | sed "s/.*\.//g"`
        ACTIVITY_PACKAGE=`echo $a | sed "s/\(.*\)\.\(.*\)/\1/g"`
        cat `dirname $0`/ACTIVITY_TEMPLATE | sed "s/\$1/$ACTIVITY_PACKAGE/g;s/\$2/$ACTIVITY_NAME/g" > $ACTIVITY_NAME.java
    done
    PROVIDERS=`$AAPT2 dump components $1 | grep provider: | sed "s/provider: //g"`
    if [ -n $APPLICATION ]; then
        APPLICATION_NAME=`echo $APPLICATION | sed "s/.*\.//g"`
        APPLICATION_PACKAGE=`echo $APPLICATION | sed "s/\(.*\)\.\(.*\)/\1/g"`
        if [ -z $APPLICATION_PACKAGE ] || [ ${APPLICATION_PACKAGE:0:1} == "." ]; then
            APPLICATION_PACKAGE=$PACKGE_NAME$APPLICATION_PACKAGE
        fi
        cat `dirname $0`/APPLICATION_TEMPLATE | sed  "s/\$1/$APPLICATION_PACKAGE/g;s/\$2/$APPLICATION_NAME/g" > $APPLICATION_NAME.java
    fi
    for p in $PROVIDERS; do
        PROVIDER_NAME=`echo $p | sed "s/.*\.//g"`
        PROVIDER_PACKAGE=`echo $p | sed "s/\(.*\)\.\(.*\)/\1/g"`
        if [ -z $PROVIDER_PACKAGE ] || [ ${PROVIDER_PACKAGE:0:1} == "." ]; then
            PROVIDER_PACKAGE=$PACKGE_NAME$PROVIDER_PACKAGE
        fi
        cat `dirname $0`/PROVIDER_TEMPLATE | sed "s/\$1/$PROVIDER_PACKAGE/g;s/\$2/$PROVIDER_NAME/g" > $PROVIDER_NAME.java
    done
    mkdir $TEMP_DIR
    $JAVAC -bootclasspath $BOOTCLASSPATH -d $TEMP_DIR  *.java
    $DX --dex --output=classes.dex --no-strict $TEMP_DIR
    rm *.java
    rm -rf $TEMP_DIR
    cp $1 $TEMP_APK_NAME
    aapt r $TEMP_APK_NAME classes.dex
    aapt a $TEMP_APK_NAME classes.dex > /dev/null
    rm classes.dex
    for i in $ICONS; do
       PATTERN="$PATTERN\|$i"
    done
    DELETE_FILES=`$AAPT l $TEMP_APK_NAME | grep -v "$PATTERN" | xargs -I '{}' -s 100000 echo \"'{}'\"`
    echo $DELETE_FILES | xargs aapt r $TEMP_APK_NAME
    $JAVA -jar $SIGNAPK_JAR $PEM $PK8 $TEMP_APK_NAME $2
    rm $TEMP_APK_NAME
else
    echo $1' not exist'
fi
