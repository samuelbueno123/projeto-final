import http from 'k6/http';
import { check, sleep, group } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 20 }, // Ramp-up para 20 VUs
    { duration: '30s', target: 50 }, // Sustentação a 50 VUs
    { duration: '10s', target: 0 },  // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<250', 'p(99)<500'], // 95% das reqs < 250ms
    http_req_failed: ['rate<0.01'],                 // Erros < 1%
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080';

export default function () {
  // 1. Token de Autenticacao OAuth2
  let token = '';
  group('01_Authentication', function () {
    const loginPayload = 'grant_type=password&username=admin@ancora.com&password=ancoraAdmin';
    const loginParams = {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Authorization': 'Basic ' + 'YW5jb3JhLWFwcDpteWNsaWVudHNlY3JldA==',
      },
    };
    const res = http.post(`${BASE_URL}/oauth2/token`, loginPayload, loginParams);
    check(res, {
      'auth status is 200': (r) => r.status === 200,
      'has access_token': (r) => r.json('access_token') !== undefined,
    });
    if (res.status === 200) {
      token = res.json('access_token');
    }
  });

  if (!token) return;

  const authHeaders = {
    headers: {
      'Authorization': `Bearer ${token}`,
      'Accept': 'application/json',
    },
  };

  // 2. Consulta de Informacoes e Healthcheck
  group('02_Health_and_Metrics', function () {
    const res = http.get(`${BASE_URL}/actuator/health`, authHeaders);
    check(res, { 'health is UP': (r) => r.status === 200 });
  });

  // 3. Consulta de Dados do Condominio e Unidades
  group('03_Condominium_Directory', function () {
    const res = http.get(`${BASE_URL}/condominiums/1/directory`, authHeaders);
    check(res, { 'directory status is 200': (r) => r.status === 200 });
  });

  // 4. Listagem e Pesquisa de Encomendas com Paginacao
  group('04_Packages_Paged', function () {
    const res = http.get(`${BASE_URL}/condominiums/1/packages?page=0&size=20`, authHeaders);
    check(res, { 'packages status is 200': (r) => r.status === 200 });
  });

  // 5. Listagem de Visitantes
  group('05_Visitors_List', function () {
    const res = http.get(`${BASE_URL}/condominiums/1/visitors`, authHeaders);
    check(res, { 'visitors status is 200': (r) => r.status === 200 });
  });

  sleep(0.5);
}
