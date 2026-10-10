import api from './api';

export const policyService = {
  /**
   * Fetch effective review policy for an authorized repository.
   * Endpoint: GET /api/v1/repositories/{repositoryId}/review-policy
   */
  async getReviewPolicy(repositoryId) {
    const response = await api.get(`/repositories/${repositoryId}/review-policy`);
    return response.data;
  },

  /**
   * Update review policy for an authorized repository.
   * Endpoint: PUT /api/v1/repositories/{repositoryId}/review-policy
   */
  async updateReviewPolicy(repositoryId, payload) {
    const response = await api.put(`/repositories/${repositoryId}/review-policy`, payload);
    return response.data;
  },

  /**
   * Reset review policy to documented system defaults.
   * Endpoint: POST /api/v1/repositories/{repositoryId}/review-policy/reset
   */
  async resetReviewPolicy(repositoryId) {
    const response = await api.post(`/repositories/${repositoryId}/review-policy/reset`);
    return response.data;
  }
};

export default policyService;
