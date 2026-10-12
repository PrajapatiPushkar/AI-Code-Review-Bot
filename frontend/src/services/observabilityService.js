import api from './api';

export const observabilityService = {
  /**
   * Fetch live engineering observability summary and operational metrics.
   * Endpoint: GET /api/v1/observability/metrics
   */
  async getMetrics() {
    const response = await api.get('/observability/metrics');
    return response.data;
  },

  /**
   * Alias for getMetrics().
   * Endpoint: GET /api/v1/observability/summary
   */
  async getSummary() {
    const response = await api.get('/observability/summary');
    return response.data;
  }
};

export default observabilityService;
