package app.lineo

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What Lineo asks the platform for, asserted against the installed package (P1-10b).
 *
 * On a device rather than by reading the manifest, because the manifest is not what ships:
 * every library merges its own into it, and the question `docs/ANDROID_STANDARDS.md` §5 asks
 * — "is the permission list exactly this?" — can only be answered by the package the user
 * would install.
 *
 * A permission that appears here without a line in this test is the test doing its job.
 */
class ManifestSecurityTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val packages: PackageManager = context.packageManager

    @Test
    fun asksForNoCapabilityAtAll() {
        val info = packages.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toList()

        // §5 permits INTERNET and ACCESS_NETWORK_STATE, and both are for currency rates and
        // ads — neither of which exists yet, so the honest list today is neither of them.
        // The one entry is androidx.core's signature-level self-permission, which keeps its
        // own runtime receivers unexported and is not a capability anyone can grant.
        assertEquals(listOf("${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"), requested)
    }

    @Test
    fun refusesCleartextTraffic() {
        // The network security config, as the platform reads it — not as the XML reads.
        assertFalse(
            "cleartext must be refused before there is any network code to refuse it for",
            packages.getApplicationInfo(context.packageName, 0).let { info ->
                info.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC != 0
            },
        )
    }

    @Test
    fun keepsBackupOnForDeviceTransferOnly() {
        val info = packages.getApplicationInfo(context.packageName, 0)

        // Backup stays enabled, because turning it off would take device transfer with it.
        // What is *in* the backup is the rules files' business: cloud excluded, transfer
        // included, both of which are XML the platform reads rather than flags to assert.
        assertTrue(info.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
    }
}
