const PROMOTION_TYPE_LABELS = {
  PERCENTAGE: 'Giảm theo phần trăm',
  FIXED_AMOUNT: 'Giảm số tiền cố định',
  OTHER: 'Ưu đãi theo điều kiện',
}

const REVIEW_TARGET_LABELS = {
  PLACE: 'Địa điểm',
  BUSINESS: 'Doanh nghiệp',
  ARTICLE: 'Bài viết',
  TOUR: 'Lịch trình',
}

function fieldValue(content, label) {
  return content?.fields?.find((field) => field.label === label)?.value
}

function displayValue(value) {
  if (value === null || value === undefined || value === '') return 'Không có'
  if (typeof value === 'string') {
    const dateTime = value.match(/^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}:\d{2}:\d{2})(?:\.\d+)?/)
    if (dateTime) return `${dateTime[3]}/${dateTime[2]}/${dateTime[1]} · ${dateTime[4]}`
    const time = value.match(/^(\d{2}:\d{2}:\d{2})(?:\.\d+)?$/)
    if (time) return time[1]
  }
  return String(value)
}

function ContentHeader({ eyebrow, title, description }) {
  return <header><p className="eyebrow">{eyebrow}</p><h2>{title}</h2><span>{description}</span></header>
}

function InfoGrid({ items }) {
  return <dl className="moderation-content__info-grid">{items.map((item) => <div key={item.label}><dt>{item.label}</dt><dd>{displayValue(item.value)}</dd></div>)}</dl>
}

function TextBlock({ title, children, emphasized = false }) {
  if (!children) return null
  return <section className={`moderation-content__text${emphasized ? ' moderation-content__text--emphasized' : ''}`}><h3>{title}</h3><p>{children}</p></section>
}

function EmptyContent() {
  return <p className="form-status">Nội dung này không có dữ liệu mô tả bổ sung.</p>
}

function ArticleContent({ content }) {
  return <>
    <ContentHeader eyebrow="Bài viết gửi duyệt" title="Nội dung bài viết" description="Kiểm tra tiêu đề, tóm tắt, liên kết và toàn bộ nội dung biên tập." />
    <div className="moderation-content moderation-content--article">
      <InfoGrid items={[
        { label: 'Đường dẫn', value: fieldValue(content, 'Đường dẫn') },
        { label: 'Địa điểm liên quan', value: fieldValue(content, 'ID địa điểm') ? `#${fieldValue(content, 'ID địa điểm')}` : null },
        { label: 'Sự kiện liên quan', value: fieldValue(content, 'ID sự kiện') ? `#${fieldValue(content, 'ID sự kiện')}` : null },
      ]} />
      <TextBlock title="Tóm tắt bài viết" emphasized>{content.summary}</TextBlock>
      <TextBlock title="Nội dung bài viết">{content.body}</TextBlock>
      {!content.summary && !content.body && <EmptyContent />}
    </div>
  </>
}

function EventContent({ content }) {
  const startAt = fieldValue(content, 'Bắt đầu')
  const endAt = fieldValue(content, 'Kết thúc')
  return <>
    <ContentHeader eyebrow="Sự kiện gửi duyệt" title="Thông tin sự kiện" description="Đối chiếu thời gian, địa điểm tổ chức và nội dung giới thiệu sự kiện." />
    <div className="moderation-content moderation-content--event">
      <section className="moderation-content__schedule" aria-label="Thời gian diễn ra">
        <div><span>Bắt đầu</span><strong>{displayValue(startAt)}</strong></div>
        <i aria-hidden="true">→</i>
        <div><span>Kết thúc</span><strong>{displayValue(endAt)}</strong></div>
      </section>
      <InfoGrid items={[
        { label: 'Địa điểm tổ chức', value: fieldValue(content, 'Địa điểm tổ chức') },
        { label: 'ID địa điểm', value: fieldValue(content, 'ID địa điểm') ? `#${fieldValue(content, 'ID địa điểm')}` : null },
      ]} />
      <TextBlock title="Mô tả sự kiện">{content.body}</TextBlock>
      {!content.body && <EmptyContent />}
    </div>
  </>
}

function BusinessPostContent({ content }) {
  return <>
    <ContentHeader eyebrow="Bài đăng doanh nghiệp" title="Nội dung truyền thông" description="Kiểm tra thông tin doanh nghiệp, phần giới thiệu ngắn và nội dung bài đăng." />
    <div className="moderation-content moderation-content--business">
      <InfoGrid items={[
        { label: 'Doanh nghiệp', value: fieldValue(content, 'ID doanh nghiệp') ? `#${fieldValue(content, 'ID doanh nghiệp')}` : null },
        { label: 'Đường dẫn', value: fieldValue(content, 'Đường dẫn') },
      ]} />
      <TextBlock title="Tóm tắt bài đăng" emphasized>{content.summary}</TextBlock>
      <TextBlock title="Nội dung bài đăng">{content.body}</TextBlock>
      {!content.summary && !content.body && <EmptyContent />}
    </div>
  </>
}

function PromotionContent({ content }) {
  const type = fieldValue(content, 'Hình thức giảm')
  const value = fieldValue(content, 'Giá trị giảm')
  const formattedValue = value === null || value === undefined || value === ''
    ? 'Theo điều kiện chương trình'
    : type === 'PERCENTAGE' ? `${value}%` : displayValue(value)

  return <>
    <ContentHeader eyebrow="Chương trình khuyến mãi" title="Điều kiện ưu đãi" description="Xác minh mã áp dụng, giá trị ưu đãi, thời hạn và nội dung chương trình." />
    <div className="moderation-content moderation-content--promotion">
      <section className="moderation-content__promotion-hero">
        <div><span>Mã khuyến mãi</span><strong>{displayValue(fieldValue(content, 'Mã khuyến mãi'))}</strong></div>
        <div><span>{PROMOTION_TYPE_LABELS[type] || displayValue(type)}</span><strong>{formattedValue}</strong></div>
      </section>
      <InfoGrid items={[
        { label: 'Bắt đầu áp dụng', value: fieldValue(content, 'Bắt đầu') },
        { label: 'Kết thúc áp dụng', value: fieldValue(content, 'Kết thúc') },
        { label: 'Doanh nghiệp', value: fieldValue(content, 'ID doanh nghiệp') ? `#${fieldValue(content, 'ID doanh nghiệp')}` : null },
      ]} />
      <TextBlock title="Mô tả và điều kiện áp dụng">{content.body}</TextBlock>
      {!content.body && <EmptyContent />}
    </div>
  </>
}

function QuizQuestions({ questions = [] }) {
  if (!questions.length) return <EmptyContent />
  return <section className="moderation-detail__questions"><div><h3>Danh sách câu hỏi</h3><span>{questions.length} câu</span></div>{questions.map((question, index) => <article key={`${index}-${question.content}`}><header><span>Câu {index + 1}</span><strong>{question.points} điểm</strong></header><h4>{question.content}</h4>{question.explanation && <p>{question.explanation}</p>}<ul>{question.answers.map((answer, answerIndex) => <li className={answer.correct ? 'is-correct' : ''} key={`${answerIndex}-${answer.content}`}><span>{String.fromCharCode(65 + answerIndex)}</span>{answer.content}{answer.correct && <strong>Đáp án đúng</strong>}</li>)}</ul></article>)}</section>
}

function QuizContent({ content }) {
  return <>
    <ContentHeader eyebrow="Quiz gửi duyệt" title="Cấu trúc bài quiz" description="Kiểm tra cấu hình bài làm, câu hỏi, đáp án đúng và phần giải thích." />
    <div className="moderation-content moderation-content--quiz">
      <InfoGrid items={[
        { label: 'Thời gian làm bài', value: fieldValue(content, 'Thời gian làm bài') },
        { label: 'Điểm đạt', value: fieldValue(content, 'Điểm đạt') },
        { label: 'Địa điểm áp dụng', value: fieldValue(content, 'ID địa điểm') ? `#${fieldValue(content, 'ID địa điểm')}` : null },
      ]} />
      <TextBlock title="Mô tả quiz">{content.body}</TextBlock>
      <QuizQuestions questions={content.questions} />
    </div>
  </>
}

function ReviewContent({ content }) {
  const rating = Number(fieldValue(content, 'Số sao')) || 0
  const targetType = fieldValue(content, 'Loại đối tượng được đánh giá')
  const targetId = fieldValue(content, 'ID đối tượng được đánh giá')
  return <>
    <ContentHeader eyebrow="Đánh giá cộng đồng" title="Nội dung đánh giá" description="Kiểm tra mức điểm, đối tượng được đánh giá và bình luận của người dùng." />
    <div className="moderation-content moderation-content--review">
      <section className="moderation-content__rating">
        <div><span>Điểm đánh giá</span><strong aria-label={`${rating} trên 5 sao`}>{Array.from({ length: 5 }, (_, index) => <i className={index < rating ? 'is-active' : ''} key={index} aria-hidden="true">★</i>)}</strong></div>
        <div><span>Đối tượng</span><strong>{REVIEW_TARGET_LABELS[targetType] || displayValue(targetType)} {targetId ? `#${targetId}` : ''}</strong></div>
      </section>
      <blockquote className="moderation-content__review-comment">{content.body || 'Người dùng không để lại bình luận.'}</blockquote>
    </div>
  </>
}

function ModerationContentDetail({ targetType, content }) {
  if (!content) return <p className="form-status">Backend chưa cung cấp nội dung chi tiết cho yêu cầu này.</p>
  if (targetType === 'ARTICLE') return <ArticleContent content={content} />
  if (targetType === 'EVENT') return <EventContent content={content} />
  if (targetType === 'BUSINESS_POST') return <BusinessPostContent content={content} />
  if (targetType === 'PROMOTION') return <PromotionContent content={content} />
  if (targetType === 'QUIZ') return <QuizContent content={content} />
  return <ReviewContent content={content} />
}

export default ModerationContentDetail
