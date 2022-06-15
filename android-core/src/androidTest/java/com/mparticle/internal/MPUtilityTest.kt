package com.mparticle.internal

import com.mparticle.testutils.BaseCleanInstallEachTest
class MPUtilityTest : BaseCleanInstallEachTest() {
    @org.junit.Test
    fun testInstantAppDetectionTest() {
        org.junit.Assert.assertFalse(MPUtility.isInstantApp(context))
    }

    @org.junit.Test
    @Throws(java.lang.Exception::class)
    fun testNullMapKey() {
        var map: MutableMap<*, *> = java.util.HashMap<Any?, Any?>()
        map["key1"] = "val1"
        map["key2"] = "val2"
        org.junit.Assert.assertFalse(MPUtility.containsNullKey(map))
        map[null] = "val3"
        org.junit.Assert.assertTrue(MPUtility.containsNullKey(map))
        map = java.util.Hashtable<Any?, Any?>()
        map["key1"] = "val1"
        map["key2"] = "val2"
        org.junit.Assert.assertFalse(MPUtility.containsNullKey(map))
        map = java.util.TreeMap(map)
        org.junit.Assert.assertFalse(MPUtility.containsNullKey(map))
        map = java.util.LinkedHashMap(map)
        org.junit.Assert.assertFalse(MPUtility.containsNullKey(map))
    }

    @org.junit.Test
    @Throws(java.lang.Exception::class)
    fun testGetInstrumentedNetworkType() {
        val manager: TelephonyManager =
            context.getSystemService(android.content.Context.TELEPHONY_SERVICE) as TelephonyManager
        val result = MPUtility.getNetworkType(context, manager)
        org.junit.Assert.assertNull(result)
    }
}