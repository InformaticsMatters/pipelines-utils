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
