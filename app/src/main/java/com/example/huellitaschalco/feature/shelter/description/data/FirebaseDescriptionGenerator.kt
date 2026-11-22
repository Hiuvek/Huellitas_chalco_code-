package com.example.huellitaschalco.feature.shelter.description.data

import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.core.firebase.FirebaseAiProvider
import com.example.huellitaschalco.feature.shelter.description.domain.*
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class FirebaseDescriptionGenerator(private val provider: FirebaseAiProvider) :
    DescriptionGenerator {
    override val isConfigured
        get() = provider.app != null

    override suspend fun generate(input: DescriptionInput): String {
        val app = requireNotNull(provider.app) { "Configura Firebase AI Logic para usar Gemini." }
        // Comprobar la autorización antes de enviar la ficha, sin desactivar App Check.
        try {
            FirebaseAppCheck.getInstance(app).getAppCheckToken(false).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw descriptionServiceFailure(e, checkingApp = true)
        }
        val model =
            Firebase.ai(app = app, backend = GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = BuildConfig.GEMINI_MODEL,
                    generationConfig =
                        generationConfig {
                            temperature = 0.1f
                            maxOutputTokens = 600
                        },
                    systemInstruction = content { text(DescriptionPolicy.instructions) },
                )
        val data =
            JSONObject()
                .put("nombre", input.name)
                .put("edadMeses", input.ageMonths)
                .put("tamano", input.size)
                .put("sexo", input.sex)
                .put("notas", input.notes)
        val result =
            try {
                model.generateContent(data.toString()).text
                    ?: throw DescriptionServiceFailure(DescriptionFailureKind.Unavailable)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DescriptionServiceFailure) {
                throw e
            } catch (e: Exception) {
                throw descriptionServiceFailure(e)
            }
        return DescriptionPolicy.validate(input, result)
    }
}
