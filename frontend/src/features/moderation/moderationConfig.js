export const MODERATION_TARGETS = {
  ARTICLE: {
    label: 'Bài viết',
    title: 'Kiểm duyệt bài viết',
    description: 'Xem và xử lý các bài viết đang chờ phê duyệt trước khi xuất bản.',
    emptyMessage: 'Hiện không có bài viết nào đang chờ duyệt.',
    path: '/moderation/articles',
    icon: '▤',
  },
  EVENT: {
    label: 'Sự kiện',
    title: 'Kiểm duyệt sự kiện',
    description: 'Kiểm tra thông tin sự kiện trước khi nội dung được công khai.',
    emptyMessage: 'Hiện không có sự kiện nào đang chờ duyệt.',
    path: '/moderation/events',
    icon: '◷',
  },
  BUSINESS_POST: {
    label: 'Bài đăng doanh nghiệp',
    title: 'Kiểm duyệt bài đăng doanh nghiệp',
    description: 'Xử lý các bài đăng do doanh nghiệp gửi lên hệ thống.',
    emptyMessage: 'Hiện không có bài đăng doanh nghiệp nào đang chờ duyệt.',
    path: '/moderation/business-posts',
    icon: '▣',
  },
  PROMOTION: {
    label: 'Khuyến mãi',
    title: 'Kiểm duyệt khuyến mãi',
    description: 'Xác minh nội dung và thời hạn của các chương trình khuyến mãi.',
    emptyMessage: 'Hiện không có chương trình khuyến mãi nào đang chờ duyệt.',
    path: '/moderation/promotions',
    icon: '%',
  },
  QUIZ: {
    label: 'Quiz điểm đến',
    title: 'Kiểm duyệt quiz',
    description: 'Kiểm tra câu hỏi, đáp án và nội dung quiz trước khi xuất bản.',
    emptyMessage: 'Hiện không có quiz nào đang chờ duyệt.',
    path: '/moderation/quizzes',
    icon: '?',
  },
  REVIEW: {
    label: 'Đánh giá cộng đồng',
    title: 'Kiểm duyệt đánh giá',
    description: 'Xử lý các đánh giá cộng đồng trước khi hiển thị công khai.',
    emptyMessage: 'Hiện không có đánh giá nào đang chờ duyệt.',
    path: '/moderation/reviews',
    icon: '★',
  },
}

export const MODERATION_WORKSPACE_LINKS = [
  ...['ARTICLE', 'QUIZ'].map((targetType) => ({
    ...MODERATION_TARGETS[targetType],
    to: MODERATION_TARGETS[targetType].path,
    label: MODERATION_TARGETS[targetType].label,
    icon: MODERATION_TARGETS[targetType].icon,
    targetType,
    end: false,
  })),
  {
    to: '/moderation/article-categories',
    label: 'Danh mục bài viết',
    icon: '▦',
    end: true,
  },
]

export function moderationConfigFor(targetType) {
  return MODERATION_TARGETS[targetType] || MODERATION_TARGETS.ARTICLE
}

export function moderationPathFor(targetType) {
  return moderationConfigFor(targetType).path
}
