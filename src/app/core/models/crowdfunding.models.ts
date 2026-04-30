import { AppTag, Sector, SubSector } from './application-taxonomy';

export enum CrowdfundingType {
  DONATION = 'DONATION',
  EQUITY = 'EQUITY'
}

export enum ApplicationRaiseStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED'
}

export enum ApplicationRaiseDraftStep {
  CONTACT = 'CONTACT',
  TYPE = 'TYPE',
  DETAILS = 'DETAILS',
  DOCUMENTS = 'DOCUMENTS'
}

export enum ProjectStage {
  IDEA = 'IDEA',
  MVP = 'MVP',
  EARLY_REVENUE = 'EARLY_REVENUE',
  GROWING = 'GROWING',
  REGISTERED_COMPANY = 'REGISTERED_COMPANY'
}

export enum DocumentReviewStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  NEEDS_REUPLOAD = 'NEEDS_REUPLOAD'
}

export enum DocumentType {
  ID_CARD = 'ID_CARD',
  PROJECT_PITCH_DECK = 'PROJECT_PITCH_DECK',
  CNRE_EXTRACT = 'CNRE_EXTRACT',
  SHAREHOLDERS_CAP_TABLE = 'SHAREHOLDERS_CAP_TABLE',
  FINANCIAL_STATEMENTS = 'FINANCIAL_STATEMENTS',
  BANK_RIB = 'BANK_RIB'
}



export enum CampaignPageStatus {
  DRAFT = 'DRAFT',
  PENDING_REVIEW = 'PENDING_REVIEW',
  PUBLISHED = 'PUBLISHED',
  CHANGES_REQUESTED = 'CHANGES_REQUESTED',
  ARCHIVED = 'ARCHIVED'
}

export type CampaignBlockType =
  | 'hero'
  | 'header'
  | 'subheader'
  | 'paragraph'
  | 'image'
  | 'youtube'
  | 'document'
  | 'funding_cta'
  | 'divider';

export type CampaignBlockSize = 'compact' | 'normal' | 'wide' | 'full';

export interface CampaignBuilderBlock {
  id: string;
  type: CampaignBlockType;
  size?: CampaignBlockSize;
  title?: string;
  subtitle?: string;
  content?: string;
  url?: string;
  documentId?: number | null;
  label?: string;
}

export interface CampaignContentJson {
  blocks: CampaignBuilderBlock[];
}

export interface CampaignStyleJson {
  fontFamily: string;
  primaryColor: string;
  accentColor: string;
  radius: 'small' | 'medium' | 'large';
  heroLayout: 'centered' | 'split' | 'editorial';
  buttonStyle: 'rounded' | 'pill' | 'sharp';
}

export interface CampaignPageDocumentResponse {
  id: number;
  campaignPageId: number;
  applicationDocumentId: number;
  docType: DocumentType;
  fileName: string;
  label: string | null;
  sizeBytes: number | null;
  createdAt: string;
}

export interface CampaignPageResponse {
  id: number;
  applicationRaiseId: number;
  ownerUserId: number;
  slug: string;
  publicUrl: string;
  title: string | null;
  subtitle: string | null;
  coverMediaUrl: string | null;
  contentJson: string | null;
  styleJson: string | null;
  status: CampaignPageStatus;
  reviewNote: string | null;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;

  applicationType: CrowdfundingType | null;
  businessName: string | null;
  website: string | null;
  sector: Sector | null;
  subSector: SubSector | null;
  tags: AppTag[];
  summary: string | null;
  problemStatement: string | null;
  solution: string | null;
  targetCustomers: string | null;
  useOfFunds: string | null;
  fundingGoal: number | null;
  investorsPledgedAmount: number | null;
  currency: string | null;
  governorate: string | null;
  city: string | null;
  equityDetail: EquityDetailResponse | null;
  availableDocuments?: ApplicationDocumentResponse[] | null;
  publicDocuments: CampaignPageDocumentResponse[];
}

export interface CampaignPageUpsertRequest {
  title?: string | null;
  subtitle?: string | null;
  slug?: string | null;
  coverMediaUrl?: string | null;
  contentJson: string;
  styleJson: string;
  publicDocumentIds: number[];
}

export interface CampaignPageStatusPatchRequest {
  status: CampaignPageStatus;
  reviewNote?: string | null;
}

export type SortDirection = 'asc' | 'desc';

export type CampaignSortKey =
  | 'trending'
  | 'createdAt'
  | 'businessName'
  | 'fundingGoal'
  | 'investorsPledgedAmount';

export type ApplicationSortKey =
  | 'updatedAt'
  | 'createdAt'
  | 'businessName'
  | 'status'
  | 'fundingGoal'
  | 'type';

export interface ApplicationRaiseSearchCriteria {
  search?: string | null;
  id?: number | null;
  ownerUserId?: number | null;
  type?: CrowdfundingType | null;
  businessName?: string | null;

  website?: string | null;
  country?: string | null;
  currency?: string | null;
  sector?: Sector | null;
  subSector?: SubSector | null;
  tag?: AppTag | null;
  summary?: string | null;
  fundingGoalMin?: number | null;
  fundingGoalMax?: number | null;
  investorsPledgedAmountMin?: number | null;
  investorsPledgedAmountMax?: number | null;
  customerCountMin?: number | null;
  customerCountMax?: number | null;
  contactFirstName?: string | null;
  contactLastName?: string | null;
  contactTitle?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
  acceptedTerms?: boolean | null;
  status?: ApplicationRaiseStatus | null;
  companyLegalName?: string | null;
  companyRegistrationNumber?: string | null;
  cnreProfileUrl?: string | null;
  equityOfferedPercentMin?: number | null;
  equityOfferedPercentMax?: number | null;
  preMoneyValuationMin?: number | null;
  preMoneyValuationMax?: number | null;
  minInvestmentMin?: number | null;
  minInvestmentMax?: number | null;
  createdFrom?: string | null;
  createdTo?: string | null;
  updatedFrom?: string | null;
  updatedTo?: string | null;
  sortBy?: ApplicationSortKey | string | null;
  sortDir?: SortDirection | string | null;
}

export interface CampaignSearchCriteria {
  search?: string | null;
  id?: number | null;
  type?: CrowdfundingType | null;
  businessName?: string | null;
  website?: string | null;
  country?: string | null;
  currency?: string | null;
  sector?: Sector | null;
  subSector?: SubSector | null;
  tag?: AppTag | null;
  summary?: string | null;
  fundingGoalMin?: number | null;
  fundingGoalMax?: number | null;
  investorsPledgedAmountMin?: number | null;
  investorsPledgedAmountMax?: number | null;
  minInvestmentMin?: number | null;
  minInvestmentMax?: number | null;
  equityOfferedPercentMin?: number | null;
  equityOfferedPercentMax?: number | null;
  preMoneyValuationMin?: number | null;
  preMoneyValuationMax?: number | null;
  sort?: CampaignSortKey | string | null;
  sortDir?: SortDirection | string | null;
}

export interface ApplicationDocumentResponse {
  id: number;
  applicationRaiseId: number;
  docType: DocumentType;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  reviewStatus?: DocumentReviewStatus | null;
  reviewedByUserId?: number | null;
  reviewedAt?: string | null;
  reviewNote?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface EquityDetailResponse {
  applicationRaiseId: number;
  companyLegalName: string;
  companyRegistrationNumber: string;
  cnreProfileUrl: string;
  equityOfferedPercent: number | null;
  preMoneyValuation: number | null;
  fundingGoal?: number | null;
  postMoneyValuation?: number | null;
  minInvestment: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ApplicationRaiseResponse {
  id: number;
  ownerUserId: number;
  type: CrowdfundingType | null;

  businessName: string | null;

  website: string | null;

  country: string | null;
  currency: string | null;

  sector: Sector | null;
  subSector: SubSector | null;
  tags: AppTag[];
  stage: ProjectStage | null;
  summary: string | null;
  problemStatement: string | null;
  solution: string | null;
  targetCustomers: string | null;
  useOfFunds: string | null;

  fundingGoal: number | null;
  investorsPledgedAmount: number | null;
  raisedAmount?: number | null;
  remainingAmount?: number | null;
  fundingProgressPercent?: number | null;
  customerCount: number | null;
  teamSize: number | null;
  governorate: string | null;
  city: string | null;

  contactFirstName: string | null;
  contactLastName: string | null;
  contactTitle: string | null;
  contactEmail: string | null;
  contactPhone: string | null;

  useProfileContact: boolean;
  acceptedTerms: boolean;

  status: ApplicationRaiseStatus;
  draftStep: ApplicationRaiseDraftStep;

  applicationCompletionPercent?: number | null;
  documentCompletionPercent?: number | null;

  createdAt: string;
  updatedAt: string;
  submittedAt?: string | null;

  equityDetail: EquityDetailResponse | null;
  documents: ApplicationDocumentResponse[];
}

export interface ApplicationRaiseCreateRequest {
  type: CrowdfundingType;
  businessName: string;

  website?: string | null;

  sector: Sector;
  subSector: SubSector;
  tags: AppTag[];
  stage?: ProjectStage | null;
  summary: string;
  problemStatement?: string | null;
  solution?: string | null;
  targetCustomers?: string | null;
  useOfFunds?: string | null;

  fundingGoal: number | null;
  customerCount?: number | null;
  teamSize?: number | null;
  governorate?: string | null;
  city?: string | null;

  contactFirstName: string;
  contactLastName: string;
  contactTitle?: string | null;
  contactEmail: string;
  contactPhone?: string | null;

  acceptedTerms: boolean;
}

export interface ApplicationRaiseContactStepRequest {
  useProfileContact: boolean;
  contactFirstName: string;
  contactLastName: string;
  contactTitle?: string | null;
  contactEmail: string;
  contactPhone?: string | null;
}

export interface ApplicationRaiseTypeStepRequest {
  type: CrowdfundingType;
}

export interface EquityDetailUpsertRequest {
  companyLegalName: string;
  companyRegistrationNumber: string;
  cnreProfileUrl: string;
  preMoneyValuation?: number | null;
  minInvestment?: number | null;
}

export interface ApplicationRaiseDetailsStepRequest {
  businessName: string;

  website?: string | null;

  sector: Sector;
  subSector: SubSector;
  tags: AppTag[];
  stage: ProjectStage;
  summary: string;
  problemStatement: string;
  solution: string;
  targetCustomers: string;
  useOfFunds: string;

  fundingGoal: number | null;
  customerCount?: number | null;
  teamSize: number | null;
  governorate: string | null;
  city: string | null;

  equityDetail?: EquityDetailUpsertRequest | null;
  acceptedTerms: boolean;
}

export interface EquityApplicationCreateRequest {
  application: ApplicationRaiseCreateRequest;
  equityDetail: EquityDetailUpsertRequest;
}

export enum PledgeStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  FAILED = 'FAILED',
  CANCELED = 'CANCELED',
  REFUNDED = 'REFUNDED'
}

export interface CampaignResponse {
  id: number;
  businessName: string;
  summary: string;
  type: CrowdfundingType;

  website: string | null;
  currency: string;

  sector: Sector | null;
  subSector: SubSector | null;
  tags: AppTag[];

  fundingGoal: number | null;
  investorsPledgedAmount: number | null;

  minInvestment: number | null;
  equityOfferedPercent: number | null;
  preMoneyValuation: number | null;

  trendScore?: number | null;
  trendLabel?: string | null;
  currentInterestLevel?: number | null;
  nearTermGrowth?: number | null;
  quarterGrowth?: number | null;
  halfYearGrowth?: number | null;
  fullYearGrowth?: number | null;

  status: ApplicationRaiseStatus;
  createdAt: string;
}

export interface PledgeCreateRequest {
  amount: number | null;
  message?: string | null;
}

export interface PledgeResponse {
  id: number;
  applicationRaiseId: number;
  backerUserId: number;

  amount: number;
  currency: string;
  message: string | null;
  status: PledgeStatus;

  campaignBusinessName: string;
  campaignType: CrowdfundingType;
  campaignFundingGoal: number | null;
  campaignInvestorsPledgedAmount: number | null;
  campaignMinInvestment: number | null;

  createdAt: string;
  updatedAt: string;
}

export interface PortfolioAllocationItemResponse {
  key: string;
  amount: number | null;
  weightPct: number | null;
}

export interface PortfolioDiversificationResponse {
  distinctSectors: number;
  distinctSubSectors: number;
  distinctTags: number;

  largestPositionWeightPct: number | null;
  top3PositionsWeightPct: number | null;
  largestSectorWeightPct: number | null;

  concentrationIndexHhi: number | null;

  breadthPenalty: number;
  positionConcentrationPenalty: number;
  sectorConcentrationPenalty: number;

  diversificationScore: number;
}

export interface PortfolioSummaryResponse {
  investorUserId: number;
  currency: string;

  totalInvested: number | null;
  activePositions: number;
  averageTicket: number | null;

  largestPositionWeightPct: number | null;
  top3PositionsWeightPct: number | null;
  largestSectorWeightPct: number | null;

  equityInvested: number | null;
  donationInvested: number | null;

  equityAllocationPct: number | null;
  donationAllocationPct: number | null;

  distinctSectors: number;
  distinctSubSectors: number;
  distinctTags: number;

  concentrationIndexHhi: number | null;
  diversificationScore: number;
}

export interface PortfolioPositionResponse {
  campaignId: number;
  campaignBusinessName: string;
  campaignType: CrowdfundingType;

  sector: string | null;
  subSector: string | null;
  tags: string[];

  investedAmount: number | null;
  currency: string;

  positionWeightPct: number | null;

  campaignFundingGoal: number | null;
  campaignRaisedAmount: number | null;
  campaignFundingProgressPct: number | null;

  minInvestment: number | null;
  equityOfferedPercent: number | null;
  preMoneyValuation: number | null;
  postMoneyValuation: number | null;
  ownershipPercent: number | null;

  pledgedAt: string;
}

export interface PortfolioOverviewResponse {
  summary: PortfolioSummaryResponse;
  sectorAllocation: PortfolioAllocationItemResponse[];
  subSectorAllocation: PortfolioAllocationItemResponse[];
  tagExposure: PortfolioAllocationItemResponse[];
  positions: PortfolioPositionResponse[];
  diversification: PortfolioDiversificationResponse;
}

export enum PaymentProvider {
  MOCK = 'MOCK'
}

export enum PaymentStatus {
  CREATED = 'CREATED',
  PENDING_PROVIDER = 'PENDING_PROVIDER',
  SUCCEEDED = 'SUCCEEDED',
  FAILED = 'FAILED',
  CANCELED = 'CANCELED',
  REFUNDED = 'REFUNDED'
}

export interface ApiMessageResponse {
  message: string;
}

export interface PaymentStatusPatchRequest {
  status: PaymentStatus;
  failureReason?: string | null;
}

export interface PaymentResponse {
  id: number;
  pledgeId: number;
  applicationRaiseId: number;
  backerUserId: number;

  amount: number;
  currency: string;

  provider: PaymentProvider;
  providerReference: string | null;
  checkoutSessionId: string | null;
  checkoutUrl: string | null;

  status: PaymentStatus;
  failureReason: string | null;

  pledgeStatus: PledgeStatus;
  campaignBusinessName: string;

  paidAt: string | null;
  refundedAt: string | null;
  createdAt: string;
  updatedAt: string;
}