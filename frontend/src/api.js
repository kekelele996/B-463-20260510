import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 5000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, error => {
  return Promise.reject(error);
});

api.interceptors.response.use(response => {
  // If the backend returns a logical error code (not 200), reject the promise
  if (response.data && response.data.code && response.data.code !== 200) {
    return Promise.reject({
      response: response,
      message: response.data.message || 'Error'
    });
  }
  return response;
}, error => {
  if (error.response && (error.response.status === 401 || error.response.status === 403)) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
  }
  return Promise.reject(error);
});

export default api
