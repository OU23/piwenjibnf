/**
 * HTTP 请求封装（暂不对接后端，预留接口）
 */

const BASE_URL = '' // 正式环境填入后端地址

/**
 * 通用请求方法
 * @param {Object} options
 * @returns {Promise}
 */
function request(options) {
  const { url, method = 'GET', data = {}, header = {} } = options

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...header
      },
      success(res) {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else {
          reject(new Error(`请求失败: ${res.statusCode}`))
        }
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

export const get = (url, data = {}) => request({ url, method: 'GET', data })
export const post = (url, data = {}) => request({ url, method: 'POST', data })
