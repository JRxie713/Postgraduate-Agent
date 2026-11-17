import { request } from './http'

// 当前登录用户的角色与权限点（前端据此显示菜单和按钮）
export const myPermissions = () => request('/rbac/me')

// 权限点目录（按模块分组）
export const permissionCatalog = () => request('/rbac/permissions')

// 权限组
export const groupList = () => request('/rbac/groups')

export const createGroup = (payload) => request('/rbac/groups', { method: 'POST', body: payload })

export const updateGroup = (id, payload) =>
  request(`/rbac/groups/${id}`, { method: 'PUT', body: payload })

export const deleteGroup = (id) => request(`/rbac/groups/${id}`, { method: 'DELETE' })

// 用户与权限组
export const getUserGroups = (userId) => request(`/rbac/users/${userId}/groups`)

export const saveUserGroups = (userId, groupIds) =>
  request(`/rbac/users/${userId}/groups`, { method: 'PUT', body: { groupIds } })
