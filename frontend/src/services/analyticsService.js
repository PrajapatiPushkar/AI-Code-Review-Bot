import api from './api';

export const analyticsService = {
  /**
   * Fetch review totals, finding totals and completion/failure summaries.
   * Endpoint: GET /api/v1/analytics/overview
   * Params: { from, to, repository, owner }
   */
  async getOverview(params = {}) {
    const response = await api.get('/analytics/overview', { params });
    return response.data || {
      totalReviews: 0,
      completedReviews: 0,
      failedReviews: 0,
      inProgressReviews: 0,
      totalFindings: 0
    };
  },

  /**
   * Fetch severity, category, and AI vs RULE source breakdowns.
   * Endpoint: GET /api/v1/analytics/findings
   * Params: { from, to, repository, owner }
   */
  async getFindings(params = {}) {
    const response = await api.get('/analytics/findings', { params });
    return response.data || {
      totalFindings: 0,
      severityBreakdown: {},
      categoryBreakdown: {},
      sourceBreakdown: {}
    };
  },

  /**
   * Fetch review and finding activity trends over time.
   * Endpoint: GET /api/v1/analytics/trends
   * Params: { from, to, repository, owner }
   */
  async getTrends(params = {}) {
    const response = await api.get('/analytics/trends', { params });
    return response.data || [];
  },

  /**
   * Fetch repository-level review, finding summaries and health states.
   * Endpoint: GET /api/v1/analytics/repositories
   * Params: { from, to, repository, owner }
   */
  async getRepositories(params = {}) {
    const response = await api.get('/analytics/repositories', { params });
    return response.data || [];
  }
};

export default analyticsService;
