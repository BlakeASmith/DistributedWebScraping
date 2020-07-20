import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile

object JarLoaderInstrumentation {
    private var inst: Instrumentation? = null

    @Synchronized
    fun addToClassPath(jarFile: File) {
        // do our best to ensure consistent behaviour across methods
        if (!jarFile.exists()) {
            throw FileNotFoundException(jarFile.absolutePath)
        }
        if (!jarFile.canRead()) {
            throw IOException("can't read jar: " + jarFile.absolutePath)
        }
        if (jarFile.isDirectory) {
            throw IOException("not a jar: " + jarFile.absolutePath)
        }

        // add the jar using instrumentation, or fall back to reflection
        inst!!.appendToSystemClassLoaderSearch(JarFile(jarFile))
    }

    fun premain(agentArgs: String, instrumentation: Instrumentation) {
        agentmain(agentArgs, instrumentation)
    }

    fun agentmain(agentArgs: String?, instrumentation: Instrumentation) {
            inst = instrumentation
    }
}