package app.lineo.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/**
 * The journey both the benchmarks and the baseline profile are taken from.
 *
 * One place, because a profile generated from a different journey than the one measured
 * would optimise code the measurement never runs. Everything here is driven through the
 * keypad, by its spoken label — the same labels `docs/CONVENTIONS.md` §8 requires TalkBack to
 * read, which makes the accessibility work of P1-08 the thing that makes this findable.
 */
internal fun MacrobenchmarkScope.typeDocument() {
    device.wait(Until.hasObject(By.pkg(LINEO).depth(0)), UI_TIMEOUT_MILLIS)

    // `12 + 3` — digits, an operator, and the evaluation each keystroke triggers.
    press("1", "2", ADD, "3")
    press(EQUALS)

    // A second line that reads the first, which is what makes this a notepad rather than a
    // calculator: the dependency graph of `docs/ARCHITECTURE.md` §6 is exercised here.
    press("7", MULTIPLY, "6")
    press(EQUALS)

    device.waitForIdle()
}

/** Presses keys by the description a screen reader would read, in order. */
private fun MacrobenchmarkScope.press(vararg keys: String) {
    keys.forEach { key ->
        val selector = By.desc(key).takeIf { key.length > 1 } ?: By.text(key)
        device.wait(Until.findObject(selector), UI_TIMEOUT_MILLIS)?.click()
    }
}

/** Spoken labels, from `core/ui/src/main/res/values/strings.xml`. */
private const val ADD = "Add"
private const val MULTIPLY = "Multiply"
private const val EQUALS = "Equals"

/** Long enough for a cold emulator, short enough that a hang fails rather than hangs CI. */
private const val UI_TIMEOUT_MILLIS = 10_000L
