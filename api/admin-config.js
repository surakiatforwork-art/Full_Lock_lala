module.exports = (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  if (req.method !== 'GET') return res.status(405).end();
  try {
    const { apiKey, authDomain, projectId, appId } = JSON.parse(process.env.FULL_LOCK_FIREBASE_WEB_CONFIG);
    if (![apiKey, authDomain, projectId, appId].every(v => typeof v === 'string' && v)) throw new Error();
    return res.json({ apiKey, authDomain, projectId, appId });
  } catch {
    return res.status(503).json({ error: 'ยังไม่ได้ตั้งค่าการเข้าสู่ระบบ' });
  }
};
