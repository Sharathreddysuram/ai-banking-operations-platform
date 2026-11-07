import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Collapse,
  IconButton,
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
  ExpandLess as ExpandLessIcon,
  ExpandMore as ExpandMoreIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { decisionApi } from '../api/services';
import type { AuditEntry } from '../api/types';
import { apiErrorMessage } from '../api/client';
import {
  ErrorState,
  formatDateTime,
  LoadingState,
  PageHeader,
  StatusChip,
} from '../components/common';

function entryAgent(entry: AuditEntry): string {
  return String(entry.agentName ?? entry.agent ?? 'Unknown agent');
}

function entryTime(entry: AuditEntry): string {
  return formatDateTime((entry.createdAt ?? entry.timestamp) as string | undefined);
}

function AuditRow({ entry, index }: { entry: AuditEntry; index: number }) {
  const [open, setOpen] = useState(false);
  const reasoning = String(entry.reasoning ?? entry.details ?? '');
  const hasReasoning = reasoning.trim().length > 0;

  return (
    <>
      <TableRow hover>
        <TableCell>{entry.id ?? index + 1}</TableCell>
        <TableCell>
          <Typography variant="subtitle2">{entryAgent(entry)}</Typography>
        </TableCell>
        <TableCell>{entry.customerId !== undefined && entry.customerId !== null ? `#${entry.customerId}` : '—'}</TableCell>
        <TableCell>
          {entry.action ?? entry.eventType ? (
            <StatusChip value={String(entry.action ?? entry.eventType)} />
          ) : (
            '—'
          )}
        </TableCell>
        <TableCell>
          {entry.confidence !== undefined && entry.confidence !== null
            ? `${Math.round(entry.confidence <= 1 ? entry.confidence * 100 : entry.confidence)}%`
            : '—'}
        </TableCell>
        <TableCell>{entryTime(entry)}</TableCell>
        <TableCell align="right">
          <IconButton
            size="small"
            disabled={!hasReasoning}
            onClick={() => setOpen((prev) => !prev)}
            aria-label={open ? 'Collapse reasoning' : 'Expand reasoning'}
          >
            {open ? <ExpandLessIcon /> : <ExpandMoreIcon />}
          </IconButton>
        </TableCell>
      </TableRow>
      <TableRow>
        <TableCell colSpan={7} sx={{ py: 0, borderBottom: open ? undefined : 'none' }}>
          <Collapse in={open} timeout="auto" unmountOnExit>
            <Box sx={{ py: 2 }}>
              <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
                Agent reasoning
              </Typography>
              <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                {reasoning}
              </Typography>
              {(entry.entityType || entry.entityId !== undefined) && (
                <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                  Entity: {String(entry.entityType ?? '')} {entry.entityId ?? ''}
                </Typography>
              )}
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    </>
  );
}

export default function ReasoningPanel() {
  const [audit, setAudit] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [agentFilter, setAgentFilter] = useState('ALL');
  const [customerSearch, setCustomerSearch] = useState('');

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setAudit(await decisionApi.audit());
    } catch (err) {
      setError(apiErrorMessage(err, 'Failed to load the audit trail.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

  const loadForCustomer = async () => {
    const custId = Number(customerSearch);
    if (!custId) {
      void loadAll();
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setAudit(await decisionApi.auditForCustomer(custId));
    } catch (err) {
      setError(apiErrorMessage(err, `Failed to load audit entries for customer #${custId}.`));
    } finally {
      setLoading(false);
    }
  };

  const agents = useMemo(
    () => Array.from(new Set(audit.map(entryAgent))).sort(),
    [audit],
  );

  const filtered = useMemo(
    () =>
      [...audit]
        .filter((entry) => agentFilter === 'ALL' || entryAgent(entry) === agentFilter)
        .sort((a, b) =>
          String(b.createdAt ?? b.timestamp ?? '').localeCompare(
            String(a.createdAt ?? a.timestamp ?? ''),
          ),
        ),
    [audit, agentFilter],
  );

  return (
    <Box>
      <PageHeader
        title="Reasoning & Audit Trail"
        subtitle="Every agent decision, with the reasoning behind it — fully auditable."
        action={
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => void loadAll()}>
            Refresh
          </Button>
        }
      />
      {error && <ErrorState message={error} />}

      <Card>
        <CardContent>
          <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }} flexWrap="wrap" useFlexGap>
            <TextField
              label="Agent"
              select
              size="small"
              value={agentFilter}
              onChange={(e) => setAgentFilter(e.target.value)}
              sx={{ minWidth: 220 }}
            >
              <MenuItem value="ALL">All agents</MenuItem>
              {agents.map((agent) => (
                <MenuItem key={agent} value={agent}>
                  {agent}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Customer ID"
              type="number"
              size="small"
              value={customerSearch}
              onChange={(e) => setCustomerSearch(e.target.value)}
            />
            <Button variant="outlined" onClick={() => void loadForCustomer()}>
              Load customer audit
            </Button>
            <Button
              onClick={() => {
                setCustomerSearch('');
                setAgentFilter('ALL');
                void loadAll();
              }}
            >
              Reset
            </Button>
            <Typography variant="body2" color="text.secondary">
              {filtered.length} entr{filtered.length === 1 ? 'y' : 'ies'}
            </Typography>
          </Stack>

          {loading ? (
            <LoadingState label="Loading audit trail…" />
          ) : filtered.length === 0 ? (
            <Typography color="text.secondary">
              No audit entries match the current filters.
            </Typography>
          ) : (
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>ID</TableCell>
                    <TableCell>Agent</TableCell>
                    <TableCell>Customer</TableCell>
                    <TableCell>Action</TableCell>
                    <TableCell>Confidence</TableCell>
                    <TableCell>Timestamp</TableCell>
                    <TableCell align="right">Reasoning</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filtered.map((entry, idx) => (
                    <AuditRow key={entry.id ?? idx} entry={entry} index={idx} />
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
