package com.example.soundvisualizer.tutorial

import com.example.soundvisualizer.R
import org.junit.Assert.assertEquals
import org.junit.Test

class TutorialPageTest {

    @Test
    fun `외부 사운드 모드면 소리 쪽은 마이크로 주변 소리를 그린다고 설명한다`() {
        // "폰에서 재생되는 소리만, 주변 소리는 듣지 않는다" 는 외부 사운드 모드에서 거짓이다(#226).
        assertEquals(R.string.tutorial_sound_body, TutorialPage.Sound.bodyFor(externalSoundMode = false))
        assertEquals(R.string.tutorial_sound_body_external, TutorialPage.Sound.bodyFor(externalSoundMode = true))
    }

    @Test
    fun `방향 쪽은 외부 사운드 모드에서도 좌우를 구분한다고 설명한다`() {
        // 모드에 따라 폰마다 다른 설명이 보여 헷갈린다는 의견으로 하나로 맞췄다(#444).
        assertEquals(R.string.tutorial_direction_title, TutorialPage.Direction.title)
        assertEquals(R.string.tutorial_direction_body, TutorialPage.Direction.bodyFor(externalSoundMode = false))
        assertEquals(R.string.tutorial_direction_body, TutorialPage.Direction.bodyFor(externalSoundMode = true))
    }

    @Test
    fun `소리 쪽 말고는 모드와 상관없다`() {
        for (page in TutorialPage.entries.filter { it != TutorialPage.Sound }) {
            for (external in listOf(false, true)) {
                assertEquals("${page.name} 쪽 설명", page.body, page.bodyFor(external))
            }
        }
    }
}
