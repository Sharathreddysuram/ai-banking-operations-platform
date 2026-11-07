import { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Chip,
  Grid,
  List,
  ListItem,
  ListItemText,
  Stack,
  Typography,
} from '@mui/material';
import {
  Groups as CustomersIcon,
  HowToReg as VerifiedIcon,
  MonetizationOn as LoanIcon,
  PendingActions as ReviewIcon,
  Security as FraudIcon,
  Speed as RiskIcon,
} from '@mui/icons-material';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip as ChartTooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { decisionApi, fraudApi, kycApi, loanApi } from '../api/services';
import type { AuditEntry, Customer, DecisionCase, FraudAssessment, LoanApplication } from '../api/types';
import { apiErrorMessage } from '../api/client';
import { ErrorState, formatDateTime, LoadingState, PageHeader } from '../components/common';

const PIE_COLORS = ['#0B2C4A', '#0E7C7B', '#B7791F', '#B42318', '#5B6B7C'];
const SEVERITY_COLORS: Record<string, string> = {
  LOW: '#1B7A43',
  MEDIUM: '#B7791F',
  HIGH: '#DC6803',
  CRITICAL: '#B42318',
};

function KpiCard({
  label,
  value,
  sub,
  icon,
}: {
  label: string;
  value: string;
  sub?: string;
  icon: React.ReactNode;
}) {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
          <Box>
            <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600 }}>
              {label}
            </Typography>
            <Typography variant="h4" sx={{ mt: 0.5 }}>
              {value}
            </Typography>
            {sub && (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                {sub}
              </Typography>
            )}
          </Box>
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 2,
              backgroundColor: '#EDF2F7',
              color: '#0B2C4A',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {icon}
          </Box>
        </Stack>
      </CardContent>
    </Card>
  );
}

export default function ExecutiveOverview() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [alerts, setAlerts] = useState<FraudAssessment[]>([]);
  const [assessments, setAssessments] = useState<FraudAssessment[]>([]);
  const [applications, setApplications] = useState<LoanApplication[]>([]);
  const [cases, setCases] = useState<DecisionCase[]>([]);
  const [audit, setAudit] = useState<AuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    (async () => {
      setLoading(true);
      setError(null);
      const results = await Promise.allSettled([
        kycApi.listCustomers(),
        fraudApi.listAlerts(),
        fraudApi.listAssessments(),
        loanApi.listApplications(),
        decisionApi.listCases(),
        decisionApi.audit(),
      ]);
      if (!active) return;
      const [c, a, fa, l, dc, au] = results;
      if (c.status === 'fulfilled') setCustomers(c.value);
      if (a.status === 'fulfilled') setAlerts(a.value);
      if (fa.status === 'fulfilled') setAssessments(fa.value);
      if (l.status === 'fulfilled') setApplications(l.value);
      if (dc.status === 'fulfilled') setCases(dc.value);
      if (au.status === 'fulfilled') setAudit(au.value);
      const firstFailure = results.find((r) => r.status === 'rejected');
      if (firstFailure && firstFailure.status === 'rejected') {
        setError(apiErrorMessage(firstFailure.reason, 'Some overview data could not be loaded.'));
      }
      setLoading(false);
    })();
    return () => {
      active = false;
    };
  }, []);

  const stats = useMemo(() => {
    const verified = customers.filter((c) => c.status === 'VERIFIED').length;
    const verifiedPct = customers.length ? Math.round((verified / customers.length) * 100) : 0;
    const loansApproved = applications.filter((a) =>
      /APPROV/i.test(a.status),
    ).length;
    const pendingReviews = cases.filter((c) => c.status === 'PENDING_REVIEW').length;
    const riskPool = [
      ...cases.map((c) => c.overallRiskScore ?? 0),
      ...assessments.map((a) => a.riskScore ?? 0),
    ].filter((n) => n > 0);
    const avgRisk = riskPool.length
      ? riskPool.reduce((sum, n) => sum + n, 0) / riskPool.length
      : 0;
    return { verified, verifiedPct, loansApproved, pendingReviews, avgRisk };
  }, [customers, applications, cases, assessments]);

  const casesByStatus = useMemo(() => {
    const counts = new Map<string, number>();
    cases.forEach((c) => counts.set(c.status, (counts.get(c.status) ?? 0) + 1));
    return Array.from(counts.entries()).map(([name, value]) => ({
      name: name.replace(/_/g, ' '),
      value,
    }));
  }, [cases]);

  const fraudBySeverity = useMemo(() => {
    const order = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
    const counts = new Map<string, number>();
    [...assessments, ...alerts].forEach((a) => {
      const key = (a.severity ?? 'LOW').toUpperCase();
      counts.set(key, (counts.get(key) ?? 0) + 1);
    });
    return order
      .filter((key) => counts.has(key))
      .map((key) => ({ name: key, count: counts.get(key) ?? 0, fill: SEVERITY_COLORS[key] }));
  }, [assessments, alerts]);

  const recentAudit = useMemo(
    () =>
      [...audit].sort((a, b) =>
        String(b.createdAt ?? b.timestamp ?? '').localeCompare(String(a.createdAt ?? a.timestamp ?? '')),
      ).slice(0, 8),
    [audit],
  );

  if (loading) return <LoadingState label="Loading executive overview…" />;

  return (
    <Box>
      <PageHeader
        title="Executive Overview"
        subtitle="Live posture across KYC, fraud, lending and AI decision orchestration."
      />
      {error && <ErrorState message={error} />}

      <Grid container spacing={2.5}>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <KpiCard label="Total customers" value={String(customers.length)} icon={<CustomersIcon />} />
        </Grid>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <KpiCard
            label="Verified KYC"
            value={`${stats.verifiedPct}%`}
            sub={`${stats.verified} of ${customers.length} verified`}
            icon={<VerifiedIcon />}
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <KpiCard
            label="Fraud alerts"
            value={String(alerts.length)}
            sub="HIGH / CRITICAL assessments"
            icon={<FraudIcon />}
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <KpiCard
            label="Loans approved"
            value={String(stats.loansApproved)}
            sub={`${applications.length} applications total`}
            icon={<LoanIcon />}
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <KpiCard
            label="Pending reviews"
            value={String(stats.pendingReviews)}
            sub="Decision cases awaiting a human"
            icon={<ReviewIcon />}
          />
        </Grid>
        <Grid item xs={12} sm={6} lg={4} xl={2}>
          <KpiCard
            label="Average risk"
            value={stats.avgRisk ? stats.avgRisk.toFixed(1) : '—'}
            sub="Across cases & fraud assessments"
            icon={<RiskIcon />}
          />
        </Grid>
      </Grid>

      <Grid container spacing={2.5} sx={{ mt: 0.5 }}>
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Decision cases by status
              </Typography>
              {casesByStatus.length === 0 ? (
                <Typography color="text.secondary">No decision cases yet.</Typography>
              ) : (
                <ResponsiveContainer width="100%" height={280}>
                  <PieChart>
                    <Pie
                      data={casesByStatus}
                      dataKey="value"
                      nameKey="name"
                      innerRadius={55}
                      outerRadius={95}
                      paddingAngle={3}
                    >
                      {casesByStatus.map((entry, idx) => (
                        <Cell key={entry.name} fill={PIE_COLORS[idx % PIE_COLORS.length]} />
                      ))}
                    </Pie>
                    <ChartTooltip />
                    <Legend />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={12} md={6}>
          <Card sx={{ height: '100%' }}>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>
                Fraud assessments by severity
              </Typography>
              {fraudBySeverity.length === 0 ? (
                <Typography color="text.secondary">No fraud assessments yet.</Typography>
              ) : (
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={fraudBySeverity}>
                    <CartesianGrid strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="name" />
                    <YAxis allowDecimals={false} />
                    <ChartTooltip />
                    <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                      {fraudBySeverity.map((entry) => (
                        <Cell key={entry.name} fill={entry.fill} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card sx={{ mt: 2.5 }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 1 }}>
            Recent agent activity
          </Typography>
          {recentAudit.length === 0 ? (
            <Typography color="text.secondary">
              No audit activity yet. Agent actions will appear here.
            </Typography>
          ) : (
            <List disablePadding>
              {recentAudit.map((entry, idx) => (
                <ListItem key={entry.id ?? idx} divider={idx < recentAudit.length - 1} sx={{ px: 0 }}>
                  <ListItemText
                    primary={
                      <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                        <Typography variant="subtitle2">
                          {entry.agentName ?? entry.agent ?? 'Agent'}
                        </Typography>
                        {(entry.action ?? entry.eventType) && (
                          <Chip label={entry.action ?? entry.eventType} size="small" variant="outlined" />
                        )}
                        {entry.customerId !== undefined && (
                          <Chip label={`Customer #${entry.customerId}`} size="small" variant="outlined" />
                        )}
                      </Stack>
                    }
                    secondary={
                      <>
                        {entry.reasoning ?? entry.details ?? ''}
                        {'  ·  '}
                        {formatDateTime((entry.createdAt ?? entry.timestamp) as string | undefined)}
                      </>
                    }
                    secondaryTypographyProps={{ sx: { whiteSpace: 'pre-wrap' } }}
                  />
                </ListItem>
              ))}
            </List>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
