#/bin/bash

ASTYLE=`which astyle 2> /dev/null`
if [[ $? -ne 0 ]]; then
    echo "astyle code formatter not installed. aborting."
    exit 1
fi

ASTYLE_ARGS="--style=kr --indent=spaces=4 --convert-tabs --pad-oper --unpad-paren --pad-header --align-pointer=type --align-reference=type"

$ASTYLE $ASTYLE_ARGS *.h
$ASTYLE $ASTYLE_ARGS *.c

