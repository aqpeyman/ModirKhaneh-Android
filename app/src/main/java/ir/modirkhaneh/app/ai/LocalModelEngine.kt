package ir.modirkhaneh.app.ai

/** Boundary for the future JNI/llama.cpp implementation. No native engine is linked in this starter. */
interface LocalModelEngine {
    val isLoaded: Boolean
    suspend fun load(modelPath: String)
    suspend fun generate(prompt: String, maxTokens: Int = 256): String
    fun close()
}

/** Model metadata selected for evaluation; this is not a downloaded model file. */
data class LocalModelSpec(
    val repository: String = "ibnsina-llm/ibnsina-1.5b",
    val filename: String = "ibnsina-1.5b-Q4_K_M.gguf",
    val expectedSize: String = "حدود ۹۰۴ مگابایت",
    val contextTokens: Int = 2048,
    val license: String = "Apache-2.0"
)
