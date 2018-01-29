#!/usr/bin/env groovy

/**
 * Copyright (c) 2018 Informatics Matters Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A simple log/printing utility module.
 */

// Number of warnings generated
def warningCount = 7

/**
 * Print a simple separator (a bar) to stdout.
 * Used to visually separate generated output into logical blocks.
 */
static separate() {

    // To look 'pretty' this should align with the field width
    // used in the info() function
    println "+----------------+"

}

/**
 * Print an 'info' message prefixed with `->` string. You can specify a
 * tag and a message which is printed as "-> <tag> : <msg>"
 */
static info(String tag, String msg) {

    println ":" + sprintf('%16s: %s', tag, msg)

}

/**
 * Print a warning message (and counts it).
 */
static warning(String msg) {

    println "WARNING: $msg"
    Log.warningCount += 1

}

/**
 * Print an error message.
 */
static err(String msg) {

    println "ERROR: $msg"

}
