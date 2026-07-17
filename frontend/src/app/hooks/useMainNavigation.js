import { useLocation } from 'react-router-dom'
import { roleLabel } from '../../features/administration/roleLabels.js'
import { useAuth } from '../../features/auth/context/AuthContext.jsx'
import { BUSINESS_OWNER_HOME, BUSINESS_OWNER_WORKSPACE_LINKS } from '../../features/content/businessOwnerConfig.js'
import { MODERATION_WORKSPACE_LINKS } from '../../features/moderation/moderationConfig.js'
import { RELIC_MANAGER_HOME, RELIC_MANAGER_WORKSPACE_LINKS } from '../../features/quiz/relicManagerConfig.js'

const PUBLIC_LINKS = [
  { to: '/', label: 'Trang chủ', end: true },
  { to: '/places', label: 'Khám phá' },
  { to: '/businesses', label: 'Doanh nghiệp' },
  { to: '/articles', label: 'Bài viết' },
  { to: '/events', label: 'Sự kiện' },
  { to: '/tours', label: 'Lịch trình' },
  { to: '/quizzes', label: 'Quiz' },
]

export function useMainNavigation() {
  const { user, isReady, logout } = useAuth()
  const { pathname } = useLocation()
  const roles = user?.roles ?? []
  const isAdministrator = roles.includes('ADMINISTRATOR')
  const isModerator = roles.includes('MODERATOR')
  const isRelicManager = roles.includes('RELIC_MANAGER')
  const isBusinessOwner = roles.includes('BUSINESS_OWNER')
  const isStaff = isAdministrator || isModerator || isRelicManager || isBusinessOwner
  const isModerationPath = /^\/moderation(?:\/|$)/.test(pathname)
  const isRelicManagerPath = /^(\/relic-manager(?:\/|$)|\/manage\/quizzes(?:\/|$))/.test(pathname)
  const isBusinessOwnerPath = /^\/business-owner(?:\/|$)/.test(pathname)
  const isAdministratorWorkspacePath = /^(\/admin(?:\/|$)|\/moderation(?:\/|$))/.test(pathname)

  const managementLinks = [
    ...(isAdministrator ? [
      { to: '/admin/dashboard', label: 'Tổng quan', icon: '◫' },
      { to: '/admin/users', label: 'Người dùng', icon: '♙' },
      { to: '/admin/audit-logs', label: 'Nhật ký', icon: '≣' },
      { to: '/moderation/articles', label: 'Kiểm duyệt', icon: '✓' },
    ] : []),
    ...(isRelicManager ? [{ to: RELIC_MANAGER_HOME, label: 'Quản lý di tích', icon: '▤' }] : []),
    ...(isBusinessOwner ? [{ to: BUSINESS_OWNER_HOME, label: 'Kinh doanh', icon: '↗' }] : []),
  ]

  const workspaceLinks = isModerationPath || (isModerator && !isAdministrator)
    ? MODERATION_WORKSPACE_LINKS
    : isRelicManagerPath || (isRelicManager && !isAdministrator && !isModerator)
      ? RELIC_MANAGER_WORKSPACE_LINKS
      : isBusinessOwnerPath || (isBusinessOwner && !isAdministrator && !isModerator && !isRelicManager)
        ? BUSINESS_OWNER_WORKSPACE_LINKS
        : managementLinks

  const accountLinks = user && !isStaff ? [
    { to: '/profile', label: 'Hồ sơ cá nhân', icon: '♙' },
    { to: '/favorites', label: 'Địa điểm yêu thích', icon: '♥' },
    { to: '/notifications', label: 'Thông báo', icon: '●' },
    { to: '/my-tours', label: 'Lịch trình của tôi', icon: '⌁' },
    { to: '/quiz-progress', label: 'Thành tích quiz', icon: '★' },
  ] : []

  const primaryLinks = isStaff
    ? isModerationPath || (isModerator && !isAdministrator)
      ? [{ to: '/moderation/articles', label: 'Kiểm duyệt nội dung' }]
      : isRelicManagerPath || (isRelicManager && !isAdministrator && !isModerator)
        ? [{ to: RELIC_MANAGER_HOME, label: 'Quản lý nội dung' }]
        : isBusinessOwnerPath || (isBusinessOwner && !isAdministrator && !isModerator && !isRelicManager)
          ? [{ to: BUSINESS_OWNER_HOME, label: 'Quản lý doanh nghiệp' }]
          : workspaceLinks
    : PUBLIC_LINKS

  let redirectTo = null
  if (isReady && isAdministrator && !isAdministratorWorkspacePath) redirectTo = '/admin/dashboard'
  else if (isReady && isModerator && !isAdministrator && !isModerationPath) redirectTo = '/moderation/articles'
  else if (isReady && isRelicManager && !isAdministrator && !isModerator && !isRelicManagerPath) redirectTo = RELIC_MANAGER_HOME
  else if (isReady && isBusinessOwner && !isAdministrator && !isModerator && !isRelicManager && !isBusinessOwnerPath) redirectTo = BUSINESS_OWNER_HOME

  const isWorkspacePath = /^(\/admin|\/moderation|\/relic-manager|\/manage\/quizzes|\/business-owner)/.test(pathname)

  return {
    user,
    isReady,
    logout,
    isAdministrator,
    isModerator,
    isRelicManager,
    isBusinessOwner,
    isStaff,
    isModerationPath,
    isRelicManagerPath,
    isBusinessOwnerPath,
    displayedRole: roleLabel(user, isAdministrator ? 'ADMINISTRATOR' : isModerator ? 'MODERATOR' : isRelicManager ? 'RELIC_MANAGER' : isBusinessOwner ? 'BUSINESS_OWNER' : roles[0]),
    workspaceLinks,
    accountLinks,
    primaryLinks,
    mobileLinks: isStaff ? workspaceLinks : primaryLinks,
    homePath: isAdministrator ? '/admin/dashboard' : isModerator ? '/moderation/articles' : isRelicManager ? RELIC_MANAGER_HOME : isBusinessOwner ? BUSINESS_OWNER_HOME : '/',
    brandSubtitle: isAdministrator ? 'Quản trị hệ thống' : isModerator ? 'Kiểm duyệt nội dung' : isRelicManager ? 'Quản lý nội dung di tích' : isBusinessOwner ? 'Quản lý doanh nghiệp' : 'Khám phá Sơn Tây',
    showWorkspace: Boolean(user && isWorkspacePath && workspaceLinks.length),
    redirectTo,
  }
}
