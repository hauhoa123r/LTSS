function commonsMedia(fileName, widthPx, heightPx, thumbnailWidth = 640) {
  return {
    mediaUrl: `https://commons.wikimedia.org/wiki/Special:FilePath/${fileName}?width=1280`,
    thumbnailUrl: `https://commons.wikimedia.org/wiki/Special:FilePath/${fileName}?width=${thumbnailWidth}`,
    widthPx,
    heightPx,
  }
}

export const DEMO_MEDIA = {
  thanhCoSonTay: commonsMedia('Th%C3%A0nh_c%E1%BB%95_S%C6%A1n_T%C3%A2y_2021.jpg', 1280, 1279, 500),
  thanhCoNorthGate: commonsMedia('Tay-Son-North-Gate.jpg', 428, 661, 428),
  thanhCoEntrance: commonsMedia('Sontay_-_Entr%C3%A9e_de_la_Citadelle.jpg', 1280, 829),
  thanhCoSouthGate: commonsMedia('CuaTienThanhSon.jpg', 1280, 315),
  thanhCoInside: commonsMedia('ThanhSon%28BenTrong%29.jpg', 1280, 960),
  thanhCoWall: commonsMedia('TuongDaoThanhSon.jpg', 1280, 960),
  chuaMia: commonsMedia('Ch%C3%B9a_M%C3%ADa.jpg', 1280, 951),
  chuaMiaEntrance: commonsMedia('L%E1%BB%91i_v%C3%A0o_ch%C3%B9a_M%C3%ADa.jpg', 1280, 960),
  chuaMiaStatues: commonsMedia('M%E1%BB%99t_s%E1%BB%91_t%C6%B0%E1%BB%A3ng_trong_ch%C3%B9a_M%C3%ADa.jpg', 1280, 960),
  chuaMiaAltar: commonsMedia('BaChuaMia.JPG', 1280, 960),
  bepLangDuongLam: commonsMedia('Street_Food_Life_%28Unsplash%29.jpg', 1280, 853),
  phoGa: commonsMedia('Chicken_Noodle_Soup_%28Pho_Ga%29%2C_T%C3%A2y_H%E1%BB%93_%287069116563%29.jpg', 1280, 720),
  gaXePhay: commonsMedia('G%C3%A0_x%C3%A9_phay.jpg', 1280, 720),
  gaXePhayTwo: commonsMedia('G%C3%A0_x%C3%A9_phay_%282%29.jpg', 1280, 720),
  chickenHanoi: commonsMedia('Chicken_in_Hanoi.jpg', 1280, 1024),
  bunCha: commonsMedia('Bun_Cha.jpg', 960, 1280),
  duongLam: commonsMedia('%C4%90%C6%B0%E1%BB%9Dng_L%C3%A2m.jpg', 1280, 850),
  duongLamGate: commonsMedia('CongLangDuongLam.jpg', 960, 1280),
  duongLamAncient: commonsMedia('The-ancient-village-3358830.jpg', 1280, 853),
  duongLamEntrance: commonsMedia('C%E1%BB%95ng_v%C3%A0o_l%C3%A0ng_c%E1%BB%95_%C4%90%C6%B0%E1%BB%9Dng_L%C3%A2m.jpg', 1280, 960),
  denNgoQuyen: commonsMedia('DenNgoQuyen.jpg', 1280, 960),
  langMoNgoQuyen: commonsMedia('LangMoNgoQuyen.jpg', 960, 1280),
  riceCrops: commonsMedia('Rice_Crops_%28148457879%29.jpeg', 1280, 853),
  dongMo: commonsMedia('%C4%90%E1%BB%93ng_M%C3%B4_Lake%2C_S%C6%A1n_T%C3%A2y%2C_Hanoi.jpg', 1280, 958),
  dongMoView: commonsMedia('HoDongMo.jpg', 1280, 557),
  dongMoVilla: commonsMedia('Ho_Dong_Mo.jpg', 1280, 960),
}

const MEDIA_POOLS = {
  heritage: [
    DEMO_MEDIA.thanhCoSonTay,
    DEMO_MEDIA.thanhCoEntrance,
    DEMO_MEDIA.thanhCoNorthGate,
    DEMO_MEDIA.thanhCoSouthGate,
    DEMO_MEDIA.thanhCoInside,
    DEMO_MEDIA.thanhCoWall,
  ],
  pagoda: [DEMO_MEDIA.chuaMia, DEMO_MEDIA.chuaMiaEntrance, DEMO_MEDIA.chuaMiaStatues, DEMO_MEDIA.chuaMiaAltar],
  food: [
    DEMO_MEDIA.bepLangDuongLam,
    DEMO_MEDIA.phoGa,
    DEMO_MEDIA.gaXePhay,
    DEMO_MEDIA.gaXePhayTwo,
    DEMO_MEDIA.chickenHanoi,
    DEMO_MEDIA.bunCha,
  ],
  duongLam: [
    DEMO_MEDIA.duongLam,
    DEMO_MEDIA.duongLamGate,
    DEMO_MEDIA.duongLamAncient,
    DEMO_MEDIA.duongLamEntrance,
    DEMO_MEDIA.denNgoQuyen,
    DEMO_MEDIA.langMoNgoQuyen,
    DEMO_MEDIA.riceCrops,
  ],
  dongMo: [DEMO_MEDIA.dongMo, DEMO_MEDIA.dongMoView, DEMO_MEDIA.dongMoVilla],
}

const MIXED_MEDIA_POOL = [
  ...MEDIA_POOLS.heritage,
  ...MEDIA_POOLS.pagoda,
  ...MEDIA_POOLS.food,
  ...MEDIA_POOLS.duongLam,
  ...MEDIA_POOLS.dongMo,
]

const DEMO_PLACE_MEDIA_BY_SLUG = {
  'demo-thanh-co-son-tay': DEMO_MEDIA.thanhCoSonTay,
  'demo-chua-mia': DEMO_MEDIA.chuaMia,
  'demo-bep-lang-duong-lam': DEMO_MEDIA.bepLangDuongLam,
}

const DEMO_CONTENT_MEDIA_BY_SLUG = {
  'demo-mot-ngay-kham-pha-thanh-co-son-tay': DEMO_MEDIA.thanhCoSonTay,
  'demo-tour-dem-thanh-co-son-tay': DEMO_MEDIA.thanhCoSonTay,
  'demo-dac-san-ga-mia-duong-lam': DEMO_MEDIA.bepLangDuongLam,
}

function isDemoCdnUrl(url) {
  return typeof url === 'string' && url.includes('cdn.ltss.local/')
}

function chooseFromPool(pool, seed) {
  if (!pool.length) return null
  const key = String(seed || '')
  let hash = 0
  for (let index = 0; index < key.length; index += 1) {
    hash = (hash * 31 + key.charCodeAt(index)) >>> 0
  }
  return pool[hash % pool.length]
}

function normalizeText(...values) {
  return values
    .filter(Boolean)
    .join(' ')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase()
}

function toMediaItem(media, id) {
  return {
    id,
    mediaType: 'IMAGE',
    mediaUrl: media.mediaUrl,
    thumbnailUrl: media.thumbnailUrl,
    mimeType: 'image/jpeg',
    fileSizeBytes: null,
    widthPx: media.widthPx,
    heightPx: media.heightPx,
    durationSeconds: null,
    usageType: 'COVER',
    displayOrder: 0,
    primary: true,
    hotspots: [],
  }
}

function replaceBrokenDemoUrls(media = [], fallback) {
  const items = Array.isArray(media) ? media : []
  if (!fallback) return items
  if (!items.length) return [toMediaItem(fallback, 'demo-cover')]
  if (items.some((item) => isDemoCdnUrl(item.mediaUrl) || isDemoCdnUrl(item.thumbnailUrl))) {
    return [toMediaItem(fallback, items[0]?.id ?? 'demo-cover')]
  }

  return items
}

export function resolveDemoPlaceCover(place, currentCoverUrl) {
  const fallback = getDemoPlaceMedia(place, currentCoverUrl)
  if (!fallback) return currentCoverUrl
  return !currentCoverUrl || isDemoCdnUrl(currentCoverUrl) ? fallback.thumbnailUrl : currentCoverUrl
}

export function resolveDemoContentCover(type, item, currentCoverUrl) {
  const fallback = getDemoContentMedia(type, item, currentCoverUrl)
  if (!fallback) return currentCoverUrl
  return !currentCoverUrl || isDemoCdnUrl(currentCoverUrl) ? fallback.thumbnailUrl : currentCoverUrl
}

export function withDemoPlaceMedia(place) {
  return replaceBrokenDemoUrls(place?.media, getDemoPlaceMedia(place))
}

export function withDemoContentMedia(type, item) {
  return replaceBrokenDemoUrls(item?.media, getDemoContentMedia(type, item))
}

function getDemoContentMedia(type, item, currentCoverUrl) {
  if (type === 'business') return getDemoPlaceMedia(item?.place, currentCoverUrl)
  if (type === 'promotion') return DEMO_MEDIA.bepLangDuongLam

  const direct = DEMO_CONTENT_MEDIA_BY_SLUG[item?.slug]
  if (direct) return direct
  const seed = [item?.id, item?.slug, item?.title, currentCoverUrl].filter(Boolean).join(':') || type

  return getMediaFromText(type, normalizeText(
    item?.slug,
    item?.title,
    item?.summary,
    item?.category?.name,
    item?.category?.slug,
    item?.locationNote,
    item?.place?.name,
    item?.place?.slug,
  ), seed)
}

function getDemoPlaceMedia(place, currentCoverUrl) {
  const direct = DEMO_PLACE_MEDIA_BY_SLUG[place?.slug]
  if (direct) return direct
  const seed = [place?.id, place?.slug, place?.name, currentCoverUrl].filter(Boolean).join(':') || 'place'

  return getMediaFromText('place', normalizeText(
    place?.slug,
    place?.name,
    place?.summary,
    place?.address,
    place?.category?.name,
    place?.category?.slug,
    place?.category?.markerIconKey,
  ), seed)
}

function getMediaFromText(type, text, seed) {
  if (!text) return type === 'business' ? DEMO_MEDIA.bepLangDuongLam : DEMO_MEDIA.thanhCoSonTay
  if (/chua|pagoda|den[-\s]va|tan[-\s]vien|tin[-\s]nguong/.test(text)) return chooseFromPool(MEDIA_POOLS.pagoda, seed)
  if (/am[-\s]thuc|ga[-\s]mia|nha[-\s]hang|restaurant|food|dac[-\s]san|che[-\s]lam|bep|khuyen[-\s]mai/.test(text)) return chooseFromPool(MEDIA_POOLS.food, seed)
  if (/dong[-\s]mo|sinh[-\s]thai|resort|nghi[-\s]duong|ven[-\s]ho/.test(text)) return chooseFromPool(MEDIA_POOLS.dongMo, seed)
  if (/duong[-\s]lam|mong[-\s]phu|dong[-\s]sang|cam[-\s]lam|da[-\s]ong|xu[-\s]doai|homestay|lang[-\s]co|lang/.test(text)) return chooseFromPool(MEDIA_POOLS.duongLam, seed)
  if (/thanh[-\s]co|lich[-\s]su|di[-\s]san|bao[-\s]ton|bao[-\s]tang|van[-\s]hoa/.test(text)) return chooseFromPool(MEDIA_POOLS.heritage, seed)
  if (/khach[-\s]san|luu[-\s]tru|dich[-\s]vu|business/.test(text)) return chooseFromPool(MIXED_MEDIA_POOL, seed)
  if (type === 'event' || type === 'article') return chooseFromPool(MEDIA_POOLS.heritage, seed)
  if (type === 'business') return chooseFromPool(MIXED_MEDIA_POOL, seed)
  return chooseFromPool(MIXED_MEDIA_POOL, seed)
}
