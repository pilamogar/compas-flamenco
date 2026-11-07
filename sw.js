// Guarda la app en el móvil para que funcione sin conexión
const CACHE = 'compas-v5';
// Grabaciones de palmas, cajón y guitarra (unos 300 KB en total)
const SONIDOS = [
  'palmas/clara_1.wav', 'palmas/clara_2.wav', 'palmas/clara_3.wav',
  'palmas/sorda_1.wav', 'palmas/sorda_2.wav', 'palmas/sorda_3.wav',
  'cajon/cajon_1.wav', 'cajon/cajon_2.wav',
  ...['E2', 'Fs2', 'Gs2', 'A2', 'B2', 'Cs3', 'D3', 'E3', 'Fs3', 'G3', 'B3', 'Cs4', 'Ds4', 'E4', 'Fs4', 'Gs4'].map(n => 'guitarra/' + n + '.mp3')
].map(f => './sonidos/' + f);
const FILES = ['./', './index.html', './manifest.webmanifest', './icon-192.png', './icon-512.png', ...SONIDOS];
self.addEventListener('install', e => { e.waitUntil(caches.open(CACHE).then(c => c.addAll(FILES))); self.skipWaiting(); });
self.addEventListener('activate', e => { e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k))))); self.clients.claim(); });
self.addEventListener('fetch', e => {
  // Red primero (para recibir mejoras), copia guardada si no hay conexión
  e.respondWith(fetch(e.request).then(r => { const c = r.clone(); caches.open(CACHE).then(k => k.put(e.request, c)); return r; }).catch(() => caches.match(e.request).then(r => r || caches.match('./index.html'))));
});
