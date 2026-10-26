const ACTION_LABELS: Record<string, string> = {
  'absensi.koreksi': 'Koreksi absensi',
  'access.role.assign': 'Beri role pengguna',
  'access.role.create': 'Buat role',
  'access.role.revoke': 'Cabut role pengguna',
  'access.role.update': 'Ubah role',
  'approval.ajukan': 'Ajukan persetujuan',
  'approval.diajukan': 'Pengajuan dibuat',
  'approval.setujui': 'Setujui pengajuan',
  'approval.tolak': 'Tolak pengajuan',
  'biaya.catat': 'Catat biaya',
  'biaya.setujui': 'Setujui biaya',
  'biaya.tolak': 'Tolak biaya',
  'hutang.bayar': 'Bayar hutang supplier',
  'karyawan.buat': 'Buat karyawan',
  'karyawan.reset_password': 'Reset sandi karyawan',
  'langganan.bayar': 'Bayar langganan',
  'langganan.berakhir': 'Langganan berakhir',
  'langganan.jatuh_tempo': 'Langganan jatuh tempo',
  'langganan.tagih': 'Tagih langganan',
  'langganan.ubah': 'Ubah langganan',
  'leave.approve': 'Setujui cuti',
  'leave.reject': 'Tolak cuti',
  'opname.approve': 'Setujui opname',
  'opname.disetujui': 'Opname disetujui',
  'payroll.approve': 'Setujui payroll',
  'payroll.pay': 'Bayar payroll',
  'payroll.review': 'Review payroll',
  'periode.buka': 'Buka periode akuntansi',
  'periode.tutup': 'Tutup periode akuntansi',
  'piutang.catat': 'Catat piutang',
  'piutang.terima': 'Terima piutang',
  'po.approve': 'Setujui PO',
  'po.cancel': 'Batalkan PO',
  'po.diajukan': 'PO diajukan',
  'po.disetujui': 'PO disetujui',
  'po.diterima': 'PO diterima',
  'po.order': 'Pesan PO',
  'po.receive': 'Terima PO',
  'po.setujui': 'Setujui PO',
  'po.submit': 'Kirim PO',
  'price.set': 'Ubah harga',
  'retur.buat': 'Buat retur',
  'sale.discount': 'Diskon nota',
  'sale.refund': 'Refund nota',
  'sale.retur': 'Retur nota',
  'sale.void': 'Void nota',
  'shift.close': 'Tutup shift',
  'shift.selisih': 'Selisih kas shift',
  'stock.opname.approve': 'Setujui opname',
  'stock.transfer.approve': 'Setujui transfer',
  'stock.transfer.cancel': 'Batalkan transfer',
  'stock.transfer.receive': 'Terima transfer',
  'stock.transfer.ship': 'Kirim transfer',
  'stock.transfer.submit': 'Ajukan transfer',
  'tenant.aktifkan': 'Aktifkan tenant',
  'tenant.tangguhkan': 'Tangguhkan tenant',
  'transaksi.void': 'Void transaksi',
  'transfer.approve': 'Setujui transfer',
  'transfer.diajukan': 'Transfer diajukan',
  'transfer.dikirim': 'Transfer dikirim',
  'transfer.disetujui': 'Transfer disetujui',
  'transfer.diterima': 'Transfer diterima',
}

const ENTITY_LABELS: Record<string, string> = {
  approval: 'Persetujuan',
  accounting_period: 'Periode akuntansi',
  attendance: 'Absensi',
  branch: 'Cabang',
  company: 'Perusahaan',
  customer: 'Pelanggan',
  expense: 'Biaya',
  holiday: 'Hari libur',
  item: 'Barang / jasa',
  leave: 'Cuti',
  payroll_period: 'Periode payroll',
  purchase_order: 'Purchase order',
  receivable: 'Piutang',
  role: 'Role akses',
  sale: 'Nota penjualan',
  stock_opname: 'Opname stok',
  stock_transfer: 'Transfer stok',
  subscription: 'Langganan',
  subscription_invoice: 'Tagihan langganan',
  supplier_bill: 'Tagihan supplier',
  user: 'Karyawan',
  work_shift: 'Shift kerja',
}

const AGING_BUCKET_LABELS: Record<string, string> = {
  lancar: 'Belum jatuh tempo',
  '1_30': 'Telat 1–30 hari',
  '31_60': 'Telat 31–60 hari',
  '61_90': 'Telat 61–90 hari',
  lebih_90: 'Telat > 90 hari',
}

const ATTENDANCE_STATUS_LABELS: Record<string, string> = {
  hadir: 'Hadir',
  terlambat: 'Terlambat',
  alpa: 'Alpa',
  izin: 'Izin',
  sakit: 'Sakit',
  cuti: 'Cuti',
}

const LEAVE_STATUS_LABELS: Record<string, string> = {
  diajukan: 'Diajukan',
  disetujui: 'Disetujui',
  ditolak: 'Ditolak',
}

const LEAVE_TYPE_LABELS: Record<string, string> = {
  izin: 'Izin',
  sakit: 'Sakit',
  cuti: 'Cuti',
}

const PAYROLL_STATUS_LABELS: Record<string, string> = {
  draft: 'Draft',
  review: 'Review',
  disetujui: 'Disetujui',
  dibayar: 'Dibayar',
}

const PAYROLL_COMPONENT_TYPE_LABELS: Record<string, string> = {
  monthly_fixed: 'Tetap bulanan',
  daily_rate: 'Harian',
  commission: 'Komisi jasa',
  manual: 'Manual',
}

const SUBSCRIPTION_STATUS_LABELS: Record<string, string> = {
  trial: 'Uji coba',
  aktif: 'Aktif',
  tenggang: 'Masa tenggang',
  read_only: 'Hanya baca',
  ditangguhkan: 'Ditangguhkan',
}

const TENANT_STATUS_LABELS: Record<string, string> = {
  aktif: 'Aktif',
  ditangguhkan: 'Ditangguhkan',
  diarsipkan: 'Diarsipkan',
}

const FIELD_LABELS: Record<string, string> = {
  amount: 'Nominal',
  balance: 'Saldo',
  branch_id: 'Cabang',
  closing_cash: 'Kas penutupan',
  closing_cash_counted: 'Kas fisik dihitung',
  company_id: 'Perusahaan',
  date: 'Tanggal',
  date_from: 'Tanggal mulai',
  date_to: 'Tanggal selesai',
  difference: 'Selisih',
  discount_amount: 'Nominal diskon',
  discount_percent: 'Persen diskon',
  divisor: 'Pembagi prorata',
  email: 'Email',
  ended_at: 'Tanggal selesai',
  entity_id: 'ID data terkait',
  entity_type: 'Jenis data',
  grand_total: 'Total akhir',
  hpp_total: 'Total HPP',
  is_manual: 'Input manual',
  item_id: 'Barang',
  items: 'Rincian barang',
  lat: 'Latitude',
  lng: 'Longitude',
  method: 'Metode',
  name: 'Nama',
  net_omzet: 'Net omzet',
  note: 'Catatan',
  opening_cash: 'Kas awal',
  password: 'Sandi',
  payload: 'Data pengajuan',
  phone: 'Telepon',
  price: 'Harga',
  ptkp_status: 'Status PTKP',
  qty: 'Jumlah',
  reason: 'Alasan',
  reference: 'Referensi',
  refund_total: 'Total refund',
  role: 'Role',
  role_id: 'Role',
  started_at: 'Tanggal mulai',
  status: 'Status',
  subtotal: 'Subtotal',
  tax_total: 'Total pajak',
  tendered: 'Uang diterima',
  total: 'Total',
  transactions: 'Transaksi',
  user_id: 'Karyawan',
  work_shift_id: 'Shift',
}

function humanize(value: string) {
  return value
    .split(/[._-]/)
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export function labelAksi(action: string, fallback?: string | null) {
  return ACTION_LABELS[action] ?? (fallback?.trim() || humanize(action))
}

export function labelAgingBucket(bucket: string) {
  return AGING_BUCKET_LABELS[bucket] ?? humanize(bucket)
}

export function labelAttendanceStatus(status: string) {
  return ATTENDANCE_STATUS_LABELS[status] ?? humanize(status)
}

export function labelLeaveStatus(status: string) {
  return LEAVE_STATUS_LABELS[status] ?? humanize(status)
}

export function labelLeaveType(type: string) {
  return LEAVE_TYPE_LABELS[type] ?? humanize(type)
}

export function labelPayrollStatus(status: string) {
  return PAYROLL_STATUS_LABELS[status] ?? humanize(status)
}

export function labelPayrollComponentType(type: string) {
  return PAYROLL_COMPONENT_TYPE_LABELS[type] ?? humanize(type)
}

export function labelSubscriptionStatus(status: string) {
  return SUBSCRIPTION_STATUS_LABELS[status] ?? humanize(status)
}

export function labelTenantStatus(status: string) {
  return TENANT_STATUS_LABELS[status] ?? humanize(status)
}

export function labelEntitas(entity: string | null | undefined) {
  if (!entity) return 'Data'
  const key = entity.split('\\').pop() ?? entity
  return ENTITY_LABELS[key] ?? humanize(key)
}

export function labelField(field: string) {
  return FIELD_LABELS[field] ?? humanize(field)
}

export function formatValue(value: unknown): string {
  if (value === null || value === undefined || value === '') return '—'
  if (typeof value === 'boolean') return value ? 'Ya' : 'Tidak'
  if (typeof value === 'number') return value.toLocaleString('id-ID')
  if (typeof value === 'object') return JSON.stringify(value)
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}(T|$)/.test(value)) {
    const date = new Date(value)
    if (!Number.isNaN(date.getTime())) {
      return value.includes('T')
        ? date.toLocaleString('id-ID', {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          })
        : date.toLocaleDateString('id-ID', { day: '2-digit', month: 'short', year: 'numeric' })
    }
  }
  return String(value)
}

export interface DiffRow {
  key: string
  label: string
  before: unknown
  after: unknown
  changed: boolean
}

export function diffFields(
  before: Record<string, unknown> | null,
  after: Record<string, unknown> | null,
): DiffRow[] {
  const keys = new Set([...Object.keys(before ?? {}), ...Object.keys(after ?? {})])
  return [...keys]
    .map((key) => {
      const oldValue = before?.[key]
      const newValue = after?.[key]
      return {
        key,
        label: labelField(key),
        before: oldValue,
        after: newValue,
        changed: JSON.stringify(oldValue) !== JSON.stringify(newValue),
      }
    })
    .filter((row) => row.changed)
}
