#!/bin/sh
. ./configuration

if [ $# -lt 2 ]; then  
    echo  'Usage: [srcApkFile] [dstApkFile]'
elif [ -f $1 ]; then
    PACKGE_NAME=`aapt dump badging $1  | grep package | sed  "s/.*name='//g" | sed "s/'.*version.*//g"`
    ACTIVITY_CLASS=`aapt dump badging $1 | grep launchable-activity | sed  "s/.*name='//g" | sed "s/'.*label.*//g"`
    ACTIVITY_NAME=`echo $ACTIVITY_CLASS | sed "s/.*\.//g"`
    cat ACTIVITY_TEMPLATE | sed "s/\$1/$PACKGE_NAME/g;s/\$2/$ACTIVITY_NAME/g" > $ACTIVITY_NAME.java
    mkdir $TEMP_DIR
    $JAVAC -bootclasspath $BOOTCLASSPATH -d $TEMP_DIR  $ACTIVITY_NAME.java 
    rm $ACTIVITY_NAME.java
    $DX --dex --output=classes.dex --no-strict $TEMP_DIR
    rm -rf $TEMP_DIR
    mkdir $TEMP_EXTRACT_DIR
    unzip $1 -d $TEMP_EXTRACT_DIR
    cd $TEMP_EXTRACT_DIR
    mv ../classes.dex ./
    zip ../$TEMP_APK_NAME -r .
    cd ..
    rm -rf $TEMP_EXTRACT_DIR
    $JAVA -jar $SIGNAPK_JAR $PEM $PK8 $TEMP_APK_NAME $2
    rm $TEMP_APK_NAME
else
    echo $1' not exist'
fi
