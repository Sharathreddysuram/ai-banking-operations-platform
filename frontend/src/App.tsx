import { Navigate, Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import ExecutiveOverview from './pages/ExecutiveOverview';
import KycCenter from './pages/KycCenter';
import FraudCenter from './pages/FraudCenter';
import LoanCenter from './pages/LoanCenter';
import SupportCenter from './pages/SupportCenter';
import OrchestrationCenter from './pages/OrchestrationCenter';
import DecisionReview from './pages/DecisionReview';
import ReasoningPanel from './pages/ReasoningPanel';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<ExecutiveOverview />} />
        <Route path="/kyc" element={<KycCenter />} />
        <Route path="/fraud" element={<FraudCenter />} />
        <Route path="/loans" element={<LoanCenter />} />
        <Route path="/support" element={<SupportCenter />} />
        <Route path="/orchestration" element={<OrchestrationCenter />} />
        <Route path="/review" element={<DecisionReview />} />
        <Route path="/reasoning" element={<ReasoningPanel />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
