import api from './api';

export const reviewService = {
  async getCodeReviews(params = {}) {
    const response = await api.get('/code-reviews', { params });
    return response.data;
  },

  async getReviewById(id) {
    const response = await api.get(`/code-reviews/${id}`);
    return response.data;
  },

  async getReviewStatus(id) {
    const response = await api.get(`/code-reviews/${id}/status`);
    return response.data;
  },

  async getReviewResult(id) {
    const response = await api.get(`/code-reviews/${id}/result`);
    return response.data;
  },

  async getReviewFindings(id, params = {}) {
    const response = await api.get(`/code-reviews/${id}/findings`, { params });
    return response.data;
  }
};

export default reviewService;
