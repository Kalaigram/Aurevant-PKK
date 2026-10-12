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
