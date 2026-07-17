function ContentMedia({ media = [] }) {
  const images = media.filter((item) => item.mediaType === 'IMAGE' || item.mediaType === 'PANORAMA_360')
  const videos = media.filter((item) => item.mediaType === 'VIDEO')
  const audio = media.filter((item) => item.mediaType === 'AUDIO')

  if (!media.length) return null
  return (
    <section className="content-media" aria-label="Nội dung đa phương tiện">
      {images.length > 0 && <div className="content-media__images">{images.map((item) => <img key={item.id} src={item.mediaUrl} alt="" loading="lazy" />)}</div>}
      {videos.map((item) => <video key={item.id} controls preload="metadata" src={item.mediaUrl} />)}
      {audio.map((item) => <audio key={item.id} controls preload="metadata" src={item.mediaUrl} />)}
    </section>
  )
}

export default ContentMedia
