import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
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
import { PlayArrow as RunIcon, SmartToy as AgentIcon } from '@mui/icons-material';
import { decisionApi } from '../api/services';
import { unwrap, type AgentStatus, type DecisionCase } from '../api/types';
import { apiErrorMessage } from '../api/client';
import {
  ErrorState,
  formatDateTime,
  LoadingState,
  PageHeader,
  RiskGauge,
  StatusChip,
} from '../components/common';

const CASE_TYPES = [
  'ACCOUNT_OPENING',
  'LOAN_DECISION',
  'FRAUD_INVESTIGATION',
  'CUSTOMER_REVIEW',
  'COMPLIANCE_REVIEW',
  'FULL_ASSESSMENT',
];

export default function OrchestrationCenter() {
  const navigate = useNavigate();
  const [agents, setAgents] = useState<AgentStatus[]>([]);
  const [cases, setCases] = useState<DecisionCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [customerId, setCustomerId] = useState('');
  const [caseType, setCaseType] = useState(CASE_TYPES[0]);
  const [running, setRunning] = useState(false);
  const [lastCase, setLastCase] = useState<DecisionCase | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [agentList, caseList] = await Promise.all([
        decisionApi.agentStatus(),
        decisionApi.listCases(),
      ]);
      setAgents(agentList);
      setCases(caseList);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load orchestration data.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleRun = async (event: FormEvent) => {
    event.preventDefault();
    const custId = Number(customerId);
    if (!custId) return;
    setRunning(true);
    setError(null);
    try {
      const res = await decisionApi.createCase(custId, caseType);
      setLastCase(unwrap(res));
      await load();
    } catch (err) {
      setError(apiErrorMessage(err, 'Orchestration run failed.'));
    } finally {
      setRunning(false);
    }
  };

  return (
    <Box>
      <PageHeader
        title="Orchestration Center"
        subtitle="The decision agent coordinates KYC, fraud, loan and support agents into one case."
      />
      {error && <ErrorState message={error} />}

      <Typography variant="h6" sx={{ mb: 1.5 }}>
        Agent fleet
      </Typography>
      {loading ? (
        <LoadingState label="Loading agents…" />
      ) : agents.length === 0 ? (
        <Typography color="text.secondary" sx={{ mb: 3 }}>
          No agent status returned by the decision service.
        </Typography>
      ) : (
        <Grid container spacing={2.5} sx={{ mb: 1 }}>
          {agents.map((agent) => (
            <Grid item xs={12} sm={6} lg={4} key={agent.name}>
              <Card sx={{ height: '100%' }}>
                <CardContent>
                  <Stack direction="row" spacing={1.5} alignItems="flex-start">
                    <Box
                      sx={{
                        width: 42,
                        height: 42,
                        borderRadius: 2,
                        backgroundColor: '#EDF2F7',
                        color: '#0B2C4A',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        flexShrink: 0,
                      }}
                    >
                      <AgentIcon />
                    </Box>
                    <Box sx={{ minWidth: 0 }}>
                      <Typography variant="subtitle1">{agent.name}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {agent.description}
                      </Typography>
                      <Stack direction="row" spacing={1} sx={{ mt: 1 }} flexWrap="wrap">
                        <StatusChip value={agent.status} />
                        {agent.mode && <Chip label={agent.mode} size="small" variant="outlined" />}
                      </Stack>
                    </Box>
                  </Stack>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <Card sx={{ mt: 2 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Run orchestration — create decision case
          </Typography>
          <form onSubmit={handleRun}>
            <Grid container spacing={2} alignItems="center">
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Customer ID"
                  type="number"
                  value={customerId}
                  onChange={(e) => setCustomerId(e.target.value)}
                  required
                  fullWidth
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <TextField
                  label="Case type"
                  value={caseType}
                  onChange={(e) => setCaseType(e.target.value)}
                  select
                  required
                  fullWidth
                >
                  {CASE_TYPES.map((type) => (
                    <MenuItem key={type} value={type}>
                      {type.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </TextField>
              </Grid>
              <Grid item xs={12} sm={4}>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  startIcon={running ? <CircularProgress size={18} color="inherit" /> : <RunIcon />}
                  disabled={running}
                  fullWidth
                >
                  {running ? 'Agents are working…' : 'Run orchestration'}
                </Button>
              </Grid>
            </Grid>
          </form>
          {lastCase && (
            <Box sx={{ mt: 2.5 }}>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                <Typography variant="subtitle2">
                  Case #{lastCase.id} created for customer #{lastCase.customerId}
                </Typography>
                <StatusChip value={lastCase.status} />
                <StatusChip value={lastCase.finalRecommendation} />
              </Stack>
              <Box sx={{ mt: 1.5, maxWidth: 420 }}>
                <RiskGauge score={lastCase.overallRiskScore} label="Overall risk" />
              </Box>
              {lastCase.reasoning && (
                <Typography variant="body2" sx={{ mt: 1.5, whiteSpace: 'pre-wrap' }}>
                  {lastCase.reasoning}
                </Typography>
              )}
              <Button sx={{ mt: 1.5 }} variant="outlined" onClick={() => navigate('/review')}>
                Open in Decision Review
              </Button>
            </Box>
          )}
        </CardContent>
      </Card>

      <Card sx={{ mt: 2.5 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Decision cases
          </Typography>
          {loading ? (
            <LoadingState label="Loading cases…" />
          ) : cases.length === 0 ? (
            <Typography color="text.secondary">No decision cases yet. Run the orchestration above.</Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Customer</TableCell>
                    <TableCell>Case type</TableCell>
                    <TableCell>Recommendation</TableCell>
                    <TableCell>Overall risk</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Created</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {cases.map((c) => (
                    <TableRow key={c.id} hover>
                      <TableCell>{c.id}</TableCell>
                      <TableCell>#{c.customerId}</TableCell>
                      <TableCell>{c.caseType?.replace(/_/g, ' ')}</TableCell>
                      <TableCell>
                        <StatusChip value={c.finalRecommendation} />
                      </TableCell>
                      <TableCell sx={{ minWidth: 150 }}>
                        <RiskGauge score={c.overallRiskScore} label="" />
                      </TableCell>
                      <TableCell>
                        <StatusChip value={c.status} />
                      </TableCell>
                      <TableCell>{formatDateTime(c.createdAt)}</TableCell>
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
