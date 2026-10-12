export function rupiah(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') return 'Rp0'
  return 'Rp' + Number(value).toLocaleString('id-ID')
}

export function tanggal(iso: string | null | undefined): string {
  if (!iso) return '-'
  const d = new Date(iso.length <= 10 ? iso + 'T00:00:00' : iso)
  return d.toLocaleDateString('id-ID', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function waktuRelatif(iso: string | null | undefined): string {
  if (!iso) return '-'
  const diff = Date.now() - new Date(iso).getTime()
  const menit = Math.floor(diff / 60000)
  if (menit < 1) return 'baru saja'
  if (menit < 60) return `${menit} mnt lalu`
  const jam = Math.floor(menit / 60)
  if (jam < 24) return `${jam} jam lalu`
  const hari = Math.floor(jam / 24)
  if (hari < 30) return `${hari} hari lalu`
  return tanggal(iso)
}

export function fieldErrors(e: unknown): string {
  if (e instanceof Error && 'errors' in e) {
    const errors = (e as { errors: Record<string, string[]> | null }).errors
    if (errors) return Object.values(errors).flat().join(' ')
  }
  return e instanceof Error ? e.message : 'Terjadi kesalahan.'
}

const BILANG = ['', 'satu', 'dua', 'tiga', 'empat', 'lima', 'enam', 'tujuh', 'delapan', 'sembilan', 'sepuluh', 'sebelas']

function ratusan(n: number): string {
  if (n < 12) return BILANG[n]!
  if (n < 20) return ratusan(n - 10) + ' belas'
  if (n < 100) return (ratusan(Math.floor(n / 10)) + ' puluh ' + ratusan(n % 10)).trim()
  if (n < 200) return ('seratus ' + ratusan(n - 100)).trim()
  return (ratusan(Math.floor(n / 100)) + ' ratus ' + ratusan(n % 100)).trim()
}

/** Terbilang rupiah sederhana untuk slip. */
export function terbilang(n: number): string {
  if (n === 0) return 'nol rupiah'
  const negatif = n < 0
  n = Math.floor(Math.abs(n))
  const parts: string[] = []
  const trilyun = Math.floor(n / 1e12)
  const milyar = Math.floor((n % 1e12) / 1e9)
  const juta = Math.floor((n % 1e9) / 1e6)
  const ribu = Math.floor((n % 1e6) / 1e3)
  const sisa = n % 1e3
  if (trilyun) parts.push(ratusan(trilyun) + ' triliun')
  if (milyar) parts.push(ratusan(milyar) + ' miliar')
  if (juta) parts.push(ratusan(juta) + ' juta')
  if (ribu) parts.push(ribu === 1 ? 'seribu' : ratusan(ribu) + ' ribu')
  if (sisa) parts.push(ratusan(sisa))
  return ((negatif ? 'minus ' : '') + parts.join(' ') + ' rupiah').replace(/\s+/g, ' ').trim()
}
