import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './context/ThemeContext';
import { ToastProvider } from './context/ToastContext';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppShell from './components/layout/AppShell';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ReviewsPage from './pages/ReviewsPage';
import SubmitReviewPage from './pages/SubmitReviewPage';
import ReviewDetailsPage from './pages/ReviewDetailsPage';
import ReviewFindingsPage from './pages/ReviewFindingsPage';
import RepositoriesPage from './pages/RepositoriesPage';
import FindingsExplorerPlaceholderPage from './pages/FindingsExplorerPlaceholderPage';
import SettingsPage from './pages/SettingsPage';

function App() {
  return (
    <ThemeProvider>
      <ToastProvider>
        <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<LoginPage />} />

            {/* Protected Application Shell Routes */}
            <Route
              element={
                <ProtectedRoute>
                  <AppShell />
                </ProtectedRoute>
              }
            >
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/repositories" element={<RepositoriesPage />} />
              <Route path="/reviews" element={<ReviewsPage />} />
              <Route path="/reviews/new" element={<SubmitReviewPage />} />
              <Route path="/reviews/:id" element={<ReviewDetailsPage />} />
              <Route path="/reviews/:id/findings" element={<ReviewFindingsPage />} />
              <Route path="/findings" element={<FindingsExplorerPlaceholderPage />} />
              <Route path="/settings" element={<SettingsPage />} />
            </Route>

            {/* Default Redirects */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
        </AuthProvider>
      </ToastProvider>
    </ThemeProvider>
  );
}

export default App;
