import { createTheme } from '@mui/material/styles';

export const NAVY = '#0B2C4A';
export const NAVY_DARK = '#071E33';
export const ACCENT = '#0E7C7B';
export const PAGE_BG = '#F4F6F9';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: NAVY, dark: NAVY_DARK, light: '#16456E', contrastText: '#FFFFFF' },
    secondary: { main: ACCENT, dark: '#0A5E5D', light: '#3A9B9A', contrastText: '#FFFFFF' },
    background: { default: PAGE_BG, paper: '#FFFFFF' },
    success: { main: '#1B7A43' },
    warning: { main: '#B7791F' },
    error: { main: '#B42318' },
    info: { main: '#175CD3' },
    text: { primary: '#12263A', secondary: '#5B6B7C' },
    divider: '#DFE6EE',
  },
  typography: {
    fontFamily: [
      'Inter',
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
    ].join(','),
    h4: { fontWeight: 700, letterSpacing: '-0.02em' },
    h5: { fontWeight: 700, letterSpacing: '-0.01em' },
    h6: { fontWeight: 600 },
    subtitle1: { fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 600 },
  },
  shape: { borderRadius: 10 },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          border: '1px solid #DFE6EE',
          boxShadow: '0 1px 2px rgba(11, 44, 74, 0.05), 0 4px 16px rgba(11, 44, 74, 0.04)',
        },
      },
    },
    MuiPaper: {
      styleOverrides: { root: { backgroundImage: 'none' } },
    },
    MuiButton: {
      styleOverrides: { root: { borderRadius: 8, boxShadow: 'none' } },
    },
    MuiChip: {
      styleOverrides: { root: { fontWeight: 600, fontSize: '0.75rem' } },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            backgroundColor: '#EDF2F7',
            color: '#0B2C4A',
            fontWeight: 700,
            fontSize: '0.78rem',
            textTransform: 'uppercase',
            letterSpacing: '0.04em',
          },
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: { backgroundColor: NAVY, color: '#DCE7F2', borderRight: 'none' },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: { backgroundColor: '#FFFFFF', color: '#12263A', boxShadow: '0 1px 0 #DFE6EE' },
      },
    },
  },
});

export default theme;
