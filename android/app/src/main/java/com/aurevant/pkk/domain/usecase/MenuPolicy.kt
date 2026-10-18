package com.aurevant.pkk.domain.usecase

/**
 * Cermin doc/menu-per-role.md: tab/baris menu apa yang boleh tampil
 * untuk sekumpulan izin. Murni (tanpa Android) agar bisa di-unit-test.
 */
object MenuPolicy {
    fun can(route: String, perms: Set<String>): Boolean = when (route) {
        "dashboard", "attendance", "notifications", "leaves", "slip", "card" -> true
        "pos" -> "transaksi.buat" in perms
        "nota" -> "nota.lihat" in perms
        "jasa" -> "transaksi.buat" in perms || "jasa.proses" in perms
        "gudang" -> "stok.transfer" in perms || "stok.opname" in perms || "po.buat" in perms
        "approvals" -> setOf("transaksi.buat", "retur.buat", "po.buat", "stok.transfer", "stok.opname").any { it in perms }
        "pelanggan" -> "pelanggan.lihat" in perms || "pelanggan.kelola" in perms
        "tim" -> "absensi.approve" in perms || "absensi.koreksi" in perms || "gaji.kelola" in perms
        "keuangan" -> "keuangan.lihat" in perms || "biaya.input" in perms
        "gaji" -> "gaji.kelola" in perms
        else -> false
    }

    /** Urutan tab bawah prioritas operasional; selalu mulai Dasbor + Absensi. */
    fun bottomTabs(perms: Set<String>): List<String> {
        val tabs = mutableListOf("dashboard", "attendance")
        for (r in listOf("pos", "nota", "jasa", "gudang", "approvals", "tim", "keuangan", "gaji")) {
            if (can(r, perms)) tabs += r
            if (tabs.size >= 5) break
        }
        if (can("notifications", perms) && tabs.size < 5) tabs += "notifications"
        return tabs
    }
}
