import api from './api';

export const webhookService = {
  /**
   * Fetch paginated webhook delivery history with optional filters.
   * Endpoint: GET /api/v1/webhooks/deliveries
   */
  async getDeliveries(params = {}) {
    const response = await api.get('/webhooks/deliveries', { params });
    return response.data;
  },

  /**
   * Fetch summary metrics for webhook deliveries.
   * Endpoint: GET /api/v1/webhooks/deliveries/summary
   */
  async getDeliverySummary() {
    const response = await api.get('/webhooks/deliveries/summary');
    return response.data;
  },

  /**
   * Fetch detailed delivery data by deliveryId.
   * Endpoint: GET /api/v1/webhooks/deliveries/{deliveryId}
   */
  async getDeliveryDetail(deliveryId) {
    const response = await api.get(`/webhooks/deliveries/${deliveryId}`);
    return response.data;
  },

  /**
   * Trigger a retry for an eligible failed webhook delivery.
   * Endpoint: POST /api/v1/webhooks/deliveries/{deliveryId}/retry
   */
  async retryDelivery(deliveryId) {
    const response = await api.post(`/webhooks/deliveries/${deliveryId}/retry`);
    return response.data;
  }
};

export default webhookService;
