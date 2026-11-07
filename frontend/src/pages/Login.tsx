import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Container,
  Divider,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { AccountBalance as AccountBalanceIcon, LockOutlined as LockIcon } from '@mui/icons-material';
import { useAuth } from '../auth/AuthContext';
import { apiErrorMessage } from '../api/client';
import { NAVY } from '../theme';

interface LocationState {
  from?: string;
}

const DEMO_ACCOUNTS = [
  { username: 'admin', password: 'password123', role: 'ADMIN — full operations access' },
  { username: 'officer', password: 'password123', role: 'Officer — KYC / loans / review' },
  { username: 'analyst', password: 'password123', role: 'Analyst — fraud & audit' },
];

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('password123');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username.trim(), password);
      const from = (location.state as LocationState | null)?.from ?? '/';
      navigate(from, { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err, 'Login failed. Check your credentials and try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        background: `linear-gradient(135deg, ${NAVY} 0%, #071E33 55%, #0E7C7B 130%)`,
        py: 4,
      }}
    >
      <Container maxWidth="sm">
        <Card sx={{ borderRadius: 3 }}>
          <CardContent sx={{ p: { xs: 3, md: 4.5 } }}>
            <Stack spacing={1} alignItems="center" sx={{ mb: 3, textAlign: 'center' }}>
              <Box
                sx={{
                  width: 56,
                  height: 56,
                  borderRadius: '50%',
                  backgroundColor: NAVY,
                  color: '#fff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <AccountBalanceIcon fontSize="large" />
              </Box>
              <Typography variant="h5">AI Banking Operations Platform</Typography>
              <Typography color="text.secondary">
                KYC, fraud, lending, support and AI decision orchestration — one console.
              </Typography>
            </Stack>

            {error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {error}
              </Alert>
            )}

            <form onSubmit={submit}>
              <Stack spacing={2}>
                <TextField
                  label="Username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                  fullWidth
                  autoComplete="username"
                />
                <TextField
                  label="Password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  fullWidth
                  autoComplete="current-password"
                />
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={submitting}
                  startIcon={submitting ? <CircularProgress size={18} color="inherit" /> : <LockIcon />}
                >
                  {submitting ? 'Signing in…' : 'Sign in'}
                </Button>
              </Stack>
            </form>

            <Divider sx={{ my: 3 }}>Demo accounts</Divider>
            <Stack spacing={1}>
              {DEMO_ACCOUNTS.map((account) => (
                <Button
                  key={account.username}
                  variant="outlined"
                  color="inherit"
                  onClick={() => {
                    setUsername(account.username);
                    setPassword(account.password);
                    setError(null);
                  }}
                  sx={{ justifyContent: 'flex-start', textTransform: 'none' }}
                >
                  <Typography variant="body2">
                    <strong>{account.username}</strong> / {account.password}
                    <Typography component="span" variant="body2" color="text.secondary">
                      {'  '}— {account.role}
                    </Typography>
                  </Typography>
                </Button>
              ))}
            </Stack>
            <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 3, textAlign: 'center' }}>
              Author: Sharath Kumar Suram · Secured with JWT via Spring Cloud Gateway
            </Typography>
          </CardContent>
        </Card>
      </Container>
    </Box>
  );
}
