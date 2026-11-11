package com.example.soundvisualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 튜토리얼 3쪽의 범례는 종류 이름과 그 예시를 나란히 보인다("사람 소리  사람 목소리"). 두 글자가 같으면 같은 말이 두 번 보인다.
 *
 * 종류 이름을 '사람이 내는 소리' 뜻으로 바꾸면서(#446) 일본어·인도네시아어·태국어·터키어는 새 이름이 원래 예시와 같은 낱말이 되어
 * "人の声  人の声" 처럼 겹쳤다(#451). 이름을 바꿀 때는 예시도 함께 본다.
 *
 * 유닛 테스트는 app 모듈 폴더에서 돈다.
 */
class TutorialLegendStringsTest {

    private val resDir = File("src/main/res")

    @Test
    fun `어느 언어에서도 범례의 종류 이름과 예시가 같은 글자가 아니다`() {
        val dirs = resDir.listFiles { f -> f.isDirectory && (f.name == "values" || f.name.startsWith("values-")) }
            .orEmpty()
            .filter { File(it, "strings.xml").isFile }
            .map { it.name }
            .sorted()
        assertTrue("res 폴더를 찾지 못했습니다: ${resDir.absolutePath}", dirs.size >= 18)

        val same = ArrayList<String>()
        for (dir in dirs) {
            val strings = SoundGroupFixtures.strings(dir)
            for (type in listOf("ambient", "speech", "danger")) {
                val name = strings["tutorial_type_$type"] ?: continue
                val examples = strings["tutorial_type_${type}_examples"] ?: continue
                if (name.trim().equals(examples.trim(), ignoreCase = true)) same += "$dir/$type: $name"
            }
        }
        assertEquals("이름과 예시가 같은 범례", emptyList<String>(), same)
    }
}
