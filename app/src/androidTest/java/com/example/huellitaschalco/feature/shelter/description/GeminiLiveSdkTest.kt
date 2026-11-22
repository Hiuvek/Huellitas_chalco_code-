package com.example.huellitaschalco.feature.shelter.description

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.huellitaschalco.BuildConfig
import com.example.huellitaschalco.app.HuellitasApplication
import com.example.huellitaschalco.feature.shelter.description.domain.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Se ejecuta explícitamente solo después de habilitar Gemini y autorizar App Check. */
class GeminiLiveSdkTest {
    @Test
    fun generatesARealProposalWithoutWritingOrPublishingADog(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("hu11RealAI") == "true")
        assumeTrue(BuildConfig.FIREBASE_AI_PROJECT == "huellitas-chalco")
        val generator =
            ApplicationProvider.getApplicationContext<HuellitasApplication>()
                .container
                .descriptionGenerator
        assertTrue(generator.isConfigured)
        val input = DescriptionInput("Luna", 12, "Mediano", "Hembra", "")
        val text = withTimeout(45_000) { generator.generate(input) }
        assertEquals(text, DescriptionPolicy.validate(input, text))
        assertTrue(text.length >= 40)
    }
}
