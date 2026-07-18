import { joinRelativeURL } from 'ufo'

export function baseURL() {
  return '/'
}

export function buildAssetsDir() {
  return '/_nuxt/'
}

export function publicAssetsURL(...path) {
  return path.length ? joinRelativeURL(baseURL(), ...path) : baseURL()
}

export function buildAssetsURL(...path) {
  return joinRelativeURL(publicAssetsURL(), buildAssetsDir(), ...path)
}
