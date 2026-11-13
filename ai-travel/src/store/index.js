import { defineStore } from 'pinia'

// ─── 模拟头像（统一使用，避免各页不一致）──────────────────────────
const AVATARS = [
  'https://api.dicebear.com/7.x/adventurer/svg?seed=1',
  'https://api.dicebear.com/7.x/adventurer/svg?seed=2',
  'https://api.dicebear.com/7.x/adventurer/svg?seed=3',
  'https://api.dicebear.com/7.x/adventurer/svg?seed=4',
  'https://api.dicebear.com/7.x/adventurer/svg?seed=5',
]

const PLACEHOLDERS = [
  'https://placehold.co/400x300/1890ff/ffffff?text=%E9%95%BF%E5%9F%8E',
  'https://placehold.co/400x300/52c41a/ffffff?text=%E4%BA%BA%E6%96%87%E6%95%85%E4%BA%8B',
  'https://placehold.co/400x300/faad14/ffffff?text=%E7%89%B9%E8%89%B2%E6%96%87%E6%97%85',
  'https://placehold.co/400x300/722ed1/ffffff?text=%E5%8F%A4%E9%95%87%E9%A3%8E%E6%83%85',
  'https://placehold.co/400x300/eb2f96/ffffff?text=%E6%B0%91%E4%BF%97%E9%A3%8E%E6%83%85',
  'https://placehold.co/400x300/13c2c2/ffffff?text=%E7%94%9F%E6%80%81%E6%97%85%E6%B8%B8',
  'https://placehold.co/400x300/f5222d/ffffff?text=%E5%90%8D%E8%83%9C%E5%8F%A4%E8%BF%B9',
  'https://placehold.co/400x300/2f54eb/ffffff?text=%E7%BA%A2%E8%89%B2%E6%97%85%E6%B8%B8',
]

// ─── 作品数据源（全局唯一，所有页面共用）──────────────────────────
export const WORKS_DATA = [
  {
    id: 1,
    title: '长城',
    cover: PLACEHOLDERS[0],
    author: '张明远',
    authorAvatar: AVATARS[0],
    category: 'human-story',
    desc: '用AI还原万里长城的雄伟壮丽，穿越千年时光，感受中华文明的厚重底蕴。画面中烽火台错落有致，云海翻涌，尽显华夏脊梁。',
  },
  {
    id: 2,
    title: '人文故事',
    cover: PLACEHOLDERS[1],
    author: '李思涵',
    authorAvatar: AVATARS[1],
    category: 'human-story',
    desc: '讲述西南山区一个古老村落的非遗传承故事，通过AI插画重现传统手工艺人的日常生活，让即将消失的技艺重新被看见。',
  },
  {
    id: 3,
    title: '特色文旅',
    cover: PLACEHOLDERS[2],
    author: '王启航',
    authorAvatar: AVATARS[2],
    category: 'travel',
    desc: '融合AI生成艺术与真实风景摄影，呈现江南水乡的独特韵味。小桥流水、白墙黛瓦在光影中交织出一幅现代版的千里江山图。',
  },
  {
    id: 4,
    title: '古镇风情',
    cover: PLACEHOLDERS[3],
    author: '陈晓燕',
    authorAvatar: AVATARS[3],
    category: 'human-story',
    desc: '以乌镇为灵感，运用AI技术将水乡古镇的四季变换浓缩于一幅画卷，晨雾中的乌篷船与夕阳下的石拱桥相映成趣。',
  },
  {
    id: 5,
    title: '民族风情',
    cover: PLACEHOLDERS[4],
    author: '杨秀兰',
    authorAvatar: AVATARS[4],
    category: 'human-story',
    desc: '记录少数民族村寨的节日庆典，AI重新演绎火把节、泼水节的盛况，色彩浓烈的服饰与欢腾的人群跃然屏上。',
  },
  {
    id: 6,
    title: '生态之旅',
    cover: PLACEHOLDERS[5],
    author: '赵文博',
    authorAvatar: AVATARS[0],
    category: 'travel',
    desc: '带领观众走进张家界奇峰秀水的秘境，AI enhances 了原片的视觉冲击力，让每一帧都成为壁纸级风景。',
  },
  {
    id: 7,
    title: '名胜古迹',
    cover: PLACEHOLDERS[6],
    author: '刘佳音',
    authorAvatar: AVATARS[1],
    category: 'travel',
    desc: '聚焦故宫、兵马俑等世界级文化遗产，AI 复原了历史场景的原本面貌，让古迹在数字世界中重获新生。',
  },
  {
    id: 8,
    title: '红色之路',
    cover: PLACEHOLDERS[7],
    author: '孙志远',
    authorAvatar: AVATARS[2],
    category: 'travel',
    desc: '重温革命圣地井冈山，AI 艺术化再现了红军长征的历史场景，红色文化与数字创意完美融合。',
  },
  {
    id: 9,
    title: '敦煌飞天',
    cover: 'https://placehold.co/400x300/fa8c16/ffffff?text=%E6%97%A7%E9%87%91%E9%B9%8A',
    author: '周婉清',
    authorAvatar: AVATARS[3],
    category: 'human-story',
    desc: '以敦煌莫高窟壁画为原型，AI 赋予飞天形象动态生命力，飘带灵动，色彩绚烂，千年艺术在数字空间焕新。',
  },
  {
    id: 10,
    title: '川西秘境',
    cover: 'https://placehold.co/400x300/36cfc9/ffffff?text=%E5%B7%9D%E8%A5%BF',
    author: '吴浩然',
    authorAvatar: AVATARS[4],
    category: 'travel',
    desc: '探索川西高原的神秘风光，雪山、草甸、海子构成绝美画卷，AI 增强让画面色彩更加饱和震撼。',
  },
]

// ─── 活动数据源 ───────────────────────────────────────────────────────
export const ACTIVITIES_DATA = [
  {
    id: 1,
    title: '2024 AI文旅创意大赛',
    sponsor: '携程',
    logo: '🏨',
    status: '进行中',
    bgColor: '#e6f7ff',
    bannerTitle: '2024 AI文旅创意大赛',
  },
  {
    id: 2,
    title: '人文故事摄影展',
    sponsor: '马蜂窝',
    logo: '📍',
    status: '未开始',
    bgColor: '#fff7e6',
    bannerTitle: '',
  },
  {
    id: 3,
    title: '特色文旅路线评选',
    sponsor: '同程旅行',
    logo: '🚄',
    status: '已结束',
    bgColor: '#f5f5f5',
    bannerTitle: '',
  },
]

// ─── 用户信息（与详情页作者头像保持一致）──────────────────────────────
const USER_INFO = {
  nickname: 'AI创作者小王',
  avatar: AVATARS[0],
  level: 'Lv.3 创作达人',
  workCount: 6,
  followCount: 12,
}

// ─── Pinia Store ────────────────────────────────────────────────────────
export const useAppStore = defineStore('app', {
  state: () => ({
    works: WORKS_DATA,
    activities: ACTIVITIES_DATA,
    user: { ...USER_INFO },
    // 收藏的作品 id 集合
    favorites: [],
    // 随机统计数（每次进入个人中心重新生成）
    fanCount: 0,
    likeCount: 0,
  }),

  actions: {
    /** 按分类筛选作品 */
    getByCategory(category) {
      if (category === 'all') return this.works
      return this.works.filter((w) => w.category === category)
    },

    /** 按 id 获取单条作品 */
    getById(id) {
      return this.works.find((w) => w.id === Number(id)) || null
    },

    /** 切换收藏状态 */
    toggleFavorite(workId) {
      const idx = this.favorites.indexOf(workId)
      if (idx > -1) {
        this.favorites.splice(idx, 1)
        uni.setStorageSync('favorites', this.favorites)
      } else {
        this.favorites.push(workId)
        uni.setStorageSync('favorites', this.favorites)
      }
    },

    /** 加载已保存的收藏 */
    loadFavorites() {
      try {
        const saved = uni.getStorageSync('favorites')
        if (saved && Array.isArray(saved)) {
          this.favorites = saved
        }
      } catch (e) {
        console.error('loadFavorites error:', e)
      }
    },

    /** 生成随机统计数 */
    generateRandomStats() {
      this.fanCount = Math.floor(Math.random() * 900) + 100  // 100~999
      this.likeCount = Math.floor(Math.random() * 1800) + 200 // 200~2000
    },
  },
})
