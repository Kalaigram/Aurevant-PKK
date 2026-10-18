package com.aurevant.pkk

import com.aurevant.pkk.domain.usecase.MenuPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cermin doc/menu-per-role.md. Tanpa Android: jalan di JVM biasa. */
class MenuPolicyTest {
    private val kasir = setOf("transaksi.buat", "nota.lihat", "shift.buka", "shift.tutup", "pelanggan.lihat")
    private val gudang = setOf("stok.transfer", "stok.opname", "po.buat", "nota.lihat")
    private val pelaksana = setOf("nota.lihat", "jadwal.lihat", "jasa.proses", "gaji.lihat_sendiri")
    private val keuangan = setOf("keuangan.lihat", "biaya.input", "biaya.setujui", "periode.tutup", "laporan.konsolidasi", "gaji.kelola", "nota.lihat", "dashboard.konsolidasi")
    private val hr = setOf("karyawan.kelola", "jadwal.lihat", "jadwal.atur", "shift.kelola", "absensi.koreksi", "absensi.approve", "gaji.kelola", "gaji.setujui")

    @Test
    fun kasirLihatKasirNotaPelangganTapiBukanGudang() {
        assertTrue(MenuPolicy.can("pos", kasir))
        assertTrue(MenuPolicy.can("nota", kasir))
        assertTrue(MenuPolicy.can("pelanggan", kasir))
        assertFalse(MenuPolicy.can("gudang", kasir))
        assertFalse(MenuPolicy.can("keuangan", kasir))
    }

    @Test
    fun pelaksanaHanyaJasaDanHalamanGlobal() {
        assertTrue(MenuPolicy.can("jasa", pelaksana))
        assertTrue(MenuPolicy.can("attendance", pelaksana))
        assertFalse(MenuPolicy.can("pos", pelaksana))
        assertFalse(MenuPolicy.can("approvals", pelaksana))
        assertFalse(MenuPolicy.can("gudang", pelaksana))
    }

    @Test
    fun gudangLihatGudangDanSetujuTapiBukanKasir() {
        assertTrue(MenuPolicy.can("gudang", gudang))
        assertTrue(MenuPolicy.can("approvals", gudang))
        assertFalse(MenuPolicy.can("pos", gudang))
        assertFalse(MenuPolicy.can("tim", gudang))
    }

    @Test
    fun keuanganDanHrDapatModulnya() {
        assertTrue(MenuPolicy.can("keuangan", keuangan))
        assertTrue(MenuPolicy.can("gaji", keuangan))
        assertFalse(MenuPolicy.can("gaji", hr.minus("gaji.kelola")))
        assertTrue(MenuPolicy.can("tim", hr))
        assertTrue(MenuPolicy.can("gaji", hr))
    }

    @Test
    fun tabBawahSelaluMulaiDasborAbsensiMaksimalLima() {
        for (perms in listOf(kasir, gudang, pelaksana, keuangan, hr)) {
            val tabs = MenuPolicy.bottomTabs(perms)
            assertEquals("dashboard", tabs.first())
            assertEquals("attendance", tabs[1])
            assertTrue(tabs.size <= 5)
        }
        assertTrue(MenuPolicy.bottomTabs(kasir).contains("pos"))
        assertTrue(MenuPolicy.bottomTabs(pelaksana).contains("jasa"))
        assertTrue(MenuPolicy.bottomTabs(hr).contains("tim"))
    }
}
