import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

const DISCOVERY_CARDS = [
  {
    to: '/places',
    icon: '⌖',
    eyebrow: 'Bản đồ địa phương',
    title: 'Di tích & điểm đến',
    description: 'Tìm địa điểm đã được kiểm duyệt, xem media, chỉ dẫn và trải nghiệm gần bạn.',
  },
  {
    to: '/businesses',
    icon: '◈',
    eyebrow: 'Cộng đồng bản địa',
    title: 'Doanh nghiệp địa phương',
    description: 'Khám phá dịch vụ, sản phẩm và ưu đãi từ những đơn vị đang hoạt động tại Sơn Tây.',
  },
  {
    to: '/events',
    icon: '✦',
    eyebrow: 'Văn hóa & lễ hội',
    title: 'Sự kiện sắp tới',
    description: 'Theo dõi các hoạt động văn hóa, lịch trình sự kiện và câu chuyện đang diễn ra.',
  },
]

const JOURNEY_STEPS = [
  { number: '01', title: 'Chọn điểm đến', description: 'Tìm kiếm theo từ khóa, danh mục hoặc vị trí hiện tại của bạn.' },
  { number: '02', title: 'Tạo lịch trình', description: 'Sắp xếp các điểm dừng thành hành trình phù hợp với thời gian riêng.' },
  { number: '03', title: 'Trải nghiệm sâu hơn', description: 'Lưu địa điểm, đánh giá và tham gia quiz khi có mặt tại điểm đến.' },
]

function HomePage() {
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')

  function submitSearch(event) {
    event.preventDefault()
    const query = keyword.trim()
    navigate(query ? `/places?q=${encodeURIComponent(query)}` : '/places')
  }

  return (
    <div className="home-page">
      <section className="home-hero" aria-labelledby="home-title">
        <div className="home-hero__glow home-hero__glow--one" aria-hidden="true" />
        <div className="home-hero__glow home-hero__glow--two" aria-hidden="true" />

        <div className="home-hero__content">
          <p className="home-hero__badge"><span>✦</span> Hành trình về miền di sản</p>
          <h1 id="home-title">
            Khám phá Sơn Tây,
            <span>chạm vào từng câu chuyện.</span>
          </h1>
          <p className="home-hero__summary">
            Một không gian du lịch địa phương giúp bạn tìm điểm đến, kết nối doanh nghiệp,
            tạo lịch trình và trải nghiệm văn hóa Sơn Tây theo cách riêng.
          </p>

          <form className="home-search" onSubmit={submitSearch} role="search">
            <span aria-hidden="true">⌕</span>
            <label className="sr-only" htmlFor="home-search-input">Tìm địa điểm tại Sơn Tây</label>
            <input
              id="home-search-input"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Tìm di tích, làng nghề, nhà hàng…"
              maxLength="255"
            />
            <button type="submit">Khám phá</button>
          </form>

          <div className="home-hero__actions">
            <Link className="button button--gold" to="/places">Xem bản đồ điểm đến <span aria-hidden="true">→</span></Link>
            <Link className="button button--glass" to="/tours">Khám phá lịch trình</Link>
          </div>

          <div className="home-hero__trust" aria-label="Các lợi ích chính">
            <span><i aria-hidden="true">✓</i> Nội dung kiểm duyệt</span>
            <span><i aria-hidden="true">✓</i> Lịch trình cá nhân</span>
            <span><i aria-hidden="true">✓</i> Kết nối địa phương</span>
          </div>
        </div>

        <aside className="home-hero__visual" aria-label="Minh họa hành trình Sơn Tây">
          <div className="heritage-orbit heritage-orbit--outer" aria-hidden="true" />
          <div className="heritage-orbit heritage-orbit--inner" aria-hidden="true" />
          <div className="heritage-map-card">
            <div className="heritage-map-card__top">
              <span>Hành trình gợi ý</span>
              <strong>Sơn Tây trong ngày</strong>
            </div>
            <div className="heritage-map-card__route" aria-hidden="true">
              <span className="route-line" />
              <i className="route-pin route-pin--one">1</i>
              <i className="route-pin route-pin--two">2</i>
              <i className="route-pin route-pin--three">3</i>
              <span className="route-label route-label--one">Làng cổ</span>
              <span className="route-label route-label--two">Thành cổ</span>
              <span className="route-label route-label--three">Ẩm thực</span>
            </div>
            <div className="heritage-map-card__bottom">
              <span><i>⌖</i> 3 điểm dừng</span>
              <span><i>◷</i> Một ngày</span>
            </div>
          </div>
          <div className="floating-note floating-note--top"><span>★</span><div><strong>Trải nghiệm đáng nhớ</strong><small>Lưu lại nơi bạn yêu thích</small></div></div>
          <div className="floating-note floating-note--bottom"><span>?</span><div><strong>Quiz tại điểm đến</strong><small>Học qua mỗi hành trình</small></div></div>
        </aside>
      </section>

      <section className="home-section" aria-labelledby="discover-heading">
        <header className="home-section__header">
          <div>
            <p className="section-eyebrow">Bắt đầu khám phá</p>
            <h2 id="discover-heading">Một Sơn Tây, nhiều cách trải nghiệm</h2>
          </div>
          <Link to="/places">Xem tất cả địa điểm <span aria-hidden="true">→</span></Link>
        </header>

        <div className="discovery-feature-grid">
          {DISCOVERY_CARDS.map((card) => (
            <Link className="discovery-feature-card" key={card.to} to={card.to}>
              <span className="discovery-feature-card__icon" aria-hidden="true">{card.icon}</span>
              <p>{card.eyebrow}</p>
              <h3>{card.title}</h3>
              <span className="discovery-feature-card__description">{card.description}</span>
              <strong>Khám phá <i aria-hidden="true">→</i></strong>
            </Link>
          ))}
        </div>
      </section>

      <section className="home-story" aria-labelledby="story-heading">
        <div className="home-story__art" aria-hidden="true">
          <div className="story-sun" />
          <div className="story-gate"><span /><span /><span /></div>
          <p>Đất cổ<br /><strong>xứ Đoài</strong></p>
        </div>
        <div className="home-story__content">
          <p className="section-eyebrow">Câu chuyện địa phương</p>
          <h2 id="story-heading">Không chỉ ghé thăm,<br />hãy thực sự hiểu nơi mình đến.</h2>
          <p>
            LTSS kết nối thông tin di sản, bài viết, doanh nghiệp và cộng đồng trong một hành trình liền mạch.
            Mỗi điểm dừng đều có câu chuyện, mỗi trải nghiệm đều có thể trở thành một ký ức đáng nhớ.
          </p>
          <div className="home-story__links">
            <Link className="button button--primary" to="/articles">Đọc câu chuyện địa phương</Link>
            <Link to="/quizzes">Thử thách kiến thức <span aria-hidden="true">→</span></Link>
          </div>
        </div>
      </section>

      <section className="home-section journey-section" aria-labelledby="journey-heading">
        <header className="home-section__header home-section__header--center">
          <div>
            <p className="section-eyebrow">Hành trình của bạn</p>
            <h2 id="journey-heading">Từ cảm hứng đến trải nghiệm</h2>
            <p>Ba bước đơn giản để bắt đầu một chuyến đi mang dấu ấn riêng.</p>
          </div>
        </header>
        <div className="journey-steps">
          {JOURNEY_STEPS.map((step, index) => (
            <article key={step.number}>
              <span>{step.number}</span>
              <div className="journey-step__icon" aria-hidden="true">{index === 0 ? '⌖' : index === 1 ? '⌁' : '★'}</div>
              <h3>{step.title}</h3>
              <p>{step.description}</p>
              {index < JOURNEY_STEPS.length - 1 && <i className="journey-step__line" aria-hidden="true" />}
            </article>
          ))}
        </div>
      </section>

      <section className="home-cta" aria-labelledby="cta-heading">
        <div className="home-cta__decoration" aria-hidden="true">✦</div>
        <p className="section-eyebrow">Sẵn sàng lên đường?</p>
        <h2 id="cta-heading">Tạo hành trình Sơn Tây của riêng bạn.</h2>
        <p>Lưu địa điểm yêu thích, xây dựng lịch trình và ghi lại từng trải nghiệm trong một tài khoản.</p>
        <div>
          <Link className="button button--gold" to="/register">Tạo tài khoản miễn phí</Link>
          <Link className="button button--glass" to="/places">Khám phá ngay</Link>
        </div>
      </section>
    </div>
  )
}

export default HomePage
