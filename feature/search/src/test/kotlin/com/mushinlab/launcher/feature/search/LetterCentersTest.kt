package com.mushinlab.launcher.feature.search

import com.google.common.truth.Truth.assertThat
import com.mushinlab.launcher.core.search.KeyboardLayouts
import org.junit.Test

class LetterCentersTest {
    private fun centers(layoutId: String): Map<Char, Pair<Float, Float>> {
        val layout = KeyboardLayouts.all.single { it.id == layoutId }
        return KeyboardGeometry.compute(layout, WIDTH, ROW, PADDING, GAP)
            .mapNotNull { key -> key.spec.char?.let { it to (key.rect.center.x to key.rect.center.y) } }
            .toMap()
    }

    private fun assertSame(actual: Map<Char, Pair<Float, Float>>, expected: Map<Char, Pair<Float, Float>>) {
        assertThat(actual.keys).containsExactlyElementsIn(expected.keys)
        expected.forEach { (char, center) ->
            assertThat(actual.getValue(char).first).isWithin(TOLERANCE).of(center.first)
            assertThat(actual.getValue(char).second).isWithin(TOLERANCE).of(center.second)
        }
    }

    @Test
    fun enLetterCentersStayWhereTheyWereWithTheDeleteKey() {
        assertSame(centers("en"), EN)
    }

    @Test
    fun ruLetterCentersStayWhereTheyWereWithTheDeleteKey() {
        assertSame(centers("ru"), RU)
    }

    @Test
    fun glideCentersStayWhereTheyWereWithTheDeleteKey() {
        val glide = glideCenters(WIDTH, ROW, PADDING, GAP).mapValues { (_, p) -> p.x to p.y }
        assertSame(glide, EN)
    }

    private companion object {
        const val WIDTH = 1080f
        const val ROW = 136f
        const val PADDING = 10f
        const val GAP = 10f
        const val TOLERANCE = 0.01f

        val EN = mapOf(
            'q' to (54.0f to 78.0f),
            'w' to (162.0f to 78.0f),
            'e' to (270.0f to 78.0f),
            'r' to (378.0f to 78.0f),
            't' to (486.0f to 78.0f),
            'y' to (594.0f to 78.0f),
            'u' to (702.0f to 78.0f),
            'i' to (810.0f to 78.0f),
            'o' to (918.0f to 78.0f),
            'p' to (1026.0f to 78.0f),
            'a' to (108.0f to 214.0f),
            's' to (216.0f to 214.0f),
            'd' to (324.0f to 214.0f),
            'f' to (432.0f to 214.0f),
            'g' to (540.0f to 214.0f),
            'h' to (648.0f to 214.0f),
            'j' to (756.0f to 214.0f),
            'k' to (864.0f to 214.0f),
            'l' to (972.0f to 214.0f),
            'z' to (216.0f to 350.0f),
            'x' to (324.0f to 350.0f),
            'c' to (432.0f to 350.0f),
            'v' to (540.0f to 350.0f),
            'b' to (648.0f to 350.0f),
            'n' to (756.0f to 350.0f),
            'm' to (864.0f to 350.0f),
        )

        val RU = mapOf(
            'й' to (45.0f to 78.0f),
            'ц' to (135.0f to 78.0f),
            'у' to (225.0f to 78.0f),
            'к' to (315.0f to 78.0f),
            'е' to (405.0f to 78.0f),
            'н' to (495.0f to 78.0f),
            'г' to (585.0f to 78.0f),
            'ш' to (675.0f to 78.0f),
            'щ' to (765.0f to 78.0f),
            'з' to (855.0f to 78.0f),
            'х' to (945.0f to 78.0f),
            'ъ' to (1035.0f to 78.0f),
            'ф' to (49.090908f to 214.0f),
            'ы' to (147.27272f to 214.0f),
            'в' to (245.45453f to 214.0f),
            'а' to (343.63635f to 214.0f),
            'п' to (441.81818f to 214.0f),
            'р' to (540.0f to 214.0f),
            'о' to (638.18176f to 214.0f),
            'л' to (736.36365f to 214.0f),
            'д' to (834.5454f to 214.0f),
            'ж' to (932.7273f to 214.0f),
            'э' to (1030.909f to 214.0f),
            'я' to (180.0f to 350.0f),
            'ч' to (270.0f to 350.0f),
            'с' to (360.0f to 350.0f),
            'м' to (450.0f to 350.0f),
            'и' to (540.0f to 350.0f),
            'т' to (630.0f to 350.0f),
            'ь' to (720.0f to 350.0f),
            'б' to (810.0f to 350.0f),
            'ю' to (900.0f to 350.0f),
        )
    }
}
