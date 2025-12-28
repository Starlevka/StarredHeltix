package set.starlev.utils.render

import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

/**
 * Упрощенный менеджер шейдеров для поддержки Chroma эффектов
 */
object ShaderManager {
    private var currentProgram = -1
    private val shaders = mutableMapOf<String, Int>()

    /**
     * Загружает и компилирует шейдерную программу
     */
    fun loadShader(name: String): Int {
        if (shaders.containsKey(name)) return shaders[name]!!

        println("Loading shader: $name")
        val vertexSource = loadResource("shaders/$name.vsh")
        val fragmentSource = loadResource("shaders/$name.fsh")

        if (vertexSource == null || fragmentSource == null) {
            println("Failed to load shader sources for: $name")
            return -1
        }

        val vertexShader = compileShader(GL20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource)

        if (vertexShader == -1 || fragmentShader == -1) {
            println("Failed to compile shaders for: $name")
            return -1
        }

        val program = GL20.glCreateProgram()
        GL20.glAttachShader(program, vertexShader)
        GL20.glAttachShader(program, fragmentShader)
        GL20.glLinkProgram(program)

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            println("Failed to link shader program: $name. Log: ${GL20.glGetProgramInfoLog(program, 1024)}")
            return -1
        }

        println("Successfully loaded shader program: $name (id: $program)")
        shaders[name] = program
        return program
    }

    private fun loadResource(path: String): String? {
        return try {
            val resource = ResourceLocation.fromNamespaceAndPath("starredheltix", path)
            val stream = Minecraft.getInstance().resourceManager.getResource(resource).get().open()
            BufferedReader(InputStreamReader(stream)).lines().collect(Collectors.joining("\n"))
        } catch (e: Exception) {
            null
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GL20.glCreateShader(type)
        GL20.glShaderSource(shader, source)
        GL20.glCompileShader(shader)

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            println("Failed to compile shader: ${GL20.glGetShaderInfoLog(shader, 1024)}")
            return -1
        }
        return shader
    }

    fun useShader(name: String): Boolean {
        val program = loadShader(name)
        if (program != -1) {
            GL20.glUseProgram(program)
            currentProgram = program
            return true
        }
        return false
    }

    fun setUniform(name: String, value: Float) {
        if (currentProgram != -1) {
            val location = GL20.glGetUniformLocation(currentProgram, name)
            if (location != -1) {
                GL20.glUniform1f(location, value)
            } else {
                // println("[ShaderManager] Uniform not found: $name in program $currentProgram")
            }
        }
    }

    fun setUniform(name: String, value: Int) {
        if (currentProgram != -1) {
            val location = GL20.glGetUniformLocation(currentProgram, name)
            if (location != -1) {
                GL20.glUniform1i(location, value)
            }
        }
    }

    fun setUniform(name: String, value: Boolean) {
        setUniform(name, if (value) 1 else 0)
    }

    fun stopShader() {
        GL20.glUseProgram(0)
        currentProgram = -1
    }
}
