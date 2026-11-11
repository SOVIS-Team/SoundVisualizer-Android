package com.example.soundvisualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 한국어 문구의 두 가지 규칙(#451).
 *
 * 1. 자리표시(%1$s 등) 바로 뒤에 받침에 따라 달라지는 조사를 붙이지 않는다. 종류 이름이 '대화음'에서 '사람 소리'로 바뀌자(#421)
 *    "%1$s은 …" 이 "사람 소리은 …" 으로 나왔다. 들어갈 말이 바뀌어도 맞도록 "%1$s 진동은 …" 처럼 뒤에 낱말을 둔다.
 * 2. 화면에서 무엇을 정하게 할 때는 '선택'을 쓴다. '고르다'가 섞이면 같은 동작이 화면마다 다른 말로 불린다.
 *
 * 유닛 테스트는 app 모듈 폴더에서 돈다.
 */
class KoreanCopyTest {

    private val ko: Map<String, String> by lazy { SoundGroupFixtures.strings("values-ko") }

    @Test
    fun `자리표시 바로 뒤에 받침을 타는 조사를 붙이지 않는다`() {
        // 따옴표로 감싼 자리표시(‘%2$s’으로)는 보지 않는다. 지금 그 자리에 들어가는 말은 진동 방식 이름(느림·중간 등)이고 모두 받침으로 끝난다.
        val particle = Regex("""%\d+\$[sd]([은는이가을를과와]|으로|로)(?![가-힣])""")
        val offenders = ko.filterValues { particle.containsMatchIn(it) }.keys.sorted()
        assertEquals("자리표시 뒤에 조사가 붙은 문구", emptyList<String>(), offenders)
    }

    @Test
    fun `외부 사운드 모드의 진동 상한 안내는 어느 종류 이름이 들어가도 맞는 문장이다`() {
        val text = ko.getValue("haptic_cap_external")
        for (type in listOf("sound_type_speech", "sound_type_danger")) {
            val shown = text.replace("%1\$s", ko.getValue(type)).replace("%2\$s", ko.getValue("haptic_mode_slow"))
            assertTrue(shown, shown.contains("${ko.getValue(type)} 진동은 ‘${ko.getValue("haptic_mode_slow")}’까지만 선택할 수 있습니다."))
        }
    }

    @Test
    fun `무엇을 정하게 할 때는 고르다가 아니라 선택을 쓴다`() {
        val word = Regex("고르|고른|고를|골라|고릅|고름|골랐")
        val offenders = ko.filterValues { word.containsMatchIn(it) }.keys.sorted()
        assertEquals("'고르다'가 남은 문구", emptyList<String>(), offenders)
    }
}
