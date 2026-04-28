import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiMessageResponse,
  ApplicationDocumentResponse,
  ApplicationRaiseContactStepRequest,
  ApplicationRaiseCreateRequest,
  ApplicationRaiseDetailsStepRequest,
  ApplicationRaiseResponse,
  ApplicationRaiseSearchCriteria,
  ApplicationRaiseStatus,
  ApplicationRaiseTypeStepRequest,
  CampaignResponse,
  CampaignSearchCriteria,
  DocumentType,
  EquityApplicationCreateRequest,
  PaymentResponse,
  PaymentStatusPatchRequest,
  PledgeCreateRequest,
  PledgeResponse,
  PortfolioDiversificationResponse,
  PortfolioOverviewResponse,
  PortfolioPositionResponse,
  PortfolioSummaryResponse
} from '../models/crowdfunding.models';

@Injectable({
  providedIn: 'root'
})
export class CrowdfundingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/crowdfunding`;
  private readonly portfolioBaseUrl = `${environment.apiBaseUrl}/portfolio`;

  listMyApplications(
    criteria: ApplicationRaiseSearchCriteria = {}
  ): Observable<ApplicationRaiseResponse[]> {
    return this.http.get<ApplicationRaiseResponse[]>(
      `${this.baseUrl}/application-raises/mine`,
      { params: this.buildParams(criteria) }
    );
  }

  getApplicationById(id: number): Observable<ApplicationRaiseResponse> {
    return this.http.get<ApplicationRaiseResponse>(
      `${this.baseUrl}/application-raises/${id}`
    );
  }

  createEmptyDraft(): Observable<ApplicationRaiseResponse> {
    return this.http.post<ApplicationRaiseResponse>(
      `${this.baseUrl}/application-raises/drafts`,
      {}
    );
  }

  saveContactStep(
    id: number,
    payload: ApplicationRaiseContactStepRequest
  ): Observable<ApplicationRaiseResponse> {
    return this.http.patch<ApplicationRaiseResponse>(
      `${this.baseUrl}/application-raises/${id}/steps/contact`,
      payload
    );
  }

  saveTypeStep(
    id: number,
    payload: ApplicationRaiseTypeStepRequest
  ): Observable<ApplicationRaiseResponse> {
    return this.http.patch<ApplicationRaiseResponse>(
      `${this.baseUrl}/application-raises/${id}/steps/type`,
      payload
    );
  }

  saveDetailsStep(
    id: number,
    payload: ApplicationRaiseDetailsStepRequest
  ): Observable<ApplicationRaiseResponse> {
    return this.http.patch<ApplicationRaiseResponse>(
      `${this.baseUrl}/application-raises/${id}/steps/details`,
      payload
    );
  }

  submitDraft(id: number): Observable<ApplicationRaiseResponse> {
    return this.http.post<ApplicationRaiseResponse>(
      `${this.baseUrl}/application-raises/${id}/submit`,
      {}
    );
  }

  createDonationDraft(
    payload: ApplicationRaiseCreateRequest
  ): Observable<ApplicationRaiseResponse> {
    return this.http.post<ApplicationRaiseResponse>(
      `${this.baseUrl}/donations`,
      payload
    );
  }

  createEquityDraft(
    payload: EquityApplicationCreateRequest
  ): Observable<ApplicationRaiseResponse> {
    return this.http.post<ApplicationRaiseResponse>(
      `${this.baseUrl}/equities`,
      payload
    );
  }

  updateDonationDraft(
    id: number,
    payload: ApplicationRaiseCreateRequest
  ): Observable<ApplicationRaiseResponse> {
    return this.http.put<ApplicationRaiseResponse>(
      `${this.baseUrl}/donations/${id}`,
      payload
    );
  }

  updateEquityDraft(
    id: number,
    payload: EquityApplicationCreateRequest
  ): Observable<ApplicationRaiseResponse> {
    return this.http.put<ApplicationRaiseResponse>(
      `${this.baseUrl}/equities/${id}`,
      payload
    );
  }

  submitApplication(id: number): Observable<ApplicationRaiseResponse> {
    return this.http.patch<ApplicationRaiseResponse>(
      `${this.baseUrl}/application-raises/${id}/status`,
      { status: ApplicationRaiseStatus.SUBMITTED }
    );
  }

  uploadDocument(
    applicationId: number,
    type: DocumentType,
    file: File
  ): Observable<ApplicationDocumentResponse> {
    const formData = new FormData();
    formData.append('type', type);
    formData.append('file', file);

    return this.http.post<ApplicationDocumentResponse>(
      `${this.baseUrl}/application-raises/${applicationId}/documents`,
      formData
    );
  }

  listDocuments(applicationId: number): Observable<ApplicationDocumentResponse[]> {
    return this.http.get<ApplicationDocumentResponse[]>(
      `${this.baseUrl}/application-raises/${applicationId}/documents`
    );
  }

  deleteDocument(applicationId: number, type: DocumentType): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/application-raises/${applicationId}/documents/${type}`
    );
  }

  deleteDonationDraft(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/donations/${id}`);
  }

  deleteEquityDraft(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/equities/${id}`);
  }

  adminListApplications(
    criteria: ApplicationRaiseSearchCriteria = {}
  ): Observable<ApplicationRaiseResponse[]> {
    return this.http.get<ApplicationRaiseResponse[]>(
      `${this.baseUrl}/application-raises/admin`,
      { params: this.buildParams(criteria) }
    );
  }

  adminPatchApplicationStatus(
    id: number,
    status: ApplicationRaiseStatus
  ): Observable<ApplicationRaiseResponse> {
    return this.http.patch<ApplicationRaiseResponse>(
      `${this.baseUrl}/application-raises/${id}/admin/status`,
      { status }
    );
  }

  fetchDocumentBlob(applicationId: number, type: DocumentType): Observable<Blob> {
    return this.http.get(
      `${this.baseUrl}/application-raises/${applicationId}/documents/${type}/content`,
      { responseType: 'blob' }
    );
  }

  listApprovedCampaigns(
    criteria: CampaignSearchCriteria = {}
  ): Observable<CampaignResponse[]> {
    return this.http.get<CampaignResponse[]>(`${this.baseUrl}/campaigns`, {
      params: this.buildParams(criteria)
    });
  }

  getApprovedCampaign(id: number): Observable<CampaignResponse> {
    return this.http.get<CampaignResponse>(`${this.baseUrl}/campaigns/${id}`);
  }

  createOrUpdateMyPledge(
    id: number,
    payload: PledgeCreateRequest
  ): Observable<PledgeResponse> {
    return this.http.post<PledgeResponse>(
      `${this.baseUrl}/campaigns/${id}/pledges`,
      payload
    );
  }

  listMyPledges(): Observable<PledgeResponse[]> {
    return this.http.get<PledgeResponse[]>(`${this.baseUrl}/my-pledges`);
  }

  getMyPledgeById(pledgeId: number): Observable<PledgeResponse> {
    return this.http.get<PledgeResponse>(`${this.baseUrl}/my-pledges/${pledgeId}`);
  }

  initiateMyPayment(pledgeId: number): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(
      `${this.baseUrl}/my-pledges/${pledgeId}/payments/initiate`,
      {}
    );
  }

  listMyPayments(): Observable<PaymentResponse[]> {
    return this.http.get<PaymentResponse[]>(`${this.baseUrl}/my-payments`);
  }

  getMyPaymentById(paymentId: number): Observable<PaymentResponse> {
    return this.http.get<PaymentResponse>(`${this.baseUrl}/my-payments/${paymentId}`);
  }

  getMyPaymentMockCheckout(paymentId: number): Observable<ApiMessageResponse> {
    return this.http.get<ApiMessageResponse>(
      `${this.baseUrl}/my-payments/${paymentId}/mock-checkout`
    );
  }

  patchMyPaymentStatus(
    paymentId: number,
    payload: PaymentStatusPatchRequest
  ): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(
      `${this.baseUrl}/my-payments/${paymentId}/mock-result`,
      payload
    );
  }

  adminListPayments(): Observable<PaymentResponse[]> {
    return this.http.get<PaymentResponse[]>(`${this.baseUrl}/admin/payments`);
  }

  adminPatchPaymentStatus(
    paymentId: number,
    payload: PaymentStatusPatchRequest
  ): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(
      `${this.baseUrl}/admin/payments/${paymentId}/mock-result`,
      payload
    );
  }

  getMyPortfolioOverview(): Observable<PortfolioOverviewResponse> {
    return this.http.get<PortfolioOverviewResponse>(
      `${this.portfolioBaseUrl}/me/overview`
    );
  }

  getMyPortfolioSummary(): Observable<PortfolioSummaryResponse> {
    return this.http.get<PortfolioSummaryResponse>(
      `${this.portfolioBaseUrl}/me/summary`
    );
  }

  getMyPortfolioPositions(): Observable<PortfolioPositionResponse[]> {
    return this.http.get<PortfolioPositionResponse[]>(
      `${this.portfolioBaseUrl}/me/positions`
    );
  }

  getMyPortfolioDiversification(): Observable<PortfolioDiversificationResponse> {
    return this.http.get<PortfolioDiversificationResponse>(
      `${this.portfolioBaseUrl}/me/diversification`
    );
  }

  private buildParams(criteria: object): HttpParams {
    let params = new HttpParams();

    Object.entries(criteria as Record<string, unknown>).forEach(([key, value]) => {
      if (value === null || value === undefined) return;

      if (typeof value === 'string') {
        const normalized = value.trim();
        if (!normalized) return;
        params = params.set(key, normalized);
        return;
      }

      if (typeof value === 'number' || typeof value === 'boolean') {
        params = params.set(key, String(value));
        return;
      }

      if (value instanceof Date) {
        params = params.set(key, value.toISOString());
        return;
      }

      if (Array.isArray(value)) {
        value
          .filter((item) => item !== null && item !== undefined && `${item}`.trim() !== '')
          .forEach((item) => {
            params = params.append(key, String(item));
          });
      }
    });

    return params;
  }
}