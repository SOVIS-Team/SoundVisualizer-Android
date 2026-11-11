package com.example.soundvisualizer.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 조기 표시(#422)의 두 시각. 둘 다 정규 추론이 소리를 가져간 때부터 잰다.
 *
 * 다음 정규 결과는 [RealtimeAiPipeline.AI_PREDICT_INTERVAL_MS] 뒤에 추론을 시작해, 전처리와 추론이 끝나야 나온다. 조기 표시가 그보다
 * 먼저 사라지면 사람 소리 → 환경음 → 사람 소리로 깜빡인다. 처음 값 275ms 는 추론이 25ms 라는 가정이었다(#456).
 */
class SpeechPreviewTimingTest {

    @Test
    fun `조기 표시는 다음 정규 추론이 시작할 때까지만 새로 받아들인다`() {
        assertEquals(RealtimeAiPipeline.AI_PREDICT_INTERVAL_MS * 1_000_000L, RealtimeAiPipeline.SPEECH_PREVIEW_ACCEPT_NS)
    }

    @Test
    fun `조기 표시는 추론이 100ms 걸리는 폰에서도 다음 정규 결과까지 이어진다`() {
        val nextResultAtNs = (RealtimeAiPipeline.AI_PREDICT_INTERVAL_MS + 100) * 1_000_000L
        assertTrue(
            "만료 ${RealtimeAiPipeline.SPEECH_PREVIEW_EXPIRY_NS}ns 가 다음 결과 ${nextResultAtNs}ns 보다 이르다",
            RealtimeAiPipeline.SPEECH_PREVIEW_EXPIRY_NS >= nextResultAtNs
        )
    }

    @Test
    fun `조기 표시는 다음 다음 정규 추론까지 남지는 않는다`() {
        // 다음 틱이 무음으로 건너뛰어지면 만료만이 조기 표시를 지운다. 두 주기를 넘기면 말이 끝난 뒤에도 사람 소리 색이 남는다.
        assertTrue(RealtimeAiPipeline.SPEECH_PREVIEW_EXPIRY_NS < 2 * RealtimeAiPipeline.AI_PREDICT_INTERVAL_MS * 1_000_000L)
    }
}
