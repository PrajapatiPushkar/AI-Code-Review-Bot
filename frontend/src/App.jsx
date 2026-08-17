import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navbar from './components/Navbar';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ReviewsPage from './pages/ReviewsPage';
import SubmitReviewPage from './pages/SubmitReviewPage';
import ReviewDetailsPage from './pages/ReviewDetailsPage';
import ReviewFindingsPage from './pages/ReviewFindingsPage';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="app-container">
          <Navbar />
          <main className="main-content">
            <Routes>
              {/* Public Routes */}
              <Route path="/login" element={<LoginPage />} />

              {/* Protected Routes */}
              <Route
                path="/dashboard"
                element={
                  <ProtectedRoute>
                    <DashboardPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/reviews"
                element={
                  <ProtectedRoute>
                    <ReviewsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/reviews/new"
                element={
                  <ProtectedRoute>
                    <SubmitReviewPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/reviews/:id"
                element={
                  <ProtectedRoute>
                    <ReviewDetailsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/reviews/:id/findings"
                element={
                  <ProtectedRoute>
                    <ReviewFindingsPage />
                  </ProtectedRoute>
                }
              />

              {/* Default Redirects */}
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
