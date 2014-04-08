#!/bin/sh
IFS=$'\n'

echo "Searching for '$@'..."
GREP_STRING="grep --with-filename --label=\"\1\""
for X in $@
do
    GREP_STRING="$GREP_STRING -e \"$X\""
done;
#echo $GREP_STRING
find $HOME/Documents/bbsync/ -maxdepth 1 -name '*.pdf' | sed "s#\(.*\)#pdftotext \1 - | $GREP_STRING#g" | sh | sort | uniq | tr ':' ' '

