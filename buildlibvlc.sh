#!/bin/sh

rootdir=`dirname $0`

checkfail()
{
    if [ ! $? -eq 0 ];then
        echo "'$1' failed"
        exit 1
    fi
}

if [ ! -d "${rootdir}/vlc-android" ]; then
    echo "VLC Android source not found, cloning"
    git clone http://code.videolan.org/videolan/vlc-android.git
    checkfail "git clone"
fi

sh -c "cd ${rootdir}/vlc-android/buildsystem && ./compile.sh -l $*"
checkfail "./vlc-android/buildsystem/compile.sh $*"

aar_file=`ls "${rootdir}"/vlc-android/libvlc/build/outputs/aar/*.aar --sort=time|head -n 1`
cp "${aar_file}" "${rootdir}"/libvlc/libvlc-3.0.0.aar
checkfail "libvlc*.arr not found"

VLC_SRC_DIR=`realpath "${rootdir}"/vlc-android/vlc`
LIBVLCJNI_LIBS=`realpath "${rootdir}"/vlc-android/libvlc/jni/libs`

for project in native_sample;do
    for jnilinkdir in ${LIBVLCJNI_LIBS}/*;do
        arch=`basename $jnilinkdir`
        if [ ! -f "${jnilinkdir}/libvlcjni.so" ];then
            continue;
        fi
        for linkdir in ${VLC_SRC_DIR}/build-android*/ndk/libs/${arch};do
            if [ ! -f "${linkdir}/libvlc.so" ];then
                continue;
            fi
            $ANDROID_NDK/ndk-build -C "${rootdir}"/${project} \
                VLC_SRC_DIR="${VLC_SRC_DIR}" \
                LIBVLC_LDLIBS="-L${linkdir} -L${jnilinkdir} -lvlc -lvlcjni" \
                APP_BUILD_SCRIPT=jni/Android.mk \
                APP_PLATFORM=android-9 \
                APP_ABI=${arch} \
                NDK_PROJECT_PATH=jni
            break
        done
    done
done

