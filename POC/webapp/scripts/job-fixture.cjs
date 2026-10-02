const { randomUUID } = require('node:crypto');
exports.completedJob = result => ({
  analysisId: randomUUID(), status: 'COMPLETED', createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(), completedAt: new Date().toISOString(),
  expiresAt: new Date(Date.now() + 86400000).toISOString(), pollAfterSeconds: null, result, error: null,
});
