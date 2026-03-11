const ROLE_HOME = {
  STUDENT: '/student/home',
  COUNSELOR: '/teacher/home',
  ADMIN: '/admin/home'
}

export function getToken() {
  return localStorage.getItem('token') || ''
}

export function getRole() {
  return localStorage.getItem('role') || ''
}

export function getRealName() {
  return localStorage.getItem('realName') || ''
}

export function getUserId() {
  return localStorage.getItem('userId') || ''
}

export function isLoggedIn() {
  return Boolean(getToken())
}

export function getHomeByRole(role = getRole()) {
  return ROLE_HOME[role] || '/login'
}

export function getProfilePathByRole(role = getRole()) {
  if (role === 'STUDENT') return '/student/me/profile'
  if (role === 'COUNSELOR') return '/teacher/me/profile'
  if (role === 'ADMIN') return '/admin/me/profile'
  return '/login'
}

export function canAccessRoute(roles, role = getRole()) {
  if (!roles || roles.length === 0) {
    return true
  }
  return roles.includes(role)
}

export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('role')
  localStorage.removeItem('userId')
  localStorage.removeItem('realName')
}

export function roleLabel(role = getRole()) {
  if (role === 'STUDENT') return '学生'
  if (role === 'COUNSELOR') return '辅导员'
  if (role === 'ADMIN') return '管理员'
  return '访客'
}
