/**
 * LocalStorage 读写封装
 */

/**
 * 读取数据
 * @param {string} key
 * @param {*} defaultValue
 * @returns {*}
 */
export function getStorage(key, defaultValue = null) {
  try {
    const value = uni.getStorageSync(key)
    return value !== '' && value !== undefined ? value : defaultValue
  } catch (e) {
    console.error('getStorage error:', e)
    return defaultValue
  }
}

/**
 * 写入数据
 * @param {string} key
 * @param {*} value
 */
export function setStorage(key, value) {
  try {
    uni.setStorageSync(key, value)
    return true
  } catch (e) {
    console.error('setStorage error:', e)
    return false
  }
}

/**
 * 删除数据
 * @param {string} key
 */
export function removeStorage(key) {
  try {
    uni.removeStorageSync(key)
  } catch (e) {
    console.error('removeStorage error:', e)
  }
}

/**
 * 清空所有缓存
 */
export function clearStorage() {
  try {
    uni.clearStorageSync()
  } catch (e) {
    console.error('clearStorage error:', e)
  }
}
