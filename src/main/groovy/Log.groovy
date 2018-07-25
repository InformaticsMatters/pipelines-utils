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

/**
 * Text colour codes
 * (see http://ozzmaker.com/add-colour-to-text-in-python/)
 *
 * Text         Style           Background
 * ----         -----           ----------
 * Black    30  No effect   0   Black   40
 * Red      31  Bold        1   Red     41
 * Green    32  Underline   2   Green   42
 * Yellow   33  Negative1   3   Yellow  43
 * Blue     34  Negative2   5   Blue    44
 * Purple   35                  Purple  45
 * Cyan     36                  Cyan    46
 * White    37                  White   47
 */

/**
 * Print a simple separator (a bar) to stdout.
 * Used to visually separate generated output into logical blocks.
 */
static separate() {

    // To look 'pretty' this should align with the field width
    // used in the info() function
    println "+------------------+"

}

/**
 * Print an 'info' message prefixed with `->` string. You can specify a
 * tag and a message which is printed as "-> <tag> : <msg>"
 */
static info(String tag, msg) {

    println "|" + sprintf('%18s| %s', tag, msg)

}

/**
 * Turns text printing BOLD.
 */
static text_style_bold() {
    print "" + (char)27 + "[1m"
}

/**
 * Turns text printing RED.
 */
static text_colour_red() {
    print "" + (char)27 + "[31m"
}

/**
 * Turns text printing GREEN.
 */
static text_colour_green() {
    print "" + (char)27 + "[32m"
}

/**
 * Returns text printing colour to normal (default).
 */
static text_normal() {
    print "" + (char)27 + "[39m" + (char)27 + "[0m"
}

/**
 * Print an error message.
 */
static err(String msg) {

    println "|" + sprintf('%18s| %s', 'ERROR', msg)

}
