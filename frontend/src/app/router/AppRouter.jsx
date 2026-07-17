import { Route, Routes } from 'react-router-dom'
import MainLayout from '../../components/layout/MainLayout.jsx'
import SystemStatusPage from '../../features/system/pages/SystemStatusPage.jsx'
import HomePage from '../../pages/HomePage.jsx'
import NotFoundPage from '../../pages/NotFoundPage.jsx'
import LoginPage from '../../features/auth/pages/LoginPage.jsx'
import RegisterPage from '../../features/auth/pages/RegisterPage.jsx'
import VerifyEmailPage from '../../features/auth/pages/VerifyEmailPage.jsx'
import ForgotPasswordPage from '../../features/auth/pages/ForgotPasswordPage.jsx'
import ResetPasswordPage from '../../features/auth/pages/ResetPasswordPage.jsx'
import ProfilePage from '../../features/auth/pages/ProfilePage.jsx'
import ProtectedRoute from '../../features/auth/components/ProtectedRoute.jsx'
import DiscoveryPage from '../../features/places/pages/DiscoveryPage.jsx'
import PlaceDetailPage from '../../features/places/pages/PlaceDetailPage.jsx'
import FavoritesPage from '../../features/places/pages/FavoritesPage.jsx'
import PublicContentListPage from '../../features/content/pages/PublicContentListPage.jsx'
import BusinessDetailPage from '../../features/content/pages/BusinessDetailPage.jsx'
import ContentDetailPage from '../../features/content/pages/ContentDetailPage.jsx'
import ModerationQueuePage from '../../features/moderation/pages/ModerationQueuePage.jsx'
import NotificationsPage from '../../features/moderation/pages/NotificationsPage.jsx'
import PublicToursPage from '../../features/tours/pages/PublicToursPage.jsx'
import TourDetailPage from '../../features/tours/pages/TourDetailPage.jsx'
import MyToursPage from '../../features/tours/pages/MyToursPage.jsx'
import TourBuilderPage from '../../features/tours/pages/TourBuilderPage.jsx'
import QuizCatalogPage from '../../features/quiz/pages/QuizCatalogPage.jsx'
import QuizAttemptPage from '../../features/quiz/pages/QuizAttemptPage.jsx'
import QuizProgressPage from '../../features/quiz/pages/QuizProgressPage.jsx'
import QuizManagementPage from '../../features/quiz/pages/QuizManagementPage.jsx'
import QuizEditorPage from '../../features/quiz/pages/QuizEditorPage.jsx'
import AnalyticsDashboardPage from '../../features/analytics/pages/AnalyticsDashboardPage.jsx'
import AuditLogsPage from '../../features/administration/pages/AuditLogsPage.jsx'
import AdminUsersPage from '../../features/administration/pages/AdminUsersPage.jsx'

function AppRouter() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path="system-status" element={<SystemStatusPage />} />
        <Route path="places" element={<DiscoveryPage />} />
        <Route path="places/:slug" element={<PlaceDetailPage />} />
        <Route path="businesses" element={<PublicContentListPage mode="businesses" />} />
        <Route path="businesses/:id" element={<BusinessDetailPage />} />
        <Route path="business-posts/:slug" element={<ContentDetailPage type="post" />} />
        <Route path="promotions/:id" element={<ContentDetailPage type="promotion" />} />
        <Route path="articles" element={<PublicContentListPage mode="articles" />} />
        <Route path="articles/:slug" element={<ContentDetailPage type="article" />} />
        <Route path="events" element={<PublicContentListPage mode="events" />} />
        <Route path="events/:slug" element={<ContentDetailPage type="event" />} />
        <Route path="tours" element={<PublicToursPage />} />
        <Route path="tours/:id" element={<TourDetailPage />} />
        <Route path="quizzes" element={<QuizCatalogPage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="register" element={<RegisterPage />} />
        <Route path="verify-email" element={<VerifyEmailPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="profile" element={<ProfilePage />} />
          <Route path="favorites" element={<FavoritesPage />} />
          <Route path="moderation" element={<ModerationQueuePage />} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="my-tours" element={<MyToursPage />} />
          <Route path="my-tours/new" element={<TourBuilderPage />} />
          <Route path="my-tours/:id/edit" element={<TourBuilderPage />} />
          <Route path="quiz-attempts/:attemptId" element={<QuizAttemptPage />} />
          <Route path="quiz-progress" element={<QuizProgressPage />} />
          <Route path="manage/quizzes" element={<QuizManagementPage />} />
          <Route path="manage/quizzes/new" element={<QuizEditorPage />} />
          <Route path="manage/quizzes/:id/edit" element={<QuizEditorPage />} />
          <Route path="business-analytics" element={<AnalyticsDashboardPage mode="business" />} />
          <Route path="admin/dashboard" element={<AnalyticsDashboardPage mode="admin" />} />
          <Route path="admin/audit-logs" element={<AuditLogsPage />} />
          <Route path="admin/users" element={<AdminUsersPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default AppRouter
