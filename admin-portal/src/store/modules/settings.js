import variables from '@/styles/element-variables.scss'
import defaultSettings from '@/settings'

const { showSettings, tagsView, fixedHeader, sidebarLogo } = defaultSettings
const SETTINGS_CACHE_KEY = 'clothing_fe_ui_settings_v1'

function readCachedSettings() {
  try {
    const raw = window.localStorage.getItem(SETTINGS_CACHE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch (e) {
    return {}
  }
}

function writeCachedSettings(settings) {
  try {
    window.localStorage.setItem(SETTINGS_CACHE_KEY, JSON.stringify(settings || {}))
  } catch (e) {
    // ignore localStorage write errors
  }
}

const cachedSettings = typeof window !== 'undefined' ? readCachedSettings() : {}

const state = {
  theme: cachedSettings.theme || variables.theme,
  showSettings: showSettings,
  tagsView: typeof cachedSettings.tagsView === 'boolean' ? cachedSettings.tagsView : tagsView,
  fixedHeader: typeof cachedSettings.fixedHeader === 'boolean' ? cachedSettings.fixedHeader : fixedHeader,
  sidebarLogo: typeof cachedSettings.sidebarLogo === 'boolean' ? cachedSettings.sidebarLogo : sidebarLogo
}

const mutations = {
  CHANGE_SETTING: (state, { key, value }) => {
    // eslint-disable-next-line no-prototype-builtins
    if (state.hasOwnProperty(key)) {
      state[key] = value
      writeCachedSettings({
        theme: state.theme,
        tagsView: state.tagsView,
        fixedHeader: state.fixedHeader,
        sidebarLogo: state.sidebarLogo
      })
    }
  }
}

const actions = {
  changeSetting({ commit }, data) {
    commit('CHANGE_SETTING', data)
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
