import { useState } from 'react'

function MediaGallery({ media }) {
  const visualMedia = media.filter((item) => item.mediaType === 'IMAGE' || item.mediaType === 'PANORAMA_360')
  const audio = media.filter((item) => item.mediaType === 'AUDIO')
  const videos = media.filter((item) => item.mediaType === 'VIDEO')
  const [activeId, setActiveId] = useState(visualMedia[0]?.id ?? null)
  const active = visualMedia.find((item) => item.id === activeId) || visualMedia[0]

  return (
    <div className="media-gallery">
      {active && (
        <div className={`media-gallery__stage ${active.mediaType === 'PANORAMA_360' ? 'media-gallery__stage--panorama' : ''}`}>
          <img src={active.mediaUrl} alt="Không gian tại địa điểm" />
          {active.mediaType === 'PANORAMA_360' && <span className="media-gallery__badge">Panorama 360° · kéo ngang để quan sát</span>}
        </div>
      )}
      {visualMedia.length > 1 && (
        <div className="media-gallery__thumbs" aria-label="Chọn ảnh">
          {visualMedia.map((item) => (
            <button key={item.id} type="button" onClick={() => setActiveId(item.id)} className={item.id === active?.id ? 'is-active' : ''}>
              <img src={item.thumbnailUrl || item.mediaUrl} alt="" />
              {item.mediaType === 'PANORAMA_360' && <span>360°</span>}
            </button>
          ))}
        </div>
      )}
      {active?.hotspots?.length > 0 && (
        <div className="hotspot-list">
          <h3>Điểm tương tác trong panorama</h3>
          <ul>{active.hotspots.map((hotspot) => <li key={hotspot.id}><strong>{hotspot.label}</strong>{hotspot.description && <span>{hotspot.description}</span>}</li>)}</ul>
        </div>
      )}
      {audio.length > 0 && (
        <div className="audio-guide">
          <h3>Audio guide</h3>
          {audio.map((item) => <audio key={item.id} controls preload="metadata" src={item.mediaUrl}>Trình duyệt không hỗ trợ audio.</audio>)}
        </div>
      )}
      {videos.map((item) => <video className="place-video" key={item.id} controls preload="metadata" src={item.mediaUrl} />)}
      {!media.length && <div className="media-gallery__empty">Media của địa điểm đang được cập nhật.</div>}
    </div>
  )
}

export default MediaGallery
