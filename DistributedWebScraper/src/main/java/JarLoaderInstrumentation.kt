import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.instrument.Instrumentation
import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile

object JarLoaderInstrumentation {
    private var inst: Instrumentation? = null
    private var addUrlThis: ClassLoader? = null

    // violates URLClassLoader API!
    private var addUrlMethod: Method? = null
        private get() {
            if (field == null) {
                addUrlThis = ClassLoader.getSystemClassLoader()
                if (addUrlThis is URLClassLoader) {
                    try {
                        val method =
                            URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
                        method.isAccessible = true
                        field = method
                    } catch (nsm: NoSuchMethodException) {
                        throw AssertionError() // violates URLClassLoader API!
                    }
                } else {
                    throw UnsupportedOperationException(
                        "did you forget -javaagent:<jarpath>?"
                    )
                }
            }
            return field
        }

    /**
     * Adds a JAR file to the list of JAR files searched by the system class
     * loader. This effectively adds a new JAR to the class path.
     *
     * @param jarFile the JAR file to add
     * @throws IOException if there is an error accessing the JAR file
     */
    @Synchronized
    @Throws(IOException::class)
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

    /**
     * Called by the JRE. *Do not call this method from user code.*
     *
     *
     *
     * This method is automatically invoked when the JRE loads this class as an
     * agent using the option `-javaagent:jarPathOfThisClass`.
     *
     *
     *
     * For this to work the `MANIFEST.MF` file **must**
     * include the line `Premain-Class: ca.cgjennings.jvm.JarLoader`.
     *
     * @param agentArgs agent arguments; currently ignored
     * @param instrumentation provided by the JRE
     */
    fun premain(agentArgs: String?, instrumentation: Instrumentation?) {
        agentmain(agentArgs, instrumentation)
    }

    /**
     * Called by the JRE. *Do not call this method from user code.*
     *
     *
     *
     * This method is called when the agent is attached to a running process. In
     * practice, this is not how JarLoader is used, but it is implemented should
     * you need it.
     *
     *
     *
     * For this to work the `MANIFEST.MF` file **must**
     * include the line `Agent-Class: ca.cgjennings.jvm.JarLoader`.
     *
     * @param agentArgs agent arguments; currently ignored
     * @param instrumentation provided by the JRE
     */
    fun agentmain(agentArgs: String?, instrumentation: Instrumentation?) {
        if (instrumentation == null) {
            throw NullPointerException("instrumentation")
        }
        if (inst == null) {
            inst = instrumentation
        }
    }

}