import java.io.File

fun main(args: Array<String>) {
    val code = StringBuilder()
    val path = ClassLoader.getSystemResource("test.bml")

    val raw_code = File(path.toURI()).bufferedReader().readLines()

    for (str in raw_code) {
        code.append(" ")
        code.append(str)
    }

    val interpreter = Interpreter(code.toString())

    interpreter.interpret()
}