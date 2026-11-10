"""
PROJETO FINAL - ÂNCORA GESTÃO CONDOMINIAL
Suite de Testes de Carga e Estresse (Locust)
Simula múltiplos perfis: Administrador, Síndico, Porteiro, Morador e Visitante.
"""

import json
import base64
import random
from locust import HttpUser, task, between, events

class CondominiumPlatformUser(HttpUser):
    wait_time = between(0.1, 0.5)
    abstract = True

    def on_start(self):
        self.auth_token = None
        self.condominium_id = 1
        self.authenticate()

    def authenticate(self, username="admin@ancora.com", password="ancoraAdmin"):
        basic_creds = base64.b64encode(b"ancora-app:myclientsecret").decode("utf-8")
        headers = {
            "Authorization": f"Basic {basic_creds}",
            "Content-Type": "application/x-www-form-urlencoded"
        }
        data = {
            "grant_type": "password",
            "username": username,
            "password": password
        }
        with self.client.post("/oauth2/token", headers=headers, data=data, catch_response=True) as resp:
            if resp.status_code == 200:
                body = resp.json()
                self.auth_token = body.get("access_token")
            else:
                resp.failure(f"Auth failed: {resp.status_code}")

    def auth_headers(self):
        headers = {"Accept": "application/json"}
        if self.auth_token:
            headers["Authorization"] = f"Bearer {self.auth_token}"
        return headers


class AdministratorUser(CondominiumPlatformUser):
    weight = 1

    def on_start(self):
        self.authenticate("admin@ancora.com", "ancoraAdmin")

    @task(3)
    def query_dashboard_indicators(self):
        self.client.get("/actuator/health", headers=self.auth_headers(), name="[Admin] Health & Indicators")

    @task(2)
    def search_users_paged(self):
        page = random.randint(0, 3)
        self.client.get(f"/users?page={page}&size=20", headers=self.auth_headers(), name="[Admin] List Users Paged")

    @task(1)
    def list_condominiums(self):
        self.client.get("/condominiums?size=20", headers=self.auth_headers(), name="[Admin] List Condominiums")


class ManagerUser(CondominiumPlatformUser):
    weight = 3

    def on_start(self):
        self.authenticate("admin@ancora.com", "ancoraAdmin")

    @task(3)
    def list_directory(self):
        self.client.get(f"/condominiums/{self.condominium_id}/directory", headers=self.auth_headers(), name="[Manager] Unit Directory")

    @task(3)
    def list_packages(self):
        self.client.get(f"/condominiums/{self.condominium_id}/packages?page=0&size=20", headers=self.auth_headers(), name="[Manager] Packages Paged")

    @task(2)
    def list_visitors(self):
        self.client.get(f"/condominiums/{self.condominium_id}/visitors", headers=self.auth_headers(), name="[Manager] Visitors List")

    @task(2)
    def list_reservations(self):
        self.client.get(f"/condominiums/{self.condominium_id}/reservations", headers=self.auth_headers(), name="[Manager] Reservations List")


class DoormanUser(CondominiumPlatformUser):
    weight = 4

    def on_start(self):
        self.authenticate("admin@ancora.com", "ancoraAdmin")

    @task(4)
    def search_packages_status(self):
        self.client.get(f"/condominiums/{self.condominium_id}/packages?status=RECEIVED&size=15", headers=self.auth_headers(), name="[Doorman] Search Packages")

    @task(3)
    def search_visitor_plates(self):
        self.client.get(f"/condominiums/{self.condominium_id}/visitors?vehiclePlate=ABC", headers=self.auth_headers(), name="[Doorman] Plate Lookup")

    @task(1)
    def create_package(self):
        payload = {
            "description": f"Encomenda Expressa #{random.randint(1000, 9999)}",
            "recipientId": 1,
            "unitId": 1
        }
        headers = self.auth_headers()
        headers["Content-Type"] = "application/json"
        self.client.post(f"/condominiums/{self.condominium_id}/packages", headers=headers, json=payload, name="[Doorman] Create Package")


class ResidentUser(CondominiumPlatformUser):
    weight = 6

    def on_start(self):
        self.authenticate("admin@ancora.com", "ancoraAdmin")

    @task(4)
    def query_my_packages(self):
        self.client.get("/packages/my?page=0&size=10", headers=self.auth_headers(), name="[Resident] My Packages")

    @task(3)
    def query_common_areas(self):
        self.client.get(f"/condominiums/{self.condominium_id}/common-areas", headers=self.auth_headers(), name="[Resident] Common Areas")

    @task(2)
    def query_my_reservations(self):
        self.client.get(f"/condominiums/{self.condominium_id}/reservations/my", headers=self.auth_headers(), name="[Resident] My Reservations")

    @task(2)
    def query_announcements(self):
        self.client.get(f"/condominiums/{self.condominium_id}/announcements", headers=self.auth_headers(), name="[Resident] Announcements")
