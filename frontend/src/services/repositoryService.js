import api from './api';

export const repositoryService = {
  /**
   * Fetch GitHub installations connected to the current user.
   * Endpoint: GET /api/v1/github/installations (or /github/installations)
   * Returns: List<GithubInstallationResponse>
   */
  async getInstallations() {
    const response = await api.get('/github/installations');
    return response.data || [];
  },

  /**
   * Fetch repositories accessible via a specific GitHub App installation.
   * Endpoint: GET /api/v1/github/installations/{installationId}/repositories
   * Returns: List<GithubRepositoryResponse>
   */
  async getRepositories(installationId, params = {}) {
    const response = await api.get(`/github/installations/${installationId}/repositories`, {
      params: {
        page: params.page || 1,
        perPage: params.perPage || 100
      }
    });
    return response.data || [];
  },

  /**
   * Fetch reviews for a specific repository if needed.
   * Endpoint: GET /api/v1/code-reviews/repository/{owner}/{repository}
   */
  async getRepositoryReviews(owner, repository) {
    const response = await api.get(`/code-reviews/repository/${owner}/${repository}`);
    return response.data || [];
  }
};

export default repositoryService;
