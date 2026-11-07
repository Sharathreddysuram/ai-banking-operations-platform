/**
 * API types for the AI Banking Operations Platform.
 * Author: Sharath Kumar Suram
 */

export interface LoginResponse {
  token: string;
  username: string;
  role: string;
}

export interface CurrentUser {
  username: string;
  role: string;
  [key: string]: unknown;
}

/** Envelope returned by every AI agent endpoint. */
export interface AgentResponse<T> {
  data: T;
  reasoning: string;
  confidence: number;
  agentName: string;
}

/** Unwrap an AgentResponse envelope, or pass a plain payload through. */
export function unwrap<T>(payload: AgentResponse<T> | T): T {
  if (
    payload !== null &&
    typeof payload === 'object' &&
    'data' in (payload as Record<string, unknown>) &&
    'agentName' in (payload as Record<string, unknown>)
  ) {
    return (payload as AgentResponse<T>).data;
  }
  return payload as T;
}

export function asAgentResponse<T>(
  payload: AgentResponse<T> | T,
): AgentResponse<T> | null {
  if (
    payload !== null &&
    typeof payload === 'object' &&
    'agentName' in (payload as Record<string, unknown>)
  ) {
    return payload as AgentResponse<T>;
  }
  return null;
}

/* ------------------------------- KYC ---------------------------------- */

export type KycStatus = 'PENDING' | 'VERIFIED' | 'REJECTED' | 'REVIEW_REQUIRED' | string;

export interface Customer {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  documentType: string;
  documentNumber: string;
  documentHolderName: string;
  address: string;
  dateOfBirth: string;
  status: KycStatus;
  riskRating: string | null;
}

export interface CustomerForm {
  fullName: string;
  email: string;
  phone: string;
  documentType: string;
  documentNumber: string;
  documentHolderName: string;
  address: string;
  dateOfBirth: string;
}

export interface Verification {
  id?: number;
  customerId?: number;
  status?: string;
  result?: string;
  reasoning?: string;
  confidence?: number;
  agentName?: string;
  createdAt?: string;
  [key: string]: unknown;
}

export type ComplianceReport = Record<string, unknown>;

/* ------------------------------ Fraud --------------------------------- */

export interface Transaction {
  id: number;
  customerId: number;
  amount: number;
  currency: string;
  merchant: string;
  category: string;
  location: string;
  channel: string;
  timestamp: string;
  status: string;
}

export interface TransactionForm {
  customerId: number;
  amount: number;
  currency: string;
  merchant: string;
  category: string;
  location: string;
  channel: string;
}

export type FraudSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | string;
export type FraudRecommendation = 'APPROVE' | 'REVIEW' | 'BLOCK' | string;

export interface FraudAssessment {
  id: number;
  transactionId: number;
  customerId: number;
  riskScore: number;
  severity: FraudSeverity;
  reasons: string;
  recommendation: FraudRecommendation;
  reasoning: string;
  createdAt: string;
}

/* ------------------------------- Loan --------------------------------- */

export interface LoanApplication {
  id: number;
  customerId: number;
  requestedAmount: number;
  termMonths: number;
  purpose: string;
  monthlyIncome: number;
  monthlyDebtPayments: number;
  annualIncome: number;
  creditScore: number;
  employmentStatus: string;
  status: string;
}

export interface LoanApplicationForm {
  customerId: number;
  requestedAmount: number;
  termMonths: number;
  purpose: string;
  monthlyIncome: number;
  monthlyDebtPayments: number;
  annualIncome: number;
  creditScore: number;
  employmentStatus: string;
}

export interface LoanAssessment {
  id: number;
  applicationId: number;
  dtiRatio: number;
  riskScore: number;
  riskBand: string;
  recommendation: string;
  recommendedAmount: number;
  reasoning: string;
  factors: string;
}

/* ----------------------------- Support -------------------------------- */

export type TicketStatus = 'ANSWERED' | 'ESCALATED' | 'CLOSED' | string;

export interface SupportTicket {
  id: number;
  customerId: number;
  subject: string;
  question: string;
  answer: string;
  category: string;
  status: TicketStatus;
  escalated: boolean;
  reasoning: string;
  createdAt: string;
}

export interface KnowledgeEntry {
  id?: number;
  title?: string;
  topic?: string;
  category?: string;
  content?: string;
  answer?: string;
  [key: string]: unknown;
}

/* ----------------------------- Decision ------------------------------- */

export type CaseStatus = 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | string;

export interface DecisionCase {
  id: number;
  customerId: number;
  caseType: string;
  kycSummary: string;
  fraudSummary: string;
  loanSummary: string;
  supportSummary: string;
  finalRecommendation: string;
  overallRiskScore: number;
  reasoning: string;
  status: CaseStatus;
  reviewedBy: string | null;
  reviewNotes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AuditEntry {
  id?: number;
  agentName?: string;
  agent?: string;
  customerId?: number;
  action?: string;
  eventType?: string;
  entityType?: string;
  entityId?: number | string;
  reasoning?: string;
  details?: string;
  confidence?: number;
  createdAt?: string;
  timestamp?: string;
  [key: string]: unknown;
}

export interface AgentStatus {
  name: string;
  description: string;
  status: string;
  mode: string;
}

/* --------------------------- Notifications ---------------------------- */

export interface AppNotification {
  id: string;
  type: string;
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
}
