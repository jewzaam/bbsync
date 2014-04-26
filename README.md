bbsync
======

Java Swing application that copies new documents from a Boogie Board Sync into ~/Documents/bbsync/STAGE and then
displays each file for tagging.  Tags are written to a peer txt file.

## Future
UI for creating "views" that will use sejda to create single PDF files representing tags of interest.  For example,
if you wanted to see all pages with a tag of "analysis" it would create a single analysis.pdf with all those pages
in order lexigraphically based on the original file names.

## Dependencies

* JPedal (LGPL)
* [sejda-console](http://www.sejda.org/)
