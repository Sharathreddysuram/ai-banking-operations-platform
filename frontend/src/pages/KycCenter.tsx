import { useCallback, useEffect, useState, type FormEvent } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  MenuItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, FactCheck as ReportIcon, VerifiedUser as VerifyIcon } from '@mui/icons-material';
import { kycApi } from '../api/services';
import {
  asAgentResponse,
  unwrap,
  type AgentResponse,
  type ComplianceReport,
  type Customer,
  type CustomerForm,
  type Verification,
} from '../api/types';
import { apiErrorMessage } from '../api/client';
import {
  AgentReasoningPanel,
  ErrorState,
  formatDateTime,
  LoadingState,
  PageHeader,
  StatusChip,
} from '../components/common';

const DOCUMENT_TYPES = ['PASSPORT', 'DRIVERS_LICENSE', 'NATIONAL_ID', 'SSN_CARD', 'UTILITY_BILL'];
const EMPTY_FORM: CustomerForm = {
  fullName: '',
  email: '',
  phone: '',
  documentType: 'PASSPORT',
  documentNumber: '',
  documentHolderName: '',
  address: '',
  dateOfBirth: '',
};

function renderReportValue(value: unknown): string {
  if (value === null || value === undefined) return '—';
  if (typeof value === 'object') return JSON.stringify(value, null, 2);
  return String(value);
}

export default function KycCenter() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState<CustomerForm>(EMPTY_FORM);
  const [creating, setCreating] = useState(false);

  const [verifyingId, setVerifyingId] = useState<number | null>(null);
  const [agentResult, setAgentResult] = useState<AgentResponse<Customer> | null>(null);
  const [selected, setSelected] = useState<Customer | null>(null);
  const [verifications, setVerifications] = useState<Verification[]>([]);

  const [reportOpen, setReportOpen] = useState(false);
  const [report, setReport] = useState<ComplianceReport | null>(null);
  const [reportCustomer, setReportCustomer] = useState<Customer | null>(null);
  const [reportLoading, setReportLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setCustomers(await kycApi.listCustomers());
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load customers.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    setCreating(true);
    setError(null);
    try {
      await kycApi.createCustomer(form);
      setCreateOpen(false);
      setForm(EMPTY_FORM);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to create customer.'));
    } finally {
      setCreating(false);
    }
  };

  const handleVerify = async (customer: Customer) => {
    setVerifyingId(customer.id);
    setError(null);
    setSelected(customer);
    try {
      const res = await kycApi.verifyCustomer(customer.id);
      setAgentResult(res);
      const updated = unwrap(res);
      if (updated && typeof updated === 'object' && 'status' in updated) {
        setSelected(updated);
      }
      const history = await kycApi.listVerifications(customer.id).catch(() => [] as Verification[]);
      setVerifications(history);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Verification failed.'));
    } finally {
      setVerifyingId(null);
    }
  };

  const openReport = async (customer: Customer) => {
    setReportCustomer(customer);
    setReport(null);
    setReportOpen(true);
    setReportLoading(true);
    try {
      const res = await kycApi.complianceReport(customer.id);
      const agent = asAgentResponse(res);
      setReport(agent ? (agent.data as ComplianceReport) : (res as ComplianceReport));
      if (agent) {
        setAgentResult({
          data: customer,
          reasoning: agent.reasoning,
          confidence: agent.confidence,
          agentName: agent.agentName,
        });
      }
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load compliance report.'));
      setReportOpen(false);
    } finally {
      setReportLoading(false);
    }
  };

  const field = (key: keyof CustomerForm, label: string, props?: Record<string, unknown>) => (
    <TextField
      label={label}
      value={form[key]}
      onChange={(e) => setForm((prev) => ({ ...prev, [key]: e.target.value }))}
      required
      fullWidth
      {...props}
    />
  );

  return (
    <Box>
      <PageHeader
        title="KYC Center"
        subtitle="Onboard customers and run the KYC verification agent."
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            New customer
          </Button>
        }
      />
      {error && <ErrorState message={error} />}

      <Card>
        <CardContent>
          {loading ? (
            <LoadingState label="Loading customers…" />
          ) : customers.length === 0 ? (
            <Typography color="text.secondary">No customers yet. Create the first one above.</Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Customer</TableCell>
                    <TableCell>Document</TableCell>
                    <TableCell>Date of birth</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Risk rating</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {customers.map((customer) => (
                    <TableRow key={customer.id} hover selected={selected?.id === customer.id}>
                      <TableCell>{customer.id}</TableCell>
                      <TableCell>
                        <Typography variant="subtitle2">{customer.fullName}</Typography>
                        <Typography variant="body2" color="text.secondary">
                          {customer.email} · {customer.phone}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {customer.documentType?.replace(/_/g, ' ')}
                        <Typography variant="body2" color="text.secondary">
                          {customer.documentNumber}
                        </Typography>
                      </TableCell>
                      <TableCell>{customer.dateOfBirth || '—'}</TableCell>
                      <TableCell>
                        <StatusChip value={customer.status} />
                      </TableCell>
                      <TableCell>
                        <StatusChip value={customer.riskRating} />
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={1} justifyContent="flex-end">
                          <Button
                            size="small"
                            variant="contained"
                            color="secondary"
                            startIcon={
                              verifyingId === customer.id ? (
                                <CircularProgress size={14} color="inherit" />
                              ) : (
                                <VerifyIcon />
                              )
                            }
                            disabled={verifyingId === customer.id}
                            onClick={() => void handleVerify(customer)}
                          >
                            Verify
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            startIcon={<ReportIcon />}
                            onClick={() => void openReport(customer)}
                          >
                            Compliance report
                          </Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      {agentResult && (
        <Card sx={{ mt: 2.5 }}>
          <CardContent>
            <Typography variant="h6">
              Verification result{selected ? ` — ${selected.fullName} (#${selected.id})` : ''}
            </Typography>
            {selected && (
              <Stack direction="row" spacing={1} sx={{ mt: 1 }} flexWrap="wrap">
                <StatusChip value={selected.status} />
                <StatusChip value={selected.riskRating} />
              </Stack>
            )}
            <AgentReasoningPanel response={agentResult} title="KYC agent reasoning" />
            {verifications.length > 0 && (
              <Box sx={{ mt: 2 }}>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>
                  Verification history
                </Typography>
                <Stack spacing={1}>
                  {verifications.map((v, idx) => (
                    <Typography key={v.id ?? idx} variant="body2" color="text.secondary">
                      {formatDateTime(v.createdAt as string | undefined)} —{' '}
                      {String(v.status ?? v.result ?? '—')}
                      {v.reasoning ? ` — ${String(v.reasoning)}` : ''}
                    </Typography>
                  ))}
                </Stack>
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleCreate}>
          <DialogTitle>New customer</DialogTitle>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={12} sm={6}>{field('fullName', 'Full name')}</Grid>
              <Grid item xs={12} sm={6}>{field('documentHolderName', 'Document holder name')}</Grid>
              <Grid item xs={12} sm={6}>{field('email', 'Email', { type: 'email' })}</Grid>
              <Grid item xs={12} sm={6}>{field('phone', 'Phone')}</Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Document type"
                  value={form.documentType}
                  onChange={(e) => setForm((prev) => ({ ...prev, documentType: e.target.value }))}
                  select
                  required
                  fullWidth
                >
                  {DOCUMENT_TYPES.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={6}>{field('documentNumber', 'Document number')}</Grid>
              <Grid item xs={12} sm={6}>
                {field('dateOfBirth', 'Date of birth', { type: 'date', InputLabelProps: { shrink: true } })}
              </Grid>
              <Grid item xs={12}>{field('address', 'Address', { multiline: true, minRows: 2 })}</Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={creating}>
              {creating ? 'Creating…' : 'Create customer'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      <Dialog open={reportOpen} onClose={() => setReportOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          Compliance report{reportCustomer ? ` — ${reportCustomer.fullName} (#${reportCustomer.id})` : ''}
        </DialogTitle>
        <DialogContent dividers>
          {reportLoading ? (
            <LoadingState label="Generating compliance report…" />
          ) : report ? (
            <Stack spacing={1.5}>
              {Object.entries(report).map(([key, value]) => (
                <Box key={key}>
                  <Typography variant="subtitle2" sx={{ textTransform: 'capitalize' }}>
                    {key.replace(/([A-Z])/g, ' $1')}
                  </Typography>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {renderReportValue(value)}
                  </Typography>
                </Box>
              ))}
            </Stack>
          ) : (
            <Typography color="text.secondary">No report data returned.</Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReportOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
