#!/bin/sh
# script to sync content from boogie board sync device and create a daily archive that can be searched.

# 1. copy files from device to staging area
DEVICE=/media/SYNCSD/SYNC/FILES/SAVED
ROOT=$HOME/Documents/bbsync
STAGING=$ROOT/STAGE

if [ ! -d $DEVICE ]; then
    echo "Boogie Board Sync device not found at this location: " $DEVICE
    echo "Did you plug it in?  Press enter to continue anyway, or kill the process with CTRL+C: "
    read X
fi

# make sure staging area exists
mkdir -p $STAGING

# get a base date
touch $STAGING/.timestamp
BASE_TIME=`date -r $STAGING/.timestamp +%s`

# copy all files
cp -v -n /media/SYNCSD/SYNC/FILES/SAVED/* $STAGING

# process staged files, either deleting duplicates or prompting user for an action
echo "Look at PDF then enter text to indicate action."
echo "  * empty line: skips tagging for now"
echo "  * +: exactly same notes as last file"
echo "  * + <more>: same notes as last file PLUS any additions"
echo "Recommendation: YYYYMMDD project-name meeting-type people"
        
for FILE_STAGED in `ls -1 $STAGING | grep -v "\.txt"`
do
    # skip any files that are already indexed
    TARGET=$STAGING/$FILE_STAGED.txt
    if [ ! -e $TARGET ]; then
        # open with default pdf viewer and then prompt use for option:
        #   skip = delete from stage
        #   other text is name of directory to stage into
        gnome-open $STAGING/$FILE_STAGED 2>&1 > /dev/null &
        PID=$!
        read A B
        if [ "$A" == "+" ]; then
            KEYWORDS="$KEYWORDS $B"
        else
            KEYWORDS="$A $B"
        fi
        kill -9 $PID

        echo $KEYWORDS >> $TARGET
    fi
done;

