#!/bin/bash
TOOLSDIR="$( cd "$( dirname "$0" )" && pwd )"
TRUNKDIR="$( dirname $TOOLSDIR )"
pushd $TRUNKDIR
# find the list of files with h & cc extensions then ignore the toplevel base dir

find . -type f|egrep "\.(h|cc)$"|grep -v "^./base" > tagindex;
# create C++ tags from the list
ctags --tag-relative=yes --languages=C++ -L tagindex;
popd
