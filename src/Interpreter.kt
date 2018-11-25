import kotlin.math.roundToInt

class Interpreter(val code: String) {
    class BslFunction(val name: String, val pos: Int)
    class BslVariable(val name: String, var value: String)

    var error = false

    val functions = ArrayList<BslFunction>()
    val variables = ArrayList<BslVariable>()

    val key_fun = '#'
    val key_comment = '§'
    val key_pattern = '!'
    val key_pattern_seperator = ':'
    val key_opening_bracket = '{'
    val key_closing_bracket = '}'
    val key_math_opening = '['
    val key_math_closing = ']'
    val key_section_marker = '"'
    val key_variable = '_'
    val key_assignment = '='
    val key_ask_return = '?'

    val key_return_one = '<'
    val key_return_two = '-'

    val key_assign_from_fun_one = 't'
    val key_assign_from_fun_two = 'o'

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
        val my_timestamp = currentOffset + timestamp

        //TODO: Invert

        System.out.println("Cube placed: " + my_timestamp + " " + type + " " + value)
    }

    fun placeBomb(timestamp: Double, value: Int) {

    }

    /*
       Returns the current word at the cursor or the Value of the variable
     */
    fun popNext(escapeChar: Char): String {
        val builder = StringBuilder()

        var variable = false

        if (code[pos] == key_variable) {
            pos++
            variable = true
        } else if (code[pos] == key_math_opening) {
            pos++
            return evalMathExpression()
        } else if (code[pos] == key_ask_return) {
            //Function call with return value
            pos++
            val fun_call = popNext(' ')
            gotoNext(fun_opening_bracket)
            return callFunction(fun_call, popArgs())
        }

        while (code[pos] != escapeChar) {
            if (code[pos] == key_section_marker) {
                while (code[pos] != key_section_marker) { //We use that for Strings with spaces.
                    builder.append(code[pos])
                    pos++
                }
                pos++
            } else {
                builder.append(code[pos])
                pos++
            }
        }
        pos++

        if (variable) {
            return getVarVal(builder.toString())
        } else {
            return builder.toString()
        }
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
            argsList.add(popNext(args_seperator))
        }
        pos++
        return argsList.toTypedArray()
    }

    fun addOrAssignVar(name: String, value: String) {
        for (variable in variables) {
            if (name.equals(variable.name)) {
                variable.value = value
            }
        }

        variables.add(BslVariable(name, value))
    }

    fun getVarVal(name: String): String {
        for (variable in variables) {
            if (name.equals(variable.name))
                return variable.value
        }

        return "VARIABLE " + name + " NOT FOUND"
    }

    fun evalMathExpression(): String {
        jumpSpaces()
        val varOne = popNext(' ')
        jumpSpaces()
        val operation = popNext(' ')
        jumpSpaces()
        val varTwo = popNext(' ')

        gotoNext(key_math_closing)

        if (operation.equals("+")) {
            return (varOne.toDouble() + varTwo.toDouble()).toString()
        } else if (operation.equals("-")) {
            return (varOne.toDouble() - varTwo.toDouble()).toString()
        } else if (operation.equals("*")) {
            return (varOne.toDouble() * varTwo.toDouble()).toString()
        } else if (operation.equals("/")) {
            return (varOne.toDouble() / varTwo.toDouble()).toString()
        } else {
            return "INVALID_MATH_OPERATION"
        }
    }

    fun executeFun(my_pos: Int, agrsNames: Array<String>, args: Array<String>): String {
        val old_pos = pos
        pos = my_pos

        gotoNext(key_opening_bracket)
        jumpSpaces()
        while (code[pos] != key_closing_bracket) {
            if (code[pos] == key_comment) {
                pos++
                gotoNext(key_comment)
            } else if (code[pos] == key_pattern) {
                pos++
                val timestamp = popNext(key_pattern_seperator)
                val t_inverted = popNext(key_pattern_seperator)
                val runs = popNext(' ').toDouble().roundToInt() - 1

                val oldOffset = currentOffset
                currentOffset += timestamp.toDouble()

                val oldInverted = inverted
                inverted = t_inverted.toBoolean()

                for (i in 0..runs) {
                    val len = executeFun(pos, arrayOf("p_timestamp", "p_inverted", "p_total_runs", "p_current_run_index"), arrayOf(timestamp, t_inverted, runs.toString(), i.toString()))

                    currentOffset += len.toDouble()
                }

                gotoNext(key_closing_bracket)

                currentOffset = oldOffset
                inverted = oldInverted
            } else if (code[pos] == key_return_one && code[pos + 1] == key_return_two) {
                pos += 2
                jumpSpaces()
                val out = popNext(' ')
                pos = old_pos
                return out
            } else if (code[pos] == key_variable) {
                pos++
                val varName = popNext(' ')
                jumpSpaces()
                if (code[pos] == key_assignment) {
                    pos++
                    jumpSpaces()
                    addOrAssignVar(varName, popNext(' '))
                }
            } else {
                val fun_call = popNext(' ')
                gotoNext(fun_opening_bracket)
                callFunction(fun_call, popArgs())
            }

            jumpSpaces()
        }

        pos = old_pos

        return "NO_RETURN"
    }

    fun callFunction(name: String, args: Array<String>): String {
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

                    return executeFun(this_fun.pos, argsNames, args)
                }
            }
        }

        return "NO_FUNCTION_FOUND"
    }

    fun interpret() {
        while (pos < code.length) {
            if (code[pos] == key_fun) {
                pos++
                val fun_typ = popNext(' ')
                jumpSpaces()

                if (fun_typ.equals(fun_typ_pattern)) {
                    val functionName = popNext(' ')
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