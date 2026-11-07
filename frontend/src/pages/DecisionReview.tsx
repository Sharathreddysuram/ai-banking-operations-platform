import { useCallback, useEffect, useState } from 'react';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
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
  Check as ApproveIcon,
  Close as RejectIcon,
  ExpandMore as ExpandMoreIcon,
} from '@mui/icons-material';
import { decisionApi } from '../api/services';
import type { DecisionCase } from '../api/types';
import { apiErrorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import {
  ErrorState,
  formatDateTime,
  LoadingState,
  PageHeader,
  RiskGauge,
  StatusChip,
} from '../components/common';

function SummaryBlock({ title, body }: { title: string; body: string }) {
  return (
    <Accordion defaultExpanded disableGutters>
      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
        <Typography variant="subtitle2">{title}</Typography>
      </AccordionSummary>
      <AccordionDetails>
        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
          {body || 'No summary returned by this agent.'}
        </Typography>
      </AccordionDetails>
    </Accordion>
  );
}

export function CaseDetail({ c }: { c: DecisionCase }) {
  return (
    <Box>
      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" sx={{ mb: 2 }}>
        <StatusChip value={c.status} />
        <StatusChip value={c.finalRecommendation} />
        <Typography variant="body2" color="text.secondary">
          {c.caseType?.replace(/_/g, ' ')} · Created {formatDateTime(c.createdAt)} · Updated{' '}
          {formatDateTime(c.updatedAt)}
        </Typography>
      </Stack>
      <Box sx={{ maxWidth: 440, mb: 2 }}>
        <RiskGauge score={c.overallRiskScore} label="Overall risk score" />
      </Box>
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <SummaryBlock title="KYC agent summary" body={c.kycSummary} />
          <SummaryBlock title="Fraud agent summary" body={c.fraudSummary} />
        </Grid>
        <Grid item xs={12} md={6}>
          <SummaryBlock title="Loan agent summary" body={c.loanSummary} />
          <SummaryBlock title="Support agent summary" body={c.supportSummary} />
        </Grid>
      </Grid>
      <Box sx={{ mt: 2 }}>
        <Typography variant="subtitle2">Orchestrator reasoning</Typography>
        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mt: 0.5 }}>
          {c.reasoning || 'No reasoning returned.'}
        </Typography>
      </Box>
      {(c.reviewedBy || c.reviewNotes) && (
        <Box sx={{ mt: 2 }}>
          <Typography variant="subtitle2">Human review</Typography>
          <Typography variant="body2" sx={{ mt: 0.5 }}>
            {c.reviewedBy ? `Reviewed by ${c.reviewedBy}. ` : ''}
            {c.reviewNotes ?? ''}
          </Typography>
        </Box>
      )}
    </Box>
  );
}

export default function DecisionReview() {
  const { username } = useAuth();
  const [cases, setCases] = useState<DecisionCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [detail, setDetail] = useState<DecisionCase | null>(null);

  const [reviewCaseItem, setReviewCaseItem] = useState<DecisionCase | null>(null);
  const [reviewDecision, setReviewDecision] = useState<'APPROVED' | 'REJECTED'>('APPROVED');
  const [reviewedBy, setReviewedBy] = useState(username ?? '');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setCases(await decisionApi.listCases());
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load decision cases.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (username) setReviewedBy((prev) => prev || username);
  }, [username]);

  const pending = cases.filter((c) => c.status === 'PENDING_REVIEW');
  const decided = cases.filter((c) => c.status !== 'PENDING_REVIEW');

  const openReview = (c: DecisionCase, decision: 'APPROVED' | 'REJECTED') => {
    setReviewCaseItem(c);
    setReviewDecision(decision);
    setNotes('');
  };

  const submitReview = async () => {
    if (!reviewCaseItem) return;
    setSubmitting(true);
    setError(null);
    try {
      const updated = await decisionApi.reviewCase(
        reviewCaseItem.id,
        reviewDecision,
        reviewedBy.trim(),
        notes.trim(),
      );
      setCases((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
      if (detail?.id === updated.id) setDetail(updated);
      setReviewCaseItem(null);
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to submit review.'));
    } finally {
      setSubmitting(false);
    }
  };

  const renderTable = (rows: DecisionCase[], showActions: boolean) => (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>ID</TableCell>
            <TableCell>Customer</TableCell>
            <TableCell>Case type</TableCell>
            <TableCell>Recommendation</TableCell>
            <TableCell>Risk</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Created</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((c) => (
            <TableRow key={c.id} hover>
              <TableCell>{c.id}</TableCell>
              <TableCell>#{c.customerId}</TableCell>
              <TableCell>{c.caseType?.replace(/_/g, ' ')}</TableCell>
              <TableCell>
                <StatusChip value={c.finalRecommendation} />
              </TableCell>
              <TableCell>{c.overallRiskScore?.toFixed(0) ?? '—'}</TableCell>
              <TableCell>
                <StatusChip value={c.status} />
              </TableCell>
              <TableCell>{formatDateTime(c.createdAt)}</TableCell>
              <TableCell align="right">
                <Stack direction="row" spacing={1} justifyContent="flex-end">
                  <Button size="small" variant="outlined" onClick={() => setDetail(c)}>
                    Detail
                  </Button>
                  {showActions && (
                    <>
                      <Button
                        size="small"
                        variant="contained"
                        color="success"
                        startIcon={<ApproveIcon />}
                        onClick={() => openReview(c, 'APPROVED')}
                      >
                        Approve
                      </Button>
                      <Button
                        size="small"
                        variant="contained"
                        color="error"
                        startIcon={<RejectIcon />}
                        onClick={() => openReview(c, 'REJECTED')}
                      >
                        Reject
                      </Button>
                    </>
                  )}
                </Stack>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );

  return (
    <Box>
      <PageHeader
        title="Decision Review"
        subtitle="Human-in-the-loop queue: approve or reject orchestrated cases with notes."
      />
      {error && <ErrorState message={error} />}

      <Card>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Pending review queue ({pending.length})
          </Typography>
          {loading ? (
            <LoadingState label="Loading review queue…" />
          ) : pending.length === 0 ? (
            <Typography color="text.secondary">
              Queue is clear — no cases are waiting for a human decision.
            </Typography>
          ) : (
            renderTable(pending, true)
          )}
        </CardContent>
      </Card>

      <Card sx={{ mt: 2.5 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 2 }}>
            Decided cases ({decided.length})
          </Typography>
          {loading ? (
            <LoadingState label="Loading decided cases…" />
          ) : decided.length === 0 ? (
            <Typography color="text.secondary">No cases have been decided yet.</Typography>
          ) : (
            renderTable(decided, false)
          )}
        </CardContent>
      </Card>

      <Dialog open={Boolean(detail)} onClose={() => setDetail(null)} maxWidth="md" fullWidth>
        <DialogTitle>
          Case #{detail?.id} — Customer #{detail?.customerId}
        </DialogTitle>
        <DialogContent dividers>{detail && <CaseDetail c={detail} />}</DialogContent>
        <DialogActions>
          <Button onClick={() => setDetail(null)}>Close</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={Boolean(reviewCaseItem)} onClose={() => setReviewCaseItem(null)} maxWidth="sm" fullWidth>
        <DialogTitle>
          {reviewDecision === 'APPROVED' ? 'Approve' : 'Reject'} case #{reviewCaseItem?.id}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Reviewed by"
              value={reviewedBy}
              onChange={(e) => setReviewedBy(e.target.value)}
              required
              fullWidth
            />
            <TextField
              label="Review notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              multiline
              minRows={3}
              fullWidth
              placeholder="Why is this decision correct? Anything the agents missed?"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReviewCaseItem(null)}>Cancel</Button>
          <Button
            variant="contained"
            color={reviewDecision === 'APPROVED' ? 'success' : 'error'}
            disabled={submitting || !reviewedBy.trim()}
            startIcon={submitting ? <CircularProgress size={16} color="inherit" /> : undefined}
            onClick={() => void submitReview()}
          >
            {submitting ? 'Submitting…' : `Confirm ${reviewDecision === 'APPROVED' ? 'approval' : 'rejection'}`}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
