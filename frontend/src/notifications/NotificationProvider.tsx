import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { Alert, Snackbar, Stack } from '@mui/material';
import { Client } from '@stomp/stompjs';
import type { AppNotification } from '../api/types';
import { TOKEN_KEY } from '../api/client';
import { useAuth } from '../auth/AuthContext';

interface NotificationState {
  notifications: AppNotification[];
  unreadCount: number;
  connected: boolean;
  markAllRead: () => void;
  clearAll: () => void;
}

const NotificationContext = createContext<NotificationState | null>(null);

function brokerUrl(): string {
  if (import.meta.env.DEV) return 'ws://localhost:8080/ws';
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws`;
}

interface RawNotification {
  type?: string;
  title?: string;
  message?: string;
  timestamp?: string;
}

export function NotificationProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [connected, setConnected] = useState(false);
  const [toast, setToast] = useState<AppNotification | null>(null);
  const clientRef = useRef<Client | null>(null);

  const push = useCallback((raw: RawNotification) => {
    const item: AppNotification = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      type: raw.type ?? 'INFO',
      title: raw.title ?? 'Notification',
      message: raw.message ?? '',
      timestamp: raw.timestamp ?? new Date().toISOString(),
      read: false,
    };
    setNotifications((prev) => [item, ...prev].slice(0, 50));
    setToast(item);
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      setConnected(false);
      return;
    }
    const token = localStorage.getItem(TOKEN_KEY);
    const client = new Client({
      brokerURL: brokerUrl(),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/notifications', (frame) => {
          try {
            push(JSON.parse(frame.body) as RawNotification);
          } catch {
            push({ title: 'Notification', message: frame.body });
          }
        });
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    });
    clientRef.current = client;
    client.activate();
    return () => {
      void client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [isAuthenticated, push]);

  const markAllRead = useCallback(() => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  }, []);

  const clearAll = useCallback(() => setNotifications([]), []);

  const value = useMemo<NotificationState>(
    () => ({
      notifications,
      unreadCount: notifications.filter((n) => !n.read).length,
      connected,
      markAllRead,
      clearAll,
    }),
    [notifications, connected, markAllRead, clearAll],
  );

  const toastSeverity =
    toast && /CRITICAL|HIGH|ALERT|ESCALAT/i.test(toast.type)
      ? 'error'
      : toast && /WARN/i.test(toast.type)
        ? 'warning'
        : 'info';

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <Stack>
        <Snackbar
          open={Boolean(toast)}
          autoHideDuration={6000}
          onClose={() => setToast(null)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        >
          <Alert
            severity={toastSeverity}
            variant="filled"
            onClose={() => setToast(null)}
            sx={{ width: '100%', maxWidth: 420 }}
          >
            <strong>{toast?.title}</strong>
            {toast?.message ? ` — ${toast.message}` : ''}
          </Alert>
        </Snackbar>
      </Stack>
    </NotificationContext.Provider>
  );
}

export function useNotifications(): NotificationState {
  const ctx = useContext(NotificationContext);
  if (!ctx) throw new Error('useNotifications must be used within NotificationProvider');
  return ctx;
}
