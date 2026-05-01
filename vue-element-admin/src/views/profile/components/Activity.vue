<template>
  <div class="user-activity">
    <div class="toolbar">
      <el-button size="mini" class="admin-ghost-btn" :loading="loading" @click="loadActivities">Làm mới</el-button>
    </div>
    <div v-if="!loading && !activities.length" class="empty-state">Chưa có hoạt động</div>
    <div v-for="item in activities" :key="item.id" class="post">
      <div class="user-block">
        <span class="username text-muted">{{ item.title || 'Thông báo' }}</span>
        <span class="description">{{ formatDate(item.createdAt) }}</span>
      </div>
      <p>{{ item.content || '-' }}</p>
      <div class="post-actions">
        <el-tag size="mini" :type="item.isRead ? 'info' : 'success'">
          {{ item.isRead ? 'Đã đọc' : 'Chưa đọc' }}
        </el-tag>
        <el-button
          v-if="!item.isRead"
          size="mini"
          type="text"
          :loading="markingId === item.id"
          @click="markRead(item)"
        >
          Đánh dấu đã đọc
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getMyNotifications, markNotificationAsRead } from '@/api/user'

export default {
  data() {
    return {
      loading: false,
      markingId: null,
      activities: []
    }
  },
  created() {
    this.loadActivities()
  },
  methods: {
    async loadActivities() {
      this.loading = true
      try {
        const data = await getMyNotifications()
        this.activities = Array.isArray(data) ? data : []
      } catch (error) {
        this.$message.error('Không tải được hoạt động')
      } finally {
        this.loading = false
      }
    },
    async markRead(item) {
      if (!item || !item.id) return
      const targetId = item.id
      this.markingId = targetId
      try {
        await markNotificationAsRead(targetId)
        this.activities = (this.activities || []).map(activity => {
          if (activity && activity.id === targetId) {
            return { ...activity, isRead: true }
          }
          return activity
        })
      } catch (error) {
        this.$message.error('Không thể cập nhật trạng thái đã đọc')
      } finally {
        this.markingId = null
      }
    },
    formatDate(value) {
      if (!value) return 'Không có thời gian'
      const d = new Date(value)
      if (Number.isNaN(d.getTime())) return 'Không có thời gian'
      return d.toLocaleString('vi-VN')
    }
  }
}
</script>

<style lang="scss" scoped>
.user-activity {
  .empty-state {
    border: 1px dashed #d1d5db;
    color: #6b7280;
    text-align: center;
    padding: 20px;
    margin-bottom: 12px;
  }

  .toolbar {
    margin-bottom: 12px;
  }
  .user-block {
    .username,
    .description {
      display: block;
      padding: 2px 0;
    }

    .username {
      font-size: 16px;
      color: #000;
    }
  }

  .post {
    font-size: 14px;
    border-bottom: 1px solid #d2d6de;
    margin-bottom: 15px;
    padding-bottom: 15px;
    color: #666;

    .post-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}
</style>
