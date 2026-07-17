export const DEFAULT_ROLE_LABELS = {
  TOURIST: 'Khách du lịch',
  BUSINESS_OWNER: 'Chủ doanh nghiệp',
  RELIC_MANAGER: 'Quản lý di tích',
  MODERATOR: 'Kiểm duyệt viên',
  ADMINISTRATOR: 'Quản trị viên',
}

export function roleLabel(account, role) {
  return account?.roleLabels?.[role] || DEFAULT_ROLE_LABELS[role] || role
}
