import { useState, type MouseEvent, type ReactNode } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Avatar,
  Badge,
  Box,
  Button,
  Chip,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Popover,
  Stack,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import {
  AccountBalance as AccountBalanceIcon,
  AssignmentTurnedIn as ReviewIcon,
  Badge as KycIcon,
  Dashboard as DashboardIcon,
  FactCheck as ReasoningIcon,
  Gavel as DecisionIcon,
  Logout as LogoutIcon,
  MonetizationOn as LoanIcon,
  Notifications as NotificationsIcon,
  Security as FraudIcon,
  SupportAgent as SupportIcon,
} from '@mui/icons-material';
import { useAuth } from '../auth/AuthContext';
import { useNotifications } from '../notifications/NotificationProvider';
import { formatDateTime } from './common';
import { NAVY_DARK } from '../theme';

export const DRAWER_WIDTH = 248;

const NAV_ITEMS = [
  { to: '/', label: 'Executive Overview', icon: <DashboardIcon /> },
  { to: '/kyc', label: 'KYC Center', icon: <KycIcon /> },
  { to: '/fraud', label: 'Fraud Center', icon: <FraudIcon /> },
  { to: '/loans', label: 'Loan Center', icon: <LoanIcon /> },
  { to: '/support', label: 'Support Center', icon: <SupportIcon /> },
  { to: '/orchestration', label: 'Orchestration', icon: <DecisionIcon /> },
  { to: '/review', label: 'Decision Review', icon: <ReviewIcon /> },
  { to: '/reasoning', label: 'Reasoning & Audit', icon: <ReasoningIcon /> },
];

function NotificationBell() {
  const { notifications, unreadCount, connected, markAllRead, clearAll } = useNotifications();
  const [anchor, setAnchor] = useState<HTMLElement | null>(null);

  const open = (event: MouseEvent<HTMLElement>) => {
    setAnchor(event.currentTarget);
    markAllRead();
  };

  return (
    <>
      <Tooltip title={connected ? 'Live notifications connected' : 'Notifications (reconnecting…)'}>
        <IconButton color="inherit" onClick={open} aria-label="Notifications">
          <Badge badgeContent={unreadCount} color="error">
            <NotificationsIcon />
          </Badge>
        </IconButton>
      </Tooltip>
      <Popover
        open={Boolean(anchor)}
        anchorEl={anchor}
        onClose={() => setAnchor(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'right' }}
        PaperProps={{ sx: { width: 380, maxHeight: 460, p: 0 } }}
      >
        <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="subtitle1">Notifications</Typography>
          <Stack direction="row" spacing={1} alignItems="center">
            <Chip
              label={connected ? 'Live' : 'Offline'}
              size="small"
              color={connected ? 'success' : 'default'}
              variant="outlined"
            />
            {notifications.length > 0 && (
              <Button size="small" onClick={clearAll}>
                Clear
              </Button>
            )}
          </Stack>
        </Box>
        <Divider />
        {notifications.length === 0 ? (
          <Typography color="text.secondary" sx={{ p: 2 }}>
            No notifications yet. Agent events arrive here in real time over WebSocket.
          </Typography>
        ) : (
          <List sx={{ overflowY: 'auto', maxHeight: 360 }}>
            {notifications.map((n) => (
              <Box key={n.id} sx={{ px: 2, py: 1.25, borderBottom: '1px solid #EEF2F6' }}>
                <Stack direction="row" spacing={1} alignItems="center">
                  <Typography variant="subtitle2">{n.title}</Typography>
                  <Chip label={n.type} size="small" variant="outlined" />
                </Stack>
                {n.message && <Typography variant="body2">{n.message}</Typography>}
                <Typography variant="caption" color="text.secondary">
                  {formatDateTime(n.timestamp)}
                </Typography>
              </Box>
            ))}
          </List>
        )}
      </Popover>
    </>
  );
}

export default function Layout({ children }: { children?: ReactNode }) {
  const { username, role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box' },
        }}
      >
        <Box sx={{ p: 2.5, backgroundColor: NAVY_DARK }}>
          <Stack direction="row" spacing={1.5} alignItems="center">
            <Avatar sx={{ bgcolor: '#0E7C7B', width: 40, height: 40 }}>
              <AccountBalanceIcon />
            </Avatar>
            <Box>
              <Typography variant="subtitle1" sx={{ color: '#FFFFFF', lineHeight: 1.2 }}>
                AI Banking Ops
              </Typography>
              <Typography variant="caption" sx={{ color: '#9FB6CC' }}>
                Operations Platform
              </Typography>
            </Box>
          </Stack>
        </Box>
        <List sx={{ px: 1.5, py: 2 }}>
          {NAV_ITEMS.map((item) => (
            <ListItemButton
              key={item.to}
              component={NavLink}
              to={item.to}
              end={item.to === '/'}
              sx={{
                borderRadius: 2,
                mb: 0.5,
                color: '#C7D6E6',
                '& .MuiListItemIcon-root': { color: '#8FA9C4', minWidth: 38 },
                '&.active': {
                  backgroundColor: 'rgba(255,255,255,0.12)',
                  color: '#FFFFFF',
                  '& .MuiListItemIcon-root': { color: '#FFFFFF' },
                },
                '&:hover': { backgroundColor: 'rgba(255,255,255,0.07)' },
              }}
            >
              <ListItemIcon>{item.icon}</ListItemIcon>
              <ListItemText
                primary={item.label}
                primaryTypographyProps={{ fontSize: '0.9rem', fontWeight: 600 }}
              />
            </ListItemButton>
          ))}
        </List>
        <Box sx={{ mt: 'auto', p: 2.5 }}>
          <Typography variant="caption" sx={{ color: '#7E97B0', display: 'block' }}>
            Built by
          </Typography>
          <Typography variant="body2" sx={{ color: '#DCE7F2', fontWeight: 600 }}>
            Sharath Kumar Suram
          </Typography>
          <Typography variant="caption" sx={{ color: '#7E97B0' }}>
            Spring AI · Multi-Agent Orchestration
          </Typography>
        </Box>
      </Drawer>

      <Box sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <AppBar position="sticky" elevation={0}>
          <Toolbar sx={{ gap: 2 }}>
            <Typography variant="h6" sx={{ flexGrow: 1, color: '#0B2C4A' }}>
              AI Banking Operations Platform
            </Typography>
            <NotificationBell />
            <Divider orientation="vertical" flexItem sx={{ my: 1.5 }} />
            <Stack direction="row" spacing={1.5} alignItems="center">
              <Avatar sx={{ bgcolor: '#0B2C4A', width: 34, height: 34, fontSize: 14 }}>
                {(username ?? 'U').slice(0, 1).toUpperCase()}
              </Avatar>
              <Box sx={{ lineHeight: 1.1 }}>
                <Typography variant="body2" sx={{ fontWeight: 700 }}>
                  {username ?? 'User'}
                </Typography>
                <Chip
                  label={(role ?? 'USER').replace(/^ROLE_/, '')}
                  size="small"
                  color="secondary"
                  variant="outlined"
                  sx={{ height: 18, fontSize: '0.65rem' }}
                />
              </Box>
              <Tooltip title="Sign out">
                <IconButton color="inherit" onClick={handleLogout} aria-label="Sign out">
                  <LogoutIcon />
                </IconButton>
              </Tooltip>
            </Stack>
          </Toolbar>
        </AppBar>
        <Box component="main" sx={{ flexGrow: 1, p: { xs: 2, md: 3.5 }, maxWidth: 1440, width: '100%', mx: 'auto' }}>
          {children ?? <Outlet />}
        </Box>
      </Box>
    </Box>
  );
}
