import { asyncRoutes, constantRoutes } from '@/router'

/**
 * Use meta.role to determine if the current user has permission
 * @param roles
 * @param route
 */
function hasPermission(roles, route) {
  if (route.meta && route.meta.roles) {
    return roles.some(role => route.meta.roles.includes(role))
  } else {
    return true
  }
}

/**
 * Filter asynchronous routing tables by recursion
 * @param routes asyncRoutes
 * @param roles
 */
export function filterAsyncRoutes(routes, roles) {
  const res = []

  routes.forEach(route => {
    const tmp = { ...route }
    if (hasPermission(roles, tmp)) {
      if (tmp.children) {
        tmp.children = filterAsyncRoutes(tmp.children, roles)
      }
      res.push(tmp)
    }
  })

  return res
}

const state = {
  routes: [],
  addRoutes: []
}

const mutations = {
  SET_ROUTES: (state, payload) => {
    const { constant, dynamic } = payload
    state.addRoutes = dynamic
    state.routes = constant.concat(dynamic)
  }
}

const actions = {
  async generateRoutes({ commit }, roles) {
    const roleFilteredRoutes = filterAsyncRoutes(asyncRoutes, roles)

    commit('SET_ROUTES', {
      constant: constantRoutes,
      dynamic: roleFilteredRoutes
    })
    return roleFilteredRoutes
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
