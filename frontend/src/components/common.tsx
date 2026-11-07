import type { ReactNode } from 'react';
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material';
import type { ChipProps } from '@mui/material';
import type { AgentResponse } from '../api/types';

export function PageHeader({
  title,
  subtitle,
  action,
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
}) {
  return (
    <Box
      sx={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        flexWrap: 'wrap',
        gap: 2,
        mb: 3,
      }}
    >
      <Box>
        <Typography variant="h4">{title}</Typography>
        {subtitle && (
          <Typography color="text.secondary" sx={{ mt: 0.5 }}>
            {subtitle}
          </Typography>
        )}
      </Box>
      {action}
    </Box>
  );
}

export function LoadingState({ label = 'Loading…' }: { label?: string }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 6, justifyContent: 'center' }}>
      <CircularProgress size={28} />
      <Typography color="text.secondary">{label}</Typography>
    </Box>
  );
}

export function ErrorState({ message }: { message: string }) {
  return (
    <Alert severity="error" sx={{ my: 2 }}>
      {message}
    </Alert>
  );
}

type ChipColor = ChipProps['color'];

const STATUS_COLORS: Record<string, ChipColor> = {
  // KYC
  VERIFIED: 'success',
  PENDING: 'warning',
  PENDING_REVIEW: 'warning',
  REVIEW_REQUIRED: 'warning',
  REJECTED: 'error',
  // Fraud
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'error',
  CRITICAL: 'error',
  APPROVE: 'success',
  APPROVED: 'success',
  REVIEW: 'warning',
  BLOCK: 'error',
  // Support
  ANSWERED: 'success',
  ESCALATED: 'error',
  CLOSED: 'default',
  // Generic
  ACTIVE: 'success',
  ONLINE: 'success',
  UP: 'success',
  HEALTHY: 'success',
  COMPLETED: 'success',
  SUBMITTED: 'info',
  IN_REVIEW: 'warning',
  UNDER_REVIEW: 'warning',
  OPEN: 'info',
  FLAGGED: 'error',
  DECLINED: 'error',
};

export function StatusChip({ value }: { value: string | null | undefined }) {
  if (!value) return <Typography color="text.secondary">—</Typography>;
  const key = value.toUpperCase();
  const color: ChipColor = STATUS_COLORS[key] ?? 'default';
  return (
    <Chip
      label={value.replace(/_/g, ' ')}
      color={color}
      size="small"
      variant={key === 'CRITICAL' || key === 'BLOCK' ? 'filled' : 'outlined'}
    />
  );
}

/** Colour for a 0-100 risk score. */
export function riskColor(score: number): string {
  if (score >= 75) return '#B42318';
  if (score >= 50) return '#DC6803';
  if (score >= 25) return '#B7791F';
  return '#1B7A43';
}

export function RiskGauge({ score, label }: { score: number; label?: string }) {
  const pct = Math.max(0, Math.min(100, score));
  return (
    <Box sx={{ minWidth: 140 }}>
      <Stack direction="row" justifyContent="space-between" alignItems="baseline">
        <Typography variant="body2" color="text.secondary">
          {label ?? 'Risk score'}
        </Typography>
        <Typography variant="subtitle2" sx={{ color: riskColor(pct), fontWeight: 700 }}>
          {pct.toFixed(0)} / 100
        </Typography>
      </Stack>
      <LinearProgress
        variant="determinate"
        value={pct}
        sx={{
          height: 10,
          borderRadius: 5,
          mt: 0.5,
          backgroundColor: '#E6ECF2',
          '& .MuiLinearProgress-bar': { backgroundColor: riskColor(pct), borderRadius: 5 },
        }}
      />
    </Box>
  );
}

/** Panel showing an AI agent's reasoning + confidence. */
export function AgentReasoningPanel<T>({
  response,
  title,
}: {
  response: AgentResponse<T> | null;
  title?: string;
}) {
  if (!response) return null;
  const confidencePct = Math.round((response.confidence ?? 0) * (response.confidence <= 1 ? 100 : 1));
  return (
    <Alert severity="info" icon={false} sx={{ mt: 2, '& .MuiAlert-message': { width: '100%' } }}>
      <Stack spacing={1}>
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
          <Typography variant="subtitle2">{title ?? 'Agent reasoning'}</Typography>
          <Chip label={response.agentName} size="small" color="primary" />
          {response.confidence !== undefined && response.confidence !== null && (
            <Chip label={`Confidence ${confidencePct}%`} size="small" variant="outlined" />
          )}
        </Stack>
        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
          {response.reasoning || 'No reasoning returned by the agent.'}
        </Typography>
      </Stack>
    </Alert>
  );
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return d.toLocaleString();
}

export function formatMoney(value: number | null | undefined, currency = 'USD'): string {
  if (value === null || value === undefined || Number.isNaN(value)) return '—';
  try {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(value);
  } catch {
    return `${currency} ${value.toLocaleString()}`;
  }
}

/** Split a `;`- / newline / comma separated agent string into list items. */
export function splitFactors(value: string | null | undefined): string[] {
  if (!value) return [];
  return value
    .split(/;|\n|\|/)
    .map((s) => s.trim())
    .filter(Boolean);
}
