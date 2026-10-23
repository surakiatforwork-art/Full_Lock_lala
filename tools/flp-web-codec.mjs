// Browser/Node reference encoder for Full Lock PHANToM v2; no server or secret required.
export const fields = Object.freeze([
  ['price', true], ['min', '100'], ['max', '2000'], ['formula', 0], ['fee', 20],
  ['originOn', false], ['origins', ''], ['destOn', false], ['destinations', ''],
  ['excludeOriginOn', false], ['excludedOrigins', ''], ['excludeDestOn', false], ['excludedDestinations', ''],
  ['type', 0], ['bulk', false], ['themeColor', 0xffe94a19 | 0], ['invertText', false], ['serviceMask', 1],
]);
const utf8 = new TextEncoder();
export function crc32(bytes) {
  let crc = 0xffffffff;
  for (const byte of bytes) { crc ^= byte; for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0); }
  return (crc ^ 0xffffffff) >>> 0;
}
export async function encodeProfile(name, settings = {}) {
  name = name.trim().normalize('NFC');
  if (!name || [...name].length > 60 || /[\u0000-\u001f\u007f-\u009f]/u.test(name)) throw Error('Invalid profile name');
  if (Object.keys(settings).some(key => !fields.some(([known]) => key === known))) throw Error('Unknown setting');
  const values = Object.fromEntries(fields.map(([key, fallback]) => [key, settings[key] ?? fallback]));
  for (const [key, fallback] of fields) {
    if (typeof values[key] !== typeof fallback || (typeof fallback === 'number' && (!Number.isInteger(values[key]) || values[key] < -2147483648 || values[key] > 2147483647))) throw Error('Wrong field type');
    if (typeof values[key] === 'string' && utf8.encode(values[key]).length > 16384) throw Error('Field too large');
  }
  const money = value => { if (!/^[0-9]{1,10}(\.[0-9]{1,2})?$/.test(value)) throw Error('Invalid price'); const [whole, fraction = ''] = value.split('.'); return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0')); };
  if (money(values.min) > money(values.max)) throw Error('Invalid price range');
  if (values.formula < 0 || values.formula > 3 || values.fee < 0 || values.fee > 100 || values.type < 0 || values.type > 2 || values.serviceMask < 1 || values.serviceMask > 3 || (values.themeColor >>> 24) !== 255) throw Error('Invalid setting range');
  const records = [[1, name], ...fields.map(([key], index) => [index + 10, values[key]])];
  const chunks = records.map(([id, value]) => {
    const type = typeof value === 'string' ? 1 : typeof value === 'boolean' ? 2 : 3;
    const payload = type === 1 ? utf8.encode(value) : new Uint8Array(type === 2 ? 1 : 4);
    if (type === 2) payload[0] = value ? 1 : 0;
    if (type === 3) new DataView(payload.buffer).setInt32(0, value, false);
    const record = new Uint8Array(payload.length + 6); record[0] = id; record[1] = type;
    new DataView(record.buffer).setUint32(2, payload.length, false); record.set(payload, 6); return record;
  });
  const raw = new Uint8Array(2 + chunks.reduce((n, chunk) => n + chunk.length, 0));
  new DataView(raw.buffer).setUint16(0, chunks.length, false); let offset = 2; for (const chunk of chunks) { raw.set(chunk, offset); offset += chunk.length; }
  const compressed = new Uint8Array(await new Response(new Blob([raw]).stream().pipeThrough(new CompressionStream('deflate'))).arrayBuffer());
  const file = new Uint8Array(21 + compressed.length); file.set(utf8.encode('FLP-PHTM')); const header = new DataView(file.buffer);
  file[8] = 2; header.setUint32(9, compressed.length, false); header.setUint32(13, raw.length, false); header.setUint32(17, crc32(raw), false); file.set(compressed, 21); return file;
}
