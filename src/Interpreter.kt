class Interpreter(val code: String) {
    class BslFunction(val name: String, val pos: Int)

    var error = false

    val functions = ArrayList<BslFunction>()

    val key_fun = '#'
    val key_comment = '?'
    val key_pattern = '!'
    val key_pattern_seperator = ':'
    val key_opening_bracket = '{'
    val key_closing_bracket = '}'
    val key_section_marker = '"'

    val fun_typ_pattern = "fun"
    val fun_typ_main = "main"

    val fun_stat_place_cube = "Cube"

    val fun_opening_bracket = '('
    val fun_closing_bracket = ')'

    val args_seperator = ','

    var pos = 0

    var currentOffset = 0.0
    var inverted = false

    fun placeCube(timestamp: Double, type: Int, value: Int) {
        System.out.println("Cube placed: " + timestamp + " " + type + " " + value)
    }

    fun placeBomb(timestamp: Double, value: Int) {

    }

    /*
       Returns the current word at the cursor.
     */
    fun popNextWord(escapeChar: Char): String {
        val builder = StringBuilder()

        while (code[pos] != escapeChar) {
            if (code[pos] == key_section_marker) {
                while (code[pos] != key_section_marker) { //We use that for Strings with spaces.
                    builder.append(code[pos])
                    pos++
                }
                pos++
            }else {
                builder.append(code[pos])
                pos++
            }
        }
        pos++

        return builder.toString()
    }

    /*
       Goes to the next specified char.
     */
    fun gotoNext(key: Char) {
        while (code[pos] != key) pos++
        pos++
    }

    /*
        Jumps to the next non-Space char
     */
    fun jumpSpaces() {
        while (code[pos] == ' ') pos++
    }

    fun popArgs(): Array<String> {
        val argsList = ArrayList<String>()
        while (code[pos] != fun_closing_bracket) {
            jumpSpaces()
            argsList.add(popNextWord(args_seperator))
        }
        pos++
        return argsList.toTypedArray()
    }

    fun executeFun(my_pos: Int, agrsNames: Array<String>, args: Array<String>) {
        val old_pos = pos
        pos = my_pos

        gotoNext(key_opening_bracket)
        jumpSpaces()
        while (code[pos] != key_closing_bracket) {
            if (code[pos] == key_comment) {
                gotoNext(key_comment)
            } else if (code[pos] == key_pattern) {
                pos++
                val timestamp = popNextWord(key_pattern_seperator)
                val t_inverted = popNextWord(key_pattern_seperator)
                val runs = popNextWord(' ').toInt() - 1

                val oldOffset = currentOffset
                currentOffset += timestamp.toDouble()

                val oldInverted = inverted
                inverted = t_inverted.toBoolean()

                for (i in 0..runs) {
                    executeFun(pos, arrayOf("p_timestamp", "p_inverted", "p_total_runs", "p_current_run_index"), arrayOf(timestamp, t_inverted, runs.toString(), i.toString()))
                }

                gotoNext(key_closing_bracket)

                currentOffset = oldOffset
                inverted = oldInverted
            } else {
                val fun_call = popNextWord(' ')
                gotoNext(fun_opening_bracket)
                callFunction(fun_call, popArgs())
            }

            jumpSpaces()
        }

        pos = old_pos
    }

    fun callFunction(name: String, args: Array<String>) {
        if (name == fun_stat_place_cube) {
            placeCube(args[0].toDouble(), args[1].toInt(), args[2].toInt())
        } else {
            //Now we need to check for custom functions

            for (this_fun in functions) {
                if (this_fun.name == name) {
                    val old_pos = pos
                    pos = this_fun.pos

                    val argsNames = popArgs()

                    pos = old_pos

                    executeFun(this_fun.pos, argsNames, args)
                    break
                }
            }
        }
    }

    fun interpret() {
        while (pos < code.length) {
            if (code[pos] == key_fun) {
                pos++
                val fun_typ = popNextWord(' ')
                jumpSpaces()

                if (fun_typ.equals(fun_typ_pattern)) {
                    val functionName = popNextWord(' ')
                    functions.add(BslFunction(functionName, pos))
                    gotoNext(key_closing_bracket)
                } else if (fun_typ == fun_typ_main) {
                    //We don't need any args for the main function
                    executeFun(pos, Array(0, { i -> ""}), Array(0, { i -> ""}))
                    return
                }
            } else if (code[pos] == key_comment) {
                //We don't need comments
                gotoNext(key_comment)
            } else {
                pos++
            }
        }
    }
}