#!/usr/bin/env bash

#
# Install JDK for Linux and Mac OS
#
# This script determines the most recent early-access build number,
# downloads the JDK archive to the user home directory and extracts
# it there.
#
# Exported environment variables (when sourcing this script)
#
#   JAVA_HOME is set to the extracted JDK directory
#   PATH is prepended with ${JAVA_HOME}/bin
#
# (C) 2019 Christian Stein
#
# https://github.com/sormuras/bach/blob/master/install-jdk.sh
#

set -o errexit
#set -o nounset # https://github.com/travis-ci/travis-ci/issues/5434
#set -o xtrace

function initialize() {
    readonly script_name="$(basename "${BASH_SOURCE[0]}")"
    readonly script_version='2019-05-02'

    dry=false
    silent=false
    verbose=false
    emit_java_home=false

    feature='ea'
    license='GPL'
    os='?'
    url='?'
    workspace="${HOME}"
    target='?'
    cacerts=false
}

function usage() {
cat << EOF
Usage: ${script_name} [OPTION]...
Download and extract the latest-and-greatest JDK from java.net or Oracle.

Version: ${script_version}
Options:
  -h|--help                 Displays this help
  -d|--dry-run              Activates dry-run mode
  -s|--silent               Displays no output
  -e|--emit-java-home       Print value of "JAVA_HOME" to stdout (ignores silent mode)
  -v|--verbose              Displays verbose output

  -f|--feature 9|10|...|ea  JDK feature release number, defaults to "ea"
  -l|--license GPL|BCL      License defaults to "GPL", BCL also indicates OTN-LA for Oracle Java SE
  -o|--os linux-x64|osx-x64 Operating system identifier (works best with GPL license)
  -u|--url "https://..."    Use custom JDK archive (provided as .tar.gz file)
  -w|--workspace PATH       Working directory defaults to \${HOME} [${HOME}]
  -t|--target PATH          Target directory, defaults to first component of the tarball
  -c|--cacerts              Link system CA certificates (currently only Debian/Ubuntu is supported)
EOF
}

function script_exit() {
    if [[ $# -eq 1 ]]; then
        printf '%s\n' "$1"
        exit 0
    fi

    if [[ $# -eq 2 && $2 =~ ^[0-9]+$ ]]; then
        printf '%b\n' "$1"
        exit "$2"
    fi

    script_exit 'Invalid arguments passed to script_exit()!' 2
}

function say() {
    if [[ ${silent} != true ]]; then
        echo "$@"
    fi
}

function verbose() {
    if [[ ${verbose} == true ]]; then
        echo "$@"
    fi
}

function parse_options() {
    local option
    while [[ $# -gt 0 ]]; do
        option="$1"
        shift
        case ${option} in
            -h|-H|--help)
                usage
                exit 0
                ;;
            -v|-V|--verbose)
                verbose=true
                ;;
            -s|-S|--silent)
                silent=true
                verbose "Silent mode activated"
                ;;
            -d|-D|--dry-run)
                dry=true
                verbose "Dry-run mode activated"
                ;;
            -e|-E|--emit-java-home)
                emit_java_home=true
                verbose "Emitting JAVA_HOME"
                ;;
            -f|-F|--feature)
                feature="$1"
                verbose "feature=${feature}"
                shift
                ;;
            -l|-L|--license)
                license="$1"
                verbose "license=${license}"
                shift
                ;;
            -o|-O|--os)
                os="$1"
                verbose "os=${os}"
                shift
                ;;
            -u|-U|--url)
                url="$1"
                verbose "url=${url}"
                shift
                ;;
            -w|-W|--workspace)
                workspace="$1"
                verbose "workspace=${workspace}"
                shift
                ;;
            -t|-T|--target)
                target="$1"
                verbose "target=${target}"
                shift
                ;;
            -c|-C|--cacerts)
                cacerts=true
                verbose "Linking system CA certificates"
                ;;
            *)
                script_exit "Invalid argument was provided: ${option}" 2
                ;;
        esac
    done
}

function determine_latest_jdk() {
    local number
    local curl_result
    local url

    verbose "Determine latest JDK feature release number"
    number=9
    while [[ ${number} != 99 ]]
    do
      url=http://jdk.java.net/${number}
      curl_result=$(curl -o /dev/null --silent --head --write-out %{http_code} ${url})
      if [[ ${curl_result} -ge 400 ]]; then
        break
      fi
      verbose "  Found ${url} [${curl_result}]"
      latest_jdk=${number}
      number=$[$number +1]
    done

    verbose "Latest JDK feature release number is: ${latest_jdk}"
}

function perform_sanity_checks() {
    if [[ ${feature} == '?' ]] || [[ ${feature} == 'ea' ]]; then
        feature=${latest_jdk}
    fi
    if [[ ${feature} -lt 9 ]] || [[ ${feature} -gt ${latest_jdk} ]]; then
        script_exit "Expected feature release number in range of 9 to ${latest_jdk}, but got: ${feature}" 3
    fi
    if [[ ${feature} -gt 11 ]] && [[ ${license} == 'BCL' ]]; then
        script_exit "BCL licensed downloads are only supported up to JDK 11, but got: ${feature}" 3
    fi
    if [[ -d "$target" ]]; then
        script_exit "Target directory must not exist, but it does: $(du -hs '${target}')" 3
    fi
}

function determine_url() {
    local DOWNLOAD='https://download.java.net/java'
    local ORACLE='http://download.oracle.com/otn-pub/java/jdk'

    # Archived feature or official GA build?
    case "${feature}-${license}" in
        9-GPL) url="${DOWNLOAD}/GA/jdk9/9.0.4/binaries/openjdk-9.0.4_${os}_bin.tar.gz"; return;;
        9-BCL) url="${ORACLE}/9.0.4+11/c2514751926b4512b076cc82f959763f/jdk-9.0.4_${os}_bin.tar.gz"; return;;
       10-GPL) url="${DOWNLOAD}/GA/jdk10/10.0.2/19aef61b38124481863b1413dce1855f/13/openjdk-10.0.2_${os}_bin.tar.gz"; return;;
       10-BCL) url="${ORACLE}/10.0.2+13/19aef61b38124481863b1413dce1855f/jdk-10.0.2_${os}_bin.tar.gz"; return;;
       11-GPL) url="${DOWNLOAD}/GA/jdk11/9/GPL/openjdk-11.0.2_${os}_bin.tar.gz"; return;;
       11-BCL) url="${ORACLE}/11.0.2+9/f51449fcd52f4d52b93a989c5c56ed3c/jdk-11.0.2_${os}_bin.tar.gz"; return;;
       12-GPL) url="${DOWNLOAD}/GA/jdk12.0.1/69cfe15208a647278a19ef0990eea691/12/GPL/openjdk-12.0.1_${os}_bin.tar.gz"; return;;
    esac

    # EA or RC or GA build?
    local JAVA_NET="http://jdk.java.net/${feature}"
    local candidates=$(wget --quiet --output-document - ${JAVA_NET} | grep -Eo 'href[[:space:]]*=[[:space:]]*"[^\"]+"' | grep -Eo '(http|https)://[^"]+')
    url=$(echo "${candidates}" | grep -Eo "${DOWNLOAD}/.+/jdk${feature}/.*${license}/.*jdk-${feature}.+${os}_bin(.tar.gz|.zip)$" || true)

    if [[ -z ${url} ]]; then
        script_exit "Couldn't determine a download url for ${feature}-${license} on ${os}" 1
    fi
}

function prepare_variables() {
    if [[ ${os} == '?' ]]; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
          os='osx-x64'
        else
          os='linux-x64'
        fi
    fi
    if [[ ${url} == '?' ]]; then
        determine_latest_jdk
        perform_sanity_checks
        determine_url
    else
        feature='<overridden by custom url>'
        license='<overridden by custom url>'
        os='<overridden by custom url>'
    fi
    archive="${workspace}/$(basename ${url})"
    status=$(curl -o /dev/null --silent --head --write-out %{http_code} ${url})
}

function print_variables() {
cat << EOF
Variables:
  feature = ${feature}
  license = ${license}
       os = ${os}
      url = ${url}
   status = ${status}
  archive = ${archive}
EOF
}

function download_and_extract_and_set_target() {
    local quiet='--quiet'; if [[ ${verbose} == true ]]; then quiet=''; fi
    local local="--directory-prefix ${workspace}"
    local remote='--timestamping --continue'
    local wget_options="${quiet} ${local} ${remote}"
    local tar_options="--file ${archive}"

    say "Downloading JDK from ${url}..."
    verbose "Using wget options: ${wget_options}"
    if [[ ${license} == 'GPL' ]]; then
        wget ${wget_options} ${url}
    else
        wget ${wget_options} --header "Cookie: oraclelicense=accept-securebackup-cookie" ${url}
    fi

    if [[ ${os} == 'windows-x64' ]]; then
        script_exit "Extracting archives on Windows isn't supported, yet" 4
    fi

    verbose "Using tar options: ${tar_options}"
    if [[ ${target} == '?' ]]; then
        tar --extract ${tar_options} -C "${workspace}"
        if [[ "$OSTYPE" != "darwin"* ]]; then
            target="${workspace}"/$(tar --list ${tar_options} | grep 'bin/javac' | tr '/' '\n' | tail -3 | head -1)
        else
            target="${workspace}"/$(tar --list ${tar_options} | head -2 | tail -1 | cut -f 2 -d '/' -)/Contents/Home
        fi
    else
        if [[ "$OSTYPE" != "darwin"* ]]; then
            mkdir --parents "${target}"
            tar --extract ${tar_options} -C "${target}" --strip-components=1
        else
            mkdir -p "${target}"
            tar --extract ${tar_options} -C "${target}" --strip-components=4 # . / <jdk> / Contents / Home
        fi
    fi

    if [[ ${verbose} == true ]]; then
        echo "Set target to: ${target}"
        echo "Content of target directory:"
        ls "${target}"
        echo "Content of release file:"
        [[ ! -f "${target}/release" ]] || cat "${target}/release"
    fi

    # Link to system certificates
    # http://openjdk.java.net/jeps/319
    # https://bugs.openjdk.java.net/browse/JDK-8196141
    if [[ ${cacerts} == true ]]; then
        mv "${target}/lib/security/cacerts" "${target}/lib/security/cacerts.jdk"
        ln -s /etc/ssl/certs/java/cacerts "${target}/lib/security/cacerts"
    fi
}

function main() {
    initialize
    parse_options "$@"

    say "$script_name $script_version"
    prepare_variables

    if [[ ${silent} == false ]]; then print_variables; fi
    if [[ ${dry} == true ]]; then exit 0; fi

    download_and_extract_and_set_target

    export JAVA_HOME=$(cd "${target}"; pwd)
    export PATH=${JAVA_HOME}/bin:$PATH

    if [[ ${silent} == false ]]; then java -version; fi
    if [[ ${emit_java_home} == true ]]; then echo "${JAVA_HOME}"; fi
}

main "$@"
