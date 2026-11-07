import { useCallback, useEffect, useState, type FormEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
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
  Psychology as AnalyzeIcon,
  WarningAmber as WarningIcon,
} from '@mui/icons-material';
import { fraudApi } from '../api/services';
import type {
  AgentResponse,
  FraudAssessment,
  Transaction,
  TransactionForm,
} from '../api/types';
import { apiErrorMessage } from '../api/client';
import {
  ErrorState,
  formatDateTime,
  formatMoney,
  LoadingState,
  PageHeader,
  RiskGauge,
  splitFactors,
  StatusChip,
} from '../components/common';

const EMPTY_FORM: TransactionForm = {
  customerId: 0,
  amount: 0,
  currency: 'USD',
  merchant: '',
  category: '',
  location: '',
  channel: 'ONLINE',
};
const CHANNELS = ['ONLINE', 'POS', 'ATM', 'MOBILE', 'BRANCH'];
const CATEGORIES = ['RETAIL', 'GROCERY', 'TRAVEL', 'ENTERTAINMENT', 'TRANSFER', 'CRYPTO', 'GAMBLING', 'OTHER'];

export default function FraudCenter() {
  const [alerts, setAlerts] = useState<FraudAssessment[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [filterCustomerId, setFilterCustomerId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState<TransactionForm>(EMPTY_FORM);
  const [ingesting, setIngesting] = useState(false);
  const [ingestMsg, setIngestMsg] = useState<string | null>(null);

  const [analyzingId, setAnalyzingId] = useState<number | null>(null);
  const [analysis, setAnalysis] = useState<AgentResponse<FraudAssessment> | null>(null);
  const [analyzedTx, setAnalyzedTx] = useState<Transaction | null>(null);

  const load = useCallback(async (customerId?: number) => {
    setLoading(true);
    setError(null);
    try {
      const [alertList, txList] = await Promise.all([
        fraudApi.listAlerts(),
        fraudApi.listTransactions(customerId),
      ]);
      setAlerts(alertList);
      setTransactions(txList);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load fraud data.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const parsedFilter = filterCustomerId.trim() ? Number(filterCustomerId) : undefined;

  const handleIngest = async (event: FormEvent) => {
    event.preventDefault();
    setIngesting(true);
    setIngestMsg(null);
    setError(null);
    try {
      await fraudApi.createTransaction(form);
      setIngestMsg(`Transaction ingested for customer #${form.customerId}.`);
      setForm((prev) => ({ ...EMPTY_FORM, customerId: prev.customerId }));
      await load(parsedFilter);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to ingest transaction.'));
    } finally {
      setIngesting(false);
    }
  };

  const handleAnalyze = async (tx: Transaction) => {
    setAnalyzingId(tx.id);
    setError(null);
    setAnalyzedTx(tx);
    try {
      const res = await fraudApi.analyzeTransaction(tx.id);
      setAnalysis(res);
      await load(parsedFilter);
    } catch (err) {
      setError(apiErrorMessage(err, 'Fraud analysis failed.'));
    } finally {
      setAnalyzingId(null);
    }
  };

  const assessment = analysis?.data;
  const reasonList = splitFactors(assessment?.reasons).length
    ? splitFactors(assessment?.reasons)
    : splitFactors(assessment?.reasoning);

  return (
    <Box>
      <PageHeader
        title="Fraud Center"
        subtitle="Ingest transactions, run the fraud-detection agent and triage alerts."
      />
      {error && <ErrorState message={error} />}

      <Grid container spacing={2.5}>
        <Grid item xs={12} lg={4}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Alerts — HIGH / CRITICAL
              </Typography>
              {loading ? (
                <LoadingState label="Loading alerts…" />
              ) : alerts.length === 0 ? (
                <Typography color="text.secondary">No high-severity alerts. Good news.</Typography>
              ) : (
                <Stack spacing={1.5}>
                  {alerts.map((alert) => (
                    <Box
                      key={alert.id}
                      sx={{ border: '1px solid #F1D3CE', backgroundColor: '#FEF6F5', borderRadius: 2, p: 1.5 }}
                    >
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                        <StatusChip value={alert.severity} />
                        <StatusChip value={alert.recommendation} />
                        <Typography variant="body2" color="text.secondary">
                          Tx #{alert.transactionId} · Customer #{alert.customerId}
                        </Typography>
                      </Stack>
                      <Box sx={{ mt: 1 }}>
                        <RiskGauge score={alert.riskScore} />
                      </Box>
                      {alert.reasons && (
                        <Typography variant="body2" sx={{ mt: 1 }}>
                          {alert.reasons}
                        </Typography>
                      )}
                      <Typography variant="caption" color="text.secondary">
                        {formatDateTime(alert.createdAt)}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={8}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Ingest transaction
              </Typography>
              {ingestMsg && (
                <Alert severity="success" sx={{ mb: 2 }} onClose={() => setIngestMsg(null)}>
                  {ingestMsg}
                </Alert>
              )}
              <form onSubmit={handleIngest}>
                <Grid container spacing={2}>
                  <Grid item xs={6} sm={3}>
                    <TextField
                      label="Customer ID"
                      type="number"
                      value={form.customerId || ''}
                      onChange={(e) => setForm((p) => ({ ...p, customerId: Number(e.target.value) }))}
                      required
                      fullWidth
                    />
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <TextField
                      label="Amount"
                      type="number"
                      inputProps={{ min: 0, step: '0.01' }}
                      value={form.amount || ''}
                      onChange={(e) => setForm((p) => ({ ...p, amount: Number(e.target.value) }))}
                      required
                      fullWidth
                    />
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <TextField
                      label="Currency"
                      value={form.currency}
                      onChange={(e) => setForm((p) => ({ ...p, currency: e.target.value.toUpperCase() }))}
                      required
                      fullWidth
                    />
                  </Grid>
                  <Grid item xs={6} sm={3}>
                    <TextField
                      label="Channel"
                      value={form.channel}
                      onChange={(e) => setForm((p) => ({ ...p, channel: e.target.value }))}
                      select
                      required
                      fullWidth
                    >
                      {CHANNELS.map((c) => (
                        <MenuItem key={c} value={c}>{c}</MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid item xs={12} sm={4}>
                    <TextField
                      label="Merchant"
                      value={form.merchant}
                      onChange={(e) => setForm((p) => ({ ...p, merchant: e.target.value }))}
                      required
                      fullWidth
                    />
                  </Grid>
                  <Grid item xs={6} sm={4}>
                    <TextField
                      label="Category"
                      value={form.category}
                      onChange={(e) => setForm((p) => ({ ...p, category: e.target.value }))}
                      select
                      required
                      fullWidth
                    >
                      {CATEGORIES.map((c) => (
                        <MenuItem key={c} value={c}>{c}</MenuItem>
                      ))}
                    </TextField>
                  </Grid>
                  <Grid item xs={6} sm={4}>
                    <TextField
                      label="Location"
                      value={form.location}
                      onChange={(e) => setForm((p) => ({ ...p, location: e.target.value }))}
                      required
                      fullWidth
                    />
                  </Grid>
                  <Grid item xs={12}>
                    <Button type="submit" variant="contained" startIcon={<AddIcon />} disabled={ingesting}>
                      {ingesting ? 'Ingesting…' : 'Ingest transaction'}
                    </Button>
                  </Grid>
                </Grid>
              </form>
            </CardContent>
          </Card>

          {analysis && assessment && (
            <Card sx={{ mt: 2.5 }}>
              <CardContent>
                <Typography variant="h6">
                  Fraud analysis{analyzedTx ? ` — Tx #${analyzedTx.id} (${formatMoney(analyzedTx.amount, analyzedTx.currency)} at ${analyzedTx.merchant})` : ''}
                </Typography>
                <Stack direction="row" spacing={1} sx={{ mt: 1.5 }} flexWrap="wrap" alignItems="center">
                  <StatusChip value={assessment.severity} />
                  <StatusChip value={assessment.recommendation} />
                  <Typography variant="body2" color="text.secondary">
                    by {analysis.agentName}
                    {analysis.confidence !== undefined &&
                      ` · confidence ${Math.round(analysis.confidence <= 1 ? analysis.confidence * 100 : analysis.confidence)}%`}
                  </Typography>
                </Stack>
                <Box sx={{ mt: 2, maxWidth: 420 }}>
                  <RiskGauge score={assessment.riskScore} label="Fraud risk score" />
                </Box>
                {reasonList.length > 0 && (
                  <List dense sx={{ mt: 1 }}>
                    {reasonList.map((reason, idx) => (
                      <ListItem key={idx} sx={{ px: 0 }}>
                        <ListItemIcon sx={{ minWidth: 30 }}>
                          <WarningIcon color="warning" fontSize="small" />
                        </ListItemIcon>
                        <ListItemText primary={reason} />
                      </ListItem>
                    ))}
                  </List>
                )}
                {analysis.reasoning && (
                  <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>
                    {analysis.reasoning}
                  </Typography>
                )}
              </CardContent>
            </Card>
          )}
        </Grid>
      </Grid>

      <Card sx={{ mt: 2.5 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }} flexWrap="wrap">
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              Transactions
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
            <LoadingState label="Loading transactions…" />
          ) : transactions.length === 0 ? (
            <Typography color="text.secondary">No transactions found.</Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Customer</TableCell>
                    <TableCell>Amount</TableCell>
                    <TableCell>Merchant / Category</TableCell>
                    <TableCell>Location · Channel</TableCell>
                    <TableCell>Timestamp</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell align="right">Action</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {transactions.map((tx) => (
                    <TableRow key={tx.id} hover>
                      <TableCell>{tx.id}</TableCell>
                      <TableCell>#{tx.customerId}</TableCell>
                      <TableCell>{formatMoney(tx.amount, tx.currency)}</TableCell>
                      <TableCell>
                        <Typography variant="subtitle2">{tx.merchant}</Typography>
                        <Typography variant="body2" color="text.secondary">{tx.category}</Typography>
                      </TableCell>
                      <TableCell>
                        {tx.location}
                        <Typography variant="body2" color="text.secondary">{tx.channel}</Typography>
                      </TableCell>
                      <TableCell>{formatDateTime(tx.timestamp)}</TableCell>
                      <TableCell>
                        <StatusChip value={tx.status} />
                      </TableCell>
                      <TableCell align="right">
                        <Button
                          size="small"
                          variant="contained"
                          color="secondary"
                          startIcon={
                            analyzingId === tx.id ? (
                              <CircularProgress size={14} color="inherit" />
                            ) : (
                              <AnalyzeIcon />
                            )
                          }
                          disabled={analyzingId === tx.id}
                          onClick={() => void handleAnalyze(tx)}
                        >
                          Analyze
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
