package io.github.ptimulka.miecz.repositories

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class UserProgressRepositoryTest {

    private val context: Context = mock()
    private val prefs: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()
    
    private var currentTime = 1000000L
    private val timeProvider: () -> Long = { currentTime }

    private lateinit var repository: UserProgressRepository

    @Before
    fun setup() {
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putInt(any(), any())).thenReturn(editor)
        whenever(editor.putLong(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.remove(any())).thenReturn(editor)
        
        repository = UserProgressRepository(context, prefs, timeProvider)
    }

    @Test
    fun `refreshShields adds one shield after 30 minutes`() {
        whenever(prefs.getInt(eq("shields_count"), any())).thenReturn(4)
        whenever(prefs.getLong(eq("last_shield_update_time"), any())).thenReturn(currentTime - 30 * 60 * 1000L)
        
        val newCount = repository.refreshShields()
        
        assertEquals(5, newCount)
        verify(editor).putInt("shields_count", 5)
        verify(editor).remove("last_shield_update_time")
    }

    @Test
    fun `refreshShields preserves remainder time`() {
        val thirtyMins = 30 * 60 * 1000L
        whenever(prefs.getInt(eq("shields_count"), any())).thenReturn(3)
        whenever(prefs.getLong(eq("last_shield_update_time"), any())).thenReturn(currentTime - (thirtyMins + 5 * 60 * 1000L))
        
        val newCount = repository.refreshShields()
        
        assertEquals(4, newCount)
        val expectedNewUpdateTime = currentTime - (thirtyMins + 5 * 60 * 1000L) + thirtyMins
        verify(editor).putLong("last_shield_update_time", expectedNewUpdateTime)
    }

    @Test
    fun `refreshShields caps at MAX_SHIELDS`() {
        whenever(prefs.getInt(eq("shields_count"), any())).thenReturn(4)
        whenever(prefs.getLong(eq("last_shield_update_time"), any())).thenReturn(currentTime - 120 * 60 * 1000L)
        
        val newCount = repository.refreshShields()
        
        assertEquals(5, newCount)
        verify(editor).putInt("shields_count", 5)
    }

    @Test
    fun `getTimeToNextShield returns correct remaining ms`() {
        whenever(prefs.getInt(eq("shields_count"), any())).thenReturn(4)
        whenever(prefs.getLong(eq("last_shield_update_time"), any())).thenReturn(currentTime - 10 * 60 * 1000L)
        
        val remaining = repository.getTimeToNextShield()
        assertEquals(20 * 60 * 1000L, remaining)
    }

    @Test
    fun `applyDailyRetentionDecay subtracts retention after one day`() {
        currentTime = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse("2026-08-07")!!.time
        
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.edit().putString("retention_decay_date", "2026-08-06").putInt("retention_1", 50).apply()
        
        val repo = UserProgressRepository(context, fakePrefs, timeProvider)
        repo.applyDailyRetentionDecay()
        
        assertEquals(45, fakePrefs.getInt("retention_1", -1))
        assertEquals("2026-08-07", fakePrefs.getString("retention_decay_date", ""))
    }

    @Test
    fun `applyDailyRetentionDecay does not drop below zero`() {
        currentTime = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse("2026-08-07")!!.time
        
        val fakePrefs = FakeSharedPreferences()
        fakePrefs.edit().putString("retention_decay_date", "2026-08-06").putInt("retention_1", 3).apply()
        
        val repo = UserProgressRepository(context, fakePrefs, timeProvider)
        repo.applyDailyRetentionDecay()
        
        assertEquals(0, fakePrefs.getInt("retention_1", -1))
    }
}

class FakeSharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = map[key] as? MutableSet<String> ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun edit(): SharedPreferences.Editor = FakeEditor(map)

    class FakeEditor(private val map: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        override fun putString(key: String, value: String?): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor { temp[key] = values; return this }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor { temp[key] = value; return this }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { temp[key] = value; return this }
        override fun remove(key: String): SharedPreferences.Editor { temp[key] = null; return this }
        override fun clear(): SharedPreferences.Editor { map.clear(); return this }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() { 
            temp.forEach { (k, v) -> if (v == null) map.remove(k) else map[k] = v }
            temp.clear()
        }
    }
}
