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
echo "Look at PDF then enter text to indicate action.  Enter empty line to skip, + to use same notes, else:"
echo "  <DIRECTORY> [words for search]"
        
for FILE_STAGED in `ls -1 $STAGING`
do
    COUNT=`find $ROOT -name $FILE_STAGED | grep -v "$STAGING" | wc -l`
    # skip any staged files that already exist
    if [ $COUNT == "0" ]; then
        # open with default pdf viewer and then prompt use for option:
        #   skip = delete from stage
        #   other text is name of directory to stage into
        gnome-open $STAGING/$FILE_STAGED 2>&1 > /dev/null &
        PID=$!
        read A B
        if [ "$A" == "" ]; then
            TARGET=""
        elif [ "$A" != "+" ]; then
            TARGET="$ROOT/$A"
        fi
        kill -9 $PID

        KEYWORDS=$B

        if [ "$TARGET" != "" ]; then
            mkdir -p $TARGET
            cp -n "$STAGING/$FILE_STAGED" $TARGET
            touch $TARGET/.timestamp
            echo $KEYWORDS >> "$TARGET/$FILE_STAGED.txt"
        fi
    fi
done;

# create single pdf.  for now, going to do everything always.  will make it smarter later
for D in `find $ROOT -mindepth 1 -maxdepth 1 -type d | grep -v "$STAGING"`
do
    if [ ! -f $D/.timestamp ]; then
        # no timestamp file, just skip (nothing to do)
        continue;
    fi

    # check date against base
    DIR_TIME=`date -r $D/.timestamp +%s`
    if [ $BASE_TIME -lt $DIR_TIME ]; then
        pushd $D
            OUTPUT_BASE=`echo $D | sed "s#$ROOT/##g"`
            # write directory to txt file so is in keywords, replace - and _ with space (replace existing output.txt file)
            echo $OUTPUT_BASE | tr '-' ' ' | tr '_' ' ' > output.txt
            # create ps from notes
            cat *.txt | sort | uniq > keywords
            enscript -p keywords.ps keywords
            # convert ps to pdf
            ps2pdf keywords.ps keywords.pdf
            # crop keywords so it's not as wide in the final document
            pdfcrop keywords.pdf cropped.pdf
            rm keywords.pdf
            # clob all pdf's together into one file
            rm output.pdf "../$OUTPUT_BASE.pdf"
            ~/Downloads/sejda-console-1.0.0.M9/bin/sejda-console merge -f *.PDF *.pdf -o "../$OUTPUT_BASE.pdf"
            # cleanup keyword temp stuff
            rm keywords keywords.ps keywords.pdf
        popd
    fi
done;

