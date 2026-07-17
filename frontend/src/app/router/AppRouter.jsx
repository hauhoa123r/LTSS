import { Navigate, Route, Routes, useParams } from 'react-router-dom'
import AppMainLayout from '../layouts/AppMainLayout.jsx'
import SystemStatusPage from '../../features/system/pages/SystemStatusPage.jsx'
import HomePage from '../../features/home/pages/HomePage.jsx'
import NotFoundPage from '../pages/NotFoundPage.jsx'
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
import RelicArticleManagementPage from '../../features/content/pages/RelicArticleManagementPage.jsx'
import RelicArticleEditorPage from '../../features/content/pages/RelicArticleEditorPage.jsx'
import BusinessOwnerProfilePage from '../../features/content/pages/BusinessOwnerProfilePage.jsx'
import BusinessOwnerContentPage from '../../features/content/pages/BusinessOwnerContentPage.jsx'
import ModerationQueuePage from '../../features/moderation/pages/ModerationQueuePage.jsx'
import ModerationDetailPage from '../../features/moderation/pages/ModerationDetailPage.jsx'
import NotificationsPage from '../../features/moderation/pages/NotificationsPage.jsx'
import ArticleCategoryManagementPage from '../../features/moderation/pages/ArticleCategoryManagementPage.jsx'
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
import AdminUserDetailPage from '../../features/administration/pages/AdminUserDetailPage.jsx'
import AuditLogDetailPage from '../../features/administration/pages/AuditLogDetailPage.jsx'

function LegacyQuizEditRedirect() {
  const { id } = useParams()
  return <Navigate to={`/relic-manager/quizzes/${id}/edit`} replace />
}

function AppRouter() {
  return (
    <Routes>
      <Route element={<AppMainLayout />}>
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
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="my-tours" element={<MyToursPage />} />
          <Route path="my-tours/new" element={<TourBuilderPage />} />
          <Route path="my-tours/:id/edit" element={<TourBuilderPage />} />
          <Route path="quiz-attempts/:attemptId" element={<QuizAttemptPage />} />
          <Route path="quiz-progress" element={<QuizProgressPage />} />
        </Route>
        <Route element={<ProtectedRoute allowedRoles={['BUSINESS_OWNER']} />}>
          <Route path="business-owner" element={<Navigate to="/business-owner/overview" replace />} />
          <Route path="business-owner/overview" element={<AnalyticsDashboardPage mode="business" />} />
          <Route path="business-owner/profile" element={<BusinessOwnerProfilePage />} />
          <Route path="business-owner/posts" element={<BusinessOwnerContentPage key="posts" mode="posts" />} />
          <Route path="business-owner/promotions" element={<BusinessOwnerContentPage key="promotions" mode="promotions" />} />
          <Route path="business-analytics" element={<Navigate to="/business-owner/overview" replace />} />
        </Route>
        <Route element={<ProtectedRoute allowedRoles={['RELIC_MANAGER']} />}>
          <Route path="relic-manager" element={<Navigate to="/relic-manager/articles" replace />} />
          <Route path="relic-manager/articles" element={<RelicArticleManagementPage />} />
          <Route path="relic-manager/articles/new" element={<RelicArticleEditorPage />} />
          <Route path="relic-manager/articles/:id/edit" element={<RelicArticleEditorPage />} />
          <Route path="relic-manager/quizzes" element={<QuizManagementPage />} />
          <Route path="relic-manager/quizzes/new" element={<QuizEditorPage />} />
          <Route path="relic-manager/quizzes/:id/edit" element={<QuizEditorPage />} />
          <Route path="relic-manager/notifications" element={<NotificationsPage workspace="relic-manager" />} />
          <Route path="manage/quizzes" element={<Navigate to="/relic-manager/quizzes" replace />} />
          <Route path="manage/quizzes/new" element={<Navigate to="/relic-manager/quizzes/new" replace />} />
          <Route path="manage/quizzes/:id/edit" element={<LegacyQuizEditRedirect />} />
        </Route>
        <Route element={<ProtectedRoute allowedRoles={['MODERATOR', 'ADMINISTRATOR']} />}>
          <Route path="moderation" element={<Navigate to="/moderation/articles" replace />} />
          <Route path="moderation/articles" element={<ModerationQueuePage key="ARTICLE" targetType="ARTICLE" />} />
          <Route path="moderation/quizzes" element={<ModerationQueuePage key="QUIZ" targetType="QUIZ" />} />
          <Route path="moderation/article-categories" element={<ArticleCategoryManagementPage />} />
          <Route path="moderation/notifications" element={<NotificationsPage workspace="moderation" />} />
          <Route path="moderation/articles/:caseId" element={<ModerationDetailPage />} />
          <Route path="moderation/quizzes/:caseId" element={<ModerationDetailPage />} />
          <Route path="moderation/events/*" element={<Navigate to="/moderation/articles" replace />} />
          <Route path="moderation/business-posts/*" element={<Navigate to="/moderation/articles" replace />} />
          <Route path="moderation/promotions/*" element={<Navigate to="/moderation/articles" replace />} />
          <Route path="moderation/reviews/*" element={<Navigate to="/moderation/articles" replace />} />
          <Route path="moderation/:caseId" element={<ModerationDetailPage />} />
        </Route>
        <Route element={<ProtectedRoute allowedRoles={['ADMINISTRATOR']} />}>
          <Route path="admin/dashboard" element={<AnalyticsDashboardPage mode="admin" />} />
          <Route path="admin/audit-logs" element={<AuditLogsPage />} />
          <Route path="admin/audit-logs/:id" element={<AuditLogDetailPage />} />
          <Route path="admin/users" element={<AdminUsersPage />} />
          <Route path="admin/users/:id" element={<AdminUserDetailPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}

export default AppRouter
