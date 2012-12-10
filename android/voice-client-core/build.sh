#!/bin/bash
CURRENT_APP_DIR="$( cd "$( dirname "$0" )" && pwd )"

function usage {
    echo "usage:    build [filename|dr|dd|tr|td]"
    echo ""
    echo "          dr - default release version"
    echo "          dd - default debug version"
    echo "          tr - tuenti release version"
    echo "          td - tuenti debug version"
    echo "          or specify exact .mk file"
    echo "without any option we build default debug version"
    echo ""
    echo "If you want to specify aditional options for ndk-build, call it: "
    echo "ndk-build NDK_APPLICATION_MK=filename.mk"
}
ndk_build_exe=`which ndk-build`
if [ -f "$ndk_build_exe" ];then
    echo "Using $ndk_build_exe"
elif [ -f "$ANDROID_NDK_ROOT/ndk-build" ];then
    echo "Using $ANDROID_NDK_ROOT/ndk-build"
    ndk_build_exe="$ANDROID_NDK_ROOT/ndk-build"
else
    usage
    echo ""
    echo "ERROR: you must have ANDROID_NDK_ROOT defined and pointing to a valid android NDK"
    echo "or"
    echo "ERROR: you must have ndk-build in your PATH"
    exit 1
fi
if [ $# -gt 0 ]; then
    if [ -f $1 ]; then
        filename=$1
    else
        while [ "$1" != "" ]; do
            case $1 in
                dr )                filename='default_release.mk'
                                    version='release';
                                    ;;
                dd )                filename='default_debug.mk'
                                    version='debug';
                                    ;;
                tr )                filename='tuenti_release.mk'
                                    version='release';
                                    ;;
                td )                filename='tuenti_debug.mk'
                                    version='debug';
                                    ;;
                -h | --help )       usage
                                    exit
                                    ;;
                * )                 usage
                                    exit 1
            esac
            shift
        done
    fi
else
    filename="default_debug.mk"
fi

# If it's a mac, let's load all the cores
platform=`uname -s`
if [ -z $num_of_cores ]; then
  if [ "${platform}" == "Darwin" ]; then
      num_of_cores=`system_profiler -detailLevel full SPHardwareDataType | awk '/Total Number of Cores/{print $5}{next;}'`
  else
      num_of_cores=$((`nproc` +2))
  fi
fi

case ${filename} in
    *debug* )     flags="-j${num_of_cores}"
                ;;
    *release* )   flags="-j${num_of_cores}"
                ;;
esac

OBJDIR=`echo $filename | awk '{sub(".mk","");print}'`

cmd="$ndk_build_exe -C $CURRENT_APP_DIR NDK_APPLICATION_MK=jni/${filename} NDK_APP_OUT=$CURRENT_APP_DIR/obj/$OBJDIR ${flags}"

echo ${cmd}
${cmd}
