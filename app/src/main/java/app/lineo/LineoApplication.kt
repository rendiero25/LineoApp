package app.lineo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point and the root of the dependency graph.
 *
 * `:app` is the only module that knows the full set of calculator modules
 * (`docs/ARCHITECTURE.md` §1); they reach it through multibinding, never through a list
 * written here.
 */
@HiltAndroidApp
class LineoApplication : Application()
