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

    private fun classNames(): List<String> =
        checkNotNull(javaClass.classLoader).getResourceAsStream("ai/yamnet_class_map.csv").use {
            com.example.soundvisualizer.ai.YamnetCoarseClassifier.loadClassNames(checkNotNull(it))
        }

    private fun headerColumns(lines: AiDebugLines) =
        lines.coarse.length + AiDebugText.HEADER_DOT_COLUMNS + lines.label.length + 1 + lines.confidence.length

    @Test fun `조기 표시의 첫 줄은 어느 이름이 실려도 한 줄에 든다`() {
        // 첫 줄은 오버레이가 "종류 ● 이름 확신도" 로 붙인다. 조기 표시는 종류가 "SPEECH ~PREVIEW" 로 여덟 자 길다.
        // 이름은 그 프레임의 1위 이름이고 종류는 상위 3개의 투표라, 1위가 환경음 이름이어도 2·3위의 사람 소리가 합쳐 이기면 그 이름이
        // 실린다. #456 의 시험은 "이름은 늘 사람 소리 이름"이라고 잘못 가정해, "Livestock, farm animals, working animals" 가 실리면
        // 63자가 되어 두 줄로 꺾이는 것을 놓쳤다(#461). 521개 이름을 모두 넣어 본다.
        val names = classNames()
        assertEquals(521, names.size)
        var longest = 0
        var shortened = 0
        for (name in names) {
            val lines = format(result(coarse = AiClassification.SPEECH, display = name, confidence = 0.99f).copy(speechPreview = true))
            assertEquals("SPEECH ~PREVIEW", lines.coarse)
            val header = headerColumns(lines)
            assertTrue("$header: ${lines.coarse} ● ${lines.label} ${lines.confidence}", header <= AiDebugText.HUD_MAX_COLUMNS)
            longest = maxOf(longest, header)
            if (lines.label != name) {
                shortened++
                assertTrue(lines.label, lines.label.endsWith(AiDebugText.ELLIPSIS))
                assertTrue(lines.label, name.startsWith(lines.label.dropLast(AiDebugText.ELLIPSIS.length)))
            }
        }
        assertEquals(AiDebugText.HUD_MAX_COLUMNS, longest)
        assertTrue("줄인 이름이 하나도 없다", shortened > 0)
        // 기본 종류가 사람 소리인 이름은 가장 긴 것(35자)도 줄지 않는다. 조기 표시에 가장 자주 실리는 이름들이다.
        fun preview(name: String) =
            format(result(coarse = AiClassification.SPEECH, display = name, confidence = 0.99f).copy(speechPreview = true)).label
        assertEquals("Hubbub, speech noise, speech babble", preview("Hubbub, speech noise, speech babble"))
        assertEquals("Livestock, farm animals, working a" + AiDebugText.ELLIPSIS, preview("Livestock, farm animals, working animals"))
    }

    @Test fun `정규 결과의 첫 줄은 어느 이름도 줄이지 않는다`() {
        // 정규 결과는 종류 칸이 짧아 가장 긴 이름(40자)도 한 줄에 든다. 채점할 때 보는 이름이 잘리면 안 된다.
        for (coarse in listOf(AiClassification.AMBIENT, AiClassification.SPEECH, AiClassification.DANGER)) {
            for (name in classNames()) {
                val lines = format(result(coarse = coarse, display = name, confidence = 0.99f))
                assertEquals(name, lines.label)
                assertTrue(headerColumns(lines) <= AiDebugText.HUD_MAX_COLUMNS)
            }
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
