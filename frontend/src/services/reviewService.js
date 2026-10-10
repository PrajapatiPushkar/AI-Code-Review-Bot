import api from './api';

export const reviewService = {
  async submitPullRequestReview(payload) {
    const response = await api.post('/code-reviews/pull-request', payload);
    return response.data;
  },

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
  },

  async generateFindingFix(findingId, data = {}) {
    const response = await api.post(`/code-reviews/findings/${findingId}/fix`, data);
    return response.data;
  },

  async getFindingFixProposals(findingId) {
    const response = await api.get(`/code-reviews/findings/${findingId}/fixes`);
    return response.data;
  },

  async getFixProposal(proposalId) {
    const response = await api.get(`/code-reviews/fixes/${proposalId}`);
    return response.data;
  },

  async updateFixProposalStatus(proposalId, status) {
    const response = await api.patch(`/code-reviews/fixes/${proposalId}/status`, { status });
    return response.data;
  },

  async downloadFixPatch(proposalId) {
    const response = await api.get(`/code-reviews/fixes/${proposalId}/patch`, {
      responseType: 'blob'
    });
    return response;
  },

  async downloadProposedContent(proposalId) {
    const response = await api.get(`/code-reviews/fixes/${proposalId}/proposed-content`, {
      responseType: 'blob'
    });
    return response;
  },

  async downloadOriginalContent(proposalId) {
    const response = await api.get(`/code-reviews/fixes/${proposalId}/original-content`, {
      responseType: 'blob'
    });
    return response;
  },

  async getQualityGate(id) {
    const response = await api.get(`/code-reviews/${id}/quality-gate`);
    return response.data;
  }
};

export default reviewService;
