import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    permissions: JSON.parse(localStorage.getItem('permissions') || '[]'),
    menus: JSON.parse(localStorage.getItem('menus') || '[]')
  }),
  getters: {
    hasPerm: (state) => (perm) => state.permissions.includes(perm)
  },
  actions: {
    setLoginData(data) {
      this.token = data.token
      this.userInfo = {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        avatar: data.avatar,
        department: data.department
      }
      this.permissions = data.permissions || []
      this.menus = data.menus || []
      localStorage.setItem('token', this.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
      localStorage.setItem('menus', JSON.stringify(this.menus))
    },
    logout() {
      this.token = ''
      this.userInfo = {}
      this.permissions = []
      this.menus = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('permissions')
      localStorage.removeItem('menus')
    }
  }
})
