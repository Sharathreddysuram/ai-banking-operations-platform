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
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
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
import {
  Add as AddIcon,
  CheckCircleOutline as FactorIcon,
  Psychology as UnderwriteIcon,
} from '@mui/icons-material';
import { loanApi } from '../api/services';
import {
  asAgentResponse,
  type AgentResponse,
  type LoanApplication,
  type LoanApplicationForm,
  type LoanAssessment,
} from '../api/types';
import { apiErrorMessage } from '../api/client';
import {
  ErrorState,
  formatMoney,
  LoadingState,
  PageHeader,
  RiskGauge,
  splitFactors,
  StatusChip,
} from '../components/common';

const EMPTY_FORM: LoanApplicationForm = {
  customerId: 0,
  requestedAmount: 25000,
  termMonths: 36,
  purpose: 'HOME_IMPROVEMENT',
  monthlyIncome: 6500,
  monthlyDebtPayments: 900,
  annualIncome: 78000,
  creditScore: 720,
  employmentStatus: 'EMPLOYED',
};
const PURPOSES = ['HOME_IMPROVEMENT', 'DEBT_CONSOLIDATION', 'AUTO', 'EDUCATION', 'MEDICAL', 'BUSINESS', 'PERSONAL', 'OTHER'];
const EMPLOYMENT = ['EMPLOYED', 'SELF_EMPLOYED', 'CONTRACT', 'UNEMPLOYED', 'RETIRED'];

export default function LoanCenter() {
  const [applications, setApplications] = useState<LoanApplication[]>([]);
  const [filterCustomerId, setFilterCustomerId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState<LoanApplicationForm>(EMPTY_FORM);
  const [creating, setCreating] = useState(false);

  const [underwritingId, setUnderwritingId] = useState<number | null>(null);
  const [assessment, setAssessment] = useState<LoanAssessment | null>(null);
  const [agentMeta, setAgentMeta] = useState<AgentResponse<LoanAssessment> | null>(null);
  const [assessedApp, setAssessedApp] = useState<LoanApplication | null>(null);
  const [loadingAssessmentId, setLoadingAssessmentId] = useState<number | null>(null);

  const load = useCallback(async (customerId?: number) => {
    setLoading(true);
    setError(null);
    try {
      setApplications(await loanApi.listApplications(customerId));
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load loan applications.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const parsedFilter = filterCustomerId.trim() ? Number(filterCustomerId) : undefined;

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    setCreating(true);
    setError(null);
    try {
      await loanApi.createApplication(form);
      setCreateOpen(false);
      setForm(EMPTY_FORM);
      await load(parsedFilter);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to create loan application.'));
    } finally {
      setCreating(false);
    }
  };

  const showAssessment = (app: LoanApplication, payload: LoanAssessment | AgentResponse<LoanAssessment>) => {
    const agent = asAgentResponse(payload);
    setAgentMeta(agent);
    setAssessment(agent ? agent.data : (payload as LoanAssessment));
    setAssessedApp(app);
  };

  const handleUnderwrite = async (app: LoanApplication) => {
    setUnderwritingId(app.id);
    setError(null);
    try {
      const res = await loanApi.underwrite(app.id);
      showAssessment(app, res);
      await load(parsedFilter);
    } catch (err) {
      setError(apiErrorMessage(err, 'Underwriting failed.'));
    } finally {
      setUnderwritingId(null);
    }
  };

  const handleViewAssessment = async (app: LoanApplication) => {
    setLoadingAssessmentId(app.id);
    setError(null);
    try {
      const res = await loanApi.getAssessment(app.id);
      showAssessment(app, res);
    } catch (err) {
      setError(apiErrorMessage(err, 'No assessment found for this application yet — run Underwrite first.'));
    } finally {
      setLoadingAssessmentId(null);
    }
  };

  const factors = splitFactors(assessment?.factors);
  const numField = (
    key: keyof LoanApplicationForm,
    label: string,
    props?: Record<string, unknown>,
  ) => (
    <TextField
      label={label}
      type="number"
      value={form[key]}
      onChange={(e) => setForm((p) => ({ ...p, [key]: Number(e.target.value) }))}
      required
      fullWidth
      {...props}
    />
  );

  return (
    <Box>
      <PageHeader
        title="Loan Center"
        subtitle="Create applications and run the AI underwriting agent."
        action={
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            New application
          </Button>
        }
      />
      {error && <ErrorState message={error} />}

      <Card>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }} flexWrap="wrap">
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              Applications
            </Typography>
            <TextField
              label="Filter by customer ID"
              type="number"
              size="small"
              value={filterCustomerId}
              onChange={(e) => setFilterCustomerId(e.target.value)}
            />
            <Button variant="outlined" onClick={() => void load(parsedFilter)}>
              Apply filter
            </Button>
            <Button
              onClick={() => {
                setFilterCustomerId('');
                void load();
              }}
            >
              Clear
            </Button>
          </Stack>

          {loading ? (
            <LoadingState label="Loading applications…" />
          ) : applications.length === 0 ? (
            <Typography color="text.secondary">No loan applications found.</Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Customer</TableCell>
                    <TableCell>Requested</TableCell>
                    <TableCell>Term / Purpose</TableCell>
                    <TableCell>Income · Credit</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {applications.map((app) => (
                    <TableRow key={app.id} hover selected={assessedApp?.id === app.id}>
                      <TableCell>{app.id}</TableCell>
                      <TableCell>#{app.customerId}</TableCell>
                      <TableCell>{formatMoney(app.requestedAmount)}</TableCell>
                      <TableCell>
                        {app.termMonths} mo
                        <Typography variant="body2" color="text.secondary">
                          {app.purpose?.replace(/_/g, ' ')}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        {formatMoney(app.annualIncome)} / yr
                        <Typography variant="body2" color="text.secondary">
                          Credit {app.creditScore} · {app.employmentStatus?.replace(/_/g, ' ')}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <StatusChip value={app.status} />
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={1} justifyContent="flex-end">
                          <Button
                            size="small"
                            variant="outlined"
                            disabled={loadingAssessmentId === app.id}
                            onClick={() => void handleViewAssessment(app)}
                          >
                            Assessment
                          </Button>
                          <Button
                            size="small"
                            variant="contained"
                            color="secondary"
                            startIcon={
                              underwritingId === app.id ? (
                                <CircularProgress size={14} color="inherit" />
                              ) : (
                                <UnderwriteIcon />
                              )
                            }
                            disabled={underwritingId === app.id}
                            onClick={() => void handleUnderwrite(app)}
                          >
                            Underwrite
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

      {assessment && (
        <Card sx={{ mt: 2.5 }}>
          <CardContent>
            <Typography variant="h6">
              Underwriting assessment
              {assessedApp
                ? ` — Application #${assessedApp.id} (Customer #${assessedApp.customerId}, ${formatMoney(assessedApp.requestedAmount)} requested)`
                : ''}
            </Typography>
            <Stack direction="row" spacing={1} sx={{ mt: 1.5 }} flexWrap="wrap" alignItems="center">
              <StatusChip value={assessment.recommendation} />
              <StatusChip value={assessment.riskBand} />
              {agentMeta && (
                <Typography variant="body2" color="text.secondary">
                  by {agentMeta.agentName}
                  {agentMeta.confidence !== undefined &&
                    ` · confidence ${Math.round(agentMeta.confidence <= 1 ? agentMeta.confidence * 100 : agentMeta.confidence)}%`}
                </Typography>
              )}
            </Stack>
            <Grid container spacing={3} sx={{ mt: 1 }}>
              <Grid item xs={12} sm={4}>
                <Typography variant="body2" color="text.secondary">
                  Debt-to-income ratio
                </Typography>
                <Typography variant="h5">
                  {assessment.dtiRatio !== null && assessment.dtiRatio !== undefined
                    ? `${(assessment.dtiRatio <= 1 ? assessment.dtiRatio * 100 : assessment.dtiRatio).toFixed(1)}%`
                    : '—'}
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5 }}>
                  Recommended amount
                </Typography>
                <Typography variant="h5">{formatMoney(assessment.recommendedAmount)}</Typography>
              </Grid>
              <Grid item xs={12} sm={4}>
                <RiskGauge score={assessment.riskScore} label="Credit risk score" />
                <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
                  Risk band
                </Typography>
                <Typography variant="h6">{assessment.riskBand || '—'}</Typography>
              </Grid>
              <Grid item xs={12} sm={4}>
                <Typography variant="body2" color="text.secondary">
                  Decision factors
                </Typography>
                {factors.length === 0 ? (
                  <Typography variant="body2">No factors returned.</Typography>
                ) : (
                  <List dense disablePadding>
                    {factors.map((factor, idx) => (
                      <ListItem key={idx} sx={{ px: 0 }}>
                        <ListItemIcon sx={{ minWidth: 30 }}>
                          <FactorIcon color="secondary" fontSize="small" />
                        </ListItemIcon>
                        <ListItemText primary={factor} />
                      </ListItem>
                    ))}
                  </List>
                )}
              </Grid>
            </Grid>
            {(agentMeta?.reasoning ?? assessment.reasoning) && (
              <Box sx={{ mt: 2 }}>
                <Typography variant="subtitle2">Reasoning</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {agentMeta?.reasoning ?? assessment.reasoning}
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <form onSubmit={handleCreate}>
          <DialogTitle>New loan application</DialogTitle>
          <DialogContent>
            <Grid container spacing={2} sx={{ mt: 0.5 }}>
              <Grid item xs={6}>{numField('customerId', 'Customer ID')}</Grid>
              <Grid item xs={6}>{numField('creditScore', 'Credit score', { inputProps: { min: 300, max: 850 } })}</Grid>
              <Grid item xs={6}>{numField('requestedAmount', 'Requested amount', { inputProps: { min: 0, step: '100' } })}</Grid>
              <Grid item xs={6}>{numField('termMonths', 'Term (months)', { inputProps: { min: 1 } })}</Grid>
              <Grid item xs={6}>{numField('monthlyIncome', 'Monthly income', { inputProps: { min: 0, step: '50' } })}</Grid>
              <Grid item xs={6}>{numField('monthlyDebtPayments', 'Monthly debt payments', { inputProps: { min: 0, step: '50' } })}</Grid>
              <Grid item xs={6}>{numField('annualIncome', 'Annual income', { inputProps: { min: 0, step: '500' } })}</Grid>
              <Grid item xs={6}>
                <TextField
                  label="Purpose"
                  value={form.purpose}
                  onChange={(e) => setForm((p) => ({ ...p, purpose: e.target.value }))}
                  select
                  required
                  fullWidth
                >
                  {PURPOSES.map((purpose) => (
                    <MenuItem key={purpose} value={purpose}>
                      {purpose.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12}>
                <TextField
                  label="Employment status"
                  value={form.employmentStatus}
                  onChange={(e) => setForm((p) => ({ ...p, employmentStatus: e.target.value }))}
                  select
                  required
                  fullWidth
                >
                  {EMPLOYMENT.map((status) => (
                    <MenuItem key={status} value={status}>
                      {status.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCreateOpen(false)}>Cancel</Button>
            <Button type="submit" variant="contained" disabled={creating}>
              {creating ? 'Creating…' : 'Create application'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
