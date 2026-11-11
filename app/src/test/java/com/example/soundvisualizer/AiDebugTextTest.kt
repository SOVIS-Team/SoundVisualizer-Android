package com.example.soundvisualizer

import com.example.soundvisualizer.ai.AiClassificationResult
import com.example.soundvisualizer.ai.YamnetCoarseClassifier.TopClassHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AiDebugTextTest {
    private fun result(
        coarse: String = AiClassification.DANGER,
        display: String = "Gunshot, gunfire",
        confidence: Float = 0.41f,
        meetsThreshold: Boolean = true,
        timestampMs: Long = 1_000L,
        dangerCuePromoted: Boolean = false,
        top5: List<TopClassHit> = emptyList()
    ) = AiClassificationResult(
        coarse = coarse,
        display = display,
        confidence = confidence,
        top5 = top5,
        dangerCuePromoted = dangerCuePromoted,
        meetsThreshold = meetsThreshold,
        timestampMs = timestampMs,
        preprocessMs = 12.0,
        yamnetMs = 48.0,
        totalMs = 65.0
    )

    private fun format(result: AiClassificationResult?, nowMs: Long = 1_200L) =
        AiDebugText.format(result, nowMs, level = 0.14f, shown = true, aiAvailable = true)

    @Test fun `조기 표시의 첫 줄은 기본 사람 소리 이름 어느 것이 와도 한 줄에 든다`() {
        // 첫 줄은 오버레이가 "종류 ● 이름 확신도" 로 붙인다(점과 양옆 여백을 세 칸으로 센다). 조기 표시는 종류가 "SPEECH ~PREVIEW" 로
        // 길어지지만, 사용자 지정이 하나라도 있으면 쓰지 않으므로 이름은 늘 기본 종류가 사람 소리인 것이다. 그 가운데 가장 긴 이름
        // (Hubbub, speech noise, speech babble)이 정확히 한 줄을 채운다. 사람 소리에 더 긴 이름을 넣으면 여기서 걸린다(#456).
        val names = checkNotNull(javaClass.classLoader).getResourceAsStream("ai/yamnet_class_map.csv").use {
            com.example.soundvisualizer.ai.YamnetCoarseClassifier.loadClassNames(checkNotNull(it))
        }
        val speech = names.filter { com.example.soundvisualizer.ai.YamnetThreeClassMapper.mapDisplayNameToCoarse(it) == AiClassification.SPEECH }
        assertTrue("사람 소리 이름을 찾지 못했다", speech.size >= 30)
        for (name in speech) {
            val lines = format(result(coarse = AiClassification.SPEECH, display = name, confidence = 0.99f).copy(speechPreview = true))
            assertEquals("SPEECH ~PREVIEW", lines.coarse)
            val header = lines.coarse.length + 3 + lines.label.length + 1 + lines.confidence.length
            assertTrue("$header: ${lines.coarse} ● ${lines.label} ${lines.confidence}", header <= AiDebugText.HUD_MAX_COLUMNS)
        }
    }

    @Test fun `종류와 원래 이름과 확신도를 보여준다`() {
        val lines = format(result())
        assertEquals("DANGER", lines.coarse)
        assertEquals("Gunshot, gunfire", lines.label)
        assertEquals("0.41", lines.confidence)
        assertEquals(AiClassification.DANGER, lines.colorLabel)
    }

    @Test fun `임계값 나이와 YAMNet safety cue를 보여준다`() {
        val lines = format(result(meetsThreshold = false, dangerCuePromoted = true))
        assertEquals("thr N   age 0.2s", lines.detail)
        assertEquals("cue Y", lines.verdict)
        assertEquals("lvl 0.14   shown Y   65ms (12/48)", lines.timing)
    }

    @Test fun `top5는 순위와 고정폭 확률로 보인다`() {
        val hit = TopClassHit(0, "Vehicle horn, car horn, honking", 0.1f)
        val line = format(result(top5 = listOf(hit))).top5.single()
        assertTrue(line.contains("Vehicle horn, car horn, h" + AiDebugText.ELLIPSIS))
        assertTrue(line.endsWith(" 0.10"))
    }

    @Test fun `결과 없음을 구분한다`() {
        val waiting = format(null)
        assertEquals(AiDebugText.COARSE_WAITING, waiting.coarse)
        assertNull(waiting.colorLabel)
    }

    @Test fun `로캘과 무관하게 숫자를 표기한다`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"))
            assertEquals("0.41", format(result()).confidence)
        } finally {
            Locale.setDefault(original)
        }
    }
}
