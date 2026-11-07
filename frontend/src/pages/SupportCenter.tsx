import { useCallback, useEffect, useState, type FormEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Grid,
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
import { Send as SendIcon } from '@mui/icons-material';
import { supportApi } from '../api/services';
import type { AgentResponse, KnowledgeEntry, SupportTicket } from '../api/types';
import { apiErrorMessage } from '../api/client';
import {
  AgentReasoningPanel,
  ErrorState,
  formatDateTime,
  LoadingState,
  PageHeader,
  StatusChip,
} from '../components/common';

const SUGGESTED = [
  'Why was my card transaction declined?',
  'How do I dispute a charge on my account?',
  'What documents do I need for a loan application?',
  'Why is my KYC verification still pending?',
];

export default function SupportCenter() {
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [knowledge, setKnowledge] = useState<KnowledgeEntry[]>([]);
  const [filterCustomerId, setFilterCustomerId] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [customerId, setCustomerId] = useState('');
  const [question, setQuestion] = useState('');
  const [asking, setAsking] = useState(false);
  const [answer, setAnswer] = useState<AgentResponse<SupportTicket> | null>(null);

  const [closingId, setClosingId] = useState<number | null>(null);

  const load = useCallback(async (custId?: number) => {
    setLoading(true);
    setError(null);
    try {
      const [ticketList, kb] = await Promise.all([
        supportApi.listTickets(custId),
        supportApi.knowledge().catch(() => [] as KnowledgeEntry[]),
      ]);
      setTickets(ticketList);
      setKnowledge(kb);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load support tickets.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const parsedFilter = filterCustomerId.trim() ? Number(filterCustomerId) : undefined;

  const handleAsk = async (event: FormEvent) => {
    event.preventDefault();
    const custId = Number(customerId);
    if (!custId || !question.trim()) return;
    setAsking(true);
    setError(null);
    try {
      const res = await supportApi.ask(custId, question.trim());
      setAnswer(res);
      setQuestion('');
      await load(parsedFilter);
    } catch (err) {
      setError(apiErrorMessage(err, 'The support agent could not answer this question.'));
    } finally {
      setAsking(false);
    }
  };

  const handleClose = async (ticket: SupportTicket) => {
    setClosingId(ticket.id);
    setError(null);
    try {
      await supportApi.closeTicket(ticket.id);
      await load(parsedFilter);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to close ticket.'));
    } finally {
      setClosingId(null);
    }
  };

  const answeredTicket = answer?.data;

  return (
    <Box>
      <PageHeader
        title="Support Center"
        subtitle="Ask the AI support agent, review answers and manage escalations."
      />
      {error && <ErrorState message={error} />}

      <Grid container spacing={2.5}>
        <Grid item xs={12} lg={7}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Ask the support agent
              </Typography>
              <form onSubmit={handleAsk}>
                <Stack spacing={2}>
                  <TextField
                    label="Customer ID"
                    type="number"
                    value={customerId}
                    onChange={(e) => setCustomerId(e.target.value)}
                    required
                    fullWidth
                  />
                  <TextField
                    label="Question"
                    value={question}
                    onChange={(e) => setQuestion(e.target.value)}
                    required
                    fullWidth
                    multiline
                    minRows={3}
                    placeholder="e.g. Why was my transaction flagged as fraud?"
                  />
                  <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                    {SUGGESTED.map((s) => (
                      <Chip
                        key={s}
                        label={s}
                        variant="outlined"
                        onClick={() => setQuestion(s)}
                        sx={{ cursor: 'pointer' }}
                      />
                    ))}
                  </Stack>
                  <Box>
                    <Button
                      type="submit"
                      variant="contained"
                      startIcon={asking ? <CircularProgress size={16} color="inherit" /> : <SendIcon />}
                      disabled={asking}
                    >
                      {asking ? 'Agent is thinking…' : 'Ask agent'}
                    </Button>
                  </Box>
                </Stack>
              </form>

              {answer && answeredTicket && (
                <Box sx={{ mt: 3 }}>
                  <Alert
                    severity={answeredTicket.escalated ? 'warning' : 'success'}
                    icon={false}
                    sx={{ '& .MuiAlert-message': { width: '100%' } }}
                  >
                    <Stack spacing={1}>
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                        <Typography variant="subtitle2">
                          {answeredTicket.subject || 'Support answer'}
                        </Typography>
                        <StatusChip value={answeredTicket.status} />
                        {answeredTicket.category && (
                          <Chip label={answeredTicket.category} size="small" variant="outlined" />
                        )}
                        {answeredTicket.escalated && (
                          <Chip label="ESCALATED TO HUMAN" size="small" color="error" />
                        )}
                      </Stack>
                      <Typography variant="body2">
                        <strong>Q:</strong> {answeredTicket.question}
                      </Typography>
                      <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>
                        {answeredTicket.answer || 'No answer returned.'}
                      </Typography>
                    </Stack>
                  </Alert>
                  <AgentReasoningPanel response={answer} title="Support agent reasoning" />
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} lg={5}>
          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 1.5 }}>
                Knowledge base
              </Typography>
              {knowledge.length === 0 ? (
                <Typography color="text.secondary">
                  No knowledge entries returned by the support service.
                </Typography>
              ) : (
                <Stack spacing={1.5}>
                  {knowledge.slice(0, 8).map((entry, idx) => (
                    <Box key={entry.id ?? idx} sx={{ borderBottom: '1px solid #EEF2F6', pb: 1.5 }}>
                      <Typography variant="subtitle2">
                        {String(entry.title ?? entry.topic ?? entry.category ?? `Entry ${idx + 1}`)}
                      </Typography>
                      <Typography variant="body2" color="text.secondary">
                        {String(entry.content ?? entry.answer ?? '')}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card sx={{ mt: 2.5 }}>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }} flexWrap="wrap">
            <Typography variant="h6" sx={{ flexGrow: 1 }}>
              Tickets
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
            <LoadingState label="Loading tickets…" />
          ) : tickets.length === 0 ? (
            <Typography color="text.secondary">No tickets found.</Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Customer</TableCell>
                    <TableCell>Subject / Question</TableCell>
                    <TableCell>Category</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Created</TableCell>
                    <TableCell align="right">Action</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tickets.map((ticket) => (
                    <TableRow
                      key={ticket.id}
                      hover
                      sx={
                        ticket.escalated
                          ? { backgroundColor: '#FEF6F5', '& td': { borderBottomColor: '#F1D3CE' } }
                          : undefined
                      }
                    >
                      <TableCell>{ticket.id}</TableCell>
                      <TableCell>#{ticket.customerId}</TableCell>
                      <TableCell sx={{ maxWidth: 380 }}>
                        <Typography variant="subtitle2">{ticket.subject || '—'}</Typography>
                        <Typography variant="body2" color="text.secondary" noWrap>
                          {ticket.question}
                        </Typography>
                        {ticket.escalated && (
                          <Chip label="ESCALATED" size="small" color="error" sx={{ mt: 0.5 }} />
                        )}
                      </TableCell>
                      <TableCell>{ticket.category || '—'}</TableCell>
                      <TableCell>
                        <StatusChip value={ticket.status} />
                      </TableCell>
                      <TableCell>{formatDateTime(ticket.createdAt)}</TableCell>
                      <TableCell align="right">
                        {ticket.status !== 'CLOSED' ? (
                          <Button
                            size="small"
                            variant="outlined"
                            disabled={closingId === ticket.id}
                            onClick={() => void handleClose(ticket)}
                          >
                            {closingId === ticket.id ? 'Closing…' : 'Close'}
                          </Button>
                        ) : (
                          <Typography variant="body2" color="text.secondary">
                            Closed
                          </Typography>
                        )}
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
