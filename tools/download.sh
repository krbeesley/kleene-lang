#!/bin/bash

base=$(dirname $0)

# Apache Commons Lang
COMMONS_LANG_BASENAME=commons-lang3-3.1
curl -C - -o $base/Apache_Commons/KEYS http://www.apache.org/dist/commons/KEYS
curl -C - -o $base/Apache_Commons/$COMMONS_LANG_BASENAME-bin.tar.gz https://archive.apache.org/dist/commons/lang/binaries/$COMMONS_LANG_BASENAME-bin.tar.gz
curl -C - -o $base/Apache_Commons/$COMMONS_LANG_BASENAME-bin.tar.gz.asc https://archive.apache.org/dist/commons/lang/binaries/$COMMONS_LANG_BASENAME-bin.tar.gz.asc
gpg --import $base/Apache_Commons/KEYS
gpg --verify $base/Apache_Commons/$COMMONS_LANG_BASENAME-bin.tar.gz.asc \
    || exit 1


# ICU4C
ICU4C=icu4c-54_1
curl -C - -o $base/ICU4C/$ICU4C-src.tgz http://download.icu-project.org/files/icu4c/54.1/$ICU4C-src.tgz
curl -C - -o $base/ICU4C/$ICU4C.md5 https://ssl.icu-project.org/files/icu4c/54.1/icu4c-src-54_1.md5
pushd $base/ICU4C
fgrep $ICU4C-src.tgz $ICU4C.md5 | md5sum -c - \
    || exit 1
popd


# ICU4J
ICU4J=icu4j-54_1_1
curl -C - -o $base/ICU4J/$ICU4J.jar http://download.icu-project.org/files/icu4j/54.1.1/$ICU4J.jar
curl -C - -o $base/ICU4J/$ICU4J.md5 https://ssl.icu-project.org/files/icu4j/54.1.1/icu4j-54_1_1.md5
pushd $base/ICU4J
fgrep $ICU4J.jar $ICU4J.md5 | md5sum -c - \
    || exit 1
popd

# JavaCC
JAVACC=javacc-5.0
curl -C - -o $base/JavaCC/$JAVACC.tar.gz https://javacc.org/downloads/$JAVACC.tar.gz

# Jaxen
JAXEN=jaxen-1.1.6-bin
curl -C - -o $base/Jaxen/$JAXEN.tar.gz http://www.cafeconleche.org/jaxen/dist/$JAXEN.tar.gz

# OpenFst
OPENFST=openfst-1.4.1
curl -C - -o $base/OpenFst/$OPENFST.tar.gz http://www.openfst.org/twiki/pub/FST/FstDownload/$OPENFST.tar.gz

# ROME
ROME=rome-1.0
curl -C - -o $base/ROME/$ROME.jar http://central.maven.org/maven2/rome/rome/1.0/$ROME.jar

# dom4j
DOM4J=dom4j-1.6.1
curl -L -C - -o $base/dom4j/$DOM4J.tar.gz https://github.com/dom4j/dom4j/releases/download/dom4j_1_6_1/dom4j-1.6.1.tar.gz

set -e
make -C $base build-libraries
