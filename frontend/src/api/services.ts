import client from './client';
import type {
  AgentResponse,
  AgentStatus,
  AuditEntry,
  ComplianceReport,
  CurrentUser,
  Customer,
  CustomerForm,
  DecisionCase,
  FraudAssessment,
  KnowledgeEntry,
  LoanApplication,
  LoanApplicationForm,
  LoanAssessment,
  LoginResponse,
  SupportTicket,
  Transaction,
  TransactionForm,
  Verification,
} from './types';

async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await client.get<T>(url, { params });
  return res.data;
}

async function post<T>(url: string, body?: unknown): Promise<T> {
  const res = await client.post<T>(url, body ?? {});
  return res.data;
}

/* -------------------------------- Auth -------------------------------- */

export const authApi = {
  login: (username: string, password: string) =>
    post<LoginResponse>('/api/auth/login', { username, password }),
  register: (username: string, password: string, role: string) =>
    post<LoginResponse>('/api/auth/register', { username, password, role }),
  me: () => get<CurrentUser>('/api/auth/me'),
};

/* --------------------------------- KYC -------------------------------- */

export const kycApi = {
  createCustomer: (form: CustomerForm) =>
    post<Customer | AgentResponse<Customer>>('/api/kyc/customers', form),
  listCustomers: () => get<Customer[]>('/api/kyc/customers'),
  getCustomer: (id: number) => get<Customer>(`/api/kyc/customers/${id}`),
  verifyCustomer: (id: number) =>
    post<AgentResponse<Customer>>(`/api/kyc/customers/${id}/verify`),
  listVerifications: (id: number) =>
    get<Verification[]>(`/api/kyc/customers/${id}/verifications`),
  complianceReport: (id: number) =>
    get<ComplianceReport | AgentResponse<ComplianceReport>>(
      `/api/kyc/customers/${id}/compliance-report`,
    ),
};

/* -------------------------------- Fraud ------------------------------- */

export const fraudApi = {
  createTransaction: (form: TransactionForm) =>
    post<Transaction | AgentResponse<Transaction>>('/api/fraud/transactions', form),
  listTransactions: (customerId?: number) =>
    get<Transaction[]>(
      '/api/fraud/transactions',
      customerId ? { customerId } : undefined,
    ),
  getTransaction: (id: number) => get<Transaction>(`/api/fraud/transactions/${id}`),
  analyzeTransaction: (id: number) =>
    post<AgentResponse<FraudAssessment>>(`/api/fraud/transactions/${id}/analyze`),
  listAssessments: (customerId?: number) =>
    get<FraudAssessment[]>(
      '/api/fraud/assessments',
      customerId ? { customerId } : undefined,
    ),
  listAlerts: () => get<FraudAssessment[]>('/api/fraud/alerts'),
};

/* --------------------------------- Loan ------------------------------- */

export const loanApi = {
  createApplication: (form: LoanApplicationForm) =>
    post<LoanApplication | AgentResponse<LoanApplication>>('/api/loan/applications', form),
  listApplications: (customerId?: number) =>
    get<LoanApplication[]>(
      '/api/loan/applications',
      customerId ? { customerId } : undefined,
    ),
  getApplication: (id: number) => get<LoanApplication>(`/api/loan/applications/${id}`),
  addDocument: (id: number, document: Record<string, unknown>) =>
    post<unknown>(`/api/loan/applications/${id}/documents`, document),
  underwrite: (id: number) =>
    post<AgentResponse<LoanAssessment>>(`/api/loan/applications/${id}/underwrite`),
  getAssessment: (id: number) =>
    get<LoanAssessment | AgentResponse<LoanAssessment>>(
      `/api/loan/applications/${id}/assessment`,
    ),
};

/* ------------------------------- Support ------------------------------ */

export const supportApi = {
  ask: (customerId: number, question: string) =>
    post<AgentResponse<SupportTicket>>('/api/support/ask', { customerId, question }),
  listTickets: (customerId?: number) =>
    get<SupportTicket[]>(
      '/api/support/tickets',
      customerId ? { customerId } : undefined,
    ),
  getTicket: (id: number) => get<SupportTicket>(`/api/support/tickets/${id}`),
  closeTicket: (id: number) => post<SupportTicket>(`/api/support/tickets/${id}/close`),
  knowledge: () => get<KnowledgeEntry[]>('/api/support/knowledge'),
};

/* ------------------------------ Decision ------------------------------ */

export const decisionApi = {
  createCase: (customerId: number, caseType: string) =>
    post<DecisionCase | AgentResponse<DecisionCase>>('/api/decision/cases', {
      customerId,
      caseType,
    }),
  listCases: () => get<DecisionCase[]>('/api/decision/cases'),
  getCase: (id: number) => get<DecisionCase>(`/api/decision/cases/${id}`),
  reviewCase: (id: number, decision: string, reviewedBy: string, notes: string) =>
    post<DecisionCase>(`/api/decision/cases/${id}/review`, {
      decision,
      reviewedBy,
      notes,
    }),
  audit: () => get<AuditEntry[]>('/api/decision/audit'),
  auditForCustomer: (customerId: number) =>
    get<AuditEntry[]>(`/api/decision/audit/customer/${customerId}`),
  agentStatus: () => get<AgentStatus[]>('/api/decision/agents/status'),
};
